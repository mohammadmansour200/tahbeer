package com.tahbeer.app.home.presentation.settings

import com.tahbeer.app.home.domain.settings.DownloadError

sealed interface SettingsEvent {
    object ModelDownloadSuccess : SettingsEvent
    object ModelDeleteSuccess : SettingsEvent
    data class ModelDownloadError(
        val error: DownloadError
    ) : SettingsEvent
}