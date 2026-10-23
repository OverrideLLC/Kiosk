package app.kiosksync

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.AppArgs
import app.kiosksync.core.AppRole
import app.kiosksync.core.DATA_DIR_NAME
import app.kiosksync.core.Strings
import app.kiosksync.designsystem.catalog.CatalogScreen
import app.kiosksync.feature.kiosk.KioskKeyEvent
import app.kiosksync.feature.kiosk.KioskKeyKind
import app.kiosksync.feature.kiosk.KioskLockManager
import app.kiosksync.feature.pairing.PairingManager
import app.kiosksync.feature.playback.ReceiverPlaybackSyncBridge
import app.kiosksync.feature.playback.ReceiverPlaylistBridge
import app.kiosksync.feature.playback.ReceiverStatusTracker
import app.kiosksync.feature.projection.ProjectionController
import app.kiosksync.platform.FileAppLogger
import app.kiosksync.platform.FileLocalStorage
import app.kiosksync.platform.FileReceiverUpdateManager
import app.kiosksync.platform.JmDnsDiscovery
import app.kiosksync.platform.KtorEmitterClient
import app.kiosksync.platform.KtorReceiverServer
import app.kiosksync.platform.LocalUpdatePackageProvider
import app.kiosksync.platform.Sha256
import app.kiosksync.platform.createKioskKeyboardGuard
import app.kiosksync.platform.createKioskMouseGuard
import app.kiosksync.platform.detectLocalIpv4Address
import app.kiosksync.ui.App
import app.kiosksync.ui.ProjectionScreen
import java.awt.Cursor
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.Executors
import javax.swing.SwingUtilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

fun main(args: Array<String>) {
    val appArgs = AppArgs.parse(args)

    if (appArgs.uiCatalog) {
        launchUiCatalog()
        return
    }

    val defaultDataDir = File(
        System.getenv("APPDATA") ?: System.getProperty("user.home"),
        DATA_DIR_NAME
    )
    val effectiveDataDir = appArgs.dataDir?.let { File(it) } ?: defaultDataDir
    val logsDir = File(effectiveDataDir, "logs").apply { mkdirs() }

    val logger = FileAppLogger(logsDir)

    // Last-resort safety net: the app must never close silently because of an
    // unexpected exception in a coroutine, the AWT thread or a Ktor worker.
    val previousUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        logger.error("Uncaught exception on thread '${thread.name}'", throwable)
        previousUncaughtHandler?.uncaughtException(thread, throwable)
    }

    logger.info("==========================================")
    logger.info("Starting $APP_NAME")
    logger.info("CLI args: ${args.joinToString(" ")}")
    logger.info("Parsed: dataDir=${appArgs.dataDir}, role=${appArgs.role}, port=${appArgs.port}, windowed=${appArgs.windowed}")
    logger.info("Effective data directory: ${effectiveDataDir.absolutePath}")
    logger.info("Logs directory: ${logsDir.absolutePath}")

    val localStorage = FileLocalStorage(effectiveDataDir, logger)
    val existingConfig = localStorage.loadConfig()
    // The saved role is the default, and --role=<role> forces it. A first run
    // without either still shows the role picker.
    val activeRole = appArgs.role ?: existingConfig?.role
    val updateRestartMarker = File(effectiveDataDir, "update-restart.marker")
    val consumeUpdateRestartMarker: () -> Boolean = {
        if (updateRestartMarker.isFile) {
            logger.info("Update restart detected; preserving receiver pairing and content")
            updateRestartMarker.delete()
            true
        } else {
            false
        }
    }

    if (activeRole == AppRole.RECEIVER) {
        // The Screen starts blank, except on the first start after an update.
        // The Controller role keeps all of its data untouched.
        if (!consumeUpdateRestartMarker()) {
            localStorage.clearReceiverData()
        }
    }

    val deviceName = try {
        System.getenv("COMPUTERNAME") ?: InetAddress.getLocalHost().hostName ?: "WindowsPC"
    } catch (_: Exception) {
        System.getenv("COMPUTERNAME") ?: "WindowsPC"
    }
    val localIpAddress = detectLocalIpv4Address()
    logger.info("Detected local IPv4 address: ${localIpAddress ?: "none"}")

    val pairingManager = PairingManager(
        localStorage = localStorage,
        logger = logger,
        initializePairingMode = activeRole == AppRole.RECEIVER,
        loadStoredPairingOnInit = activeRole == AppRole.RECEIVER
    )
    val passwordHasher: (String) -> String = { input ->
        Sha256.hex(input.toByteArray(Charsets.UTF_8))
    }
    val isWindows = System.getProperty("os.name").orEmpty()
        .startsWith("Windows", ignoreCase = true)
    val kioskLockAvailable = isWindows && !appArgs.windowed
    val kioskLockManager = KioskLockManager(
        logger = logger,
        passwordHasher = passwordHasher,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        platformLockAvailable = kioskLockAvailable
    )
    kioskLockManager.setConfig(localStorage.loadKioskLock())
    // The lock is engaged from App.kt as soon as the Screen is paired, never at
    // startup: an unpaired Screen and the role picker must stay usable.

    val projectionController = ProjectionController()
    val keyboardGuard = createKioskKeyboardGuard(
        enabled = kioskLockAvailable,
        isLocked = { kioskLockManager.uiState.value.isLocked },
        onKey = { kioskLockManager.onKey(it) },
        onProjectionToggle = { projectionController.requestToggle() },
        logger = logger
    )
    val mouseGuard = createKioskMouseGuard(
        enabled = kioskLockAvailable,
        isLocked = { kioskLockManager.uiState.value.isLocked },
        logger = logger
    )

    val statusTracker = ReceiverStatusTracker()
    val playlistBridge = ReceiverPlaylistBridge()
    val playbackSyncBridge = ReceiverPlaybackSyncBridge()
    val discovery = JmDnsDiscovery(logger)
    val receiverUpdateManager = FileReceiverUpdateManager(
        dataDir = effectiveDataDir,
        logger = logger
    )
    val receiverServer = KtorReceiverServer(
        pairingManager = pairingManager,
        logger = logger,
        storage = localStorage,
        playlistBridge = playlistBridge,
        playbackSyncBridge = playbackSyncBridge,
        kioskLockManager = kioskLockManager,
        statusProvider = { statusTracker.snapshot() },
        updateManager = receiverUpdateManager
    )
    val emitterClient = KtorEmitterClient(logger)
    val updatePackageProvider = LocalUpdatePackageProvider(
        dataDir = effectiveDataDir,
        logger = logger
    )
    val pickImageFiles: suspend () -> List<String> = {
        withContext(Dispatchers.IO) {
            runCatching { showImagePicker() }
                .onFailure { logger.warn("Could not open the image picker: ${it.message}") }
                .getOrDefault(emptyList())
        }
    }

    // Server shutdown, JmDNS close and registration may block for hundreds of
    // milliseconds. A dedicated serial worker keeps them off the UI thread and
    // guarantees that a start never races with the previous stop.
    val serviceDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "OverrideKiosk-services").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    val container = AppContainer(
        args = appArgs,
        dataDirPath = effectiveDataDir.absolutePath,
        logger = logger,
        localStorage = localStorage,
        deviceName = deviceName,
        pairingManager = pairingManager,
        discovery = discovery,
        receiverServer = receiverServer,
        receiverUpdateManager = receiverUpdateManager,
        emitterClient = emitterClient,
        updatePackageProvider = updatePackageProvider,
        receiverStatusTracker = statusTracker,
        receiverPlaylistBridge = playlistBridge,
        receiverPlaybackSyncBridge = playbackSyncBridge,
        localIpAddress = localIpAddress,
        kioskLockManager = kioskLockManager,
        kioskKeyboardGuard = keyboardGuard,
        kioskMouseGuard = mouseGuard,
        projectionController = projectionController,
        passwordHasher = passwordHasher,
        newDeviceId = { UUID.randomUUID().toString() },
        pickImageFiles = pickImageFiles,
        consumeUpdateRestartMarker = consumeUpdateRestartMarker,
        backgroundDispatcher = Dispatchers.IO,
        serviceDispatcher = serviceDispatcher
    )

    keyboardGuard.start()
    mouseGuard.start()

    application {
        // The role picker starts in a normal window. When the Screen role is
        // chosen, App.kt reports it and this state turns the window into the
        // fullscreen, borderless kiosk used for playback.
        var receiverModeActive by remember {
            mutableStateOf(activeRole == AppRole.RECEIVER)
        }
        val receiverKioskWindow = receiverModeActive && !appArgs.windowed
        val windowState = rememberWindowState(
            placement = if (receiverKioskWindow) WindowPlacement.Fullscreen else WindowPlacement.Floating,
            width = 900.dp,
            height = 650.dp
        )

        LaunchedEffect(receiverKioskWindow) {
            windowState.placement = if (receiverKioskWindow) {
                WindowPlacement.Fullscreen
            } else {
                WindowPlacement.Floating
            }
        }

        val projectionPlaylist by projectionController.playlist.collectAsState()
        val lockState by kioskLockManager.uiState.collectAsState()
        val receiverUpdateState by receiverUpdateManager.uiState.collectAsState()
        // The update prompt needs a usable cursor and mouse, but the keyboard
        // remains locked: the screen is still a kiosk, only this decision is
        // temporarily clickable.
        val updatePromptVisible =
            receiverKioskWindow && receiverUpdateState.isVisible

        // The screen is a kiosk. If Windows ever minimizes or restores the
        // fullscreen window (remote session, shell shortcut, display change),
        // put it back while the lock is active. The native input guards should
        // prevent the user from doing this; this is a second barrier.
        LaunchedEffect(
            receiverKioskWindow,
            lockState.isLocked,
            windowState.placement,
            windowState.isMinimized
        ) {
            if (receiverKioskWindow && lockState.isLocked) {
                if (windowState.isMinimized) {
                    windowState.isMinimized = false
                }
                if (windowState.placement != WindowPlacement.Fullscreen) {
                    windowState.placement = WindowPlacement.Fullscreen
                }
            }
        }

        LaunchedEffect(projectionPlaylist != null) {
            val isProjecting = projectionPlaylist != null
            // Hide the management window while projecting so a mouse cannot
            // reach the admin actions behind the fullscreen projection.
            windowState.isMinimized = isProjecting
            if (isProjecting) {
                // The controller is a display too while projecting: lock it with
                // the same password as the screens. `force` also covers Linux and
                // --windowed debugging, where the native hook is disabled.
                kioskLockManager.lock(force = true)
                logger.info("Projection active: controller locked")
            }
        }

        LaunchedEffect(lockState.isLocked, updatePromptVisible) {
            // Capture/release the pointer exactly when the kiosk lock changes.
            // Releasing it here is what makes the screen options card usable
            // with the mouse or touchpad right after the unlock sequence. The
            // update prompt also releases it while keeping the keyboard locked.
            mouseGuard.setLocked(lockState.isLocked && !updatePromptVisible)
            if (!lockState.isLocked && projectionController.isProjecting) {
                logger.info("Projection stopped after unlocking the controller")
                projectionController.stop()
            }
        }

        var escHandler: (() -> Unit)? = null
        var unpairHandler: (() -> Unit)? = null

        // When the native hook is not available (Linux or --windowed debug),
        // Compose feeds the unlock detector so the lock stays usable.
        val feedLockFromCompose: (KeyEvent) -> Unit = { event ->
            if (!kioskLockAvailable && event.type == KeyEventType.KeyDown) {
                composeKeyToKioskEvent(event)?.let { kioskLockManager.onKey(it) }
            }
        }

        Window(
            onCloseRequest = {
                logger.info("Application close requested. Exiting.")
                container.shutdown()
                exitApplication()
            },
            title = APP_NAME,
            state = windowState,
            // AWT does not allow changing this flag once the frame is
            // displayable, so it stays false. Fullscreen placement is enough to
            // hide the decorations and fill the display.
            undecorated = false,
            alwaysOnTop = receiverKioskWindow,
            resizable = !receiverKioskWindow,
            onKeyEvent = { keyEvent ->
                when {
                    // The native hook already swallows keys on Windows. This is a
                    // second barrier so Compose cannot open dialogs while locked.
                    kioskLockManager.uiState.value.isLocked -> {
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
            }
        ) {
            // Hide the pointer only while the paired Screen is actually locked.
            // The role picker and the pairing code keep a visible cursor.
            window.cursor = if (
                receiverKioskWindow && lockState.isLocked && !updatePromptVisible
            ) {
                blankCursor()
            } else {
                Cursor.getDefaultCursor()
            }

            App(
                container = container,
                onExitRequested = {
                    container.shutdown()
                    exitApplication()
                },
                registerEscHandler = { handler ->
                    escHandler = handler
                },
                registerUnpairHandler = { handler ->
                    unpairHandler = handler
                },
                onReceiverModeChanged = { active ->
                    receiverModeActive = active
                }
            )
        }

        projectionPlaylist?.let { playlist ->
            Window(
                onCloseRequest = {
                    // Closing the projection is only possible after unlocking.
                    projectionController.stop()
                },
                title = "$APP_NAME · Proyección",
                state = rememberWindowState(placement = WindowPlacement.Fullscreen),
                undecorated = true,
                alwaysOnTop = true,
                resizable = false,
                onKeyEvent = { event ->
                    if (kioskLockManager.uiState.value.isLocked) {
                        feedLockFromCompose(event)
                        true
                    } else if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.Escape
                    ) {
                        projectionController.stop()
                        true
                    } else {
                        false
                    }
                }
            ) {
                window.cursor = blankCursor()
                ProjectionScreen(
                    playlist = playlist,
                    container = container,
                    lockState = lockState
                )
            }
        }
    }
}

private fun blankCursor() = Toolkit.getDefaultToolkit().createCustomCursor(
    BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
    Point(0, 0),
    "blank cursor"
)

/**
 * Translates a Compose key event into the pure unlock key model. Used only as
 * a fallback when the native Windows hook is not active.
 */
private fun composeKeyToKioskEvent(event: KeyEvent): KioskKeyEvent? =
    when (event.key) {
        Key.ShiftLeft, Key.ShiftRight -> KioskKeyEvent(KioskKeyKind.SHIFT)
        Key.CtrlLeft, Key.CtrlRight -> KioskKeyEvent(KioskKeyKind.CTRL)
        Key.AltLeft, Key.AltRight -> KioskKeyEvent(KioskKeyKind.ALT)
        Key.Enter, Key.NumPadEnter -> KioskKeyEvent(KioskKeyKind.ENTER)
        Key.Backspace -> KioskKeyEvent(KioskKeyKind.BACKSPACE)
        else -> {
            val codePoint = event.utf16CodePoint
            if (codePoint in 33..126) {
                KioskKeyEvent(KioskKeyKind.CHARACTER, codePoint.toChar())
            } else {
                KioskKeyEvent(KioskKeyKind.OTHER)
            }
        }
    }

private fun showImagePicker(): List<String> {
    // Never open the modal dialog inside Compose's FlushCoroutineDispatcher: the nested
    // event loop re-enters rendering and corrupts snapshot continuations.
    if (SwingUtilities.isEventDispatchThread()) {
        return showImagePickerOnEventDispatchThread()
    }

    var files: List<String> = emptyList()
    var failure: Throwable? = null
    SwingUtilities.invokeAndWait {
        try {
            files = showImagePickerOnEventDispatchThread()
        } catch (error: Throwable) {
            failure = error
        }
    }
    failure?.let { throw it }
    return files
}

private fun showImagePickerOnEventDispatchThread(): List<String> {
    val dialog = FileDialog(null as Frame?, Strings.ADD_IMAGES, FileDialog.LOAD)
    dialog.isMultipleMode = true
    dialog.file = "*.jpg;*.jpeg;*.png;*.webp"
    dialog.isVisible = true
    return dialog.files?.map { it.absolutePath }.orEmpty()
}

/**
 * Development-only component gallery. It needs no data directory, server or
 * logger, so it can be opened with `--ui-catalog` on any machine.
 */
private fun launchUiCatalog() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "$APP_NAME · Catálogo de UI",
            state = rememberWindowState(width = 1280.dp, height = 900.dp),
        ) {
            CatalogScreen()
        }
    }
}
