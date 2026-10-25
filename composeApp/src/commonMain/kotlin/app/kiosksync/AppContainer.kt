package app.kiosksync

import app.kiosksync.core.AppArgs
import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppRole
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.kiosk.domain.KioskLockConfigurationUseCases
import app.kiosksync.feature.emitter.domain.EmitterScreensRepository
import app.kiosksync.feature.pairing.domain.PairingManager
import app.kiosksync.feature.playback.domain.ReceiverPlaybackSyncBridge
import app.kiosksync.feature.playback.domain.ReceiverPlaylistBridge
import app.kiosksync.feature.playback.domain.ReceiverStatusTracker
import app.kiosksync.feature.projection.presentation.ProjectionController
import app.kiosksync.feature.role.domain.AppConfig
import app.kiosksync.feature.role.domain.AppConfigStore
import app.kiosksync.feature.playlist.domain.PlaylistRepository
import app.kiosksync.feature.sync.domain.EmitterSyncRepository
import app.kiosksync.feature.pairing.domain.DeviceDiscovery
import app.kiosksync.feature.emitter.domain.EmitterClient
import app.kiosksync.feature.emitter.domain.EmitterService
import app.kiosksync.feature.kiosk.domain.KioskKeyboardGuard
import app.kiosksync.feature.kiosk.domain.KioskMouseGuard
import app.kiosksync.feature.media.domain.MediaRepository
import app.kiosksync.feature.receiver.domain.ReceiverDataResetter
import app.kiosksync.feature.receiver.domain.ReceiverServer
import app.kiosksync.feature.update.domain.ReceiverUpdateManager
import app.kiosksync.feature.update.domain.UpdatePackageProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val screensRepository: EmitterScreensRepository,
    val playlistRepository: PlaylistRepository,
    val kioskLockRepository: KioskLockRepository,
    val kioskLockConfigurationUseCases: KioskLockConfigurationUseCases,
    val mediaRepository: MediaRepository,
    val emitterSyncRepository: EmitterSyncRepository,
    val emitterService: EmitterService,
    val receiverDataResetter: ReceiverDataResetter,
    val configStore: AppConfigStore,
    val initialConfig: AppConfig?,
    val deviceName: String,
    val pairingManager: PairingManager,
    val discovery: DeviceDiscovery,
    val receiverServer: ReceiverServer,
    val receiverUpdateManager: ReceiverUpdateManager,
    val emitterClient: EmitterClient,
    val updatePackageProvider: UpdatePackageProvider,
    val receiverStatusTracker: ReceiverStatusTracker,
    val receiverPlaylistBridge: ReceiverPlaylistBridge,
    val receiverPlaybackSyncBridge: ReceiverPlaybackSyncBridge,
    val localIpAddress: String?,
    val kioskLockManager: KioskLockManager,
    val kioskKeyboardGuard: KioskKeyboardGuard,
    val kioskMouseGuard: KioskMouseGuard,
    val projectionController: ProjectionController,
    val passwordHasher: (String) -> String,
    val newDeviceId: () -> String,
    val pickImageFiles: suspend () -> List<String>,
    private val closePersistence: () -> Unit,
    private val closeServiceDispatcher: () -> Unit = {},
    val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val serviceDispatcher: CoroutineDispatcher = backgroundDispatcher,
    val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + serviceDispatcher),
) {
    private val shutdownStarted = MutableStateFlow(false)
    private val _activeRole = MutableStateFlow(args.role ?: initialConfig?.role)
    val activeRole: StateFlow<AppRole?> = _activeRole.asStateFlow()

    fun updateActiveRole(role: AppRole?) {
        _activeRole.value = role
    }

    /**
     * Starts cleanup without blocking the caller. The window can close immediately;
     * the operating system releases the port if the process ends before cleanup.
     */
    fun shutdown() {
        if (!shutdownStarted.compareAndSet(expect = false, update = true)) return
        runCatching { kioskKeyboardGuard.stop() }
        runCatching { kioskMouseGuard.stop() }
        serviceScope.launch {
            runCatching { emitterService.stop() }
                .onFailure { logger.warn("Could not stop Emitter service during shutdown", it) }
            runCatching { discovery.unregisterService() }
                .onFailure { logger.warn("Could not unregister mDNS service during shutdown", it) }
            runCatching { discovery.stopDiscovery() }
                .onFailure { logger.warn("Could not stop mDNS discovery during shutdown", it) }
            runCatching { receiverServer.stop() }
                .onFailure { logger.warn("Could not stop Receiver server during shutdown", it) }
            runCatching { emitterClient.close() }
                .onFailure { logger.warn("Could not close Emitter client during shutdown", it) }
            runCatching { configStore.close() }
                .onFailure { logger.warn("Could not close DataStore during shutdown", it) }
            runCatching { closePersistence() }
                .onFailure { logger.warn("Could not close Room during shutdown", it) }
            runCatching { closeServiceDispatcher() }
                .onFailure { logger.warn("Could not close service dispatcher during shutdown", it) }
        }
    }
}
