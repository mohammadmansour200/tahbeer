package com.tahbeer.app.home.presentation.settings

sealed interface SettingsAction {
    data class OnThemeChange(val theme: String) : SettingsAction
    data class OnDynamicColorChange(val enabled: Boolean) : SettingsAction
    data class OnWhisperModelDownload(val name: String) : SettingsAction
    data class OnWhisperModelDownloadCancel(val name: String) : SettingsAction
    data class OnWhisperModelDelete(val name: String) : SettingsAction
    data class OnMlkitModelDownload(val lang: String) : SettingsAction
    data class OnMlkitModelDelete(val lang: String) : SettingsAction
}