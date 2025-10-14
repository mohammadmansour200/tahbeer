package com.tahbeer.app.home.presentation.transcription_list

import androidx.compose.runtime.Immutable
import com.tahbeer.app.core.domain.model.TranscriptionItem

@Immutable
data class TranscriptionListState(
    val transcriptions: List<TranscriptionItem> = emptyList(),
    val selectedTranscriptionId: String? = null,
    val isLoading: Boolean = false,
    val translationProgress: Float? = null,
)