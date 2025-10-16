package com.tahbeer.app.details.utils

import com.tahbeer.app.details.domain.model.ExportError

fun parseFfmpegError(logs: String): ExportError {
    val contains = { label: String -> logs.contains(label, ignoreCase = true) }

    if (contains("No such file or directory")) ExportError.ERROR_READING_INPUT

    if (contains("Could not open file")) ExportError.ERROR_READING_INPUT

    if (contains("Invalid data found when processing input")) ExportError.ERROR_INVALID_FORMAT

    if (contains("No space left on device")) ExportError.ERROR_WRITING_OUTPUT

    if (contains("Output file #0 does not contain any stream")) ExportError.ERROR_WRITING_OUTPUT

    return ExportError.ERROR_UNKNOWN
}