package app.kiosksync

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
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
import app.kiosksync.platform.FileAppLogger
import app.kiosksync.platform.FileLocalStorage
import app.kiosksync.ui.App
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import java.net.InetAddress

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

    val container = AppContainer(
        args = appArgs,
        dataDirPath = effectiveDataDir.absolutePath,
        logger = logger,
        localStorage = localStorage,
        deviceName = deviceName
    )

    application {
        val windowState = rememberWindowState(
            placement = if (isFullscreenReceiver) WindowPlacement.Fullscreen else WindowPlacement.Floating,
            width = 900.dp,
            height = 650.dp
        )

        var escHandler: (() -> Unit)? = null

        Window(
            onCloseRequest = {
                logger.info("Application close requested. Exiting.")
                exitApplication()
            },
            title = APP_NAME,
            state = windowState,
            undecorated = isFullscreenReceiver,
            alwaysOnTop = isFullscreenReceiver,
            onKeyEvent = { keyEvent ->
                if (keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyDown) {
                    escHandler?.invoke()
                    true
                } else {
                    false
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
                onExitRequested = ::exitApplication,
                registerEscHandler = { handler ->
                    escHandler = handler
                }
            )
        }
    }
}
