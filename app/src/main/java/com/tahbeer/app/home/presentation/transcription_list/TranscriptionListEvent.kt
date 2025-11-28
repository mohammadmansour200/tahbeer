package com.tahbeer.app.home.presentation.transcription_list

sealed interface TranscriptionListEvent {
    object SplitError : TranscriptionListEvent
    object SrtFormatError : TranscriptionListEvent
    object SrtEmptyError : TranscriptionListEvent
}