package com.tahbeer.app.home.presentation.settings

import android.util.Log
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tahbeer.app.home.domain.model.VoskModelList.models
import com.tahbeer.app.home.domain.settings.ModelError
import com.tahbeer.app.home.domain.settings.ModelException
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
    private val voskModelManager: ModelManager
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    voskModels = voskModelManager.loadAvailableModels()
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

            is SettingsAction.OnVoskModelsFilter -> {
                if (action.query.isBlank()) {
                    _state.update { it.copy(voskModels = models) }
                } else {
                    _state.update {
                        it.copy(voskModels = models.filter { model ->
                            val locale = Locale(model.lang).platformLocale
                            val name =
                                "${locale.displayLanguage} ${locale.displayCountry} ${locale.language} ${locale.country}"
                            name.contains(action.query, ignoreCase = true)
                        })
                    }
                }
            }

            is SettingsAction.OnVoskModelDownload -> {
                viewModelScope.launch {
                    val modelIndex =
                        _state.value.voskModels.indexOfFirst { it.lang == action.lang }
                    try {
                        val updatedList = _state.value.voskModels.toMutableList().apply {
                            this[modelIndex] =
                                this[modelIndex].copy(downloadingProgress = 0f)
                        }
                        _state.update {
                            it.copy(voskModels = updatedList)
                        }
                        voskModelManager.downloadModel(
                            lang = action.lang
                        ) { progress ->
                            val updatedList = _state.value.voskModels.toMutableList().apply {
                                this[modelIndex] =
                                    this[modelIndex].copy(downloadingProgress = progress)
                            }
                            _state.update {
                                it.copy(voskModels = updatedList)
                            }
                        }.fold(
                            onSuccess = {
                                val updatedList = _state.value.voskModels.toMutableList().apply {
                                    this[modelIndex] =
                                        this[modelIndex].copy(
                                            isDownloaded = true,
                                            downloadingProgress = null
                                        )
                                }
                                _state.update {
                                    it.copy(voskModels = updatedList)
                                }
                                _events.send(SettingsEvent.ModelDownloadSuccess)
                            },
                            onFailure = { throwable ->
                                val updatedList = _state.value.voskModels.toMutableList().apply {
                                    this[modelIndex] =
                                        this[modelIndex].copy(
                                            isDownloaded = false,
                                            downloadingProgress = null
                                        )
                                }
                                _state.update {
                                    it.copy(voskModels = updatedList)
                                }
                                when (val modelException = throwable as? ModelException) {
                                    null -> {
                                        Log.e("Download", "Unknown error", throwable)
                                        _events.send(SettingsEvent.ModelDownloadError(ModelError.UNKNOWN_ERROR))
                                    }

                                    else -> {
                                        Log.e("Download", modelException.message, throwable)
                                        _events.send(
                                            SettingsEvent.ModelDownloadError(
                                                modelException.modelError
                                            )
                                        )
                                    }
                                }
                            })

                    } catch (e: Throwable) {
                        val updatedList = _state.value.voskModels.toMutableList().apply {
                            this[modelIndex] =
                                this[modelIndex].copy(
                                    isDownloaded = true,
                                    downloadingProgress = null
                                )
                        }
                        _state.update {
                            it.copy(voskModels = updatedList)
                        }
                        Log.e("Download", "Unknown error", e)
                        _events.send(SettingsEvent.ModelDownloadError(ModelError.UNKNOWN_ERROR))
                    }
                }
            }

            is SettingsAction.OnVoskModelDelete -> {
                viewModelScope.launch {
                    val modelIndex =
                        _state.value.voskModels.indexOfFirst { it.lang == action.lang }
                    val updatedList = _state.value.voskModels.toMutableList().apply {
                        this[modelIndex] =
                            this[modelIndex].copy(
                                isDownloaded = false,
                            )
                    }
                    _state.update {
                        it.copy(voskModels = updatedList)
                    }

                    voskModelManager.deleteModel(action.lang)
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