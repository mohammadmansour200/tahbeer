package com.tahbeer.app.home.presentation

import androidx.lifecycle.ViewModel
import com.tahbeer.app.home.domain.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ThemeViewModel(
    private val themePreferences: ThemePreferences
) : ViewModel() {
    private val _state = MutableStateFlow(ThemeState())
    val state: StateFlow<ThemeState> = _state

    init {
        loadThemePrefs()
    }

    fun onAction(action: ThemeAction) {
        when (action) {
            is ThemeAction.OnDynamicColorChange -> {
                themePreferences.setDynamicColorsEnabled(action.enabled)
                _state.update { it.copy(dynamicColorsEnabled = action.enabled) }
            }

            is ThemeAction.OnThemeChange -> {
                themePreferences.setTheme(action.theme)
                _state.update { it.copy(theme = action.theme) }
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