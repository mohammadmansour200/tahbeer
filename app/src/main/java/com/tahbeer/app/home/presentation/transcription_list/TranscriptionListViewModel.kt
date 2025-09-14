package com.tahbeer.app.home.presentation.transcription_list

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tahbeer.app.core.domain.model.MediaType
import com.tahbeer.app.core.domain.model.TranscriptionItem
import com.tahbeer.app.core.domain.model.TranscriptionStatus
import com.tahbeer.app.core.domain.model.toDomainModel
import com.tahbeer.app.core.utils.fileName
import com.tahbeer.app.core.utils.isAudio
import com.tahbeer.app.core.utils.isVideo
import com.tahbeer.app.home.domain.list.SpeechRecognition
import com.tahbeer.app.home.utils.SubtitleManager.parseSubtitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class TranscriptionListViewModel(
    context: Context,
    private val speechRecognition: SpeechRecognition
) : ViewModel() {
    private val appContext = context

    private val _state = MutableStateFlow(TranscriptionListState())
    val state: StateFlow<TranscriptionListState> = _state

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
        }
    }

    private fun transcribeFile(modelType: String, lang: String, uri: Uri) {
        viewModelScope.launch {
            val type = appContext.contentResolver.getType(uri)

            val mediaType = when {
                type!!.isVideo() -> MediaType.VIDEO
                type.isAudio() -> MediaType.AUDIO
                else -> MediaType.SUBTITLE
            }

            val fileName = uri.fileName(appContext)
            val transcriptionItem = TranscriptionItem(
                id = UUID.randomUUID().toString(),
                lang = lang,
                title = fileName,
                status = TranscriptionStatus.PROCESSING,
                mediaUri = uri.toString(),
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
                                transcriptions = _state.value.transcriptions.toMutableList().apply {
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
                                transcriptions = _state.value.transcriptions.toMutableList().apply {
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
                            transcriptions = _state.value.transcriptions.toMutableList().apply {
                                this[transcriptionIndex] =
                                    this[transcriptionIndex].copy(
                                        status = TranscriptionStatus.ERROR_PROCESSING
                                    )
                            }
                        )
                    }
                }
            } else {
                speechRecognition.processFile(modelType, uri, lang) { progress ->
                    _state.update {
                        it.copy(
                            transcriptions = _state.value.transcriptions.toMutableList().apply {
                                this[transcriptionIndex] =
                                    this[transcriptionIndex].copy(
                                        progress = progress
                                    )
                            }
                        )
                    }
                }.fold(
                    onSuccess = { result ->
                        _state.update {
                            it.copy(
                                transcriptions = _state.value.transcriptions.toMutableList().apply {
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
    }

    private suspend fun cacheTranscription(transcriptionItem: TranscriptionItem) =
        withContext(Dispatchers.IO) {
            val transcriptionItemJson = Json.encodeToString(transcriptionItem)
            File(appContext.cacheDir, "${transcriptionItem.id}.json").writeText(
                transcriptionItemJson
            )
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