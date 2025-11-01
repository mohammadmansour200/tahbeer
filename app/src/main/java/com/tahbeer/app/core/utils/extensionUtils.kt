package com.tahbeer.app.core.utils

fun String.isVideo(): Boolean {
    if (this.contains("/")) return this.contains("video")

    val mimeType = MimeTypeMap.getMimeTypeFromExtension(this)!!
    return mimeType.contains("video")
}

fun String.isAudio(): Boolean {
    if (this.contains("/")) return this.contains("audio")

    val mimeType = MimeTypeMap.getMimeTypeFromExtension(this)!!
    return mimeType.contains("audio")
}

fun String.isSubtitle(): Boolean {
    if (this.contains("/")) return this.contains("text") || this.contains("application")

    val mimeType = MimeTypeMap.getMimeTypeFromExtension(this)!!
    return mimeType.contains("text") || mimeType.contains("application")
}

