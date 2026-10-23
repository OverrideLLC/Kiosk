package app.kiosksync.feature.emitter.data

import app.kiosksync.core.AppLogger
import app.kiosksync.feature.emitter.domain.EmitterClient
import app.kiosksync.core.DeviceInfo
import app.kiosksync.core.ErrorResponse
import app.kiosksync.core.HEADER_AUTH_TOKEN
import app.kiosksync.core.HEADER_CONTENT_SHA256
import app.kiosksync.core.HEADER_EMITTER_ID
import app.kiosksync.core.HTTP_TIMEOUT_MS
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.MediaUploadResponse
import app.kiosksync.core.PairError
import app.kiosksync.core.PairRejectedException
import app.kiosksync.core.PairRequest
import app.kiosksync.core.PairResponse
import app.kiosksync.core.PlaybackSyncRequest
import app.kiosksync.core.PlaylistApplyResponse
import app.kiosksync.core.PlaylistRejectedException
import app.kiosksync.core.ReceiverStatusResponse
import app.kiosksync.core.SYNC_TIMEOUT_MS
import app.kiosksync.core.SyncStateResponse
import app.kiosksync.core.UPDATE_UPLOAD_TIMEOUT_MS
import app.kiosksync.core.UPLOAD_TIMEOUT_MS
import app.kiosksync.core.UpdateUploadResponse
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.core.PlaylistUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.IOException

class KtorEmitterClient(
    private val logger: AppLogger
) : EmitterClient {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = HTTP_TIMEOUT_MS
            connectTimeoutMillis = HTTP_TIMEOUT_MS
            socketTimeoutMillis = HTTP_TIMEOUT_MS
        }
        expectSuccess = false
    }

    override suspend fun getInfo(host: String, port: Int): Result<DeviceInfo> = runCatching {
        val response = client.get("${baseUrl(host, port)}/info")
        if (response.status != HttpStatusCode.OK) {
            throw IOException("GET /info returned HTTP ${response.status}")
        }
        response.body<DeviceInfo>()
    }.onFailure {
        logger.warn("Could not read screen info from $host:$port: ${it.message}")
    }

    override suspend fun pair(
        host: String,
        port: Int,
        emitterId: String,
        emitterName: String,
        code: String
    ): Result<PairResponse> = runCatching {
        val response = client.post("${baseUrl(host, port)}/pair") {
            contentType(ContentType.Application.Json)
            setBody(PairRequest(emitterId = emitterId, emitterName = emitterName, code = code))
        }

        when (response.status) {
            HttpStatusCode.OK -> response.body<PairResponse>()

            HttpStatusCode.Conflict ->
                throw PairRejectedException(PairError.ALREADY_PAIRED)

            HttpStatusCode.Unauthorized -> {
                val error = runCatching { response.body<ErrorResponse>() }.getOrNull()
                throw PairRejectedException(
                    error = PairError.INVALID_CODE,
                    remainingAttempts = error?.remainingAttempts
                )
            }

            HttpStatusCode.TooManyRequests -> {
                val error = runCatching { response.body<ErrorResponse>() }.getOrNull()
                throw PairRejectedException(
                    error = PairError.LOCKED_OUT,
                    remainingSeconds = error?.remainingSeconds
                )
            }

            else -> throw IOException("POST /pair returned HTTP ${response.status}")
        }
    }

    override suspend fun unpair(
        host: String,
        port: Int,
        emitterId: String,
        token: String
    ): Result<Boolean> = runCatching {
        val response = client.delete("${baseUrl(host, port)}/pair") {
            header(HEADER_EMITTER_ID, emitterId)
            header(HEADER_AUTH_TOKEN, token)
        }
        if (!response.status.isSuccess()) {
            throw IOException("DELETE /pair returned HTTP ${response.status}")
        }
        true
    }

    override suspend fun getStatus(
        host: String,
        port: Int,
        emitterId: String,
        token: String
    ): Result<ReceiverStatusResponse> = runCatching {
        val response = client.get("${baseUrl(host, port)}/status") {
            header(HEADER_EMITTER_ID, emitterId)
            header(HEADER_AUTH_TOKEN, token)
        }
        if (response.status != HttpStatusCode.OK) {
            throw IOException("GET /status returned HTTP ${response.status}")
        }
        response.body<ReceiverStatusResponse>()
    }

    override suspend fun synchronizePlayback(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        delayMs: Long
    ): Result<Boolean> = runCatching {
        val response = client.post("${baseUrl(host, port)}/playback/sync") {
            header(HEADER_EMITTER_ID, emitterId)
            header(HEADER_AUTH_TOKEN, token)
            contentType(ContentType.Application.Json)
            setBody(PlaybackSyncRequest(delayMs = delayMs))
        }
        if (response.status != HttpStatusCode.OK) {
            throw IOException("POST /playback/sync returned HTTP ${response.status}")
        }
        true
    }

    override suspend fun getSyncState(
        host: String,
        port: Int,
        emitterId: String,
        token: String
    ): Result<SyncStateResponse> = runCatching {
        val response = client.get("${baseUrl(host, port)}/sync/state") {
            header(HEADER_EMITTER_ID, emitterId)
            header(HEADER_AUTH_TOKEN, token)
            timeout { requestTimeoutMillis = SYNC_TIMEOUT_MS }
        }
        if (response.status != HttpStatusCode.OK) {
            throw IOException("GET /sync/state returned HTTP ${response.status}")
        }
        response.body<SyncStateResponse>()
    }

    override suspend fun uploadMedia(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        mediaId: String,
        sha256: String,
        bytes: ByteArray
    ): Result<MediaUploadResponse> = runCatching {
        val response = client.put("${baseUrl(host, port)}/media/$mediaId") {
            header(HEADER_EMITTER_ID, emitterId)
            header(HEADER_AUTH_TOKEN, token)
            header(HEADER_CONTENT_SHA256, sha256)
            contentType(ContentType.Application.OctetStream)
            setBody(bytes)
            timeout {
                requestTimeoutMillis = UPLOAD_TIMEOUT_MS
                socketTimeoutMillis = UPLOAD_TIMEOUT_MS
            }
        }
        if (response.status != HttpStatusCode.OK) {
            throw IOException("PUT /media returned HTTP ${response.status}")
        }
        response.body<MediaUploadResponse>()
    }

    override suspend fun uploadUpdate(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        version: String,
        sha256: String,
        bytes: ByteArray
    ): Result<UpdateUploadResponse> = runCatching {
        val response = client.put("${baseUrl(host, port)}/update/$version") {
            header(HEADER_EMITTER_ID, emitterId)
            header(HEADER_AUTH_TOKEN, token)
            header(HEADER_CONTENT_SHA256, sha256)
            contentType(ContentType.Application.OctetStream)
            setBody(bytes)
            timeout {
                requestTimeoutMillis = UPDATE_UPLOAD_TIMEOUT_MS
                socketTimeoutMillis = UPDATE_UPLOAD_TIMEOUT_MS
            }
        }
        if (response.status != HttpStatusCode.OK) {
            throw IOException("PUT /update returned HTTP ${response.status}")
        }
        response.body<UpdateUploadResponse>()
    }.onFailure {
        logger.warn("Could not send update $version to $host:$port: ${it.message}")
    }

    override suspend fun putPlaylist(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        playlist: Playlist,
        lock: KioskLockConfig?
    ): Result<PlaylistApplyResponse> = runCatching {
        val response = client.put("${baseUrl(host, port)}/playlist") {
            header(HEADER_EMITTER_ID, emitterId)
            header(HEADER_AUTH_TOKEN, token)
            contentType(ContentType.Application.Json)
            setBody(PlaylistUpdateRequest(playlist = playlist, lock = lock))
            timeout { requestTimeoutMillis = SYNC_TIMEOUT_MS }
        }
        when (response.status) {
            HttpStatusCode.OK -> response.body<PlaylistApplyResponse>()

            HttpStatusCode.Conflict -> {
                val rejected = runCatching { response.body<PlaylistApplyResponse>() }.getOrNull()
                throw PlaylistRejectedException(rejected?.missingMediaIds.orEmpty())
            }

            else -> throw IOException("PUT /playlist returned HTTP ${response.status}")
        }
    }

    override suspend fun putLock(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        lock: KioskLockConfig
    ): Result<Boolean> = runCatching {
        val response = client.put("${baseUrl(host, port)}/lock") {
            header(HEADER_EMITTER_ID, emitterId)
            header(HEADER_AUTH_TOKEN, token)
            contentType(ContentType.Application.Json)
            setBody(lock)
        }
        if (!response.status.isSuccess()) {
            throw IOException("PUT /lock returned HTTP ${response.status}")
        }
        true
    }

    override fun close() {
        runCatching { client.close() }
    }

    private fun baseUrl(host: String, port: Int): String {
        val formattedHost = if (host.contains(':') && !host.startsWith("[")) "[$host]" else host
        return "http://$formattedHost:$port"
    }
}
