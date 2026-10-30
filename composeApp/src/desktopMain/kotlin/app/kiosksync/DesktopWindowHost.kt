package app.kiosksync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.AppRole
import app.kiosksync.feature.kiosk.domain.KioskKeyEvent
import app.kiosksync.feature.kiosk.domain.KioskKeyKind
import app.kiosksync.feature.kiosk.presentation.KioskAction
import app.kiosksync.feature.kiosk.presentation.KioskState
import app.kiosksync.feature.kiosk.presentation.KioskViewModel
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playback.presentation.PlaybackStateHolder
import app.kiosksync.feature.update.presentation.ReceiverUpdateViewModel
import app.kiosksync.ui.App
import app.kiosksync.ui.ProjectionScreen
import java.awt.Cursor
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage

@Composable
internal fun DesktopWindowHost(
    container: AppContainer,
    onExit: () -> Unit,
) {
    val args = container.args
    val scope = rememberCoroutineScope()
    val kioskViewModel = remember(container) {
        KioskViewModel(container.kioskLockManager, scope)
    }
    val receiverUpdateViewModel = remember(container, scope) {
        ReceiverUpdateViewModel(
            manager = container.receiverUpdateManager,
            scope = scope,
            onExit = {
                container.shutdown()
                onExit()
            },
        )
    }
    val activeRole by container.activeRole.collectAsState()
    val appFullscreen = !args.windowed
    val isReceiverWindow = activeRole == AppRole.RECEIVER
    val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
    val kioskLockAvailable = isWindows && !args.windowed

    DisposableEffect(container) {
        container.kioskKeyboardGuard.start()
        container.kioskMouseGuard.start()
        onDispose { container.shutdown() }
    }

    val windowState = rememberWindowState(
        placement = if (appFullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating,
        width = 900.dp,
        height = 650.dp,
    )
    val projectionPlaylist by container.projectionController.playlist.collectAsState()
    val lockState by kioskViewModel.state.collectAsState()
    val receiverUpdateState by receiverUpdateViewModel.state.collectAsState()
    val updatePromptVisible = isReceiverWindow && receiverUpdateState.isVisible

    LaunchedEffect(
        appFullscreen,
        windowState.placement,
        windowState.isMinimized,
        projectionPlaylist != null,
    ) {
        if (projectionPlaylist == null && appFullscreen) {
            if (windowState.isMinimized) windowState.isMinimized = false
            if (windowState.placement != WindowPlacement.Fullscreen) {
                windowState.placement = WindowPlacement.Fullscreen
            }
        } else if (!appFullscreen) {
            if (windowState.placement != WindowPlacement.Floating) {
                windowState.placement = WindowPlacement.Floating
            }
        }
    }

    LaunchedEffect(projectionPlaylist != null) {
        val isProjecting = projectionPlaylist != null
        windowState.isMinimized = isProjecting
        if (isProjecting) {
            kioskViewModel.onAction(KioskAction.Lock(force = true))
            container.logger.info("Projection active: controller locked")
        }
    }

    LaunchedEffect(lockState.isLocked, updatePromptVisible) {
        container.kioskMouseGuard.setLocked(lockState.isLocked && !updatePromptVisible)
        if (!lockState.isLocked && container.projectionController.isProjecting) {
            container.logger.info("Projection stopped after unlocking the controller")
            container.projectionController.stop()
        }
    }

    var escHandler: (() -> Unit)? = null
    var unpairHandler: (() -> Unit)? = null
    val feedLockFromCompose: (KeyEvent) -> Unit = { event ->
        if (!kioskLockAvailable && event.type == KeyEventType.KeyDown) {
            composeKeyToKioskEvent(event)?.let { kioskViewModel.onAction(KioskAction.KeyPressed(it)) }
        }
    }

    Window(
        onCloseRequest = {
            container.logger.info("Application close requested. Exiting.")
            container.shutdown()
            onExit()
        },
        title = APP_NAME,
        state = windowState,
        undecorated = appFullscreen,
        alwaysOnTop = isReceiverWindow && appFullscreen,
        resizable = !appFullscreen,
        onKeyEvent = { keyEvent ->
            when {
                kioskViewModel.state.value.isLocked -> {
                    feedLockFromCompose(keyEvent)
                    true
                }

                keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.isCtrlPressed &&
                    keyEvent.isShiftPressed &&
                    keyEvent.key == Key.P -> {
                    unpairHandler?.invoke()
                    true
                }

                keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyDown -> {
                    escHandler?.invoke()
                    true
                }

                else -> false
            }
        },
    ) {
        window.cursor = if (isReceiverWindow && lockState.isLocked && !updatePromptVisible) {
            blankCursor()
        } else {
            Cursor.getDefaultCursor()
        }
        App(
            container = container,
            kioskViewModel = kioskViewModel,
            receiverUpdateViewModel = receiverUpdateViewModel,
            onExitRequested = {
                container.shutdown()
                onExit()
            },
            registerEscHandler = { escHandler = it },
            registerUnpairHandler = { unpairHandler = it },
        )
    }

    projectionPlaylist?.let { playlist ->
        Window(
            onCloseRequest = { container.projectionController.stop() },
            title = "$APP_NAME · Proyección",
            state = rememberWindowState(placement = WindowPlacement.Fullscreen),
            undecorated = true,
            alwaysOnTop = true,
            resizable = false,
            onKeyEvent = { event ->
                if (kioskViewModel.state.value.isLocked) {
                    feedLockFromCompose(event)
                    true
                } else if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    container.projectionController.stop()
                    true
                } else {
                    false
                }
            },
        ) {
            window.cursor = blankCursor()
            ProjectionPlayback(
                playlist = playlist,
                container = container,
                lockState = lockState,
            )
        }
    }
}

@Composable
private fun ProjectionPlayback(
    playlist: Playlist,
    container: AppContainer,
    lockState: KioskState,
) {
    val scope = rememberCoroutineScope()
    val playbackStateHolder = remember(playlist.id, playlist.version, container) {
        PlaybackStateHolder(
            playlistRepository = container.playlistRepository,
            mediaRepository = container.mediaRepository,
            logger = container.logger,
            scope = scope,
            statusTracker = null,
            backgroundDispatcher = container.backgroundDispatcher,
            loadFromStorage = false,
        )
    }
    val playbackState by playbackStateHolder.state.collectAsState()

    LaunchedEffect(playbackStateHolder, playlist) {
        playbackStateHolder.updatePlaylist(playlist)
        playbackStateHolder.start()
    }
    DisposableEffect(playbackStateHolder) {
        onDispose { playbackStateHolder.stop() }
    }

    ProjectionScreen(state = playbackState, lockState = lockState)
}

private fun blankCursor() = Toolkit.getDefaultToolkit().createCustomCursor(
    BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
    Point(0, 0),
    "blank cursor",
)

private fun composeKeyToKioskEvent(event: KeyEvent): KioskKeyEvent? = when (event.key) {
    Key.ShiftLeft, Key.ShiftRight -> KioskKeyEvent(KioskKeyKind.SHIFT)
    Key.CtrlLeft, Key.CtrlRight -> KioskKeyEvent(KioskKeyKind.CTRL)
    Key.AltLeft, Key.AltRight -> KioskKeyEvent(KioskKeyKind.ALT)
    Key.Enter, Key.NumPadEnter -> KioskKeyEvent(KioskKeyKind.ENTER)
    Key.Backspace -> KioskKeyEvent(KioskKeyKind.BACKSPACE)
    else -> {
        val codePoint = event.utf16CodePoint
        if (codePoint in 33..126) KioskKeyEvent(KioskKeyKind.CHARACTER, codePoint.toChar())
        else KioskKeyEvent(KioskKeyKind.OTHER)
    }
}

