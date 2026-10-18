package app.kiosksync.feature.playlist

import app.kiosksync.core.DEFAULT_DURATION_SEC
import app.kiosksync.core.MAX_DURATION_SEC
import app.kiosksync.core.MIN_DURATION_SEC
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistItem(
    val mediaId: String,
    val fileName: String,
    val sha256: String,
    val durationSec: Int? = null
)

@Serializable
data class Playlist(
    val version: Int = 1,
    val defaultDurationSec: Int = DEFAULT_DURATION_SEC,
    val items: List<PlaylistItem> = emptyList()
) {
    fun getEffectiveDurationSec(item: PlaylistItem): Int {
        val duration = item.durationSec ?: defaultDurationSec
        return duration.coerceIn(MIN_DURATION_SEC, MAX_DURATION_SEC)
    }
}
