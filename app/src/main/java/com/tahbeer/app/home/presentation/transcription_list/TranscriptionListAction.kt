package com.tahbeer.app.home.presentation.transcription_list

import android.net.Uri
import com.tahbeer.app.core.domain.model.SubtitleEntry

sealed interface TranscriptionListAction {
    data class OnTranscriptFile(
        val modelType: String,
        val lang: String,
        val uri: Uri,
    ) :
        TranscriptionListAction

    data class OnTranscriptClick(val transcriptionId: String?) : TranscriptionListAction
    data class OnTranscriptDelete(val transcriptionId: String) : TranscriptionListAction
    data class OnTranscriptEdit(
        val transcriptionId: String,
        val editedResults: List<SubtitleEntry>
    ) :
        TranscriptionListAction

    data class OnTranscriptTranslate(
        val transcriptionId: String,
        val outputLang: String,
    ) :
        TranscriptionListAction

    object OnCancelTranslation : TranscriptionListAction
}