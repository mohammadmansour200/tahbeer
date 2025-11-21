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
    data class OnSubtitleEntryEdit(
        val transcriptionId: String,
        val editedResults: List<SubtitleEntry>
    ) :
        TranscriptionListAction

    data class OnSubtitleEntrySplit(
        val transcriptionId: String,
        val index: Int
    ) :
        TranscriptionListAction

    data class OnSubtitleEntryDelete(
        val transcriptionId: String,
        val index: Int
    ) :
        TranscriptionListAction

    data class OnLinkVideoWithTranscript(
        val transcriptionId: String,
        val uri: Uri,
    ) :
        TranscriptionListAction

    data class OnTranscriptTranslate(
        val transcriptionId: String,
        val outputLang: String,
    ) :
        TranscriptionListAction

    object OnCancelTranslation : TranscriptionListAction
}