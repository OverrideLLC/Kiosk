package app.kiosksync.core.common.playback

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackSyncResponse(
    val accepted: Boolean,
    val delayMs: Long = 0
)
