package com.tahbeer.app.home.data.model

import android.content.Context
import com.tahbeer.app.home.domain.model.ModelError
import com.tahbeer.app.home.domain.model.ModelException
import com.tahbeer.app.home.domain.model.ModelManager
import com.tahbeer.app.home.domain.model.VoskModel
import com.tahbeer.app.home.domain.model.VoskModelList.getModelsByLanguage
import com.tahbeer.app.home.domain.model.VoskModelList.models
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class VoskModelManager(context: Context) : ModelManager {
    private val appContext = context
    private val modelDir = appContext.filesDir

    override suspend fun loadAvailableModels(): List<VoskModel> {
        val filesDirContent = modelDir.listFiles().map {
            it.name
        }
        val voskModels = models.toMutableList().map {
            if (filesDirContent.contains(it.lang))
                it.copy(isDownloaded = true)
            else it
        }

        return voskModels
    }


    override suspend fun deleteModel(lang: String) {
        val directoryPath =
            "${appContext.filesDir}/${lang}"
        val model = File(directoryPath)
        model.deleteRecursively()
    }

    override suspend fun downloadModel(
        lang: String,
        onProgress: (progress: Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = getModelsByLanguage(lang)[0]
            val zipFile = File(modelDir, "${model.lang}-model.zip")

            if (!hasEnoughSpace(model.size)) {
                return@withContext Result.failure(ModelException(ModelError.INSUFFICIENT_SPACE))
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(model.url).build()

            client.newCall(request).execute().use { response ->
                when {
                    !response.isSuccessful -> {
                        val error = when (response.code) {
                            in 400..599 -> ModelError.DOWNLOAD_FAILED
                            else -> ModelError.NETWORK_ERROR
                        }
                        return@withContext Result.failure(ModelException(error))
                    }
                }

                val totalBytes = model.size
                response.body.let { body ->
                    try {
                        zipFile.outputStream().use { fileOut ->
                            val buffer = ByteArray(8192)
                            var bytesDownloaded = 0L

                            body.byteStream().use { input ->
                                var bytesRead = input.read(buffer)
                                while (bytesRead != -1) {
                                    fileOut.write(buffer, 0, bytesRead)
                                    bytesDownloaded += bytesRead
                                    onProgress((bytesDownloaded.toDouble() / totalBytes).toFloat())
                                    bytesRead = input.read(buffer)
                                }
                            }
                        }
                    } catch (e: IOException) {
                        return@withContext Result.failure(
                            ModelException(
                                ModelError.INSUFFICIENT_SPACE,
                                e
                            )
                        )
                    }
                }

                try {
                    extractZip(zipFile, modelDir, lang)
                    zipFile.delete()
                } catch (e: ModelException) {
                    zipFile.delete()
                    return@withContext Result.failure(
                        ModelException(
                            ModelError.DOWNLOAD_FAILED,
                            e
                        )
                    )
                }

                Result.success(File(modelDir, model.lang).absolutePath)
            }
        } catch (e: okio.IOException) {
            Result.failure(ModelException(ModelError.NETWORK_ERROR, e))
        } catch (e: Exception) {
            Result.failure(ModelException(ModelError.UNKNOWN_ERROR, e))
        }
    }

    private fun extractZip(zipFile: File, destDir: File, customFolderName: String) {
        val targetDir = File(destDir, customFolderName)
        targetDir.mkdirs()

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryPath = entry.name
                val pathParts = entryPath.split("/")

                val newPath = if (pathParts.size > 1) {
                    pathParts.drop(1).joinToString("/")
                } else {
                    entryPath
                }

                val file = File(targetDir, newPath)

                if (!file.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    throw SecurityException("Zip entry is outside target directory")
                }

                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun hasEnoughSpace(requiredBytes: Long): Boolean {
        val availableBytes = modelDir.freeSpace
        return availableBytes > requiredBytes * 1.2
    }
}