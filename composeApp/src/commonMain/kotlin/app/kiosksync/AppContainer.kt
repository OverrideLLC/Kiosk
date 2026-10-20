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

/**
 * Manual Dependency Injection container holding global application configuration,
 * arguments, platform storage and the platform services used by one role at a time.
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
    val pickImageFiles: suspend () -> List<String>
) {
    fun shutdown() {
        runCatching { discovery.unregisterService() }
        runCatching { discovery.stopDiscovery() }
        runCatching { receiverServer.stop() }
        runCatching { emitterClient.close() }
    }
}
