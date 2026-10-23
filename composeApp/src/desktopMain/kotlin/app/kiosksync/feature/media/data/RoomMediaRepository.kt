package app.kiosksync.feature.media.data

import app.kiosksync.core.AppLogger
import app.kiosksync.core.ImageFormat
import app.kiosksync.core.MAX_IMAGE_SIZE_BYTES
import app.kiosksync.data.local.room.KioskDao
import app.kiosksync.data.local.room.MediaEntity
import app.kiosksync.feature.media.domain.MediaImportError
import app.kiosksync.feature.media.domain.MediaImportResult
import app.kiosksync.feature.media.domain.MediaRepository
import app.kiosksync.feature.media.domain.StoredMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class RoomMediaRepository(
    private val dao: KioskDao,
    private val mediaStore: FileMediaStore,
    private val logger: AppLogger,
) : MediaRepository {
    override suspend fun loadBitmap(mediaId: String, fileName: String) = withContext(Dispatchers.IO) {
        mediaStore.loadBitmap(mediaId, fileName)
    }

    override suspend fun import(sourcePath: String): MediaImportResult = withContext(Dispatchers.IO) {
        val result = mediaStore.importMedia(sourcePath)
        if (result !is MediaImportResult.Success) return@withContext result
        try {
            dao.upsertMediaItem(result.media.toEntity())
            result
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            logger.error("Could not save imported media metadata", error)
            mediaStore.delete(result.media.mediaId)
            MediaImportResult.Failure(MediaImportError.UNREADABLE)
        }
    }

    override suspend fun getBytes(mediaId: String, fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        mediaStore.getBytes(mediaId, fileName)
    }

    override suspend fun listStored(): List<StoredMedia> = withContext(Dispatchers.IO) {
        val files = mediaStore.listFiles().associateBy(StoredMedia::mediaId)
        dao.getMedia().mapNotNull { entity ->
            files[entity.mediaId]?.let {
                StoredMedia(entity.mediaId, entity.fileName, entity.sha256, entity.sizeBytes)
            } ?: run {
                dao.deleteMedia(entity.mediaId)
                null
            }
        }
    }

    override suspend fun writeReceived(
        mediaId: String,
        format: ImageFormat,
        bytes: ByteArray,
    ): StoredMedia? = withContext(Dispatchers.IO) {
        if (bytes.size.toLong() > MAX_IMAGE_SIZE_BYTES) return@withContext null
        val stored = mediaStore.writeReceived(mediaId, format, bytes) ?: return@withContext null
        try {
            dao.upsertMediaItem(stored.toEntity())
            stored
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            logger.error("Could not save received media metadata", error)
            mediaStore.delete(mediaId)
            null
        }
    }

    override suspend fun delete(mediaId: String): Boolean = withContext(Dispatchers.IO) {
        val deleted = mediaStore.delete(mediaId)
        dao.deleteMedia(mediaId)
        deleted
    }

    override suspend fun clearMedia() = withContext(Dispatchers.IO) {
        mediaStore.clear()
        dao.deleteAllMedia()
    }

    private fun StoredMedia.toEntity() = MediaEntity(mediaId, fileName, sha256, sizeBytes)
}
