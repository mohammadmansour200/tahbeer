package com.tahbeer.app.home.presentation

import androidx.compose.runtime.Immutable
import com.tahbeer.app.home.domain.model.VoskModel
import com.tahbeer.app.home.domain.model.VoskModelList.models

@Immutable
data class SettingsState(
    val theme: String = "system",
    val dynamicColorsEnabled: Boolean = false,
    val voskModels: List<VoskModel> = models,
)
