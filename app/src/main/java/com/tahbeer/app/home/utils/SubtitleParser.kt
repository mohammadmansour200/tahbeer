package com.tahbeer.app.home.utils

import android.content.Context
import android.net.Uri
import com.tahbeer.app.core.domain.model.SubtitleEntry
import java.io.InputStream
import java.util.regex.Pattern

class Parser {
    companion object {
        // SRT: 00:00:00,000
        private val SRT_TIMESTAMP_PATTERN =
            Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})")

        // VTT: 00:00:00.000 or 00:00.000
        private val VTT_TIMESTAMP_PATTERN =
            Pattern.compile("(?:(\\d+):)?(\\d{1,2}):(\\d{2})\\.(\\d{3})")
    }

    fun parse(subtitleInputStream: InputStream): List<SubtitleEntry> {
        val reader = subtitleInputStream.bufferedReader()

        // 1. Read the full content once.
        val fileContent = reader.readText()

        if (!fileContent.contains("-->")) {
            throw Exception("This file isn't in SRT/VTT format")
        }

        // 2. determine format based on header or content
        val isVtt = fileContent.contains("VTT", ignoreCase = true)

        // 3. Create a new reader from the String we just read
        val lines = fileContent.lineSequence().iterator()

        val cues = mutableListOf<SubtitleEntry>()

        while (lines.hasNext()) {
            var line = lines.next().trim()
            if (line.isBlank()) continue

            // Logic to find the timestamp line
            if (!line.contains("-->")) {
                if (lines.hasNext()) {
                    line = lines.next().trim()
                } else {
                    break
                }
            }

            // If we still don't have arrows, skip this chunk
            if (!line.contains("-->")) continue

            val timestamps = line.split("-->").map { it.trim() }
            if (timestamps.size != 2) continue

            val startTimeMs = parseTimestampMs(timestamps[0], isVtt)
            val endTimeMs = parseTimestampMs(timestamps[1], isVtt)

            val textBuilder = StringBuilder()

            // Read text lines until an empty line or end of file
            while (lines.hasNext()) {
                val textLine = lines.next()
                if (textLine.isBlank()) break // Subtitle block ends on empty line

                if (textBuilder.isNotEmpty()) {
                    textBuilder.append("\n")
                }
                textBuilder.append(textLine.trim())
            }

            if (startTimeMs != -1L && endTimeMs != -1L) {
                cues.add(SubtitleEntry(startTimeMs, endTimeMs, textBuilder.toString()))
            }
        }
        return cues
    }

    fun parse(subtitleText: String): List<SubtitleEntry> {
        if (!subtitleText.contains("-->")) {
            throw Exception("This file isn't in SRT/VTT format")
        }

        // determine format based on header or content
        val isVtt = subtitleText.contains("VTT", ignoreCase = true)

        // Create a new reader from the String we just read
        val lines = subtitleText.lineSequence().iterator()

        val cues = mutableListOf<SubtitleEntry>()

        while (lines.hasNext()) {
            var line = lines.next().trim()
            if (line.isBlank()) continue

            // Logic to find the timestamp line
            // In SRT, we often hit the index number (1, 2, 3) first.
            // We need to skip lines until we find "-->"
            if (!line.contains("-->")) {
                if (lines.hasNext()) {
                    line = lines.next().trim()
                } else {
                    break
                }
            }

            // If we still don't have arrows, skip this chunk
            if (!line.contains("-->")) continue

            val timestamps = line.split("-->").map { it.trim() }
            if (timestamps.size != 2) continue

            val startTimeMs = parseTimestampMs(timestamps[0], isVtt)
            val endTimeMs = parseTimestampMs(timestamps[1], isVtt)

            val textBuilder = StringBuilder()

            // Read text lines until an empty line or end of file
            while (lines.hasNext()) {
                val textLine = lines.next()
                if (textLine.isBlank()) break // Subtitle block ends on empty line

                if (textBuilder.isNotEmpty()) {
                    textBuilder.append("\n")
                }
                textBuilder.append(textLine.trim())
            }

            if (startTimeMs != -1L && endTimeMs != -1L) {
                cues.add(SubtitleEntry(startTimeMs, endTimeMs, textBuilder.toString()))
            }
        }
        return cues
    }

    private fun parseTimestampMs(timestamp: String, isVtt: Boolean): Long {
        val matcher =
            (if (isVtt) VTT_TIMESTAMP_PATTERN else SRT_TIMESTAMP_PATTERN).matcher(timestamp)

        if (!matcher.matches()) return -1L

        val hours = if (isVtt) matcher.group(1)?.toLong() ?: 0L else matcher.group(1)!!.toLong()
        val minutes = matcher.group(2)!!.toLong()
        val seconds = matcher.group(3)!!.toLong()
        val millis = matcher.group(4)!!.toLong()
        return (hours * 3600000) + (minutes * 60000) + (seconds * 1000) + millis
    }
}


object SubtitleManager {
    fun parseSubtitle(context: Context, uri: Uri): List<SubtitleEntry>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                Parser().parse(inputStream)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    fun parseSubtitle(subtitleText: String): List<SubtitleEntry>? {
        return try {
            Parser().parse(subtitleText)
        } catch (e: Exception) {
            throw e
        }
    }
}

