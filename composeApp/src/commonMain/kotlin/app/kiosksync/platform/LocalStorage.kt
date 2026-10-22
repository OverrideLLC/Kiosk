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
    fun close() = Unit

    fun clearPairing()

    /** Removes Screen-owned playlist, media, kiosk lock and pairing on an explicit role reset. */
    fun clearReceiverData()

    // Receiver pairing
    fun loadReceiverPairedInfo(): ReceiverPairedInfo?
    fun saveReceiverPairedInfo(info: ReceiverPairedInfo)

    // Emitter paired devices list
    fun loadEmitterPairedDevices(): List<EmitterPairedDevice>
    fun saveEmitterPairedDevices(devices: List<EmitterPairedDevice>)
    fun addEmitterPairedDevice(device: EmitterPairedDevice)
    fun removeEmitterPairedDevice(deviceId: String)

    // Playlist and media
    fun loadPlaylist(): Playlist?
    fun savePlaylist(playlist: Playlist)

    // Emitter named playlists
    fun loadPlaylistLibrary(): PlaylistLibrary?
    fun savePlaylistLibrary(library: PlaylistLibrary)

    // Kiosk lock
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
