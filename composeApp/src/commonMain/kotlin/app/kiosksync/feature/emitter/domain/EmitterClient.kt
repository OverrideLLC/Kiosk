package app.kiosksync.feature.emitter.domain

import app.kiosksync.core.DeviceInfo
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.MediaUploadResponse
import app.kiosksync.core.PairResponse
import app.kiosksync.core.PlaylistApplyResponse
import app.kiosksync.core.ReceiverStatusResponse
import app.kiosksync.core.SyncStateResponse
import app.kiosksync.core.UpdateUploadResponse
import app.kiosksync.feature.playlist.domain.Playlist

interface EmitterClient {
    suspend fun getInfo(host: String, port: Int): Result<DeviceInfo>
    suspend fun pair(host: String, port: Int, emitterId: String, emitterName: String, code: String): Result<PairResponse>
    suspend fun unpair(host: String, port: Int, emitterId: String, token: String): Result<Boolean>
    suspend fun getStatus(host: String, port: Int, emitterId: String, token: String): Result<ReceiverStatusResponse>

    suspend fun synchronizePlayback(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        delayMs: Long
    ): Result<Boolean>

    suspend fun getSyncState(
        host: String,
        port: Int,
        emitterId: String,
        token: String
    ): Result<SyncStateResponse>

    suspend fun uploadMedia(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        mediaId: String,
        sha256: String,
        bytes: ByteArray
    ): Result<MediaUploadResponse>

    /** Sends the controller's packaged application payload to an older screen. */
    suspend fun uploadUpdate(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        version: String,
        sha256: String,
        bytes: ByteArray
    ): Result<UpdateUploadResponse>

    suspend fun putPlaylist(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        playlist: Playlist,
        lock: KioskLockConfig? = null
    ): Result<PlaylistApplyResponse>

    /**
     * Sends only the kiosk lock. The controller calls this right after pairing
     * (or when the password changes) so a screen locks immediately instead of
     * waiting for the next content push.
     */
    suspend fun putLock(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        lock: KioskLockConfig
    ): Result<Boolean>

    fun close()
}
