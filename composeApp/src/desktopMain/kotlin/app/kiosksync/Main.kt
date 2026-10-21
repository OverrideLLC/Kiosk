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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.AppArgs
import app.kiosksync.core.AppRole
import app.kiosksync.designsystem.catalog.CatalogScreen
import app.kiosksync.feature.kiosk.KioskKeyEvent
import app.kiosksync.feature.kiosk.KioskKeyKind
import app.kiosksync.feature.kiosk.KioskLockManager
import app.kiosksync.feature.pairing.PairingManager
import app.kiosksync.feature.playback.ReceiverPlaylistBridge
import app.kiosksync.feature.playback.ReceiverStatusTracker
import app.kiosksync.feature.projection.ProjectionController
import app.kiosksync.platform.FileAppLogger
import app.kiosksync.platform.FileLocalStorage
import app.kiosksync.platform.JmDnsDiscovery
import app.kiosksync.platform.KtorEmitterClient
import app.kiosksync.platform.KtorReceiverServer
import app.kiosksync.platform.Sha256
import app.kiosksync.platform.createKioskKeyboardGuard
import app.kiosksync.ui.App
import app.kiosksync.ui.ProjectionScreen
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
        APP_NAME
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
    val activeRole = appArgs.role ?: existingConfig?.role

    if (activeRole == AppRole.RECEIVER) {
        // A screen always starts blank: wipe any content, blocking password and
        // pairing left by a previous session so it never replays stale content.
        // The Controller role keeps all of its data untouched.
        localStorage.clearReceiverData()
    }

    val isFullscreenReceiver = !appArgs.windowed && activeRole == AppRole.RECEIVER
    val deviceName = try {
        System.getenv("COMPUTERNAME") ?: InetAddress.getLocalHost().hostName ?: "WindowsPC"
    } catch (_: Exception) {
        System.getenv("COMPUTERNAME") ?: "WindowsPC"
    }

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
    if (activeRole == AppRole.RECEIVER) {
        // The screen role is always a kiosk. `force` locks even on Linux or in
        // --windowed debug, where the native hook is disabled and Compose feeds
        // the key events instead.
        kioskLockManager.lock(force = true)
    }

    val projectionController = ProjectionController()
    val keyboardGuard = createKioskKeyboardGuard(
        enabled = kioskLockAvailable,
        isLocked = { kioskLockManager.uiState.value.isLocked },
        onKey = { kioskLockManager.onKey(it) },
        onProjectionToggle = { projectionController.requestToggle() },
        logger = logger
    )

    val statusTracker = ReceiverStatusTracker()
    val playlistBridge = ReceiverPlaylistBridge()
    val discovery = JmDnsDiscovery(logger)
    val receiverServer = KtorReceiverServer(
        pairingManager = pairingManager,
        logger = logger,
        storage = localStorage,
        playlistBridge = playlistBridge,
        kioskLockManager = kioskLockManager,
        statusProvider = { statusTracker.snapshot() }
    )
    val emitterClient = KtorEmitterClient(logger)
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
        Thread(runnable, "KioskSync-services").apply { isDaemon = true }
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
        emitterClient = emitterClient,
        receiverStatusTracker = statusTracker,
        receiverPlaylistBridge = playlistBridge,
        kioskLockManager = kioskLockManager,
        kioskKeyboardGuard = keyboardGuard,
        projectionController = projectionController,
        passwordHasher = passwordHasher,
        newDeviceId = { UUID.randomUUID().toString() },
        pickImageFiles = pickImageFiles,
        backgroundDispatcher = Dispatchers.IO,
        serviceDispatcher = serviceDispatcher
    )

    keyboardGuard.start()

    application {
        val windowState = rememberWindowState(
            placement = if (isFullscreenReceiver) WindowPlacement.Fullscreen else WindowPlacement.Floating,
            width = 900.dp,
            height = 650.dp
        )

        val projectionPlaylist by projectionController.playlist.collectAsState()
        val lockState by kioskLockManager.uiState.collectAsState()

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

        LaunchedEffect(lockState.isLocked) {
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
            undecorated = isFullscreenReceiver,
            alwaysOnTop = isFullscreenReceiver,
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
            if (isFullscreenReceiver) {
                window.cursor = blankCursor()
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
    val dialog = FileDialog(null as Frame?, "Agregar imágenes", FileDialog.LOAD)
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
