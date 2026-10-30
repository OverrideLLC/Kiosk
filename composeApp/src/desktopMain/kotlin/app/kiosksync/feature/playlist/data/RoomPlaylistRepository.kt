package app.kiosksync.feature.playlist.data

import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.domain.PlaylistItem
import app.kiosksync.feature.playlist.domain.PlaylistLibrary
import app.kiosksync.feature.playlist.domain.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext



internal class RoomPlaylistRepository(private val dao: PlaylistDao) : PlaylistRepository {
    override fun observeReceiverPlaylist(): Flow<Playlist?> = combine(
        dao.observePlaylists(PlaylistDao.OWNER_RECEIVER),
        dao.observePlaylistItems(PlaylistDao.RECEIVER_PLAYLIST_KEY),
    ) { playlists, items ->
        playlists.firstOrNull { it.playlistKey == PlaylistDao.RECEIVER_PLAYLIST_KEY }
            ?.let { it.toPlaylist(items) }
    }

    override fun observeEmitterPlaylistLibrary(): Flow<PlaylistLibrary?> = combine(
        dao.observePlaylists(PlaylistDao.OWNER_EMITTER),
        dao.observeAllPlaylistItems(),
    ) { entities, items ->
        entities.toPlaylistLibrary(items)
    }

    override suspend fun loadReceiverPlaylist(): Playlist? = withContext(Dispatchers.IO) {
        dao.getPlaylist(PlaylistDao.RECEIVER_PLAYLIST_KEY)?.let { entity ->
            entity.toPlaylist(dao.getPlaylistItems(entity.playlistKey))
        }
    }

    override suspend fun saveReceiverPlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        val entity = playlist.toEntity(
            playlistKey = PlaylistDao.RECEIVER_PLAYLIST_KEY,
            owner = PlaylistDao.OWNER_RECEIVER,
            position = 0,
            active = true,
        )
        dao.savePlaylist(entity, playlist.toItemEntities(entity.playlistKey))
    }

    override suspend fun loadEmitterPlaylistLibrary(): PlaylistLibrary? = withContext(Dispatchers.IO) {
        val entities = dao.getPlaylists(PlaylistDao.OWNER_EMITTER)
        val rows = entities.flatMap { entity -> dao.getPlaylistItems(entity.playlistKey) }
        entities.toPlaylistLibrary(rows)
    }

    override suspend fun saveEmitterPlaylistLibrary(library: PlaylistLibrary) = withContext(Dispatchers.IO) {
        val activeId = library.activePlaylistId ?: library.playlists.firstOrNull()?.id
        val entities = library.playlists.mapIndexed { index, playlist ->
            playlist.toEntity(
                playlistKey = playlist.id,
                owner = PlaylistDao.OWNER_EMITTER,
                position = index,
                active = playlist.id == activeId,
            )
        }
        val items = library.playlists.flatMap { playlist -> playlist.toItemEntities(playlist.id) }
        dao.savePlaylistLibrary(entities, items)
    }

    private fun Playlist.toEntity(
        playlistKey: String,
        owner: String,
        position: Int,
        active: Boolean,
    ) = PlaylistEntity(
        playlistKey = playlistKey,
        owner = owner,
        domainId = id,
        name = name,
        version = version,
        defaultDurationSec = defaultDurationSec,
        position = position,
        active = active,
    )

    private fun Playlist.toItemEntities(playlistKey: String) = items.mapIndexed { index, item ->
        PlaylistItemEntity(playlistKey, index, item.mediaId, item.fileName, item.sha256, item.durationSec)
    }

    private fun PlaylistEntity.toPlaylist(rows: List<PlaylistItemEntity>) = Playlist(
        id = domainId,
        name = name,
        version = version,
        defaultDurationSec = defaultDurationSec,
        items = rows.asSequence()
            .filter { it.playlistKey == playlistKey }
            .sortedBy(PlaylistItemEntity::position)
            .map { PlaylistItem(it.mediaId, it.fileName, it.sha256, it.durationSec) }
            .toList(),
    )

    private fun List<PlaylistEntity>.toPlaylistLibrary(items: List<PlaylistItemEntity>): PlaylistLibrary? {
        if (isEmpty()) return null
        val playlists = sortedBy(PlaylistEntity::position).map { entity -> entity.toPlaylist(items) }
        val activeId = firstOrNull(PlaylistEntity::active)?.domainId ?: playlists.firstOrNull()?.id
        return PlaylistLibrary(playlists, activeId)
    }
}
