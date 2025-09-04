package com.tahbeer.app.home.presentation.settings

import androidx.compose.runtime.Immutable
import com.tahbeer.app.home.domain.model.VoskModel
import com.tahbeer.app.home.domain.model.VoskModelList

@Immutable
data class SettingsState(
    val theme: String = "system",
    val dynamicColorsEnabled: Boolean = false,
    val voskModels: List<VoskModel> = VoskModelList.models,
)