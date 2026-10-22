package app.kiosksync.platform.server

import app.kiosksync.core.AppLogger
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.feature.kiosk.KioskLockManager
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.pairing.PairingManager
import app.kiosksync.feature.playback.ReceiverPlaybackSyncBridge
import app.kiosksync.feature.playback.ReceiverPlaylistBridge
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.domain.PlaylistRepository
import app.kiosksync.platform.MediaRepository
import app.kiosksync.platform.ReceiverUpdateManager
import app.kiosksync.core.ReceiverStatusResponse

internal data class ReceiverRouteContext(
    val pairingManager: PairingManager,
    val logger: AppLogger,
    val playlistRepository: PlaylistRepository,
    val mediaRepository: MediaRepository,
    val kioskLockRepository: KioskLockRepository,
    val playlistBridge: ReceiverPlaylistBridge,
    val playbackSyncBridge: ReceiverPlaybackSyncBridge,
    val kioskLockManager: KioskLockManager,
    val statusProvider: () -> ReceiverStatusResponse,
    val updateManager: ReceiverUpdateManager,
    val deviceId: () -> String,
    val deviceName: () -> String,
)

internal val MEDIA_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,100}$")

internal fun Playlist.isValidForReceiver(): Boolean {
    if (version < 1 || defaultDurationSec !in app.kiosksync.core.MIN_DURATION_SEC..app.kiosksync.core.MAX_DURATION_SEC) {
        return false
    }
    if (items.map { it.mediaId }.distinct().size != items.size) return false
    return items.all { item ->
        MEDIA_ID_REGEX.matches(item.mediaId) &&
            item.fileName.isNotBlank() &&
            item.fileName != "." &&
            item.fileName != ".." &&
            item.fileName.none { it in "/\\:*?\"<>|" || it.code < 32 } &&
            item.sha256.length == 64 &&
            (item.durationSec == null ||
                item.durationSec in app.kiosksync.core.MIN_DURATION_SEC..app.kiosksync.core.MAX_DURATION_SEC)
    }
}

internal fun KioskLockConfig.isValidForReceiver(): Boolean {
    if (!enabled || !app.kiosksync.core.isValidUnlockSequence(unlockSequence)) return false
    if (passwordHash != null) {
        if (passwordLength !in 4..64 || !passwordHash.isSha256Hex()) return false
    } else if (passwordLength != 0) {
        return false
    }
    if (recoveryHash != null) {
        if (recoveryLength !in
            app.kiosksync.core.MIN_RECOVERY_PHRASE_LENGTH..app.kiosksync.core.MAX_RECOVERY_PHRASE_LENGTH ||
            !recoveryHash.isSha256Hex()
        ) return false
    } else if (recoveryLength != 0) {
        return false
    }
    return true
}

private fun String.isSha256Hex(): Boolean =
    length == 64 && all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

