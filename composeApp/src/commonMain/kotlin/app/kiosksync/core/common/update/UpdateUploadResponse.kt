package app.kiosksync.core.common.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUploadResponse(
    val accepted: Boolean,
    val version: String,
    val forced: Boolean = false
)
