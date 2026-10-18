package app.kiosksync

import app.kiosksync.core.AppArgs
import app.kiosksync.core.AppLogger
import app.kiosksync.platform.LocalStorage

/**
 * Manual Dependency Injection container holding global application configuration,
 * arguments, platform storage and core services.
 */
class AppContainer(
    val args: AppArgs,
    val dataDirPath: String,
    val logger: AppLogger,
    val localStorage: LocalStorage,
    val deviceName: String
)
