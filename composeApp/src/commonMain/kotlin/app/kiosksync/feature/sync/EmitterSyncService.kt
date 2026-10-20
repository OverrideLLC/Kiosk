package app.kiosksync.feature.sync

import app.kiosksync.core.AppLogger
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.PlaylistRejectedException
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.platform.EmitterClient
import app.kiosksync.platform.LocalStorage
import kotlinx.coroutines.CancellationException

data class SyncProgress(
    val uploaded: Int,
    val total: Int
) {
    val fraction: Float
        get() = if (total <= 0) 0f else uploaded.toFloat() / total.toFloat()
}

data class SyncSummary(
    val uploadedCount: Int,
    val skippedCount: Int,
    val appliedVersion: Int
)

class LocalMediaMissingException(val mediaId: String) :
    Exception("Local media file is missing for mediaId='$mediaId'")

/**
 * Sends one playlist to one receiver following the diff protocol:
 * /sync/state -> upload missing media -> /playlist.
 */
class EmitterSyncService(
    private val client: EmitterClient,
    private val storage: LocalStorage,
    private val logger: AppLogger
) {
    suspend fun sync(
        device: EmitterPairedDevice,
        host: String,
        port: Int,
        emitterId: String,
        playlist: Playlist,
        onUploadProgress: (SyncProgress) -> Unit = {},
        onApplying: () -> Unit = {}
    ): Result<SyncSummary> {
        return try {
            val remoteState = client.getSyncState(host, port, emitterId, device.token).getOrThrow()
            val plan = SyncPlanner.plan(playlist, remoteState)

            val payloads = plan.uploads.map { item ->
                val bytes = storage.getMediaBytes(item.mediaId, item.fileName)
                    ?: throw LocalMediaMissingException(item.mediaId)
                item to bytes
            }

            var uploaded = 0
            for ((item, bytes) in payloads) {
                client.uploadMedia(
                    host = host,
                    port = port,
                    emitterId = emitterId,
                    token = device.token,
                    mediaId = item.mediaId,
                    sha256 = item.sha256,
                    bytes = bytes
                ).getOrThrow()
                uploaded++
                onUploadProgress(SyncProgress(uploaded = uploaded, total = payloads.size))
            }

            onApplying()
            val newVersion = maxOf(remoteState.playlistVersion, playlist.version) + 1
            val response = client.putPlaylist(
                host = host,
                port = port,
                emitterId = emitterId,
                token = device.token,
                playlist = playlist.copy(version = newVersion)
            ).getOrThrow()

            if (!response.applied) {
                throw PlaylistRejectedException(response.missingMediaIds)
            }

            logger.info(
                "Sync to '${device.deviceName}' finished: uploaded=$uploaded, " +
                    "skipped=${plan.unchangedCount}, version=${response.version}"
            )
            Result.success(
                SyncSummary(
                    uploadedCount = uploaded,
                    skippedCount = plan.unchangedCount,
                    appliedVersion = response.version
                )
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.warn("Sync to '${device.deviceName}' failed: ${error.message}")
            Result.failure(error)
        }
    }
}
