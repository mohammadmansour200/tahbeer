package com.tahbeer.app.home.domain.model

import com.tahbeer.app.R


data class WhisperModel(
    val type: String,
    val size: Long,
    val url: String,
    val isDownloaded: Boolean = false,
    val downloadingProgress: Float? = null,
    val enOnly: Boolean = false,
    val description: Int
)

object WhisperModelList {
    val models = listOf(
        WhisperModel(
            "small",
            43550795,
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en-q8_0.bin",
            enOnly = true,
            description = R.string.settings_model_small_desc
        ),
        WhisperModel(
            "medium",
            81768585,
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q8_0.bin",
            description = R.string.settings_model_medium_desc
        ),
        WhisperModel(
            "large",
            190085487,
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin",
            description = R.string.settings_model_large_desc
        ),
    )

    fun getModelsByLanguage(language: String): WhisperModel {
        return models.find { it.type == language } as WhisperModel
    }
}
