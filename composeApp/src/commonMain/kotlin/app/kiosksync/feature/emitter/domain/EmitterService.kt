package app.kiosksync.feature.emitter.domain

import app.kiosksync.core.KioskLockConfig
import app.kiosksync.feature.playlist.domain.Playlist
import kotlinx.coroutines.flow.StateFlow

data class EmitterServiceDevice(
    val deviceId: String,
    val deviceName: String,
    val host: String,
    val port: Int,
    val pairedToThisEmitter: Boolean,
    val pairedToAnotherEmitter: Boolean,
    val isOnline: Boolean,
    val appVersion: String?,
)

enum class EmitterUpdateStatus {
    PREPARING,
    UPLOADING,
    AWAITING_CONFIRMATION,
    APPLYING,
    SKIPPED,
    FAILED,
    UNAVAILABLE,
}

data class EmitterUpdateState(
    val status: EmitterUpdateStatus,
    val version: String,
    val forced: Boolean = false,
)

data class EmitterServiceState(
    val devices: List<EmitterServiceDevice> = emptyList(),
    val updateByDevice: Map<String, EmitterUpdateState> = emptyMap(),
    val isSearching: Boolean = true,
)

data class EmitterDeliveryResult(
    val deviceId: String,
    val result: Result<EmitterDeliverySummary>,
)

data class EmitterDeliveryProgress(
    val uploaded: Int,
    val total: Int,
)

data class EmitterDeliverySummary(
    val appliedVersion: Int,
    val uploadedCount: Int,
)

class EmitterLocalMediaMissingException(cause: Throwable? = null) :
    IllegalStateException("A local media file required for delivery is missing", cause)

data class PlaybackSyncResult(
    val successCount: Int,
    val failureCount: Int,
)

/** Operational boundary for the Controller's network, discovery and local-data workflows. */
interface EmitterService {
    val state: StateFlow<EmitterServiceState>

    fun start(emitterId: String, emitterName: String)
    fun stop()
    fun refresh()
    fun requestUpdate(deviceId: String, emitterId: String)

    suspend fun addReceiver(address: String): Result<EmitterServiceDevice>
    suspend fun pair(
        deviceId: String,
        code: String,
        emitterId: String,
        emitterName: String,
    ): Result<PairReceiverResult>
    suspend fun unpair(deviceId: String, emitterId: String): Result<UnpairReceiverResult>
    suspend fun loadKioskLock(): Result<KioskLockConfig?>

    fun sendLockToDevice(deviceId: String, emitterId: String, lock: KioskLockConfig)
    fun sendLockToAllPaired(emitterId: String, lock: KioskLockConfig)

    suspend fun deliver(
        emitterId: String,
        deviceIds: List<String>,
        playlist: Playlist,
        lock: KioskLockConfig,
        onProgress: (deviceId: String, progress: EmitterDeliveryProgress) -> Unit,
        onApplying: (deviceId: String) -> Unit,
    ): List<EmitterDeliveryResult>

    suspend fun synchronizePlayback(emitterId: String): Result<PlaybackSyncResult>
}
