package com.tahbeer.app.home.data.settings

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.tahbeer.app.home.domain.model.MlkitModel
import com.tahbeer.app.home.domain.model.MlkitModelList
import com.tahbeer.app.home.domain.settings.ModelManager
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MlkitModelManager : ModelManager<MlkitModel> {

    private val modelManager = RemoteModelManager.getInstance()

    override suspend fun downloadModel(
        name: String,
        onProgress: (Float) -> Unit
    ): Result<String> = suspendCoroutine { continuation ->
        try {
            val options =
                TranslateRemoteModel.Builder(TranslateLanguage.fromLanguageTag(name) as String)
                    .build()

            val conditions = DownloadConditions.Builder()
                .build()

            modelManager.download(options, conditions)
                .addOnSuccessListener {
                    continuation.resume(Result.success(name))
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }

        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    override suspend fun deleteModel(name: String) {
        val model =
            TranslateRemoteModel.Builder(TranslateLanguage.fromLanguageTag(name) as String).build()
        modelManager.deleteDownloadedModel(model).await()
    }

    override suspend fun loadAvailableModels(): List<MlkitModel> {
        val downloadedModels = try {
            val models =
                modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
            models.mapNotNull { it.language }
        } catch (e: Exception) {
            emptyList()
        }

        val mlkitModels = MlkitModelList.models.map {
            if (downloadedModels.contains(it.lang)) it.copy(isDownloaded = true)
            else it
        }

        return mlkitModels
    }
}
