package com.tahbeer.app.home.presentation

import com.tahbeer.app.home.domain.model.ModelError

sealed interface SettingsEvent {
    object ModelDownloadSuccess : SettingsEvent
    object ModelDeleteSuccess : SettingsEvent
    data class ModelDownloadError(
        val error: ModelError
    ) : SettingsEvent
}