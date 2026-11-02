package app.kiosksync.core.common.status

import app.kiosksync.core.common.app.APP_VERSION
import kotlinx.serialization.Serializable

@Serializable
data class ReceiverStatusResponse(
    val playlistVersion: Int,
    val currentMediaId: String? = null,
    val currentFileName: String? = null,
    val appVersion: String = APP_VERSION,
    val skippedUpdateVersion: String? = null
)
