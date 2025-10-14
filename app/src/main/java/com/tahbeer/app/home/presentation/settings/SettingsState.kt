package com.tahbeer.app.home.presentation.settings

import androidx.compose.runtime.Immutable
import com.tahbeer.app.home.domain.model.MlkitModel
import com.tahbeer.app.home.domain.model.MlkitModelList
import com.tahbeer.app.home.domain.model.WhisperModel
import com.tahbeer.app.home.domain.model.WhisperModelList

@Immutable
data class SettingsState(
    val theme: String = "system",
    val dynamicColorsEnabled: Boolean = false,
    val whisperModels: List<WhisperModel> = WhisperModelList.models,
    val mlkitModels: List<MlkitModel> = MlkitModelList.models,
)