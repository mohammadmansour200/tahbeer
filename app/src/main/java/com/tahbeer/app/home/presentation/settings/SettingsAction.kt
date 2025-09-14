package com.tahbeer.app.home.presentation.settings

sealed interface SettingsAction {
    data class OnThemeChange(val theme: String) : SettingsAction
    data class OnDynamicColorChange(val enabled: Boolean) : SettingsAction
    data class OnWhisperModelDownload(val type: String) : SettingsAction
    data class OnWhisperModelDelete(val type: String) : SettingsAction
}