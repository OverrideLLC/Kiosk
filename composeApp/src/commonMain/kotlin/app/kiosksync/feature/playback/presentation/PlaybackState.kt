package app.kiosksync.feature.playback.presentation

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.feature.playlist.domain.PlaylistItem

/** Only the data currently rendered by the carousel. */
data class PlaybackState(
    val currentItem: PlaylistItem? = null,
    val currentBitmap: ImageBitmap? = null,
) {
    val hasItems: Boolean
        get() = currentItem != null && currentBitmap != null
}
