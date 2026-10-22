package app.kiosksync.core

import app.kiosksync.feature.playlist.domain.Playlist
import kotlinx.serialization.Serializable

/**
 * Body of PUT /playlist. The lock hash is optional so a controller without a
 * kiosk password can still update content (for example in windowed debug mode).
 */
@Serializable
data class PlaylistUpdateRequest(
    val playlist: Playlist,
    val lock: KioskLockConfig? = null
)
