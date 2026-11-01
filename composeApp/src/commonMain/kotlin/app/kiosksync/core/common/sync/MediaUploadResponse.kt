package app.kiosksync.core.common.sync

import kotlinx.serialization.Serializable

@Serializable
data class MediaUploadResponse(
    val mediaId: String,
    val sha256: String,
    val sizeBytes: Long
)
