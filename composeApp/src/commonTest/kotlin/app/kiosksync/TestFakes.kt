package app.kiosksync

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.core.AppLogger
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.ImageFormat
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.playlist.PlaylistLibrary
import app.kiosksync.feature.role.AppConfig
import app.kiosksync.platform.LocalStorage
import app.kiosksync.platform.MediaImportResult
import app.kiosksync.platform.ReceiverUpdateManager
import app.kiosksync.platform.ReceiverUpdateUi
import app.kiosksync.platform.StoredMedia
import app.kiosksync.platform.UpdatePackage
import app.kiosksync.platform.UpdatePackageProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeLocalStorage(
    var playlist: Playlist? = null,
    val availableBitmaps: MutableMap<String, ImageBitmap> = mutableMapOf(),
    private val hashProvider: (ByteArray) -> String = { "hash" }
) : LocalStorage {
    var savedConfig: AppConfig? = null
    var receiverPairedInfo: ReceiverPairedInfo? = null
    var emitterPairedDevices: List<EmitterPairedDevice> = emptyList()
    var clearConfigCalled = false
    var clearPairingCalled = false
    var clearReceiverDataCalled = false
    var loadMediaBitmapCount = 0
    var playlistLibrary: PlaylistLibrary? = null
    var kioskLock: KioskLockConfig? = null
    var clearKioskLockCalled = false
    val storedMedia: MutableMap<String, StoredMedia> = mutableMapOf()
    val mediaBytes: MutableMap<String, ByteArray> = mutableMapOf()

    override fun loadConfig(): AppConfig? = savedConfig

    override fun saveConfig(config: AppConfig) {
        savedConfig = config
    }

    override fun clearConfig() {
        clearConfigCalled = true
        savedConfig = null
        clearPairing()
        clearKioskLock()
    }

    override fun clearPairing() {
        clearPairingCalled = true
        receiverPairedInfo = null
        emitterPairedDevices = emptyList()
    }

    override fun clearReceiverData() {
        clearReceiverDataCalled = true
        playlist = null
        receiverPairedInfo = null
        kioskLock = null
        storedMedia.clear()
        mediaBytes.clear()
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

    override fun loadPlaylistLibrary(): PlaylistLibrary? = playlistLibrary

    override fun savePlaylistLibrary(library: PlaylistLibrary) {
        playlistLibrary = library
    }

    override fun loadKioskLock(): KioskLockConfig? = kioskLock

    override fun saveKioskLock(config: KioskLockConfig) {
        kioskLock = config
    }

    override fun clearKioskLock() {
        clearKioskLockCalled = true
        kioskLock = null
    }

    override fun loadMediaBitmap(mediaId: String, fileName: String): ImageBitmap? {
        loadMediaBitmapCount++
        return availableBitmaps[mediaId]
    }

    override fun importMedia(sourcePath: String): MediaImportResult =
        MediaImportResult.Failure(app.kiosksync.platform.MediaImportError.UNREADABLE)

    override fun getMediaBytes(mediaId: String, fileName: String): ByteArray? = mediaBytes[mediaId]

    override fun listStoredMedia(): List<StoredMedia> = storedMedia.values.toList()

    override fun writeReceivedMedia(mediaId: String, format: ImageFormat, bytes: ByteArray): StoredMedia? {
        val media = StoredMedia(
            mediaId = mediaId,
            fileName = "$mediaId.${format.extension}",
            sha256 = hashProvider(bytes),
            sizeBytes = bytes.size.toLong(),
        )
        storedMedia[mediaId] = media
        mediaBytes[mediaId] = bytes
        return media
    }

    override fun deleteStoredMedia(mediaId: String): Boolean {
        mediaBytes.remove(mediaId)
        return storedMedia.remove(mediaId) != null
    }
}

class FakeReceiverUpdateManager : ReceiverUpdateManager {
    private val state = MutableStateFlow(ReceiverUpdateUi())
    override val uiState: StateFlow<ReceiverUpdateUi> = state.asStateFlow()
    override var skippedVersion: String? = null

    var stagedVersion: String? = null
    var stagedBytes: ByteArray? = null
    var stageResult: Result<Boolean> = Result.success(false)
    var acceptResult: Boolean = true
    var declineCount: Int = 0

    override suspend fun stagePackage(
        version: String,
        sha256: String,
        bytes: ByteArray
    ): Result<Boolean> {
        stagedVersion = version
        stagedBytes = bytes
        return stageResult.onSuccess { forced ->
            state.value = ReceiverUpdateUi(
                version = version,
                forced = forced
            )
        }
    }

    override fun acceptUpdate(): Boolean {
        if (acceptResult) {
            state.value = state.value.copy(applying = true)
        }
        return acceptResult
    }

    override fun declineUpdate() {
        declineCount++
        state.value = ReceiverUpdateUi()
    }

    override fun dismissError() {
        state.value = state.value.copy(error = null)
    }
}

class FakeUpdatePackageProvider(
    private val update: UpdatePackage? = null
) : UpdatePackageProvider {
    override fun isAvailable(): Boolean = update != null

    override suspend fun getPackage(): Result<UpdatePackage> =
        update?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No package available"))
}

class NoOpLogger : AppLogger {
    override fun info(message: String) {}
    override fun warn(message: String, throwable: Throwable?) {}
    override fun error(message: String, throwable: Throwable?) {}
    override fun debug(message: String) {}
}
