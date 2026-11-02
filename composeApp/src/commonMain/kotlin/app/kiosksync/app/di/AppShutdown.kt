package app.kiosksync.app.di

import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.feature.emitter.domain.EmitterClient
import app.kiosksync.feature.emitter.domain.EmitterService
import app.kiosksync.feature.kiosk.domain.KioskKeyboardGuard
import app.kiosksync.feature.kiosk.domain.KioskMouseGuard
import app.kiosksync.feature.pairing.domain.DeviceDiscovery
import app.kiosksync.feature.receiver.domain.ReceiverServer
import app.kiosksync.feature.role.domain.AppConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Closes hooks, network and persistence for this process.
 * Cleanup starts without blocking the caller so the window can close immediately.
 */
class AppShutdown(
    private val logger: AppLogger,
    private val keyboardGuard: KioskKeyboardGuard,
    private val mouseGuard: KioskMouseGuard,
    private val emitterService: EmitterService,
    private val discovery: DeviceDiscovery,
    private val receiverServer: ReceiverServer,
    private val emitterClient: EmitterClient,
    private val configStore: AppConfigStore,
    private val closePersistence: () -> Unit,
    private val closeServiceDispatcher: () -> Unit,
    private val serviceScope: CoroutineScope,
) {
    private val shutdownStarted = MutableStateFlow(false)

    fun shutdown() {
        if (!shutdownStarted.compareAndSet(expect = false, update = true)) return
        runCatching { keyboardGuard.stop() }
        runCatching { mouseGuard.stop() }
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
