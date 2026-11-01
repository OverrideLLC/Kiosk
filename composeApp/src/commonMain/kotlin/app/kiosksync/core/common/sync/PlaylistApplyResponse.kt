package app.kiosksync.core.common.sync

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistApplyResponse(
    val applied: Boolean,
    val version: Int = 0,
    val missingMediaIds: List<String> = emptyList()
)
