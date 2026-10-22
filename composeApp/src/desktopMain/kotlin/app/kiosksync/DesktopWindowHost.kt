package app.kiosksync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val activeRole by container.activeRole.collectAsState()
    val isFullscreenReceiver = !args.windowed && activeRole == AppRole.RECEIVER
    val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
    val kioskLockAvailable = isWindows && !args.windowed

    DisposableEffect(container) {
        container.kioskKeyboardGuard.start()
        container.kioskMouseGuard.start()
        onDispose { container.shutdown() }
    }

    val windowState = rememberWindowState(
        placement = if (isFullscreenReceiver) WindowPlacement.Fullscreen else WindowPlacement.Floating,
        width = 900.dp,
        height = 650.dp,
    )
    val projectionPlaylist by container.projectionController.playlist.collectAsState()
    val lockState by container.kioskLockManager.uiState.collectAsState()
    val receiverUpdateState by container.receiverUpdateManager.uiState.collectAsState()
    val updatePromptVisible = isFullscreenReceiver && receiverUpdateState.isVisible

    LaunchedEffect(
        isFullscreenReceiver,
        lockState.isLocked,
        windowState.placement,
        windowState.isMinimized,
    ) {
        if (isFullscreenReceiver) {
            if (lockState.isLocked && windowState.isMinimized) windowState.isMinimized = false
            if (windowState.placement != WindowPlacement.Fullscreen) {
                windowState.placement = WindowPlacement.Fullscreen
            }
        } else {
            if (windowState.placement != WindowPlacement.Floating) {
                windowState.placement = WindowPlacement.Floating
            }
        }
    }

    LaunchedEffect(projectionPlaylist != null) {
        val isProjecting = projectionPlaylist != null
        windowState.isMinimized = isProjecting
        if (isProjecting) {
            container.kioskLockManager.lock(force = true)
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
            composeKeyToKioskEvent(event)?.let(container.kioskLockManager::onKey)
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
        undecorated = isFullscreenReceiver,
        alwaysOnTop = isFullscreenReceiver,
        resizable = !isFullscreenReceiver,
        onKeyEvent = { keyEvent ->
            when {
                container.kioskLockManager.uiState.value.isLocked -> {
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
        if (isFullscreenReceiver) {
            window.cursor = if (updatePromptVisible) Cursor.getDefaultCursor() else blankCursor()
        }
        App(
            container = container,
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
                if (container.kioskLockManager.uiState.value.isLocked) {
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
            ProjectionScreen(playlist, container, lockState)
        }
    }
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

