package com.tahbeer.app.home.presentation.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tahbeer.app.home.domain.settings.DownloadError
import com.tahbeer.app.home.domain.settings.ModelDownloadException
import com.tahbeer.app.home.domain.settings.ModelManager
import com.tahbeer.app.home.domain.settings.ThemePreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val themePreferences: ThemePreferences,
    private val whisperModelManager: ModelManager
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    whisperModels = whisperModelManager.loadAvailableModels()
                )
            }
            loadThemePrefs()
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnDynamicColorChange -> {
                themePreferences.setDynamicColorsEnabled(action.enabled)
                _state.update { it.copy(dynamicColorsEnabled = action.enabled) }
            }

            is SettingsAction.OnThemeChange -> {
                themePreferences.setTheme(action.theme)
                _state.update { it.copy(theme = action.theme) }
            }


            is SettingsAction.OnWhisperModelDownload -> {
                viewModelScope.launch {
                    val modelIndex =
                        _state.value.whisperModels.indexOfFirst { it.type == action.type }

                    try {
                        val updatedList = _state.value.whisperModels.toMutableList().apply {
                            this[modelIndex] =
                                this[modelIndex].copy(downloadingProgress = 0f)
                        }
                        _state.update {
                            it.copy(whisperModels = updatedList)
                        }

                        whisperModelManager.downloadModel(
                            type = action.type
                        ) { progress ->
                            // Update progress state
                            val updatedList = _state.value.whisperModels.toMutableList().apply {
                                this[modelIndex] =
                                    this[modelIndex].copy(downloadingProgress = progress)
                            }
                            _state.update {
                                it.copy(whisperModels = updatedList)
                            }
                        }.fold(
                            onSuccess = {
                                _events.send(SettingsEvent.ModelDownloadSuccess)
                            },
                            onFailure = { throwable ->
                                when (val modelDownloadException =
                                    throwable as? ModelDownloadException) {
                                    null -> {
                                        Log.e("Download", "Unknown error", throwable)
                                        _events.send(
                                            SettingsEvent.ModelDownloadError(
                                                DownloadError.DOWNLOAD_FAILED
                                            )
                                        )
                                    }

                                    else -> {
                                        Log.e("Download", modelDownloadException.message, throwable)
                                        _events.send(
                                            SettingsEvent.ModelDownloadError(
                                                modelDownloadException.downloadError
                                            )
                                        )
                                    }
                                }
                            })

                    } catch (e: Throwable) {
                        Log.e("Download", "Unknown error", e)
                        _events.send(SettingsEvent.ModelDownloadError(DownloadError.DOWNLOAD_FAILED))
                    } finally {
                        val updatedList = _state.value.whisperModels.toMutableList().apply {
                            this[modelIndex] =
                                this[modelIndex].copy(
                                    downloadingProgress = null
                                )
                        }
                        _state.update {
                            it.copy(whisperModels = updatedList)
                        }
                    }
                }
            }

            is SettingsAction.OnWhisperModelDelete -> {
                viewModelScope.launch {
                    val modelIndex =
                        _state.value.whisperModels.indexOfFirst { it.type == action.type }
                    val updatedList = _state.value.whisperModels.toMutableList().apply {
                        this[modelIndex] =
                            this[modelIndex].copy(
                                isDownloaded = false,
                            )
                    }
                    _state.update {
                        it.copy(whisperModels = updatedList)
                    }

                    whisperModelManager.deleteModel(action.type)
                    _events.send(SettingsEvent.ModelDeleteSuccess)
                }
            }
        }
    }

    private fun loadThemePrefs() {
        _state.update {
            it.copy(
                theme = themePreferences.getTheme() ?: "system",
                dynamicColorsEnabled = themePreferences.getDynamicColorsEnabled()
            )
        }
    }
}