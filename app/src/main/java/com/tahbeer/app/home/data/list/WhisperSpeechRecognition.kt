package com.tahbeer.app.home.data.list

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.tahbeer.app.home.domain.list.SpeechRecognition
import com.whispercpp.whisper.SubtitleEntry
import com.whispercpp.whisper.WhisperContext
import com.whispercpp.whisper.WhisperContext.Companion.createContextFromFile
import com.whispercpp.whisper.WhisperProgressCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder


class WhisperSpeechRecognition(context: Context) : SpeechRecognition {
    private val appContext = context

    override suspend fun processFile(
        modelType: String,
        uri: Uri,
        lang: String,
        id: String,
        onProgress: (Float) -> Unit
    ): Result<List<SubtitleEntry>> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val context =
                    createContextFromFile("${appContext.filesDir.absolutePath}/${modelType}.bin")

                val results = processAudioFile(
                    context = context,
                    uri = uri,
                    lang = lang,
                    id = id
                ) { onProgress(it) }

                context.release()
                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun processAudioFile(
        context: WhisperContext,
        uri: Uri,
        lang: String,
        id: String,
        onProgress: (Float) -> Unit
    ): List<SubtitleEntry> {
        var result: List<SubtitleEntry> = emptyList()

        // Convert file to WAV
        val inputPath = FFmpegKitConfig.getSafParameterForRead(appContext, uri)
        val wavFile = File(appContext.cacheDir, "$id.wav")

        try {
            val session = FFmpegKit.execute(
                "-y -protocol_whitelist saf,file,crypto -i $inputPath -vn -ar 16000 ${wavFile.absolutePath}"
            )
            Log.d("LibWhisper", session.allLogsAsString)
            when {
                session.returnCode.isValueSuccess -> {
                    // Decode WAV file to float array
                    val wavFileData = decodeWavFile(wavFile)
                    result = context.transcribeData(
                        data = wavFileData,
                        language = lang,
                        onProgressCallback = object : WhisperProgressCallback {
                            override fun onProgress(progress: Int) {
                                onProgress(progress.toFloat() / 100)
                            }
                        })
                }

                session.returnCode.isValueError -> {
                    throw Exception("FFmpeg conversion failed: ${session.allLogsAsString}")
                }
            }
        } finally {
            wavFile.delete()
        }

        return result
    }

    private fun decodeWavFile(file: File): FloatArray {
        val baos = ByteArrayOutputStream()
        file.inputStream().use { it.copyTo(baos) }
        val buffer = ByteBuffer.wrap(baos.toByteArray())
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val channel = buffer.getShort(22).toInt()
        buffer.position(44)
        val shortBuffer = buffer.asShortBuffer()
        val shortArray = ShortArray(shortBuffer.limit())
        shortBuffer.get(shortArray)
        return FloatArray(shortArray.size / channel) { index ->
            when (channel) {
                1 -> (shortArray[index] / 32767.0f).coerceIn(-1f..1f)
                else -> ((shortArray[2 * index] + shortArray[2 * index + 1]) / 32767.0f / 2.0f).coerceIn(
                    -1f..1f
                )
            }
        }
    }
}