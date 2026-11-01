package app.kiosksync.core.common.sync

import kotlinx.serialization.Serializable

@Serializable
data class SyncStateResponse(
    val playlistVersion: Int = 0,
    val media: List<MediaState> = emptyList()
)
