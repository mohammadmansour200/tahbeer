package com.tahbeer.app.home.presentation

sealed interface SettingsAction {
    data class OnThemeChange(val theme: String) : SettingsAction
    data class OnDynamicColorChange(val enabled: Boolean) : SettingsAction
    data class OnVoskModelsFilter(val query: String) : SettingsAction
    data class OnVoskModelDownload(val lang: String) : SettingsAction
    data class OnVoskModelDelete(val lang: String) : SettingsAction
}