package app.kiosksync.feature.emitter

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import app.kiosksync.FakeRepositories
import app.kiosksync.NoOpLogger
import app.kiosksync.feature.emitter.presentation.actions.EmitterAction
import app.kiosksync.feature.emitter.presentation.screen.EmitterScreen
import app.kiosksync.feature.emitter.presentation.viewmodel.EmitterViewModel
import app.kiosksync.feature.guide.domain.GuideManager
import app.kiosksync.core.AppTheme
import app.kiosksync.core.DeviceInfo
import app.kiosksync.core.DiscoveredDevice
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.MediaUploadResponse
import app.kiosksync.core.PairResponse
import app.kiosksync.core.PlaylistApplyResponse
import app.kiosksync.core.ReceiverStatusResponse
import app.kiosksync.core.SyncStateResponse
import app.kiosksync.core.UpdateUploadResponse
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.presentation.PlaylistEditorStateHolder
import app.kiosksync.feature.playlist.domain.PlaylistItem
import app.kiosksync.feature.playlist.domain.PlaylistLibrary
import app.kiosksync.feature.projection.presentation.ProjectionController
import app.kiosksync.feature.pairing.domain.DeviceDiscovery
import app.kiosksync.feature.emitter.domain.EmitterClient
import java.io.File
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skia.EncodedImageFormat

/**
 * Offscreen snapshots of the controller in realistic states (empty and with
 * screens + playlist). They verify the responsive layout, the dialogs and the
 * bundled fonts/icons without opening a window.
 */
class ControllerRenderTest {

    @Test
    fun rendersControllerStates() {
        val output = File("build/reports/ui").apply { mkdirs() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val storage = FakeRepositories().apply {
                emitterPairedDevices = listOf(
                    EmitterPairedDevice(
                        deviceId = "rx-sala",
                        deviceName = "Pantalla sala",
                        token = "token",
                        host = "192.168.1.40",
                        port = 8765,
                    ),
                    EmitterPairedDevice(
                        deviceId = "rx-mostrador",
                        deviceName = "Mostrador",
                        token = "token",
                        host = "192.168.1.41",
                        port = 8765,
                    ),
                )
                playlistLibrary = PlaylistLibrary(
                    playlists = listOf(
                        Playlist(
                            id = "p1",
                            name = "Promociones",
                            version = 3,
                            defaultDurationSec = 10,
                            items = listOf(
                                PlaylistItem("m1", "ficha-verano.png", "h1"),
                                PlaylistItem("m2", "promo-2x1.png", "h2", 6),
                                PlaylistItem("m3", "horarios.png", "h3"),
                            ),
                        ),
                        Playlist(id = "p2", name = "Fin de semana", version = 1),
                    ),
                    activePlaylistId = "p1",
                )
            }
            val discovery = FakeDiscovery(
                listOf(
                    DiscoveredDevice("rx-sala", "Pantalla sala", "192.168.1.40", 8765, true),
                    DiscoveredDevice("rx-mostrador", "Mostrador", "192.168.1.41", 8765, true),
                    DiscoveredDevice("rx-bodega", "Bodega", "192.168.1.42", 8765, false),
                )
            )
            val holder = EmitterViewModel(
                discovery = discovery,
                client = FakeEmitterClient(),
                screensRepository = storage,
                kioskLockRepository = storage,
                syncRepository = storage,
                logger = NoOpLogger(),
                emitterId = "tx-1",
                emitterName = "Caja principal",
                scope = scope,
                serviceScope = scope,
                kioskLockManager = KioskLockManager(
                    logger = NoOpLogger(),
                    passwordHasher = { it },
                    scope = scope,
                    platformLockAvailable = false,
                ),
                projectionController = ProjectionController(),
                passwordHasher = { it },
                backgroundDispatcher = Dispatchers.Unconfined,
            )
            holder.start()
            holder.onAction(EmitterAction.ToggleSelection("rx-sala"))
            holder.onAction(EmitterAction.ToggleSelection("rx-mostrador"))

            val editor = PlaylistEditorStateHolder(
                playlistRepository = storage,
                mediaRepository = storage,
                logger = NoOpLogger(),
                scope = scope,
                newPlaylistId = { "new" },
            )
            val guideManager = GuideManager(NoOpLogger())

            render(output, "controller-dark", AppTheme.DARK, 1280, 800) {
                EmitterScreen(
                    state = holder.state.value,
                    onAction = holder::onAction,
                    playlistStateHolder = editor,
                    mediaRepository = storage,
                    guideManager = guideManager,
                    onExitRequested = {},
                    onRoleReset = {},
                    theme = AppTheme.DARK,
                )
            }

            render(output, "controller-light", AppTheme.LIGHT, 1280, 800) {
                EmitterScreen(
                    state = holder.state.value,
                    onAction = holder::onAction,
                    playlistStateHolder = editor,
                    mediaRepository = storage,
                    guideManager = guideManager,
                    onExitRequested = {},
                    onRoleReset = {},
                    theme = AppTheme.LIGHT,
                )
            }

            render(output, "controller-compact", AppTheme.DARK, 900, 650) {
                EmitterScreen(
                    state = holder.state.value,
                    onAction = holder::onAction,
                    playlistStateHolder = editor,
                    mediaRepository = storage,
                    guideManager = guideManager,
                    onExitRequested = {},
                    onRoleReset = {},
                    theme = AppTheme.DARK,
                )
            }

            render(output, "controller-fullhd", AppTheme.DARK, 1920, 1080) {
                EmitterScreen(
                    state = holder.state.value,
                    onAction = holder::onAction,
                    playlistStateHolder = editor,
                    mediaRepository = storage,
                    guideManager = guideManager,
                    onExitRequested = {},
                    onRoleReset = {},
                    theme = AppTheme.DARK,
                )
            }

            holder.onAction(EmitterAction.OpenSecurityDialog)
            render(output, "controller-security", AppTheme.DARK, 1280, 800) {
                EmitterScreen(
                    state = holder.state.value,
                    onAction = holder::onAction,
                    playlistStateHolder = editor,
                    mediaRepository = storage,
                    guideManager = guideManager,
                    onExitRequested = {},
                    onRoleReset = {},
                    theme = AppTheme.DARK,
                )
            }
            holder.onAction(EmitterAction.DismissSecurityDialog)

            val unpaired = holder.state.value.devices.first { !it.isPairedToThisEmitter }
            holder.onAction(EmitterAction.OpenPairDialog(unpaired))
            render(output, "controller-pair", AppTheme.DARK, 1280, 800) {
                EmitterScreen(
                    state = holder.state.value,
                    onAction = holder::onAction,
                    playlistStateHolder = editor,
                    mediaRepository = storage,
                    guideManager = guideManager,
                    onExitRequested = {},
                    onRoleReset = {},
                    theme = AppTheme.DARK,
                )
            }

            holder.stop()
        } finally {
            scope.cancel()
        }
    }

    private fun render(
        output: File,
        name: String,
        theme: AppTheme,
        width: Int,
        height: Int,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        val scene = ImageComposeScene(
            width = width,
            height = height,
            density = Density(1f),
        ) {
            KioskTheme(theme = theme, reduceMotion = true) {
                content()
            }
        }
        try {
            var nanos = 0L
            repeat(3) {
                scene.render(nanos)
                nanos += 250_000_000L
            }
            val image = scene.render(nanos)
            val png = requireNotNull(image.encodeToData(EncodedImageFormat.PNG)) {
                "Could not encode $name"
            }
            File(output, "$name.png").writeBytes(png.bytes)
        } finally {
            scene.close()
        }
    }
}

private class FakeDiscovery(devices: List<DiscoveredDevice>) : DeviceDiscovery {
    override val discoveredDevices = MutableStateFlow(devices)
    override fun startDiscovery() {}
    override fun stopDiscovery() {}
    override fun refresh() {}
    override fun registerService(deviceName: String, port: Int, deviceId: String, isPaired: Boolean) {}
    override fun unregisterService() {}
}

private class FakeEmitterClient : EmitterClient {
    override suspend fun getInfo(host: String, port: Int): Result<DeviceInfo> =
        Result.failure(UnsupportedOperationException())

    override suspend fun pair(
        host: String,
        port: Int,
        emitterId: String,
        emitterName: String,
        code: String,
    ): Result<PairResponse> = Result.failure(UnsupportedOperationException())

    override suspend fun unpair(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
    ): Result<Boolean> = Result.success(true)

    override suspend fun getStatus(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
    ): Result<ReceiverStatusResponse> = Result.success(ReceiverStatusResponse(playlistVersion = 3))

    override suspend fun synchronizePlayback(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        delayMs: Long,
    ): Result<Boolean> = Result.success(true)

    override suspend fun getSyncState(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
    ): Result<SyncStateResponse> = Result.success(SyncStateResponse())

    override suspend fun uploadMedia(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        mediaId: String,
        sha256: String,
        bytes: ByteArray,
    ): Result<MediaUploadResponse> = Result.failure(UnsupportedOperationException())

    override suspend fun uploadUpdate(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        version: String,
        sha256: String,
        bytes: ByteArray,
    ): Result<UpdateUploadResponse> = Result.failure(UnsupportedOperationException())

    override suspend fun putPlaylist(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        playlist: Playlist,
        lock: KioskLockConfig?,
    ): Result<PlaylistApplyResponse> = Result.success(PlaylistApplyResponse(applied = true))

    override suspend fun putLock(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        lock: KioskLockConfig,
    ): Result<Boolean> = Result.success(true)

    override fun close() {}
}
