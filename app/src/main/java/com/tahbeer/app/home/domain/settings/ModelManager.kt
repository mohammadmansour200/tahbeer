package com.tahbeer.app.home.domain.settings

import com.tahbeer.app.home.domain.model.VoskModel

interface ModelManager {
    suspend fun downloadModel(
        lang: String,
        onProgress: (progress: Float) -> Unit
    ): Result<String>

    suspend fun deleteModel(
        lang: String,
    )

    suspend fun loadAvailableModels(): List<VoskModel>
}