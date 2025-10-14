package com.tahbeer.app.details.utils

import java.util.Locale

fun longToTimestamp(t: Long, subtitleTimestamp: Boolean = false, comma: Boolean = false): String {
    var msec = t
    val hr = msec / (1000 * 60 * 60)
    msec -= hr * (1000 * 60 * 60)
    val min = msec / (1000 * 60)
    msec -= min * (1000 * 60)
    val sec = msec / 1000
    msec -= sec * 1000

    val delimiter = if (comma) "," else "."
    return if (subtitleTimestamp)
        String.format(Locale.US, "%02d:%02d:%02d%s%03d", hr, min, sec, delimiter, msec)
    else
        if (hr == 0L) {
            String.format(Locale.US, "%02d:%02d", min, sec)
        } else {
            String.format(Locale.US, "%02d:%02d:%02d", hr, min, sec)
        }
}