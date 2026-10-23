package app.kiosksync.feature.receiver.data

import app.kiosksync.core.AppLogger
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.pairing.domain.PairingManager
import app.kiosksync.feature.playback.domain.ReceiverPlaybackSyncBridge
import app.kiosksync.feature.playback.domain.ReceiverPlaylistBridge
import app.kiosksync.feature.playlist.domain.PlaylistRepository
import app.kiosksync.feature.media.domain.MediaRepository
import app.kiosksync.feature.receiver.domain.ReceiverServer
import app.kiosksync.feature.update.domain.ReceiverUpdateManager
import app.kiosksync.feature.receiver.data.server.ReceiverRouteContext
import app.kiosksync.feature.receiver.data.server.configureReceiverRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

class KtorReceiverServer(
    private val pairingManager: PairingManager,
    private val logger: AppLogger,
    private val playlistRepository: PlaylistRepository,
    private val mediaRepository: MediaRepository,
    private val kioskLockRepository: KioskLockRepository,
    private val playlistBridge: ReceiverPlaylistBridge,
    private val playbackSyncBridge: ReceiverPlaybackSyncBridge,
    private val kioskLockManager: KioskLockManager,
    private val statusProvider: () -> app.kiosksync.core.ReceiverStatusResponse,
    private val updateManager: ReceiverUpdateManager,
) : ReceiverServer {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private var server: EmbeddedServer<*, *>? = null
    private var deviceId: String = ""
    private var deviceName: String = ""

    override var boundPort: Int = 0
        private set

    @Synchronized
    override fun start(port: Int, deviceId: String, deviceName: String): Int {
        stop()
        this.deviceId = deviceId
        this.deviceName = deviceName

        var candidate = port.coerceAtLeast(1)
        var lastError: Exception? = null
        repeat(50) {
            try {
                val newServer = embeddedServer(CIO, port = candidate, host = "0.0.0.0") {
                    install(ContentNegotiation) { json(json) }
                    configureRoutes()
                }
                newServer.start(wait = false)
                server = newServer
                boundPort = candidate
                logger.info("Receiver server listening on port $candidate")
                return candidate
            } catch (error: Exception) {
                lastError = error
                logger.warn("Port $candidate is not available, trying the next one: ${error.message}")
                candidate++
            }
        }
        throw IllegalStateException("No available port found starting at $port", lastError)
    }

    @Synchronized
    override fun stop() {
        server?.let { running ->
            runCatching { running.stop(500, 1000) }
                .onFailure { logger.warn("Error stopping receiver server: ${it.message}") }
        }
        server = null
        boundPort = 0
    }

    private fun Application.configureRoutes() {
        configureReceiverRoutes(
            ReceiverRouteContext(
                pairingManager = pairingManager,
                logger = logger,
                playlistRepository = playlistRepository,
                mediaRepository = mediaRepository,
                kioskLockRepository = kioskLockRepository,
                playlistBridge = playlistBridge,
                playbackSyncBridge = playbackSyncBridge,
                kioskLockManager = kioskLockManager,
                statusProvider = statusProvider,
                updateManager = updateManager,
                deviceId = { deviceId },
                deviceName = { deviceName },
            ),
        )
    }
}
