package com.tahbeer.app.home.utils

import android.content.Context
import android.net.Uri
import com.tahbeer.app.core.domain.model.SubtitleEntry
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

class VttParser {
    companion object {
        private val TIMESTAMP_PATTERN = Pattern.compile("(\\d+)?(?::)?(\\d{2}):(\\d{2})\\.(\\d{3})")
    }

    fun parse(inputStream: InputStream): List<SubtitleEntry> {
        val cues = mutableListOf<SubtitleEntry>()
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

        // First line must be WEBVTT
        val header = reader.readLine()
        require(
            header != null && header.trim().contains("WEBVTT")
        ) { "Invalid VTT file: Missing WEBVTT header." }

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line.isNullOrBlank()) continue

            var timestampLine = line
            if (!timestampLine!!.contains("-->")) {
                timestampLine = reader.readLine()
                if (timestampLine == null || !timestampLine.contains("-->")) {
                    continue // Skip malformed entry
                }
            }

            val timestamps = timestampLine.split("-->").map { it.trim() }
            if (timestamps.size != 2) continue

            val startTimeMs = parseTimestampMs(timestamps[0])
            val endTimeMs = parseTimestampMs(timestamps[1])

            val textBuilder = StringBuilder()
            while (reader.readLine().also { line = it } != null && !line.isNullOrBlank()) {
                if (textBuilder.isNotEmpty()) {
                    textBuilder.append("\n")
                }
                textBuilder.append(line)
            }

            if (startTimeMs != -1L && endTimeMs != -1L) {
                cues.add(SubtitleEntry(startTimeMs, endTimeMs, textBuilder.toString()))
            }
        }
        return cues
    }

    // Parses a VTT timestamp string (e.g., "00:01:23.456") into milliseconds.
    private fun parseTimestampMs(timestamp: String): Long {
        val matcher =
            TIMESTAMP_PATTERN.matcher(timestamp.substringBefore(" ")) // Ignore styling info
        if (!matcher.matches()) return -1L

        val hours = matcher.group(1)?.toLongOrNull() ?: 0
        val minutes = matcher.group(2)!!.toLong()
        val seconds = matcher.group(3)!!.toLong()
        val millis = matcher.group(4)!!.toLong()

        return (hours * 3600000) + (minutes * 60000) + (seconds * 1000) + millis
    }
}

class SrtParser {
    companion object {
        private val TIMESTAMP_PATTERN = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})")
    }

    fun parse(inputStream: InputStream): List<SubtitleEntry> {
        val cues = mutableListOf<SubtitleEntry>()
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            // Skip empty lines and the numeric index line
            if (line.isNullOrBlank() || line?.toIntOrNull() != null) {
                continue
            }

            val timestampLine = line
            if (timestampLine == null || !timestampLine.contains("-->")) {
                continue // Malformed entry
            }

            val timestamps = timestampLine.split("-->").map { it.trim() }
            if (timestamps.size != 2) continue

            val startTimeMs = parseTimestampMs(timestamps[0])
            val endTimeMs = parseTimestampMs(timestamps[1])

            val textBuilder = StringBuilder()
            while (reader.readLine().also { line = it } != null && !line.isNullOrBlank()) {
                if (textBuilder.isNotEmpty()) {
                    textBuilder.append("\n")
                }
                textBuilder.append(line)
            }

            if (startTimeMs != -1L && endTimeMs != -1L) {
                cues.add(SubtitleEntry(startTimeMs, endTimeMs, textBuilder.toString()))
            }
        }

        return cues
    }

    // Parses an SRT timestamp string (e.g., "00:01:23,456") into milliseconds.
    private fun parseTimestampMs(timestamp: String): Long {
        val matcher = TIMESTAMP_PATTERN.matcher(timestamp)
        if (!matcher.matches()) return -1L

        val hours = matcher.group(1)!!.toLong()
        val minutes = matcher.group(2)!!.toLong()
        val seconds = matcher.group(3)!!.toLong()
        val millis = matcher.group(4)!!.toLong()

        return (hours * 3600000) + (minutes * 60000) + (seconds * 1000) + millis
    }
}

object SubtitleManager {
    fun parseSubtitle(context: Context, uri: Uri): List<SubtitleEntry>? {
        val type = context.contentResolver.getType(uri)
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                when {
                    type!!.contains("vtt") -> VttParser().parse(inputStream)
                    else -> SrtParser().parse(inputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

