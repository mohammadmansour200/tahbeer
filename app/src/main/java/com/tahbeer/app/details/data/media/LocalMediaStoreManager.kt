package com.tahbeer.app.details.data.media

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import com.tahbeer.app.core.utils.MimeTypeMap
import com.tahbeer.app.core.utils.isAudio
import com.tahbeer.app.core.utils.isSubtitle
import com.tahbeer.app.core.utils.isVideo
import com.tahbeer.app.details.domain.MediaStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class LocalMediaStoreManager(private val context: Context) : MediaStoreManager {
    private val contentResolver: ContentResolver = context.contentResolver
    private val isAndroidQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override suspend fun createMediaUri(
        name: String?,
        extension: String
    ): Result<Uri?> =
        withContext(Dispatchers.IO) {
            val contentValues = ContentValues()
            val displayName = "$name.$extension"
            val mimeType = MimeTypeMap.getMimeTypeFromExtension(extension)!!
            if (isAndroidQOrLater) {
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1)

                if (extension.isVideo()) {
                    val collectionUri =
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    contentValues.apply {
                        put(
                            MediaStore.Video.Media.MIME_TYPE,
                            mimeType
                        )
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies")
                        put(
                            MediaStore.Video.Media.DISPLAY_NAME,
                            displayName
                        )
                    }

                    return@withContext Result.success(
                        contentResolver.insert(
                            collectionUri,
                            contentValues
                        )
                    )
                } else {
                    val collectionUri =
                        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                    contentValues.apply {
                        put(
                            MediaStore.Files.FileColumns.DISPLAY_NAME,
                            displayName
                        )
                        put(
                            MediaStore.Files.FileColumns.MIME_TYPE,
                            "application/octet-stream"
                        )
                        put(MediaStore.Files.FileColumns.RELATIVE_PATH, "Documents")
                    }

                    return@withContext Result.success(
                        contentResolver.insert(
                            collectionUri,
                            contentValues
                        )
                    )
                }

            } else {
                val directory = when {
                    extension.isAudio() -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    extension.isSubtitle() -> Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS
                    )

                    extension.isVideo() -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    else -> null
                }
                val file = File(directory, displayName)
                file.createNewFile()

                return@withContext Result.success(
                    file.toUri()
                )
            }
        }

    override suspend fun saveMedia(
        uri: Uri
    ) =
        withContext(Dispatchers.IO) {
            if (isAndroidQOrLater) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, values, null, null)
            }
        }

    override suspend fun writeText(
        uri: Uri,
        text: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.writer(Charsets.UTF_8).use { writer ->
                        writer.write(text)
                    }
                    Result.success(Unit)
                } ?: Result.failure(IOException("Could not open output stream for $uri"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }


    override suspend fun deleteMedia(uri: Uri): Unit =
        withContext(Dispatchers.IO) {
            if (isAndroidQOrLater) {
                context.contentResolver.delete(uri, null, null)
            } else {
                val file = File(uri.path!!)
                file.delete()
            }
        }
}