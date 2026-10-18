package app.kiosksync.platform

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.role.AppConfig

interface LocalStorage {
    fun loadConfig(): AppConfig?
    fun saveConfig(config: AppConfig)
    fun clearConfig()
    fun clearPairing()

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
    fun loadMediaBitmap(mediaId: String, fileName: String): ImageBitmap?
}
