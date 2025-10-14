package com.tahbeer.app.details.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tahbeer.app.details.domain.MediaStoreManager
import com.tahbeer.app.details.domain.model.ExportError
import com.tahbeer.app.details.domain.model.ExportFormat
import com.tahbeer.app.details.utils.longToTimestamp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailScreenViewModel(
    context: Context,
    val mediaStoreManager: MediaStoreManager,
) : ViewModel() {
    private val appContext = context

    private val _state = MutableStateFlow(DetailScreenState())
    val state: StateFlow<DetailScreenState> = _state

    private val _events = Channel<DetailScreenEvent>()
    val events = _events.receiveAsFlow()

    private val deferred = CompletableDeferred<Boolean>()

    private val isAndroidQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

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
        }
    }
}