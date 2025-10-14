package com.tahbeer.app.home.domain.settings

interface ModelManager<T> {
    suspend fun downloadModel(
        name: String,
        onProgress: (progress: Float) -> Unit
    ): Result<String>

    suspend fun deleteModel(
        name: String,
    )

    suspend fun loadAvailableModels(): List<T>
}