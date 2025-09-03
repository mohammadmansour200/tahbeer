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

