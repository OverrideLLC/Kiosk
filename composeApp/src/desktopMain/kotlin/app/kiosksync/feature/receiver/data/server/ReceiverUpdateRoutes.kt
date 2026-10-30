package app.kiosksync.feature.receiver.data.server

import app.kiosksync.core.ErrorResponse
import app.kiosksync.core.HEADER_CONTENT_SHA256
import app.kiosksync.core.HEADER_AUTH_TOKEN
import app.kiosksync.core.HEADER_EMITTER_ID
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.MAX_UPDATE_PACKAGE_BYTES
import app.kiosksync.core.UpdateUploadResponse
import app.kiosksync.core.isValidAppVersion
import app.kiosksync.feature.receiver.data.server.isValidForReceiver
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.put

internal fun Route.registerReceiverUpdateRoutes(context: ReceiverRouteContext) {
    put("/update/{version}") {
        if (!call.requireAuthorization(context)) return@put
        val version = call.parameters["version"].orEmpty().trim()
        if (!isValidAppVersion(version)) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_version"))
            return@put
        }

        val expectedHash = call.request.headers[HEADER_CONTENT_SHA256]?.trim()?.lowercase()
        if (expectedHash.isNullOrBlank() || expectedHash.length != 64) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("missing_hash"))
            return@put
        }
        val declaredLength = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (declaredLength != null && declaredLength > MAX_UPDATE_PACKAGE_BYTES) {
            call.respond(HttpStatusCode.PayloadTooLarge, ErrorResponse("update_too_large"))
            return@put
        }
        val bytes = call.receiveOrNull<ByteArray>()
        if (bytes == null || bytes.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_body"))
            return@put
        }
        if (bytes.size.toLong() > MAX_UPDATE_PACKAGE_BYTES) {
            call.respond(HttpStatusCode.PayloadTooLarge, ErrorResponse("update_too_large"))
            return@put
        }

        val actualHash = app.kiosksync.core.security.Sha256.hex(bytes)
        if (!actualHash.equals(expectedHash, ignoreCase = true)) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("hash_mismatch"))
            return@put
        }

        context.updateManager.stagePackage(version, actualHash, bytes).fold(
            onSuccess = { forced ->
                context.logger.info("Update package $version staged (${bytes.size} bytes, forced=$forced)")
                call.respond(UpdateUploadResponse(accepted = true, version = version, forced = forced))
            },
            onFailure = { error ->
                context.logger.warn("Rejected update package $version: ${error.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("update_rejected", error.message),
                )
            },
        )
    }

    put("/lock") {
        if (!call.requireAuthorization(context)) return@put
        val lock = call.receiveOrNull<KioskLockConfig>()
        if (lock == null || !lock.isValidForReceiver()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_lock"))
            return@put
        }

        context.kioskLockRepository.save(lock)
        context.kioskLockManager.setConfig(lock)
        context.logger.info("Kiosk lock provisioned by the controller")
        call.respondText("ok", status = HttpStatusCode.OK)
    }
}
