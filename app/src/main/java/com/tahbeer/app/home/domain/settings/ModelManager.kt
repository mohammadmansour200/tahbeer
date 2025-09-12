package com.tahbeer.app.home.domain.settings

import com.tahbeer.app.home.domain.model.WhisperModel

interface ModelManager {
    suspend fun downloadModel(
        type: String,
        onProgress: (progress: Float) -> Unit
    ): Result<String>

    suspend fun deleteModel(
        type: String,
    )

    suspend fun loadAvailableModels(): List<WhisperModel>
}