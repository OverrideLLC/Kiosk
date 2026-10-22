package app.kiosksync.platform

import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.AppLogger
import app.kiosksync.core.DeviceInfo
import app.kiosksync.core.ErrorResponse
import app.kiosksync.core.HEADER_AUTH_TOKEN
import app.kiosksync.core.HEADER_CONTENT_SHA256
import app.kiosksync.core.HEADER_EMITTER_ID
import app.kiosksync.core.ImageFormat
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.MAX_DURATION_SEC
import app.kiosksync.core.MAX_IMAGE_SIZE_BYTES
import app.kiosksync.core.MAX_RECOVERY_PHRASE_LENGTH
import app.kiosksync.core.MIN_DURATION_SEC
import app.kiosksync.core.MIN_RECOVERY_PHRASE_LENGTH
import app.kiosksync.core.MediaState
import app.kiosksync.core.MediaUploadResponse
import app.kiosksync.core.PairRequest
import app.kiosksync.core.PairResponse
import app.kiosksync.core.PlaybackSyncRequest
import app.kiosksync.core.PlaybackSyncResponse
import app.kiosksync.core.PlaylistApplyResponse
import app.kiosksync.core.ReceiverStatusResponse
import app.kiosksync.core.SyncStateResponse
import app.kiosksync.core.isValidUnlockSequence
import app.kiosksync.feature.pairing.PairResult
import app.kiosksync.feature.pairing.PairingManager
import app.kiosksync.feature.playback.ReceiverPlaybackSyncBridge
import app.kiosksync.feature.playback.ReceiverPlaylistBridge
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.sync.PlaylistUpdateRequest
import app.kiosksync.feature.kiosk.KioskLockManager
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

class KtorReceiverServer(
    private val pairingManager: PairingManager,
    private val logger: AppLogger,
    private val storage: LocalStorage,
    private val playlistBridge: ReceiverPlaylistBridge,
    private val playbackSyncBridge: ReceiverPlaybackSyncBridge,
    private val kioskLockManager: KioskLockManager,
    private val statusProvider: () -> ReceiverStatusResponse
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
                    install(ContentNegotiation) {
                        json(json)
                    }
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
        routing {
            get("/info") {
                call.respond(
                    DeviceInfo(
                        deviceId = deviceId,
                        deviceName = deviceName,
                        role = "receiver",
                        paired = pairingManager.uiState.value.isPaired,
                        appVersion = APP_VERSION
                    )
                )
            }

            post("/pair") {
                val request = runCatching { call.receive<PairRequest>() }.getOrNull()
                if (request == null || request.emitterId.isBlank() || request.code.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_request"))
                    return@post
                }

                when (val result = pairingManager.attemptPair(
                    emitterId = request.emitterId,
                    emitterName = request.emitterName,
                    code = request.code
                )) {
                    is PairResult.Success -> {
                        call.respond(
                            HttpStatusCode.OK,
                            PairResponse(token = result.token, receiverId = deviceId)
                        )
                    }

                    is PairResult.InvalidCode -> {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse(
                                error = "invalid_code",
                                remainingAttempts = result.remainingAttempts
                            )
                        )
                    }

                    is PairResult.LockedOut -> {
                        call.respond(
                            HttpStatusCode.TooManyRequests,
                            ErrorResponse(
                                error = "locked_out",
                                remainingSeconds = result.remainingSeconds
                            )
                        )
                    }

                    PairResult.AlreadyPaired -> {
                        call.respond(
                            HttpStatusCode.Conflict,
                            ErrorResponse("already_paired")
                        )
                    }
                }
            }

            delete("/pair") {
                if (!call.requireAuthorization()) return@delete
                pairingManager.unpair()
                call.respondText("ok", status = HttpStatusCode.OK)
            }

            get("/status") {
                try {
                    if (!call.requireAuthorization()) return@get
                    call.respond(statusProvider())
                } catch (error: Exception) {
                    logger.error("Error handling GET /status", error)
                    runCatching {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse("status_error", error.message)
                        )
                    }
                }
            }

            post("/playback/sync") {
                if (!call.requireAuthorization()) return@post
                val request = runCatching { call.receive<PlaybackSyncRequest>() }.getOrNull()
                if (request == null || request.delayMs !in 0L..60_000L) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_sync_request"))
                    return@post
                }
                if (!playbackSyncBridge.scheduleRestart(request.delayMs)) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("playback_not_ready"))
                    return@post
                }
                logger.info("Playback synchronisation accepted with delay ${request.delayMs} ms")
                call.respond(
                    PlaybackSyncResponse(
                        accepted = true,
                        delayMs = request.delayMs
                    )
                )
            }

            get("/sync/state") {
                if (!call.requireAuthorization()) return@get
                val currentPlaylist = storage.loadPlaylist()
                call.respond(
                    SyncStateResponse(
                        playlistVersion = currentPlaylist?.version ?: 0,
                        media = storage.listStoredMedia().map {
                            MediaState(mediaId = it.mediaId, sha256 = it.sha256)
                        }
                    )
                )
            }

            put("/media/{mediaId}") {
                if (!call.requireAuthorization()) return@put

                val mediaId = call.parameters["mediaId"].orEmpty()
                if (!MEDIA_ID_REGEX.matches(mediaId)) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_media_id"))
                    return@put
                }

                val expectedHash = call.request.headers[HEADER_CONTENT_SHA256]
                    ?.trim()
                    ?.lowercase()
                if (expectedHash.isNullOrBlank() || expectedHash.length != 64) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("missing_hash"))
                    return@put
                }

                val declaredLength = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                if (declaredLength != null && declaredLength > MAX_IMAGE_SIZE_BYTES) {
                    call.respond(
                        HttpStatusCode.PayloadTooLarge,
                        ErrorResponse("image_too_large")
                    )
                    return@put
                }

                val bytes = runCatching { call.receive<ByteArray>() }.getOrNull()
                if (bytes == null || bytes.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_body"))
                    return@put
                }
                if (bytes.size.toLong() > MAX_IMAGE_SIZE_BYTES) {
                    call.respond(
                        HttpStatusCode.PayloadTooLarge,
                        ErrorResponse("image_too_large")
                    )
                    return@put
                }

                val actualHash = Sha256.hex(bytes)
                if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("hash_mismatch"))
                    return@put
                }

                val format = ImageFormat.detect(bytes)
                if (format == null) {
                    call.respond(
                        HttpStatusCode.UnsupportedMediaType,
                        ErrorResponse("unsupported_format")
                    )
                    return@put
                }

                val stored = storage.writeReceivedMedia(mediaId, format, bytes)
                if (stored == null) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("storage_error")
                    )
                    return@put
                }

                logger.info("Received media '$mediaId' (${stored.sizeBytes} bytes, ${format.extension})")
                call.respond(
                    MediaUploadResponse(
                        mediaId = stored.mediaId,
                        sha256 = stored.sha256,
                        sizeBytes = stored.sizeBytes
                    )
                )
            }

            put("/playlist") {
                if (!call.requireAuthorization()) return@put

                val request = runCatching { call.receive<PlaylistUpdateRequest>() }.getOrNull()
                val playlist = request?.playlist
                if (request == null || playlist == null || !playlist.isValid()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_playlist"))
                    return@put
                }

                val lock = request.lock
                if (lock != null && !lock.isValid()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_lock"))
                    return@put
                }

                val storedById = storage.listStoredMedia().associateBy { it.mediaId }
                val missing = playlist.items
                    .distinctBy { it.mediaId }
                    .filter { item ->
                        storedById[item.mediaId]?.sha256?.equals(item.sha256, ignoreCase = true) != true
                    }
                    .map { it.mediaId }

                if (missing.isNotEmpty()) {
                    logger.warn("Rejected playlist version ${playlist.version}; missing media: ${missing.joinToString()}")
                    call.respond(
                        HttpStatusCode.Conflict,
                        PlaylistApplyResponse(
                            applied = false,
                            version = storage.loadPlaylist()?.version ?: 0,
                            missingMediaIds = missing
                        )
                    )
                    return@put
                }

                storage.savePlaylist(playlist)
                if (lock != null) {
                    storage.saveKioskLock(lock)
                    kioskLockManager.setConfig(lock)
                    logger.info("Kiosk lock updated from the controller")
                }
                val referenced = playlist.items.map { it.mediaId }.toSet()
                storedById.values
                    .filterNot { it.mediaId in referenced }
                    .forEach { storage.deleteStoredMedia(it.mediaId) }

                runCatching { playlistBridge.apply(playlist) }
                    .onFailure { logger.error("Error notifying playback about the new playlist", it) }

                logger.info("Applied playlist version ${playlist.version} with ${playlist.items.size} items")
                call.respond(
                    PlaylistApplyResponse(
                        applied = true,
                        version = playlist.version
                    )
                )
            }

            put("/lock") {
                if (!call.requireAuthorization()) return@put

                val lock = runCatching { call.receive<KioskLockConfig>() }.getOrNull()
                if (lock == null || !lock.isValid()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_lock"))
                    return@put
                }

                storage.saveKioskLock(lock)
                kioskLockManager.setConfig(lock)
                logger.info("Kiosk lock provisioned by the controller")
                call.respondText("ok", status = HttpStatusCode.OK)
            }
        }
    }

    private fun Playlist.isValid(): Boolean {
        if (version < 1) return false
        if (defaultDurationSec !in MIN_DURATION_SEC..MAX_DURATION_SEC) return false
        if (items.map { it.mediaId }.distinct().size != items.size) return false
        return items.all { item ->
            MEDIA_ID_REGEX.matches(item.mediaId) &&
                item.fileName.isNotBlank() &&
                item.fileName != "." &&
                item.fileName != ".." &&
                item.fileName.none { it in "/\\:*?\"<>|" || it.code < 32 } &&
                item.sha256.length == 64 &&
                (item.durationSec == null || item.durationSec in MIN_DURATION_SEC..MAX_DURATION_SEC)
        }
    }

    private fun KioskLockConfig.isValid(): Boolean {
        if (!enabled) return false
        if (!isValidUnlockSequence(unlockSequence)) return false

        // A configuration with no password is allowed: it only customizes the
        // unlock sequence while the screen is blank.
        if (passwordHash != null) {
            if (passwordLength !in 4..64) return false
            if (!passwordHash.isSha256Hex()) return false
        } else if (passwordLength != 0) {
            return false
        }

        if (recoveryHash != null) {
            if (recoveryLength !in MIN_RECOVERY_PHRASE_LENGTH..MAX_RECOVERY_PHRASE_LENGTH) return false
            if (!recoveryHash.isSha256Hex()) return false
        } else if (recoveryLength != 0) {
            return false
        }

        return true
    }

    private fun String.isSha256Hex(): Boolean =
        length == 64 && all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

    private suspend fun ApplicationCall.requireAuthorization(): Boolean {
        val emitterId = request.headers[HEADER_EMITTER_ID]
        val token = request.headers[HEADER_AUTH_TOKEN]
        if (pairingManager.isAuthorized(emitterId, token)) {
            return true
        }
        respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
        return false
    }

    companion object {
        private val MEDIA_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,100}$")
    }
}
