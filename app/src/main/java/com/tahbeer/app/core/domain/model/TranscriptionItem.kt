package com.tahbeer.app.core.domain.model

import kotlinx.serialization.Serializable

enum class TranscriptionStatus {
    PROCESSING, ERROR_PROCESSING, ERROR_EMPTY, ERROR_FORMAT, ERROR_MODEL, SUCCESS,
}


fun com.whispercpp.whisper.SubtitleEntry.toDomainModel(): SubtitleEntry {
    return SubtitleEntry(
        text = this.text.trim(),
        startTime = this.startTime * 10,
        endTime = this.endTime * 10
    )
}

@Serializable
data class SubtitleEntry(
    val startTime: Long,
    val endTime: Long,
    val text: String
)

@Serializable
data class TranscriptionItem(
    val id: String,
    val mediaUri: String? = null,
    val mediaType: MediaType,
    val lang: String,
    val title: String,
    val status: TranscriptionStatus = TranscriptionStatus.PROCESSING,
    val progress: Float? = null,
    val result: List<SubtitleEntry>? = null
)