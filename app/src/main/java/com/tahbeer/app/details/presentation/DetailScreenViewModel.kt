package com.tahbeer.app.details.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.tahbeer.app.core.utils.extension
import com.tahbeer.app.details.domain.MediaPlaybackManager
import com.tahbeer.app.details.domain.MediaStoreManager
import com.tahbeer.app.details.domain.model.ExportError
import com.tahbeer.app.details.domain.model.ExportFormat
import com.tahbeer.app.details.presentation.components.SubtitleStyles
import com.tahbeer.app.details.utils.generateAssHeader
import com.tahbeer.app.details.utils.longToTimestamp
import com.tahbeer.app.details.utils.parseFfmpegError
import com.tahbeer.app.details.utils.progress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DetailScreenViewModel(
    context: Context,
    val player: Player,
    val mediaPlaybackManager: MediaPlaybackManager,
    private val mediaStoreManager: MediaStoreManager,
) : ViewModel() {
    private val appContext = context

    private val _state = MutableStateFlow(DetailScreenState())
    val state: StateFlow<DetailScreenState> = _state

    private val _events = Channel<DetailScreenEvent>()
    val events = _events.receiveAsFlow()

    private val deferred = CompletableDeferred<Boolean>()

    private val isAndroidQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun onCleared() {
        super.onCleared()
        _state.update { DetailScreenState() }
        mediaPlaybackManager.releaseMedia()
    }

    private suspend fun handleError(
        exportError: ExportError,
        e: Throwable?,
        uri: Uri?
    ) {
        uri?.let { mediaStoreManager.deleteMedia(uri) }
        Log.e("OperationScreenViewModel", "Error: ${exportError.name}", e)
        _state.update {
            it.copy(
                isOperating = false,
                error = exportError,
                detailedErrorMessage = e?.message
            )
        }
        _events.send(DetailScreenEvent.Error)
    }

    private suspend fun handleSuccess(uri: Uri) {
        mediaStoreManager.saveMedia(uri)
        _state.update {
            it.copy(
                isOperating = false,
                outputedFile = uri,
            )
        }
        _events.send(DetailScreenEvent.Success)
    }

    private fun safeExecute(operationBlock: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isOperating = true,
                    error = null,
                )
            }

            val isWriteGranted =
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) ==
                        PackageManager.PERMISSION_GRANTED

            if (!isWriteGranted && !isAndroidQOrLater) {
                _events.send(DetailScreenEvent.PermissionRequired(deferred))
                val granted = deferred.await()

                if (!granted) {
                    handleError(
                        exportError = ExportError.ERROR_WRITING_OUTPUT,
                        null,
                        null
                    )
                    return@launch
                }
            }

            try {
                operationBlock()
            } catch (e: Throwable) {
                handleError(
                    exportError = ExportError.ERROR_UNKNOWN,
                    e = e,
                    uri = null
                )
            }
        }
    }

    fun onAction(action: DetailScreenAction) {
        when (action) {
            is DetailScreenAction.OnLoadMedia -> {
                viewModelScope.launch {
                    mediaPlaybackManager.loadMedia(uri = action.uri)

                    launch { observePlaybackEvents() }
                }
            }

            is DetailScreenAction.OnSeek -> {
                _state.update { it.copy(mediaPosition = action.position) }
                player.seekTo(
                    action.position
                )
            }

            is DetailScreenAction.OnExport -> safeExecute {
                if (action.transcriptionItem.result == null) {
                    handleError(
                        ExportError.ERROR_NO_SUBTITLES,
                        e = null,
                        uri = null
                    )
                    return@safeExecute
                }

                mediaStoreManager.createMediaUri(
                    action.transcriptionItem.title,
                    action.exportFormat.name.lowercase()
                ).fold(onSuccess = { uri ->
                    if (uri == null) {
                        handleError(ExportError.ERROR_INVALID_FORMAT, null, null)
                        return@safeExecute
                    }

                    val subtitles = buildString {
                        val results = action.transcriptionItem.result
                        for (i in results.indices) {
                            val item = results[i]
                            val start = item.startTime
                            val end = item.endTime
                            val text = item.text

                            when (action.exportFormat) {
                                ExportFormat.TXT -> {
                                    append("$text\n\n")
                                }

                                ExportFormat.VTT -> {
                                    if (i == 0) append("WEBVTT\n\n")
                                    val timestamp = "${
                                        longToTimestamp(
                                            start,
                                            subtitleTimestamp = true
                                        )
                                    } --> ${longToTimestamp(end, subtitleTimestamp = true)}"
                                    append("$timestamp\n$text\n\n")
                                }

                                ExportFormat.SRT -> {
                                    val timestamp = "${
                                        longToTimestamp(
                                            start,
                                            subtitleTimestamp = true,
                                            comma = true
                                        )
                                    } --> ${
                                        longToTimestamp(
                                            end,
                                            subtitleTimestamp = true,
                                            comma = true
                                        )
                                    }"
                                    append("${i + 1}\n$timestamp\n$text\n\n")
                                }
                            }
                        }
                    }

                    mediaStoreManager.writeText(uri, subtitles)
                        .onFailure {
                            handleError(ExportError.ERROR_WRITING_OUTPUT, null, uri)
                            return@safeExecute
                        }
                    handleSuccess(uri)

                }, onFailure = {
                    handleError(ExportError.ERROR_WRITING_OUTPUT, null, null)
                    return@safeExecute
                })
            }

            is DetailScreenAction.OnBurnSubtitle -> safeExecute {
                if (action.transcriptionItem.result == null) {
                    handleError(
                        ExportError.ERROR_NO_SUBTITLES,
                        e = null,
                        uri = null
                    )
                    return@safeExecute
                }

                val regularFont = "IBMPlexSansArabic-Regular.ttf"
                val boldFont = "IBMPlexSansArabic-Bold.ttf"
                val fontDirectory = File(appContext.filesDir, "fonts").apply { mkdirs() }
                copyAssetsToInternalStorage(appContext, regularFont, fontDirectory)
                copyAssetsToInternalStorage(appContext, boldFont, fontDirectory)

                val subtitles = buildString {
                    val results = action.transcriptionItem.result
                    for (i in results.indices) {
                        val item = results[i]
                        val start = item.startTime
                        val end = item.endTime
                        val text = item.text

                        val timestamp = "${
                            longToTimestamp(
                                start,
                                subtitleTimestamp = true,
                                comma = true
                            )
                        } --> ${
                            longToTimestamp(
                                end,
                                subtitleTimestamp = true,
                                comma = true
                            )
                        }"
                        append("${i + 1}\n$timestamp\n$text\n\n")
                    }
                }

                val tempSrtSubtitleFile = File(appContext.cacheDir, "subtitle.srt")
                val tempAssSubtitleFile = File(appContext.cacheDir, "subtitle.ass")
                mediaStoreManager.writeText(tempSrtSubtitleFile.toUri(), subtitles)
                    .onFailure {
                        handleError(ExportError.ERROR_NO_SUBTITLES, null, null)
                        return@safeExecute
                    }

                val pickedUri = action.transcriptionItem.mediaUri.toUri()
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(appContext, pickedUri)
                try {
                    val session = FFmpegKit.execute(
                        "-y -i ${tempSrtSubtitleFile.absolutePath} ${tempAssSubtitleFile.absolutePath}"
                    )
                    Log.d("LibWhisper", session.allLogsAsString)
                    when {
                        session.returnCode.isValueSuccess -> {
                            val widthString =
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            val heightString =
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            val videoWidth = widthString?.toIntOrNull() ?: 1280
                            val videoHeight = heightString?.toIntOrNull() ?: 720

                            applyStylesToAssFile(
                                action.subtitleStyles, tempAssSubtitleFile, videoWidth, videoHeight
                            )
                            addFadeEffectToAssFile(tempAssSubtitleFile)
                            replaceDefaultStyleInAssFile(tempAssSubtitleFile)

                            val inputPath =
                                FFmpegKitConfig.getSafParameterForRead(
                                    appContext,
                                    pickedUri
                                )
                            FFmpegKitConfig.setFontDirectory(
                                appContext,
                                fontDirectory.absolutePath, emptyMap()
                            )

                            val durationMs =
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                    ?.toLongOrNull() ?: 0L
                            runFFmpeg(
                                command = "-i $inputPath -vf \"ass=${tempAssSubtitleFile.absolutePath}:fontsdir=${fontDirectory.absolutePath}\" -c:a copy",
                                outputExtension = pickedUri.extension(appContext) ?: "mp4",
                                outputTitle = action.transcriptionItem.title,
                                durationMs = durationMs,
                                onFinish = {
                                    tempSrtSubtitleFile.delete()
                                    tempAssSubtitleFile.delete()
                                }
                            )

                        }

                        session.returnCode.isValueError -> {
                            throw Exception("FFmpeg conversion failed: ${session.allLogsAsString}")
                        }
                    }
                } finally {
                    retriever.release()
                }
            }
        }
    }


    private suspend fun runFFmpeg(
        command: String,
        outputTitle: String,
        outputExtension: String,
        durationMs: Long,
        onFinish: () -> Unit
    ) = withContext(Dispatchers.IO) {
        mediaStoreManager.createMediaUri(
            outputTitle,
            outputExtension
        )
            .fold(onSuccess = { uri ->
                if (uri == null) {
                    handleError(ExportError.ERROR_INVALID_FORMAT, null, null)
                    return@withContext
                }

                val outputPath = if (isAndroidQOrLater) FFmpegKitConfig.getSafParameterForWrite(
                    appContext,
                    uri
                ) else uri.path

                FFmpegKit.executeAsync(
                    "-y -protocol_whitelist saf,file,crypto $command $outputPath",
                    { session ->
                        val returnCode = session.returnCode
                        val logs = session.allLogsAsString

                        viewModelScope.launch {
                            when {
                                returnCode.isValueSuccess -> {
                                    onFinish()
                                    handleSuccess(uri)
                                }

                                returnCode.isValueCancel -> {
                                    onFinish
                                    mediaStoreManager.deleteMedia(uri)
                                    _state.update {
                                        it.copy(
                                            isOperating = false,
                                        )
                                    }
                                }

                                returnCode.isValueError -> {
                                    onFinish()
                                    val parsedError = parseFfmpegError(logs)
                                    handleError(parsedError, null, uri)
                                }
                            }
                        }
                    },
                    { log ->
                        Log.d("ffmpeg-kit", log?.message.toString())
                        val progress = log?.progress(durationMs.div(1000))
                        if (progress != null) _state.update { it.copy(progress = progress) }
                    },
                    null
                )
            }, onFailure = {
                onFinish()
                handleError(ExportError.ERROR_WRITING_OUTPUT, null, null)
                return@withContext
            })
    }

    private suspend fun copyAssetsToInternalStorage(
        context: Context,
        assetFileName: String,
        targetDir: File
    ): File = withContext(Dispatchers.IO) {
        val targetFile = File(targetDir, assetFileName)
        if (targetFile.exists()) return@withContext targetFile

        context.assets.open(assetFileName).use { inputStream ->
            FileOutputStream(targetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return@withContext targetFile
    }

    private suspend fun applyStylesToAssFile(
        styles: SubtitleStyles,
        tempAssSubtitleFile: File,
        videoWidth: Int,
        videoHeight: Int
    ) = withContext(Dispatchers.IO) {
        val customFullHeader = generateAssHeader(
            styles,
            videoWidth,
            videoHeight
        )

        val assContent = tempAssSubtitleFile.readText()
        val eventsRegex = Regex("(\\[Events\\][\\s\\S]*)", setOf(RegexOption.MULTILINE))
        val eventsMatch = eventsRegex.find(assContent)

        if (eventsMatch == null) {
            handleError(ExportError.ERROR_READING_INPUT, null, null)
            return@withContext
        }

        val eventsSectionAndDialogues = eventsMatch.groupValues[1]
        val finalAssContent = "$customFullHeader\n\n$eventsSectionAndDialogues"

        tempAssSubtitleFile.writeText(finalAssContent)
    }

    private suspend fun addFadeEffectToAssFile(
        tempAssSubtitleFile: File,
    ) = withContext(Dispatchers.IO) {
        val assContent = tempAssSubtitleFile.readText()

        val dialogueRegex =
            Regex("^(Dialogue:(?:\\s*[^,]*?,){9})(.+)", setOf(RegexOption.MULTILINE))

        val fadeTag = "{\\\\fad(300,300)}"
        val newContent = assContent.replace(
            dialogueRegex,
            "$1$fadeTag$2"
        )

        tempAssSubtitleFile.writeText(newContent)
    }

    private suspend fun replaceDefaultStyleInAssFile(
        tempAssSubtitleFile: File,
        newStyle: String = "CustomStyle"
    ) = withContext(Dispatchers.IO) {
        val assContent = tempAssSubtitleFile.readText()

        val styleRegex = Regex("""^(Dialogue:(?:[^,]*?,){3})[^,]+(,.*)$""", RegexOption.MULTILINE)

        val newContent = assContent.replace(styleRegex, "$1$newStyle$2")

        tempAssSubtitleFile.writeText(newContent)
    }


    private suspend fun observePlaybackEvents() {
        mediaPlaybackManager.events.collectLatest {
            when (it) {
                is MediaPlaybackManager.Event.PositionChanged -> {
                    updatePlaybackPosition(it.position)
                }

                MediaPlaybackManager.Event.MediaError -> {
                    _state.update { state -> state.copy(mediaStatus = MediaStatus.ERROR) }
                }

                MediaPlaybackManager.Event.MediaReady -> {
                    _state.update { state -> state.copy(mediaStatus = MediaStatus.READY) }
                }
            }
        }
    }

    private fun updatePlaybackPosition(position: Long) {
        _state.update { it.copy(mediaPosition = position) }
    }
}