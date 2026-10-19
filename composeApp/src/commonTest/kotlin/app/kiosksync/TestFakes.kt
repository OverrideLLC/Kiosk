package app.kiosksync

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.core.AppLogger
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.role.AppConfig
import app.kiosksync.platform.LocalStorage

class FakeLocalStorage(
    var playlist: Playlist? = null,
    val availableBitmaps: MutableMap<String, ImageBitmap> = mutableMapOf()
) : LocalStorage {
    var savedConfig: AppConfig? = null
    var receiverPairedInfo: ReceiverPairedInfo? = null
    var emitterPairedDevices: List<EmitterPairedDevice> = emptyList()
    var clearConfigCalled = false
    var clearPairingCalled = false

    override fun loadConfig(): AppConfig? = savedConfig

    override fun saveConfig(config: AppConfig) {
        savedConfig = config
    }

    override fun clearConfig() {
        clearConfigCalled = true
        savedConfig = null
        clearPairing()
    }

    override fun clearPairing() {
        clearPairingCalled = true
        receiverPairedInfo = null
        emitterPairedDevices = emptyList()
    }

    override fun loadReceiverPairedInfo(): ReceiverPairedInfo? = receiverPairedInfo

    override fun saveReceiverPairedInfo(info: ReceiverPairedInfo) {
        receiverPairedInfo = info
    }

    override fun loadEmitterPairedDevices(): List<EmitterPairedDevice> = emitterPairedDevices

    override fun saveEmitterPairedDevices(devices: List<EmitterPairedDevice>) {
        emitterPairedDevices = devices
    }

    override fun addEmitterPairedDevice(device: EmitterPairedDevice) {
        emitterPairedDevices = emitterPairedDevices.filterNot { it.deviceId == device.deviceId } + device
    }

    override fun removeEmitterPairedDevice(deviceId: String) {
        emitterPairedDevices = emitterPairedDevices.filterNot { it.deviceId == deviceId }
    }

    override fun loadPlaylist(): Playlist? = playlist

    override fun savePlaylist(playlist: Playlist) {
        this.playlist = playlist
    }

    override fun loadMediaBitmap(mediaId: String, fileName: String): ImageBitmap? =
        availableBitmaps[mediaId]
}

class NoOpLogger : AppLogger {
    override fun info(message: String) {}
    override fun warn(message: String, throwable: Throwable?) {}
    override fun error(message: String, throwable: Throwable?) {}
    override fun debug(message: String) {}
}
