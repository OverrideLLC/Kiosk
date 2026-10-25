package app.kiosksync.feature.emitter.domain

import app.kiosksync.core.KioskLockConfig
import app.kiosksync.feature.playlist.domain.Playlist

data class ReceiverInfo(
    val deviceId: String,
    val deviceName: String,
    val isReceiver: Boolean,
    val isPaired: Boolean,
    val appVersion: String,
    val skippedUpdateVersion: String?,
)

data class ReceiverPairingCredential(val token: String)

enum class PairError {
    INVALID_CODE,
    LOCKED_OUT,
    ALREADY_PAIRED,
}

class PairRejectedException(
    val error: PairError,
    val remainingAttempts: Int? = null,
    val remainingSeconds: Long? = null,
) : Exception("Pairing rejected: $error")

data class ReceiverStatus(
    val appVersion: String,
    val skippedUpdateVersion: String?,
)

data class RemoteMediaState(
    val mediaId: String,
    val sha256: String,
)

data class RemoteSyncState(
    val playlistVersion: Int,
    val media: List<RemoteMediaState>,
)

data class UpdateUploadResult(
    val accepted: Boolean,
    val version: String,
    val forced: Boolean,
)

data class PlaylistApplyResult(
    val applied: Boolean,
    val version: Int,
    val missingMediaIds: List<String>,
)

class PlaylistRejectedException(val missingMediaIds: List<String>) :
    Exception("Receiver rejected the playlist because media is missing")

interface EmitterClient {
    suspend fun getInfo(host: String, port: Int): Result<ReceiverInfo>
    suspend fun pair(
        host: String,
        port: Int,
        emitterId: String,
        emitterName: String,
        code: String,
    ): Result<ReceiverPairingCredential>
    suspend fun unpair(host: String, port: Int, emitterId: String, token: String): Result<Boolean>
    suspend fun getStatus(host: String, port: Int, emitterId: String, token: String): Result<ReceiverStatus>

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
    ): Result<RemoteSyncState>

    suspend fun uploadMedia(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        mediaId: String,
        sha256: String,
        bytes: ByteArray
    ): Result<Unit>

    /** Sends the controller's packaged application payload to an older screen. */
    suspend fun uploadUpdate(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        version: String,
        sha256: String,
        bytes: ByteArray
    ): Result<UpdateUploadResult>

    suspend fun putPlaylist(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        playlist: Playlist,
        lock: KioskLockConfig? = null
    ): Result<PlaylistApplyResult>

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
