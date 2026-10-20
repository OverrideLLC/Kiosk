package app.kiosksync.feature.sync

import app.kiosksync.core.SyncStateResponse
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.playlist.PlaylistItem

data class SyncPlan(
    val uploads: List<PlaylistItem>,
    val mediaToDelete: List<String>,
    val unchangedCount: Int
) {
    val hasChanges: Boolean
        get() = uploads.isNotEmpty() || mediaToDelete.isNotEmpty()
}

/**
 * Compares the local playlist with the receiver's /sync/state response.
 * Only images whose mediaId is unknown or whose hash differs are uploaded.
 */
object SyncPlanner {
    fun plan(localPlaylist: Playlist, remoteState: SyncStateResponse): SyncPlan {
        val remoteById = remoteState.media.associateBy { it.mediaId }
        val seen = mutableSetOf<String>()

        val uploads = localPlaylist.items.filter { item ->
            if (!seen.add(item.mediaId)) {
                return@filter false
            }
            val remote = remoteById[item.mediaId]
            remote == null || !remote.sha256.equals(item.sha256, ignoreCase = true)
        }

        val localIds = localPlaylist.items.map { it.mediaId }.toSet()
        val mediaToDelete = remoteState.media
            .map { it.mediaId }
            .distinct()
            .filterNot { it in localIds }

        return SyncPlan(
            uploads = uploads,
            mediaToDelete = mediaToDelete,
            unchangedCount = (localPlaylist.items.size - uploads.size).coerceAtLeast(0)
        )
    }
}
