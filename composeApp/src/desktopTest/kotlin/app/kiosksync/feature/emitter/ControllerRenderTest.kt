package app.kiosksync.feature.emitter

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import app.kiosksync.FakeRepositories
import app.kiosksync.FakeUpdatePackageProvider
import app.kiosksync.NoOpLogger
import app.kiosksync.feature.emitter.presentation.actions.EmitterAction
import app.kiosksync.feature.emitter.presentation.EmitterScreen
import app.kiosksync.feature.emitter.presentation.EmitterViewModel
import app.kiosksync.feature.emitter.data.EmitterServiceImpl
import app.kiosksync.feature.guide.domain.GuideManager
import app.kiosksync.core.AppTheme
import app.kiosksync.core.DiscoveredDevice
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.kiosk.domain.KioskLockConfigurationUseCases
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.presentation.PlaylistEditorStateHolder
import app.kiosksync.feature.playlist.presentation.PlaylistEditorDialogs
import app.kiosksync.feature.playlist.presentation.PlaylistPanel
import app.kiosksync.feature.playlist.domain.PlaylistItem
import app.kiosksync.feature.playlist.domain.PlaylistLibrary
import app.kiosksync.feature.projection.presentation.ProjectionController
import app.kiosksync.feature.pairing.domain.DeviceDiscovery
import app.kiosksync.feature.emitter.domain.EmitterClient
import app.kiosksync.feature.emitter.domain.ReceiverInfo
import app.kiosksync.feature.emitter.domain.ReceiverPairingCredential
import app.kiosksync.feature.emitter.domain.ReceiverStatus
import app.kiosksync.feature.emitter.domain.PlaylistApplyResult
import app.kiosksync.feature.emitter.domain.RemoteSyncState
import app.kiosksync.feature.emitter.domain.UpdateUploadResult
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
            val service = EmitterServiceImpl(
                discovery = discovery,
                client = FakeEmitterClient(),
                screensRepository = storage,
                kioskLockRepository = storage,
                syncRepository = storage,
                logger = NoOpLogger(),
                serviceScope = scope,
                backgroundDispatcher = Dispatchers.Unconfined,
                updatePackageProvider = FakeUpdatePackageProvider(),
            )
            val lockManager = KioskLockManager(
                logger = NoOpLogger(),
                passwordHasher = { it },
                scope = scope,
                platformLockAvailable = false,
            )
            val holder = EmitterViewModel(
                service = service,
                kioskLockConfigurationUseCases = KioskLockConfigurationUseCases(
                    repository = storage,
                    manager = lockManager,
                    passwordHasher = { it },
                ),
                logger = NoOpLogger(),
                emitterId = "tx-1",
                emitterName = "Caja principal",
                scope = scope,
                kioskLockManager = lockManager,
                projectionController = ProjectionController(),
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
                renderEmitterScreen(holder, editor, storage, guideManager, AppTheme.DARK)
            }

            render(output, "controller-light", AppTheme.LIGHT, 1280, 800) {
                renderEmitterScreen(holder, editor, storage, guideManager, AppTheme.LIGHT)
            }

            render(output, "controller-compact", AppTheme.DARK, 900, 650) {
                renderEmitterScreen(holder, editor, storage, guideManager, AppTheme.DARK)
            }

            render(output, "controller-fullhd", AppTheme.DARK, 1920, 1080) {
                renderEmitterScreen(holder, editor, storage, guideManager, AppTheme.DARK)
            }

            holder.onAction(EmitterAction.OpenSecurityDialog)
            render(output, "controller-security", AppTheme.DARK, 1280, 800) {
                renderEmitterScreen(holder, editor, storage, guideManager, AppTheme.DARK)
            }
            holder.onAction(EmitterAction.DismissSecurityDialog)

            val unpaired = holder.state.value.devices.first { !it.isPairedToThisEmitter }
            holder.onAction(EmitterAction.OpenPairDialog(unpaired))
            render(output, "controller-pair", AppTheme.DARK, 1280, 800) {
                renderEmitterScreen(holder, editor, storage, guideManager, AppTheme.DARK)
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

@Composable
private fun renderEmitterScreen(
    viewModel: EmitterViewModel,
    editor: PlaylistEditorStateHolder,
    storage: FakeRepositories,
    guideManager: GuideManager,
    theme: AppTheme,
) {
    val editorState = editor.uiState.collectAsState().value
    EmitterScreen(
        state = viewModel.state.value,
        onAction = viewModel::onAction,
        playlist = editorState.playlist,
        onAddImages = editor::addImages,
        playlistPanel = { modifier, fillHeight, onRequestRemove ->
            PlaylistPanel(
                stateHolder = editor,
                state = editorState,
                mediaRepository = storage,
                fillHeight = fillHeight,
                onRequestRemove = onRequestRemove,
                modifier = modifier,
            )
        },
        playlistDialogs = { removeTarget, onDismissRemove ->
            PlaylistEditorDialogs(
                stateHolder = editor,
                state = editorState,
                removeTarget = removeTarget,
                onDismissRemove = onDismissRemove,
            )
        },
        guideManager = guideManager,
        onExitRequested = {},
        onRoleReset = {},
        theme = theme,
    )
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
    override suspend fun getInfo(host: String, port: Int): Result<ReceiverInfo> =
        Result.failure(UnsupportedOperationException())

    override suspend fun pair(
        host: String,
        port: Int,
        emitterId: String,
        emitterName: String,
        code: String,
    ): Result<ReceiverPairingCredential> = Result.failure(UnsupportedOperationException())

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
    ): Result<ReceiverStatus> = Result.success(
        ReceiverStatus(appVersion = "1.0.0", skippedUpdateVersion = null)
    )

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
    ): Result<RemoteSyncState> = Result.success(RemoteSyncState(playlistVersion = 0, media = emptyList()))

    override suspend fun uploadMedia(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        mediaId: String,
        sha256: String,
        bytes: ByteArray,
    ): Result<Unit> = Result.failure(UnsupportedOperationException())

    override suspend fun uploadUpdate(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        version: String,
        sha256: String,
        bytes: ByteArray,
    ): Result<UpdateUploadResult> = Result.failure(UnsupportedOperationException())

    override suspend fun putPlaylist(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        playlist: Playlist,
        lock: KioskLockConfig?,
    ): Result<PlaylistApplyResult> = Result.success(
        PlaylistApplyResult(applied = true, version = 0, missingMediaIds = emptyList())
    )

    override suspend fun putLock(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        lock: KioskLockConfig,
    ): Result<Boolean> = Result.success(true)

    override fun close() {}
}
