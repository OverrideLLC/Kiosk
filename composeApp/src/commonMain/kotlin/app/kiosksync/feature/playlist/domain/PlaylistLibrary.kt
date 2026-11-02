package app.kiosksync.feature.playlist.domain

import kotlinx.serialization.Serializable

/**
 * Named playlists owned by the controller. Receivers store only the playlist
 * currently in playback.
 */
@Serializable
data class PlaylistLibrary(
    val playlists: List<Playlist> = emptyList(),
    val activePlaylistId: String? = null
) {
    fun activePlaylist(): Playlist? =
        playlists.firstOrNull { it.id == activePlaylistId } ?: playlists.firstOrNull()

    fun select(playlistId: String): PlaylistLibrary {
        if (playlists.none { it.id == playlistId }) return this
        return copy(activePlaylistId = playlistId)
    }

    fun upsert(playlist: Playlist): PlaylistLibrary {
        val existing = playlists.any { it.id == playlist.id }
        val updated = if (existing) {
            playlists.map { if (it.id == playlist.id) playlist else it }
        } else {
            playlists + playlist
        }
        return copy(playlists = updated)
    }

    fun remove(playlistId: String): PlaylistLibrary {
        val updated = playlists.filterNot { it.id == playlistId }
        if (updated.isEmpty()) return this
        val newActive = if (activePlaylistId == playlistId) updated.first().id else activePlaylistId
        return PlaylistLibrary(playlists = updated, activePlaylistId = newActive)
    }
}
