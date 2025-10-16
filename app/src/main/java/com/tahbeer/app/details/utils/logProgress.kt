package com.tahbeer.app.details.utils

import com.arthenica.ffmpegkit.Log

fun Log.progress(duration: Long): Float {
    val timeRegex = "time=(\\d{2}:\\d{2}:\\d{2}\\.\\d{2})".toRegex()
    val match = timeRegex.find(message)

    if (match == null || match.groupValues.size < 2) return 0f

    val currentEncodingProgress = match.groupValues[1].parseDuration()
    val currentEncodingProgressSeconds = currentEncodingProgress / 1000
    val progressPercentage = currentEncodingProgressSeconds / duration
    android.util.Log.d(
        "ffmpeg-kit",
        "Encoding Progress: $currentEncodingProgressSeconds, Duration: $duration "
    )
    return progressPercentage.toFloat()
}

private fun String.parseDuration(): Double {
    val parts = this.split(":", ".", limit = 4)
    // Check if milliseconds are present
    val hasMillis = this.contains(".")

    return when {
        parts.size == 2 && !hasMillis -> { // mm:ss
            val minutes = parts[0].toLong()
            val seconds = parts[1].toLong()
            (minutes * 60_000 + seconds * 1000).toDouble()
        }

        parts.size == 3 && hasMillis -> { // mm:ss.mmm
            val minutes = parts[0].toLong()
            val seconds = parts[1].toLong()
            val millis = parts[2].padEnd(3, '0').take(3).toLong()
            (minutes * 60_000 + seconds * 1000 + millis).toDouble()
        }

        parts.size == 3 && !hasMillis -> { // hh:mm:ss
            val hours = parts[0].toLong()
            val minutes = parts[1].toLong()
            val seconds = parts[2].toLong()
            (hours * 3600_000 + minutes * 60_000 + seconds * 1000).toDouble()
        }

        parts.size == 4 && hasMillis -> { // hh:mm:ss.mmm
            val hours = parts[0].toLong()
            val minutes = parts[1].toLong()
            val seconds = parts[2].toLong()
            val millis = parts[3].padEnd(3, '0').take(3).toLong()
            (hours * 3600_000 + minutes * 60_000 + seconds * 1000 + millis).toDouble()
        }

        else -> throw IllegalArgumentException("Invalid duration format: $this")
    }
}