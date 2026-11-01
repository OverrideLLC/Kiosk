package app.kiosksync.core.common.pairing

import app.kiosksync.core.common.app.APP_VERSION
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val role: String,
    val paired: Boolean,
    val appVersion: String = APP_VERSION,
    val skippedUpdateVersion: String? = null
)
