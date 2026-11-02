package app.kiosksync.core.common.http

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String? = null,
    val remainingAttempts: Int? = null,
    val remainingSeconds: Long? = null
)
