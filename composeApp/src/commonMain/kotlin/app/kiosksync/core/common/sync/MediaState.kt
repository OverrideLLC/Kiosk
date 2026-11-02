package app.kiosksync.core.common.sync

import kotlinx.serialization.Serializable

@Serializable
data class MediaState(
    val mediaId: String,
    val sha256: String
)
