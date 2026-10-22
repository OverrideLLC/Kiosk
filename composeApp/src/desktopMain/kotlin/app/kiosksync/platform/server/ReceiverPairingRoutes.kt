package app.kiosksync.platform.server

import app.kiosksync.core.ErrorResponse
import app.kiosksync.core.PairRequest
import app.kiosksync.core.PairResponse
import app.kiosksync.feature.pairing.domain.PairResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post

internal fun Route.registerReceiverPairingRoutes(context: ReceiverRouteContext) {
    post("/pair") {
        val request = call.receiveOrNull<PairRequest>()
        if (request == null || request.emitterId.isBlank() || request.code.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_request"))
            return@post
        }

        when (val result = context.pairingManager.attemptPair(
            emitterId = request.emitterId,
            emitterName = request.emitterName,
            code = request.code,
        )) {
            is PairResult.Success -> call.respond(
                HttpStatusCode.OK,
                PairResponse(token = result.token, receiverId = context.deviceId()),
            )
            is PairResult.InvalidCode -> call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse("invalid_code", remainingAttempts = result.remainingAttempts),
            )
            is PairResult.LockedOut -> call.respond(
                HttpStatusCode.TooManyRequests,
                ErrorResponse("locked_out", remainingSeconds = result.remainingSeconds),
            )
            PairResult.AlreadyPaired -> call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse("already_paired"),
            )
        }
    }

    delete("/pair") {
        if (!call.requireAuthorization(context)) return@delete
        context.pairingManager.unpair()
        call.respondText("ok", status = HttpStatusCode.OK)
    }
}
