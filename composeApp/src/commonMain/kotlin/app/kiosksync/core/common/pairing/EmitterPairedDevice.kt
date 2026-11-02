package app.kiosksync.core.common.pairing

import kotlinx.serialization.Serializable

@Serializable
data class EmitterPairedDevice(
    val deviceId: String,
    val deviceName: String,
    val token: String,
    val host: String? = null,
    val port: Int? = null
)
