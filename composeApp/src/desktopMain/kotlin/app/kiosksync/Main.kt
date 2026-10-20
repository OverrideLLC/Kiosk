package app.kiosksync

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.AppArgs
import app.kiosksync.core.AppRole
import app.kiosksync.feature.pairing.PairingManager
import app.kiosksync.feature.playback.ReceiverPlaylistBridge
import app.kiosksync.feature.playback.ReceiverStatusTracker
import app.kiosksync.platform.FileAppLogger
import app.kiosksync.platform.FileLocalStorage
import app.kiosksync.platform.JmDnsDiscovery
import app.kiosksync.platform.KtorEmitterClient
import app.kiosksync.platform.KtorReceiverServer
import app.kiosksync.ui.App
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import java.net.InetAddress
import java.util.concurrent.Executors
import javax.swing.SwingUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

fun main(args: Array<String>) {
    val appArgs = AppArgs.parse(args)

    val defaultDataDir = File(
        System.getenv("APPDATA") ?: System.getProperty("user.home"),
        APP_NAME
    )
    val effectiveDataDir = appArgs.dataDir?.let { File(it) } ?: defaultDataDir
    val logsDir = File(effectiveDataDir, "logs").apply { mkdirs() }

    val logger = FileAppLogger(logsDir)
    logger.info("==========================================")
    logger.info("Starting $APP_NAME")
    logger.info("CLI args: ${args.joinToString(" ")}")
    logger.info("Parsed: dataDir=${appArgs.dataDir}, role=${appArgs.role}, port=${appArgs.port}, windowed=${appArgs.windowed}")
    logger.info("Effective data directory: ${effectiveDataDir.absolutePath}")
    logger.info("Logs directory: ${logsDir.absolutePath}")

    val localStorage = FileLocalStorage(effectiveDataDir, logger)
    val existingConfig = localStorage.loadConfig()
    val activeRole = appArgs.role ?: existingConfig?.role

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
    val statusTracker = ReceiverStatusTracker()
    val playlistBridge = ReceiverPlaylistBridge()
    val discovery = JmDnsDiscovery(logger)
    val receiverServer = KtorReceiverServer(
        pairingManager = pairingManager,
        logger = logger,
        storage = localStorage,
        playlistBridge = playlistBridge,
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
        pickImageFiles = pickImageFiles,
        backgroundDispatcher = Dispatchers.IO,
        serviceDispatcher = serviceDispatcher
    )

    application {
        val windowState = rememberWindowState(
            placement = if (isFullscreenReceiver) WindowPlacement.Fullscreen else WindowPlacement.Floating,
            width = 900.dp,
            height = 650.dp
        )

        var escHandler: (() -> Unit)? = null
        var unpairHandler: (() -> Unit)? = null

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
                // Hide cursor in fullscreen receiver mode
                window.cursor = Toolkit.getDefaultToolkit().createCustomCursor(
                    BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
                    Point(0, 0),
                    "blank cursor"
                )
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
