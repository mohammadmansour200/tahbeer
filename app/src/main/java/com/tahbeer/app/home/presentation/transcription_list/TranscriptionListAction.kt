package com.tahbeer.app.home.presentation.transcription_list

import android.net.Uri

sealed interface TranscriptionListAction {
    data class OnTranscriptFile(
        val modelType: String,
        val lang: String,
        val uri: Uri,
    ) :
        TranscriptionListAction

    data class OnTranscriptClick(val transcriptionId: String?) : TranscriptionListAction
}