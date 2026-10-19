package app.kiosksync.core

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val role: String,
    val paired: Boolean,
    val appVersion: String = APP_VERSION
)

@Serializable
data class PairRequest(
    val emitterId: String,
    val emitterName: String,
    val code: String
)

@Serializable
data class PairResponse(
    val token: String,
    val receiverId: String
)

@Serializable
data class ReceiverStatusResponse(
    val playlistVersion: Int,
    val currentMediaId: String? = null,
    val currentFileName: String? = null
)

@Serializable
data class ReceiverPairedInfo(
    val emitterId: String,
    val emitterName: String,
    val token: String
)

@Serializable
data class EmitterPairedDevice(
    val deviceId: String,
    val deviceName: String,
    val token: String,
    val host: String? = null,
    val port: Int? = null
)

@Serializable
data class DiscoveredDevice(
    val deviceId: String,
    val deviceName: String,
    val host: String,
    val port: Int,
    val paired: Boolean
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String? = null,
    val remainingAttempts: Int? = null,
    val remainingSeconds: Long? = null
)

enum class PairError {
    INVALID_CODE,
    LOCKED_OUT,
    ALREADY_PAIRED
}

class PairRejectedException(
    val error: PairError,
    val remainingAttempts: Int? = null,
    val remainingSeconds: Long? = null
) : Exception("Pairing rejected: $error")
