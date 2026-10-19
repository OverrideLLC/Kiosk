package app.kiosksync.platform

import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.AppLogger
import app.kiosksync.core.DeviceInfo
import app.kiosksync.core.ErrorResponse
import app.kiosksync.core.HEADER_AUTH_TOKEN
import app.kiosksync.core.HEADER_EMITTER_ID
import app.kiosksync.core.PairRequest
import app.kiosksync.core.PairResponse
import app.kiosksync.core.ReceiverStatusResponse
import app.kiosksync.feature.pairing.PairResult
import app.kiosksync.feature.pairing.PairingManager
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
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

class KtorReceiverServer(
    private val pairingManager: PairingManager,
    private val logger: AppLogger,
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
                if (!call.requireAuthorization()) return@get
                call.respond(statusProvider())
            }
        }
    }

    private suspend fun ApplicationCall.requireAuthorization(): Boolean {
        val emitterId = request.headers[HEADER_EMITTER_ID]
        val token = request.headers[HEADER_AUTH_TOKEN]
        if (pairingManager.isAuthorized(emitterId, token)) {
            return true
        }
        respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
        return false
    }
}
