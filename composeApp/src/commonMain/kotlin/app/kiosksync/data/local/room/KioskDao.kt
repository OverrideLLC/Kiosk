package app.kiosksync.data.local.room

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface KioskDao {
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

    @Query("DELETE FROM playlist_items WHERE playlistKey = :playlistKey")
    suspend fun deletePlaylistItems(playlistKey: String)

    @Query("SELECT * FROM paired_devices WHERE kind = :kind AND deviceId = :deviceId LIMIT 1")
    suspend fun getPairedDevice(kind: String, deviceId: String): PairedDeviceEntity?

    @Query("SELECT * FROM paired_devices WHERE kind = :kind ORDER BY deviceName COLLATE NOCASE")
    suspend fun getPairedDevices(kind: String): List<PairedDeviceEntity>

    @Upsert
    suspend fun upsertPairedDevice(device: PairedDeviceEntity)

    @Upsert
    suspend fun upsertPairedDevices(devices: List<PairedDeviceEntity>)

    @Transaction
    suspend fun replacePairedDevices(kind: String, devices: List<PairedDeviceEntity>) {
        deletePairedDevices(kind)
        if (devices.isNotEmpty()) upsertPairedDevices(devices)
    }

    @Query("DELETE FROM paired_devices WHERE kind = :kind")
    suspend fun deletePairedDevices(kind: String)

    @Query("DELETE FROM paired_devices WHERE kind = :kind AND deviceId = :deviceId")
    suspend fun deletePairedDevice(kind: String, deviceId: String)

    @Query("SELECT * FROM kiosk_lock WHERE id = :id LIMIT 1")
    suspend fun getKioskLock(id: Int): KioskLockEntity?

    @Query("SELECT * FROM kiosk_lock WHERE id = :id LIMIT 1")
    fun observeKioskLock(id: Int): Flow<KioskLockEntity?>

    @Upsert
    suspend fun upsertKioskLock(lock: KioskLockEntity)

    @Query("DELETE FROM kiosk_lock WHERE id = :id")
    suspend fun deleteKioskLock(id: Int)

    @Query("SELECT * FROM media ORDER BY mediaId")
    suspend fun getMedia(): List<MediaEntity>

    @Query("SELECT * FROM media WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getMedia(mediaId: String): MediaEntity?

    @Upsert
    suspend fun upsertMediaItem(media: MediaEntity)

    @Upsert
    suspend fun upsertMedia(media: List<MediaEntity>)

    @Query("DELETE FROM media WHERE mediaId = :mediaId")
    suspend fun deleteMedia(mediaId: String)

    @Query("DELETE FROM media")
    suspend fun deleteAllMedia()

    @Query("SELECT value FROM app_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadata(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: AppMetadataEntity)

    @Transaction
    suspend fun importLegacy(data: LegacyRoomData) {
        if (data.playlists.isNotEmpty()) upsertPlaylists(data.playlists)
        if (data.playlistItems.isNotEmpty()) upsertPlaylistItems(data.playlistItems)
        if (data.pairedDevices.isNotEmpty()) upsertPairedDevices(data.pairedDevices)
        data.kioskLock?.let { upsertKioskLock(it) }
        if (data.media.isNotEmpty()) upsertMedia(data.media)
    }

    @Transaction
    suspend fun completeLegacyMigration() {
        upsertMetadata(AppMetadataEntity(LEGACY_MIGRATION_KEY, LEGACY_MIGRATION_COMPLETE))
    }

    @Transaction
    suspend fun clearReceiverData() {
        deletePlaylists(OWNER_RECEIVER)
        deletePairedDevices(PAIR_KIND_RECEIVER)
        deleteKioskLock(KioskLockEntity.SINGLETON_ID)
        deleteAllMedia()
    }

    companion object {
        const val OWNER_EMITTER = "emitter"
        const val OWNER_RECEIVER = "receiver"
        const val PAIR_KIND_EMITTER = "emitter"
        const val PAIR_KIND_RECEIVER = "receiver"
        const val RECEIVER_PLAYLIST_KEY = "receiver-current"
        const val LEGACY_MIGRATION_KEY = "legacy-json-migration-v1"
        const val LEGACY_MIGRATION_COMPLETE = "complete"
    }
}

data class LegacyRoomData(
    val playlists: List<PlaylistEntity>,
    val playlistItems: List<PlaylistItemEntity>,
    val pairedDevices: List<PairedDeviceEntity>,
    val kioskLock: KioskLockEntity?,
    val media: List<MediaEntity>
)
