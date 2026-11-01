package app.kiosksync.core.common.playback

import kotlinx.serialization.Serializable

/** Lead time so every screen receives the command before the shared start. */
const val PLAYBACK_SYNC_DELAY_MS = 2_000L

@Serializable
data class PlaybackSyncRequest(
    val delayMs: Long
)
