package app.kiosksync.platform

import app.kiosksync.core.AppLogger
import app.kiosksync.core.DeviceInfo
import app.kiosksync.core.ErrorResponse
import app.kiosksync.core.HEADER_AUTH_TOKEN
import app.kiosksync.core.HEADER_EMITTER_ID
import app.kiosksync.core.HTTP_TIMEOUT_MS
import app.kiosksync.core.PairError
import app.kiosksync.core.PairRejectedException
import app.kiosksync.core.PairRequest
import app.kiosksync.core.PairResponse
import app.kiosksync.core.ReceiverStatusResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
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

    override fun close() {
        runCatching { client.close() }
    }

    private fun baseUrl(host: String, port: Int): String {
        val formattedHost = if (host.contains(':') && !host.startsWith("[")) "[$host]" else host
        return "http://$formattedHost:$port"
    }
}
