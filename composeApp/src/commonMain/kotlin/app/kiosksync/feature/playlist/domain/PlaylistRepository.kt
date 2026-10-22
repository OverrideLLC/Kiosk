package app.kiosksync.feature.playlist.domain

import kotlinx.coroutines.flow.Flow

/** Persistence contract for the Receiver playlist and the Emitter's playlist library. */
interface PlaylistRepository {
    fun observeReceiverPlaylist(): Flow<Playlist?>
    fun observeEmitterPlaylistLibrary(): Flow<PlaylistLibrary?>
    suspend fun loadReceiverPlaylist(): Playlist?
    suspend fun saveReceiverPlaylist(playlist: Playlist)
    suspend fun loadEmitterPlaylistLibrary(): PlaylistLibrary?
    suspend fun saveEmitterPlaylistLibrary(library: PlaylistLibrary)
}
