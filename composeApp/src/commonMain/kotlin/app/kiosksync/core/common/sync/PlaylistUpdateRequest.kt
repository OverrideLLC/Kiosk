package app.kiosksync.core.common.sync

import app.kiosksync.core.common.kiosk.KioskLockConfig
import app.kiosksync.feature.playlist.domain.Playlist
import kotlinx.serialization.Serializable

/**
 * Body of PUT /playlist. The lock is optional so a controller without a kiosk
 * password can still update content.
 */
@Serializable
data class PlaylistUpdateRequest(
    val playlist: Playlist,
    val lock: KioskLockConfig? = null
)
