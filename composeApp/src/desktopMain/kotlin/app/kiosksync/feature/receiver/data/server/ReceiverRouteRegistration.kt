package app.kiosksync.feature.receiver.data.server

import app.kiosksync.core.ErrorResponse
import app.kiosksync.core.HEADER_AUTH_TOKEN
import app.kiosksync.core.HEADER_EMITTER_ID
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.request.receive
import kotlinx.coroutines.CancellationException

internal fun Application.configureReceiverRoutes(context: ReceiverRouteContext) {
    routing {
        registerReceiverPublicRoutes(context)
        registerReceiverPairingRoutes(context)
        registerReceiverStatusRoutes(context)
        registerReceiverContentRoutes(context)
        registerReceiverUpdateRoutes(context)
    }
}

internal suspend fun ApplicationCall.requireAuthorization(context: ReceiverRouteContext): Boolean {
    val emitterId = request.headers[HEADER_EMITTER_ID]
    val token = request.headers[HEADER_AUTH_TOKEN]
    if (context.pairingManager.isAuthorized(emitterId, token)) return true
    respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
    return false
}

/** Treat malformed payloads as a client error while keeping request cancellation cooperative. */
internal suspend inline fun <reified T : Any> ApplicationCall.receiveOrNull(): T? = try {
    receive<T>()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

