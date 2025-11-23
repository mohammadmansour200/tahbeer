package com.tahbeer.app.home.data.settings

import android.content.Context
import com.tahbeer.app.home.domain.model.WhisperModel
import com.tahbeer.app.home.domain.model.WhisperModelList
import com.tahbeer.app.home.domain.settings.DownloadError
import com.tahbeer.app.home.domain.settings.ModelDownloadException
import com.tahbeer.app.home.domain.settings.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class WhisperModelManager(context: Context) : ModelManager<WhisperModel> {
    private val appContext = context
    private val modelDir = appContext.filesDir

    override suspend fun loadAvailableModels(): List<WhisperModel> {
        // Models are present in filesDir originally
        val filesDirContent = modelDir.listFiles().map {
            it.name
        }
        val whisperModels = WhisperModelList.models.toMutableList().map {
            if (filesDirContent.contains("${it.name}.bin"))
                it.copy(isDownloaded = true)
            else it
        }

        return whisperModels
    }


    override suspend fun deleteModel(name: String) {
        val path =
            "${appContext.filesDir}/${name}.bin"
        val model = File(path)
        model.delete()
    }

    override suspend fun downloadModel(
        name: String,
        onProgress: (progress: Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = WhisperModelList.getModelsByLanguage(name)
            val ggmlFile = File(modelDir, "${model.name}.bin")

            // Check available space
            if (!hasEnoughSpace(model.size)) {
                deleteModel(name)
                return@withContext Result.failure(ModelDownloadException(DownloadError.INSUFFICIENT_SPACE))
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(model.url).build()

            client.newCall(request).execute().use { response ->
                when {
                    !response.isSuccessful -> {
                        deleteModel(name)
                        val error = when (response.code) {
                            in 400..599 -> DownloadError.DOWNLOAD_FAILED
                            else -> DownloadError.NETWORK_ERROR
                        }
                        return@withContext Result.failure(ModelDownloadException(error))
                    }
                }

                val totalBytes = model.size
                response.body.let { body ->
                    try {
                        // Write body's bytestream to the ggml model
                        ggmlFile.outputStream().use { fileOut ->
                            val buffer = ByteArray(8192)
                            var bytesDownloaded = 0L

                            body.byteStream().use { input ->
                                var bytesRead = input.read(buffer)
                                while (bytesRead != -1) {
                                    if (!coroutineContext.isActive) {
                                        fileOut.close()
                                        input.close()
                                        deleteModel(name)
                                        throw CancellationException()
                                    }

                                    fileOut.write(buffer, 0, bytesRead)
                                    bytesDownloaded += bytesRead
                                    onProgress((bytesDownloaded.toDouble() / totalBytes).toFloat())
                                    bytesRead = input.read(buffer)
                                }
                            }
                        }
                    } catch (e: IOException) {
                        deleteModel(name)
                        return@withContext Result.failure(
                            ModelDownloadException(
                                DownloadError.DOWNLOAD_FAILED,
                                e
                            )
                        )
                    }
                }

                Result.success(File(modelDir, model.name).absolutePath)
            }
        } catch (e: okio.IOException) {
            deleteModel(name)
            Result.failure(ModelDownloadException(DownloadError.NETWORK_ERROR, e))
        } catch (e: Exception) {
            deleteModel(name)
            Result.failure(ModelDownloadException(DownloadError.DOWNLOAD_FAILED, e))
        }
    }

    private fun hasEnoughSpace(requiredBytes: Long): Boolean {
        val availableBytes = modelDir.freeSpace
        return availableBytes > requiredBytes * 1.2
    }
}