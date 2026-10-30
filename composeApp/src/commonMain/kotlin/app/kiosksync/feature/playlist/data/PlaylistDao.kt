package app.kiosksync.feature.playlist.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE playlistKey = :playlistKey LIMIT 1")
    suspend fun getPlaylist(playlistKey: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE owner = :owner ORDER BY position")
    suspend fun getPlaylists(owner: String): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE owner = :owner ORDER BY position")
    fun observePlaylists(owner: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistKey = :playlistKey ORDER BY position")
    suspend fun getPlaylistItems(playlistKey: String): List<PlaylistItemEntity>

    @Query("SELECT * FROM playlist_items WHERE playlistKey = :playlistKey ORDER BY position")
    fun observePlaylistItems(playlistKey: String): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_items ORDER BY playlistKey, position")
    fun observeAllPlaylistItems(): Flow<List<PlaylistItemEntity>>

    @Upsert
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Upsert
    suspend fun upsertPlaylists(playlists: List<PlaylistEntity>)

    @Upsert
    suspend fun upsertPlaylistItems(items: List<PlaylistItemEntity>)

    @Query("DELETE FROM playlists WHERE playlistKey = :playlistKey")
    suspend fun deletePlaylist(playlistKey: String)

    @Query("DELETE FROM playlists WHERE owner = :owner")
    suspend fun deletePlaylists(owner: String)

    @Query("DELETE FROM playlist_items WHERE playlistKey = :playlistKey")
    suspend fun deletePlaylistItems(playlistKey: String)

    @Transaction
    suspend fun savePlaylist(playlist: PlaylistEntity, items: List<PlaylistItemEntity>) {
        upsertPlaylist(playlist)
        deletePlaylistItems(playlist.playlistKey)
        if (items.isNotEmpty()) upsertPlaylistItems(items)
    }

    @Transaction
    suspend fun savePlaylistLibrary(playlists: List<PlaylistEntity>, items: List<PlaylistItemEntity>) {
        deletePlaylists(OWNER_EMITTER)
        if (playlists.isNotEmpty()) upsertPlaylists(playlists)
        if (items.isNotEmpty()) upsertPlaylistItems(items)
    }

    companion object {
        const val OWNER_EMITTER = "emitter"
        const val OWNER_RECEIVER = "receiver"
        const val RECEIVER_PLAYLIST_KEY = "receiver-current"
    }
}
