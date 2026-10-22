package app.kiosksync.platform

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.ImageFormat
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.playlist.PlaylistLibrary

data class StoredMedia(
    val mediaId: String,
    val fileName: String,
    val sha256: String,
    val sizeBytes: Long
)

sealed interface MediaImportResult {
    data class Success(val media: StoredMedia) : MediaImportResult
    data class Failure(val reason: MediaImportError) : MediaImportResult
}

enum class MediaImportError {
    UNSUPPORTED_FORMAT,
    TOO_LARGE,
    UNREADABLE
}

interface LocalStorage {
    fun clearPairing()

    /**
     * Wipes everything that belongs to the Screen role (the active playlist, the
     * stored images, the blocking password and the pairing) so that a screen
     * always starts blank instead of replaying content from a previous session.
     * It is never called for the Controller role, which keeps all of its data.
     */
    fun clearReceiverData()

    // Receiver pairing (paired.json on receiver)
    fun loadReceiverPairedInfo(): ReceiverPairedInfo?
    fun saveReceiverPairedInfo(info: ReceiverPairedInfo)

    // Emitter paired devices list (paired.json on emitter)
    fun loadEmitterPairedDevices(): List<EmitterPairedDevice>
    fun saveEmitterPairedDevices(devices: List<EmitterPairedDevice>)
    fun addEmitterPairedDevice(device: EmitterPairedDevice)
    fun removeEmitterPairedDevice(deviceId: String)

    // Playlist and media
    fun loadPlaylist(): Playlist?
    fun savePlaylist(playlist: Playlist)

    // Emitter named playlists (playlists.json)
    fun loadPlaylistLibrary(): PlaylistLibrary?
    fun savePlaylistLibrary(library: PlaylistLibrary)

    // Kiosk lock (lock.json)
    fun loadKioskLock(): KioskLockConfig?
    fun saveKioskLock(config: KioskLockConfig)
    fun clearKioskLock()

    fun loadMediaBitmap(mediaId: String, fileName: String): ImageBitmap?

    // Emitter media import
    fun importMedia(sourcePath: String): MediaImportResult

    // Emitter uploads and receiver storage
    fun getMediaBytes(mediaId: String, fileName: String): ByteArray?
    fun listStoredMedia(): List<StoredMedia>
    fun writeReceivedMedia(mediaId: String, format: ImageFormat, bytes: ByteArray): StoredMedia?
    fun deleteStoredMedia(mediaId: String): Boolean
}
