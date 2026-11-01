package app.kiosksync.core.common.pairing

import kotlinx.serialization.Serializable

@Serializable
data class DiscoveredDevice(
    val deviceId: String,
    val deviceName: String,
    val host: String,
    val port: Int,
    val paired: Boolean,
    val appVersion: String? = null
)
