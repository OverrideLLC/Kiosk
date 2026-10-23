package app.kiosksync.feature.sync.data

import app.kiosksync.core.AppLogger
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.PlaylistRejectedException
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.emitter.domain.EmitterClient
import app.kiosksync.feature.sync.domain.EmitterSyncRepository
import app.kiosksync.feature.sync.domain.LocalMediaMissingException
import app.kiosksync.feature.sync.domain.SyncProgress
import app.kiosksync.feature.sync.domain.SyncSummary
import app.kiosksync.feature.sync.domain.SyncPlanner
import app.kiosksync.platform.MediaRepository
import kotlinx.coroutines.CancellationException

/**
 * Sends one playlist to one receiver following the diff protocol:
 * /sync/state -> upload missing media -> /playlist.
 */
class EmitterSyncRepositoryImpl(
    private val client: EmitterClient,
    private val mediaRepository: MediaRepository,
    private val logger: AppLogger
) : EmitterSyncRepository {
    override suspend fun sync(
        device: EmitterPairedDevice,
        host: String,
        port: Int,
        emitterId: String,
        playlist: Playlist,
        lock: KioskLockConfig?,
        onUploadProgress: (SyncProgress) -> Unit,
        onApplying: () -> Unit
    ): Result<SyncSummary> {
        return try {
            val remoteState = client.getSyncState(host, port, emitterId, device.token).getOrThrow()
            val plan = SyncPlanner.plan(playlist, remoteState)

            val payloads = plan.uploads.map { item ->
                val bytes = mediaRepository.getBytes(item.mediaId, item.fileName)
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
                playlist = playlist.copy(version = newVersion),
                lock = lock
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
