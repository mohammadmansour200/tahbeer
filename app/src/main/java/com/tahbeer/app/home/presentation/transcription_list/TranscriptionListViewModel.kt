package com.tahbeer.app.home.presentation.transcription_list

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions.Builder
import com.tahbeer.app.core.domain.model.MediaType
import com.tahbeer.app.core.domain.model.SubtitleEntry
import com.tahbeer.app.core.domain.model.TranscriptionItem
import com.tahbeer.app.core.domain.model.TranscriptionStatus
import com.tahbeer.app.core.domain.model.toDomainModel
import com.tahbeer.app.core.utils.fileName
import com.tahbeer.app.core.utils.isAudio
import com.tahbeer.app.core.utils.isVideo
import com.tahbeer.app.home.domain.list.SpeechRecognition
import com.tahbeer.app.home.utils.SubtitleManager.parseSubtitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

class TranscriptionListViewModel(
    context: Context,
    private val speechRecognition: SpeechRecognition
) : ViewModel() {
    private val appContext = context

    private val _state = MutableStateFlow(TranscriptionListState())
    val state: StateFlow<TranscriptionListState> = _state

    private val _events = Channel<TranscriptionListEvent>()
    val events = _events.receiveAsFlow()

    private var translationJob: Job? = null
    private val transcriptionJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true)
            }
            val transcriptions = loadTranscriptions()
            _state.update {
                it.copy(transcriptions = transcriptions, isLoading = false)
            }
        }
    }

    fun onAction(action: TranscriptionListAction) {
        when (action) {
            is TranscriptionListAction.OnTranscriptFile -> {
                transcribeFile(action.modelType, action.lang, action.uri)
            }

            is TranscriptionListAction.OnTranscriptClick -> {
                _state.update { it.copy(selectedTranscriptionId = action.transcriptionId) }
            }

            is TranscriptionListAction.OnTranscriptDelete -> {
                transcriptionJobs[action.transcriptionId]?.cancel()
                transcriptionJobs.remove(action.transcriptionId)

                _state.update { it.copy(transcriptions = it.transcriptions.filterNot { transcription -> transcription.id == action.transcriptionId }) }

                viewModelScope.launch {
                    deleteTranscription(action.transcriptionId)
                }
            }

            is TranscriptionListAction.OnSubtitleEntryEdit -> {
                viewModelScope.launch {
                    val transcriptionIndex =
                        _state.value.transcriptions.indexOfFirst { it.id == action.transcriptionId }

                    _state.update {
                        it.copy(
                            transcriptions = it.transcriptions.toMutableList().apply {
                                this[transcriptionIndex] =
                                    this[transcriptionIndex].copy(
                                        result = action.editedResults
                                    )
                            }
                        )
                    }
                    cacheTranscription(_state.value.transcriptions[transcriptionIndex])
                }
            }

            is TranscriptionListAction.OnSubtitleEntrySplit -> {
                viewModelScope.launch {
                    val transcriptionIndex =
                        _state.value.transcriptions.indexOfFirst { it.id == action.transcriptionId }
                    val currentSubtitleEntry =
                        _state.value.transcriptions[transcriptionIndex].result?.get(action.index)!!

                    val splitPoint = findBestSubtitleEntrySplitPoint(currentSubtitleEntry.text)

                    if (splitPoint == -1) {
                        _events.send(TranscriptionListEvent.SplitError)
                        return@launch
                    }

                    val newTranscriptionResult =
                        _state.value.transcriptions[transcriptionIndex].result!!.toMutableList()

                    newTranscriptionResult.removeAt(action.index)

                    val duration = currentSubtitleEntry.endTime - currentSubtitleEntry.startTime
                    val splitTime = currentSubtitleEntry.startTime + (duration / 2)

                    val firstCue = SubtitleEntry(
                        startTime = currentSubtitleEntry.startTime,
                        endTime = splitTime,
                        text = currentSubtitleEntry.text.take(splitPoint).trim()
                    )
                    val secondCue = SubtitleEntry(
                        startTime = splitTime,
                        endTime = currentSubtitleEntry.endTime,
                        text = currentSubtitleEntry.text.substring(splitPoint).trim()
                    )

                    newTranscriptionResult.addAll(action.index, listOf(firstCue, secondCue))

                    _state.update {
                        it.copy(
                            transcriptions = it.transcriptions.toMutableList().apply {
                                this[transcriptionIndex] =
                                    this[transcriptionIndex].copy(
                                        result = newTranscriptionResult
                                    )
                            }
                        )
                    }
                    cacheTranscription(_state.value.transcriptions[transcriptionIndex])
                }
            }

            is TranscriptionListAction.OnSubtitleEntryDelete -> {
                viewModelScope.launch {
                    val transcriptionIndex =
                        _state.value.transcriptions.indexOfFirst { it.id == action.transcriptionId }

                    val newTranscriptionResult =
                        _state.value.transcriptions[transcriptionIndex].result!!.toMutableList()

                    newTranscriptionResult.removeAt(action.index)

                    _state.update {
                        it.copy(
                            transcriptions = it.transcriptions.toMutableList().apply {
                                this[transcriptionIndex] =
                                    this[transcriptionIndex].copy(
                                        result = newTranscriptionResult
                                    )
                            }
                        )
                    }
                    cacheTranscription(_state.value.transcriptions[transcriptionIndex])
                }
            }

            is TranscriptionListAction.OnTranscriptTranslate -> {
                val transcriptionIndex =
                    _state.value.transcriptions.indexOfFirst { it.id == action.transcriptionId }

                val transcriptionItem = _state.value.transcriptions[transcriptionIndex]
                val totalResultsSize = transcriptionItem.result!!.size

                val sourceLang = TranslateLanguage.fromLanguageTag(transcriptionItem.lang)!!
                val targetLang = TranslateLanguage.fromLanguageTag(action.outputLang)!!

                translationJob = viewModelScope.launch {
                    try {
                        val options = Builder()
                            .setSourceLanguage(sourceLang)
                            .setTargetLanguage(targetLang)
                            .build()

                        val translator = Translation.getClient(options)

                        val translatedSubtitles =
                            transcriptionItem.result.mapIndexed { index, entry ->
                                val translatedText = try {
                                    translator.translate(entry.text).await()
                                } catch (_: Exception) {
                                    entry.text
                                }

                                _state.update { it.copy(translationProgress = (index + 1).toFloat() / totalResultsSize) }

                                SubtitleEntry(
                                    startTime = entry.startTime,
                                    endTime = entry.endTime,
                                    text = translatedText
                                )
                            }

                        ensureActive()
                        _state.update {
                            it.copy(
                                transcriptions = it.transcriptions.toMutableList().apply {
                                    this[transcriptionIndex] =
                                        this[transcriptionIndex].copy(
                                            result = translatedSubtitles,
                                            lang = action.outputLang
                                        )
                                },

                                translationProgress = null
                            )
                        }

                        ensureActive()
                        cacheTranscription(_state.value.transcriptions[transcriptionIndex])

                    } catch (e: CancellationException) {
                        throw e
                    } finally {
                        if (!isActive) {
                            _state.update { it.copy(translationProgress = null) }
                        }
                    }
                }
            }

            TranscriptionListAction.OnCancelTranslation -> {
                _state.update {
                    it.copy(
                        translationProgress = null
                    )
                }

                translationJob?.cancel()
                translationJob = null
            }
        }
    }

    private fun transcribeFile(modelType: String, lang: String, uri: Uri) {
        val transcriptionId = UUID.randomUUID().toString()

        val job = viewModelScope.launch {
            val type = appContext.contentResolver.getType(uri)

            val mediaType = when {
                type!!.isVideo() -> MediaType.VIDEO
                type.isAudio() -> MediaType.AUDIO
                else -> MediaType.SUBTITLE
            }

            val fileName = uri.fileName(appContext)
            val transcriptionItem = TranscriptionItem(
                id = transcriptionId,
                lang = lang,
                title = fileName,
                status = TranscriptionStatus.PROCESSING,
                mediaUri = if (mediaType != MediaType.SUBTITLE) uri.toString() else null,
                mediaType = mediaType,
            )

            // Add to transcriptions list
            _state.update {
                it.copy(
                    transcriptions = it.transcriptions + transcriptionItem
                )
            }


            // Process the file
            val transcriptionIndex =
                _state.value.transcriptions.indexOfFirst { it.id == transcriptionItem.id }
            if (mediaType === MediaType.SUBTITLE) {
                try {
                    val result = parseSubtitle(appContext, uri)
                    if (result.isNullOrEmpty()) {
                        _state.update {
                            it.copy(
                                transcriptions = it.transcriptions.toMutableList().apply {
                                    this[transcriptionIndex] =
                                        this[transcriptionIndex].copy(
                                            status = TranscriptionStatus.ERROR_PROCESSING
                                        )
                                }
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                transcriptions = it.transcriptions.toMutableList().apply {
                                    this[transcriptionIndex] =
                                        this[transcriptionIndex].copy(
                                            status = TranscriptionStatus.SUCCESS,
                                            result = result
                                        )
                                }
                            )
                        }
                        cacheTranscription(_state.value.transcriptions[transcriptionIndex])
                    }
                } catch (error: Exception) {
                    Log.e("LibWhisper", error.message.toString(), error)
                    _state.update {
                        it.copy(
                            transcriptions = it.transcriptions.toMutableList().apply {
                                this[transcriptionIndex] =
                                    this[transcriptionIndex].copy(
                                        status = TranscriptionStatus.ERROR_PROCESSING
                                    )
                            }
                        )
                    }
                } finally {
                    transcriptionJobs.remove(transcriptionId)
                }
            } else {
                speechRecognition.processFile(modelType, uri, lang, transcriptionId) { progress ->
                    _state.update {
                        it.copy(
                            transcriptions = it.transcriptions.toMutableList().apply {
                                this[transcriptionIndex] =
                                    this[transcriptionIndex].copy(
                                        progress = progress
                                    )
                            }
                        )
                    }
                }.fold(
                    onSuccess = { result ->
                        transcriptionJobs.remove(transcriptionId)
                        _state.update {
                            it.copy(
                                transcriptions = it.transcriptions.toMutableList().apply {
                                    this[transcriptionIndex] =
                                        this[transcriptionIndex].copy(
                                            status = TranscriptionStatus.SUCCESS,
                                            result = result.map { result -> result.toDomainModel() }
                                        )
                                }
                            )
                        }
                        cacheTranscription(_state.value.transcriptions[transcriptionIndex])
                    },
                    onFailure = { error ->
                        transcriptionJobs.remove(transcriptionId)
                        Log.e("LibWhisper", error.message.toString(), error)
                        _state.update {
                            it.copy(
                                transcriptions = _state.value.transcriptions.toMutableList().apply {
                                    this[transcriptionIndex] =
                                        this[transcriptionIndex].copy(
                                            status = TranscriptionStatus.ERROR_PROCESSING
                                        )
                                }
                            )
                        }
                    }
                )
            }
        }

        transcriptionJobs[transcriptionId] = job
    }

    private fun findBestSubtitleEntrySplitPoint(text: String): Int {
        val midpoint = text.length / 2
        var minDistance = Int.MAX_VALUE
        var bestSplit = -1

        val punctuation = setOf(
            ',', '.', '!', '?', ';', ':', '-',
            '¿', '¡',
            '、', '，', '。', '？', '！', '：', '；',
            '،', '؟', '؛',
            '(', ')', '[', ']', '「', '」', '『', '』', '«', '»', '„', '"'
        )
        for (i in text.indices) {
            // Regular space or CJK full-width space
            if (text[i] == ' ' || text[i] == '\u3000' || text[i] in punctuation) {
                val distance = abs(i - midpoint)
                if (distance < minDistance) {
                    minDistance = distance
                    bestSplit = i
                }
            }
        }

        return bestSplit
    }

    private suspend fun cacheTranscription(transcriptionItem: TranscriptionItem) =
        withContext(Dispatchers.IO) {
            val transcriptionItemJson = Json.encodeToString(transcriptionItem)
            File(appContext.cacheDir, "${transcriptionItem.id}.json").writeText(
                transcriptionItemJson
            )
        }

    private suspend fun deleteTranscription(transcriptionItemId: String) =
        withContext(Dispatchers.IO) {
            File(appContext.cacheDir, "${transcriptionItemId}.json").delete()
            File(appContext.cacheDir, "${transcriptionItemId}.wav").delete()
        }

    private suspend fun loadTranscriptions(): List<TranscriptionItem> =
        withContext(Dispatchers.IO) {
            val cacheDirContents =
                appContext.cacheDir.listFiles().filter { it.extension.contains("json") }

            val transcriptions = mutableListOf<TranscriptionItem>()
            cacheDirContents.forEach { json ->
                transcriptions += Json.decodeFromString<TranscriptionItem>(json.readText())
            }

            return@withContext transcriptions
        }
}