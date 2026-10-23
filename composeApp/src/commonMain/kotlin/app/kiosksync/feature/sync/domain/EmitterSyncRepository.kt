package app.kiosksync.feature.sync.domain

import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.feature.playlist.domain.Playlist

data class SyncProgress(
    val uploaded: Int,
    val total: Int,
) {
    val fraction: Float
        get() = if (total <= 0) 0f else uploaded.toFloat() / total.toFloat()
}

data class SyncSummary(
    val uploadedCount: Int,
    val skippedCount: Int,
    val appliedVersion: Int,
)

class LocalMediaMissingException(val mediaId: String) :
    Exception("Local media file is missing for mediaId='$mediaId'")

/** Domain-facing operation for applying a playlist to a paired Receiver. */
interface EmitterSyncRepository {
    suspend fun sync(
        device: EmitterPairedDevice,
        host: String,
        port: Int,
        emitterId: String,
        playlist: Playlist,
        lock: KioskLockConfig? = null,
        onUploadProgress: (SyncProgress) -> Unit = {},
        onApplying: () -> Unit = {},
    ): Result<SyncSummary>
}
