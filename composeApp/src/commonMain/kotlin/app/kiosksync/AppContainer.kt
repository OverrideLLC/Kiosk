package app.kiosksync

import app.kiosksync.core.AppArgs
import app.kiosksync.core.AppLogger
import app.kiosksync.feature.pairing.PairingManager
import app.kiosksync.feature.playback.ReceiverPlaylistBridge
import app.kiosksync.feature.playback.ReceiverStatusTracker
import app.kiosksync.platform.DeviceDiscovery
import app.kiosksync.platform.EmitterClient
import app.kiosksync.platform.LocalStorage
import app.kiosksync.platform.ReceiverServer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual Dependency Injection container holding global application configuration,
 * arguments, platform storage and the platform services used by one role at a time.
 *
 * All network, mDNS and server lifecycle work is dispatched away from the Compose
 * UI thread. [backgroundDispatcher] is used for blocking I/O and [serviceDispatcher]
 * is a serial dispatcher so start/stop operations can never overlap.
 */
class AppContainer(
    val args: AppArgs,
    val dataDirPath: String,
    val logger: AppLogger,
    val localStorage: LocalStorage,
    val deviceName: String,
    val pairingManager: PairingManager,
    val discovery: DeviceDiscovery,
    val receiverServer: ReceiverServer,
    val emitterClient: EmitterClient,
    val receiverStatusTracker: ReceiverStatusTracker,
    val receiverPlaylistBridge: ReceiverPlaylistBridge,
    val pickImageFiles: suspend () -> List<String>,
    val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val serviceDispatcher: CoroutineDispatcher = backgroundDispatcher
) {
    val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + serviceDispatcher)

    /**
     * Starts cleanup without blocking the caller. The window can close immediately;
     * the operating system releases the port if the process ends before cleanup.
     */
    fun shutdown() {
        serviceScope.launch {
            runCatching { discovery.unregisterService() }
            runCatching { discovery.stopDiscovery() }
            runCatching { receiverServer.stop() }
            runCatching { emitterClient.close() }
        }
    }
}
