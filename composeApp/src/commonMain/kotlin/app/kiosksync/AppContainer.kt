package app.kiosksync

import app.kiosksync.core.AppArgs
import app.kiosksync.core.AppLogger

/**
 * Manual Dependency Injection container holding global application configuration,
 * arguments, and core services.
 */
class AppContainer(
    val args: AppArgs,
    val dataDirPath: String,
    val logger: AppLogger
)
