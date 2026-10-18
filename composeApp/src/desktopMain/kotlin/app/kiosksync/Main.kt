package app.kiosksync

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.AppArgs
import app.kiosksync.platform.FileAppLogger
import app.kiosksync.ui.App
import java.io.File

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

    val container = AppContainer(
        args = appArgs,
        dataDirPath = effectiveDataDir.absolutePath,
        logger = logger
    )

    application {
        val windowState = rememberWindowState(width = 800.dp, height = 600.dp)
        Window(
            onCloseRequest = {
                logger.info("Application close requested. Exiting.")
                exitApplication()
            },
            title = APP_NAME,
            state = windowState
        ) {
            App(container)
        }
    }
}
