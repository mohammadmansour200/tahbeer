package com.tahbeer.app.details.domain

import android.net.Uri

interface MediaStoreManager {
    suspend fun createMediaUri(
        name: String?,
        extension: String
    ): Result<Uri?>

    suspend fun saveMedia(uri: Uri)

    suspend fun writeText(
        uri: Uri,
        text: String
    ): Result<Unit>

    suspend fun deleteMedia(uri: Uri)
}