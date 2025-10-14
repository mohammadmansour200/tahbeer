package com.tahbeer.app.home.domain.list

import android.net.Uri
import com.whispercpp.whisper.SubtitleEntry

interface SpeechRecognition {
    suspend fun processFile(
        modelType: String,
        uri: Uri,
        lang: String,
        id: String,
        onProgress: (Float) -> Unit
    ): Result<List<SubtitleEntry>>
}