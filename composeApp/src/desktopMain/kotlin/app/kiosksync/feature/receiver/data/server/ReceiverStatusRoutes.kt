package app.kiosksync.feature.receiver.data.server

import app.kiosksync.core.ErrorResponse
import app.kiosksync.core.PlaybackSyncRequest
import app.kiosksync.core.PlaybackSyncResponse
import app.kiosksync.core.ReceiverStatusResponse
import app.kiosksync.core.APP_VERSION
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

internal fun Route.registerReceiverStatusRoutes(context: ReceiverRouteContext) {
    get("/status") {
        try {
            if (!call.requireAuthorization(context)) return@get
            call.respond(
                context.statusProvider().copy(
                    appVersion = APP_VERSION,
                    skippedUpdateVersion = context.updateManager.skippedVersion,
                ),
            )
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            context.logger.error("Error handling GET /status", error)
            try {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("status_error", error.message),
                )
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The connection may already be closed after the original failure.
            }
        }
    }

    post("/playback/sync") {
        if (!call.requireAuthorization(context)) return@post
        val request = call.receiveOrNull<PlaybackSyncRequest>()
        if (request == null || request.delayMs !in 0L..60_000L) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_sync_request"))
            return@post
        }
        if (!context.playbackSyncBridge.scheduleRestart(request.delayMs)) {
            call.respond(HttpStatusCode.Conflict, ErrorResponse("playback_not_ready"))
            return@post
        }
        context.logger.info("Playback synchronisation accepted with delay ${request.delayMs} ms")
        call.respond(PlaybackSyncResponse(accepted = true, delayMs = request.delayMs))
    }
}
