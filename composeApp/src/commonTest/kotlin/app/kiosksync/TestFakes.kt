package app.kiosksync

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.core.AppLogger
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.ImageFormat
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.feature.emitter.domain.EmitterScreensRepository
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.pairing.domain.PairingRepository
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.domain.PlaylistLibrary
import app.kiosksync.feature.playlist.domain.PlaylistRepository
import app.kiosksync.feature.sync.domain.EmitterSyncRepository
import app.kiosksync.feature.sync.domain.SyncProgress
import app.kiosksync.feature.sync.domain.SyncSummary
import app.kiosksync.platform.MediaImportError
import app.kiosksync.platform.MediaImportResult
import app.kiosksync.platform.MediaRepository
import app.kiosksync.platform.ReceiverDataResetter
import app.kiosksync.platform.ReceiverUpdateManager
import app.kiosksync.platform.ReceiverUpdateUi
import app.kiosksync.platform.StoredMedia
import app.kiosksync.platform.UpdatePackage
import app.kiosksync.platform.UpdatePackageProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeRepositories(
    initialPlaylist: Playlist? = null,
    val availableBitmaps: MutableMap<String, ImageBitmap> = mutableMapOf(),
    private val hashProvider: (ByteArray) -> String = { "hash" }
) : PairingRepository,
    EmitterScreensRepository,
    PlaylistRepository,
    KioskLockRepository,
    MediaRepository,
    EmitterSyncRepository,
    ReceiverDataResetter {
    var receiverPairedInfo: ReceiverPairedInfo? = null
    var emitterPairedDevices: List<EmitterPairedDevice> = emptyList()
    var clearPairingCalled = false
    var clearReceiverDataCalled = false
    var loadMediaBitmapCount = 0
    var clearKioskLockCalled = false
    val storedMedia: MutableMap<String, StoredMedia> = mutableMapOf()
    val mediaBytes: MutableMap<String, ByteArray> = mutableMapOf()

    private val receiverPlaylistState = MutableStateFlow(initialPlaylist)
    private val playlistLibraryState = MutableStateFlow<PlaylistLibrary?>(null)
    private val kioskLockState = MutableStateFlow<KioskLockConfig?>(null)

    var playlist: Playlist?
        get() = receiverPlaylistState.value
        set(value) { receiverPlaylistState.value = value }
    var playlistLibrary: PlaylistLibrary?
        get() = playlistLibraryState.value
        set(value) { playlistLibraryState.value = value }
    var kioskLock: KioskLockConfig?
        get() = kioskLockState.value
        set(value) { kioskLockState.value = value }

    override val config: Flow<KioskLockConfig?> = kioskLockState.asStateFlow()

    override suspend fun loadPairedEmitter(): ReceiverPairedInfo? = receiverPairedInfo
    override suspend fun savePairedEmitter(info: ReceiverPairedInfo) { receiverPairedInfo = info }
    override suspend fun clearPairedEmitter() {
        clearPairingCalled = true
        receiverPairedInfo = null
    }

    override suspend fun loadPairedScreens(): List<EmitterPairedDevice> = emitterPairedDevices
    override suspend fun savePairedScreens(devices: List<EmitterPairedDevice>) { emitterPairedDevices = devices }
    override suspend fun addPairedScreen(device: EmitterPairedDevice) {
        emitterPairedDevices = emitterPairedDevices.filterNot { it.deviceId == device.deviceId } + device
    }
    override suspend fun removePairedScreen(deviceId: String) {
        emitterPairedDevices = emitterPairedDevices.filterNot { it.deviceId == deviceId }
    }
    override suspend fun clearPairedScreens() {
        clearPairingCalled = true
        emitterPairedDevices = emptyList()
    }

    override fun observeReceiverPlaylist(): Flow<Playlist?> = receiverPlaylistState.asStateFlow()
    override fun observeEmitterPlaylistLibrary(): Flow<PlaylistLibrary?> = playlistLibraryState.asStateFlow()
    override suspend fun loadReceiverPlaylist(): Playlist? = playlist
    override suspend fun saveReceiverPlaylist(playlist: Playlist) { this.playlist = playlist }
    override suspend fun loadEmitterPlaylistLibrary(): PlaylistLibrary? = playlistLibrary
    override suspend fun saveEmitterPlaylistLibrary(library: PlaylistLibrary) { playlistLibrary = library }

    override suspend fun load(): KioskLockConfig? = kioskLock
    override suspend fun save(config: KioskLockConfig) { kioskLock = config }
    override suspend fun clearLock() {
        clearKioskLockCalled = true
        kioskLock = null
    }

    override suspend fun loadBitmap(mediaId: String, fileName: String): ImageBitmap? {
        loadMediaBitmapCount++
        return availableBitmaps[mediaId]
    }
    override suspend fun import(sourcePath: String): MediaImportResult =
        MediaImportResult.Failure(MediaImportError.UNREADABLE)
    override suspend fun getBytes(mediaId: String, fileName: String): ByteArray? = mediaBytes[mediaId]
    override suspend fun listStored(): List<StoredMedia> = storedMedia.values.toList()
    override suspend fun writeReceived(mediaId: String, format: ImageFormat, bytes: ByteArray): StoredMedia {
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
    override suspend fun delete(mediaId: String): Boolean {
        mediaBytes.remove(mediaId)
        return storedMedia.remove(mediaId) != null
    }
    override suspend fun clearMedia() {
        storedMedia.clear()
        mediaBytes.clear()
    }

    override suspend fun sync(
        device: EmitterPairedDevice,
        host: String,
        port: Int,
        emitterId: String,
        playlist: Playlist,
        lock: KioskLockConfig?,
        onUploadProgress: (SyncProgress) -> Unit,
        onApplying: () -> Unit,
    ): Result<SyncSummary> = Result.failure(UnsupportedOperationException("Sync not configured in test fake"))

    override suspend fun clearReceiverData() {
        clearReceiverDataCalled = true
        playlist = null
        receiverPairedInfo = null
        kioskLock = null
        clearMedia()
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
