package app.kiosksync

import androidx.compose.ui.window.application
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.AppArgs
import app.kiosksync.core.DATA_DIR_NAME
import app.kiosksync.core.Strings
import app.kiosksync.platform.FileAppLogger
import java.io.File

fun main(args: Array<String>) {
    val appArgs = AppArgs.parse(args)
    val dataDir = appArgs.dataDir?.let(::File) ?: File(
        System.getenv("APPDATA") ?: System.getProperty("user.home"),
        DATA_DIR_NAME,
    )
    val logger = FileAppLogger(File(dataDir, "logs").apply { mkdirs() })
    installUncaughtExceptionLogger(logger, args)

    logger.info("==========================================")
    logger.info("Starting $APP_NAME")
    logger.info("CLI args: ${args.joinToString(" ")}")
    logger.info(
        "Parsed: dataDir=${appArgs.dataDir}, role=${appArgs.role}, " +
            "port=${appArgs.port}, windowed=${appArgs.windowed}",
    )
    logger.info("Effective data directory: ${dataDir.absolutePath}")

    application {
        if (appArgs.uiCatalog) {
            UiCatalogWindow(onExit = ::exitApplication)
        } else {
            DesktopApplicationRoot(
                appArgs = appArgs,
                dataDir = dataDir,
                logger = logger,
                onExit = ::exitApplication,
            )
        }
    }
}

private fun installUncaughtExceptionLogger(logger: FileAppLogger, args: Array<String>) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        logger.error("Uncaught exception on thread '${thread.name}'", throwable)
        previous?.uncaughtException(thread, throwable)
    }
}
