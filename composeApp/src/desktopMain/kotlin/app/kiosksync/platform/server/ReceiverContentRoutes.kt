package app.kiosksync.platform.server

import app.kiosksync.core.ErrorResponse
import app.kiosksync.core.HEADER_CONTENT_SHA256
import app.kiosksync.core.ImageFormat
import app.kiosksync.core.MAX_IMAGE_SIZE_BYTES
import app.kiosksync.core.MAX_DURATION_SEC
import app.kiosksync.core.MIN_DURATION_SEC
import app.kiosksync.core.MediaState
import app.kiosksync.core.MediaUploadResponse
import app.kiosksync.core.PlaylistApplyResponse
import app.kiosksync.core.SyncStateResponse
import app.kiosksync.core.isValidUnlockSequence
import app.kiosksync.core.PlaylistUpdateRequest
import app.kiosksync.platform.Sha256
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put

internal fun Route.registerReceiverContentRoutes(context: ReceiverRouteContext) {
    get("/sync/state") {
        if (!call.requireAuthorization(context)) return@get
        val currentPlaylist = context.playlistRepository.loadReceiverPlaylist()
        call.respond(
            SyncStateResponse(
                playlistVersion = currentPlaylist?.version ?: 0,
                media = context.mediaRepository.listStored().map { MediaState(it.mediaId, it.sha256) },
            ),
        )
    }

    put("/media/{mediaId}") {
        if (!call.requireAuthorization(context)) return@put
        val mediaId = call.parameters["mediaId"].orEmpty()
        if (!MEDIA_ID_REGEX.matches(mediaId)) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_media_id"))
            return@put
        }

        val expectedHash = call.request.headers[HEADER_CONTENT_SHA256]?.trim()?.lowercase()
        if (expectedHash.isNullOrBlank() || expectedHash.length != 64) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("missing_hash"))
            return@put
        }

        val declaredLength = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (declaredLength != null && declaredLength > MAX_IMAGE_SIZE_BYTES) {
            call.respond(HttpStatusCode.PayloadTooLarge, ErrorResponse("image_too_large"))
            return@put
        }

        val bytes = call.receiveOrNull<ByteArray>()
        if (bytes == null || bytes.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_body"))
            return@put
        }
        if (bytes.size.toLong() > MAX_IMAGE_SIZE_BYTES) {
            call.respond(HttpStatusCode.PayloadTooLarge, ErrorResponse("image_too_large"))
            return@put
        }

        val actualHash = Sha256.hex(bytes)
        if (!actualHash.equals(expectedHash, ignoreCase = true)) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("hash_mismatch"))
            return@put
        }
        val format = ImageFormat.detect(bytes)
        if (format == null) {
            call.respond(HttpStatusCode.UnsupportedMediaType, ErrorResponse("unsupported_format"))
            return@put
        }

        val stored = context.mediaRepository.writeReceived(mediaId, format, bytes)
        if (stored == null) {
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("storage_error"))
            return@put
        }

        context.logger.info("Received media '$mediaId' (${stored.sizeBytes} bytes, ${format.extension})")
        call.respond(MediaUploadResponse(stored.mediaId, stored.sha256, stored.sizeBytes))
    }

    put("/playlist") {
        if (!call.requireAuthorization(context)) return@put
        val request = call.receiveOrNull<PlaylistUpdateRequest>()
        val playlist = request?.playlist
        if (request == null || playlist == null || !playlist.isValidForReceiver()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_playlist"))
            return@put
        }

        val lock = request.lock
        if (lock != null && !lock.isValidForReceiver()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_lock"))
            return@put
        }

        val storedById = context.mediaRepository.listStored().associateBy { it.mediaId }
        val missing = playlist.items
            .distinctBy { it.mediaId }
            .filter { item ->
                storedById[item.mediaId]?.sha256?.equals(item.sha256, ignoreCase = true) != true
            }
            .map { it.mediaId }
        if (missing.isNotEmpty()) {
            context.logger.warn(
                "Rejected playlist version ${playlist.version}; missing media: ${missing.joinToString()}",
            )
            call.respond(
                HttpStatusCode.Conflict,
                PlaylistApplyResponse(
                    applied = false,
                    version = context.playlistRepository.loadReceiverPlaylist()?.version ?: 0,
                    missingMediaIds = missing,
                ),
            )
            return@put
        }

        context.playlistRepository.saveReceiverPlaylist(playlist)
        if (lock != null) {
            context.kioskLockRepository.save(lock)
            context.kioskLockManager.setConfig(lock)
            context.logger.info("Kiosk lock updated from the controller")
        }
        val referenced = playlist.items.map { it.mediaId }.toSet()
        storedById.values.filterNot { it.mediaId in referenced }
            .forEach { context.mediaRepository.delete(it.mediaId) }

        try {
            context.playlistBridge.apply(playlist)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            context.logger.error("Error notifying playback about the new playlist", error)
        }
        context.logger.info("Applied playlist version ${playlist.version} with ${playlist.items.size} items")
        call.respond(PlaylistApplyResponse(applied = true, version = playlist.version))
    }
}
