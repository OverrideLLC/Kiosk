package app.kiosksync.feature.media.domain

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.core.ImageFormat

data class StoredMedia(
    val mediaId: String,
    val fileName: String,
    val sha256: String,
    val sizeBytes: Long,
)

sealed interface MediaImportResult {
    data class Success(val media: StoredMedia) : MediaImportResult
    data class Failure(val reason: MediaImportError) : MediaImportResult
}

enum class MediaImportError {
    UNSUPPORTED_FORMAT,
    TOO_LARGE,
    UNREADABLE,
}

/** File-backed media operations, implemented by the desktop host. */
interface MediaRepository {
    suspend fun loadBitmap(mediaId: String, fileName: String): ImageBitmap?
    suspend fun import(sourcePath: String): MediaImportResult
    suspend fun getBytes(mediaId: String, fileName: String): ByteArray?
    suspend fun listStored(): List<StoredMedia>
    suspend fun writeReceived(mediaId: String, format: ImageFormat, bytes: ByteArray): StoredMedia?
    suspend fun delete(mediaId: String): Boolean
    suspend fun clearMedia()
}
