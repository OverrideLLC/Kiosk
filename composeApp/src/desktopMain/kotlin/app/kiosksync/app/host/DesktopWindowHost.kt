package app.kiosksync.app.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
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
import app.kiosksync.app.App
import app.kiosksync.app.di.AppShutdown
import app.kiosksync.app.di.rememberInjected
import app.kiosksync.app.di.AppSession
import app.kiosksync.app.navigation.session.LocalSessionViewModelStoreOwner
import app.kiosksync.app.navigation.session.sessionViewModel
import app.kiosksync.core.common.app.APP_NAME
import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.core.common.app.AppRole
import app.kiosksync.feature.kiosk.domain.KioskKeyboardGuard
import app.kiosksync.feature.kiosk.domain.KioskKeyEvent
import app.kiosksync.feature.kiosk.domain.KioskKeyKind
import app.kiosksync.feature.kiosk.domain.KioskMouseGuard
import app.kiosksync.feature.kiosk.presentation.KioskAction
import app.kiosksync.feature.kiosk.presentation.KioskState
import app.kiosksync.feature.kiosk.presentation.KioskViewModel
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playback.presentation.PlaybackHost
import app.kiosksync.feature.playback.presentation.PlaybackStateHolder
import app.kiosksync.feature.update.presentation.ReceiverUpdateHost
import app.kiosksync.feature.update.presentation.ReceiverUpdateViewModel
import app.kiosksync.feature.projection.domain.ProjectionController
import app.kiosksync.feature.projection.presentation.ProjectionScreen
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import java.awt.Cursor
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage

@Composable
internal fun DesktopWindowHost(
    onExit: () -> Unit,
) {
    val session = koinInject<AppSession>()
    val logger = koinInject<AppLogger>()
    val shutdown = koinInject<AppShutdown>()
    val keyboardGuard = koinInject<KioskKeyboardGuard>()
    val mouseGuard = koinInject<KioskMouseGuard>()
    val projectionController = koinInject<ProjectionController>()
    val args = session.args
    val scope = rememberCoroutineScope()
    val viewModelStore = remember { ViewModelStore() }
    DisposableEffect(viewModelStore) {
        onDispose { viewModelStore.clear() }
    }
    val viewModelOwner = remember(viewModelStore) {
        object : ViewModelStoreOwner {
            override val viewModelStore = viewModelStore
        }
    }
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides viewModelOwner,
        LocalSessionViewModelStoreOwner provides viewModelOwner,
    ) {
        DesktopWindow(
            session = session,
            logger = logger,
            shutdown = shutdown,
            keyboardGuard = keyboardGuard,
            mouseGuard = mouseGuard,
            projectionController = projectionController,
            onExit = onExit,
            scope = scope,
            viewModelOwner = viewModelOwner,
        )
    }
}

@Composable
private fun DesktopWindow(
    session: AppSession,
    logger: AppLogger,
    shutdown: AppShutdown,
    keyboardGuard: KioskKeyboardGuard,
    mouseGuard: KioskMouseGuard,
    projectionController: ProjectionController,
    onExit: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    viewModelOwner: ViewModelStoreOwner,
) {
    val args = session.args
    val kioskViewModel = sessionViewModel<KioskViewModel>()
    val receiverUpdateViewModel = rememberInjected<ReceiverUpdateViewModel>(scope) {
        parametersOf(
            ReceiverUpdateHost(
                scope = scope,
                onExit = {
                    shutdown.shutdown()
                    onExit()
                },
            ),
        )
    }
    val activeRole by session.activeRole.collectAsState()
    val appFullscreen = !args.windowed
    val isReceiverWindow = activeRole == AppRole.RECEIVER
    val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
    val kioskLockAvailable = isWindows && !args.windowed

    DisposableEffect(Unit) {
        keyboardGuard.start()
        mouseGuard.start()
        onDispose { shutdown.shutdown() }
    }

    val windowState = rememberWindowState(
        placement = if (appFullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating,
        width = 900.dp,
        height = 650.dp,
    )
    val projectionPlaylist by projectionController.playlist.collectAsState()
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
            logger.info("Projection active: controller locked")
        }
    }

    LaunchedEffect(lockState.isLocked, updatePromptVisible) {
        mouseGuard.setLocked(lockState.isLocked && !updatePromptVisible)
        if (!lockState.isLocked && projectionController.isProjecting) {
            logger.info("Projection stopped after unlocking the controller")
            projectionController.stop()
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
            logger.info("Application close requested. Exiting.")
            shutdown.shutdown()
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
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides viewModelOwner,
            LocalSessionViewModelStoreOwner provides viewModelOwner,
        ) {
            window.cursor = if (isReceiverWindow && lockState.isLocked && !updatePromptVisible) {
                blankCursor()
            } else {
                Cursor.getDefaultCursor()
            }
            App(
                kioskViewModel = kioskViewModel,
                receiverUpdateViewModel = receiverUpdateViewModel,
                onExitRequested = {
                    shutdown.shutdown()
                    onExit()
                },
                registerEscHandler = { escHandler = it },
                registerUnpairHandler = { unpairHandler = it },
            )
        }
    }

    projectionPlaylist?.let { playlist ->
        Window(
            onCloseRequest = { projectionController.stop() },
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
                    projectionController.stop()
                    true
                } else {
                    false
                }
            },
        ) {
            window.cursor = blankCursor()
            ProjectionPlayback(
                playlist = playlist,
                lockState = lockState,
            )
        }
    }
}

@Composable
private fun ProjectionPlayback(
    playlist: Playlist,
    lockState: KioskState,
) {
    val scope = rememberCoroutineScope()
    val koin = getKoin()
    val playbackStateHolder = remember(playlist.id, playlist.version) {
        koin.get<PlaybackStateHolder>(named("projectionPlayback")) {
            parametersOf(PlaybackHost(scope, trackStatus = false, loadFromStorage = false))
        }
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

