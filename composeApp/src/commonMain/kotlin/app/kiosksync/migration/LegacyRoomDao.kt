package app.kiosksync.migration

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import app.kiosksync.feature.kiosk.data.KioskLockEntity
import app.kiosksync.feature.media.data.MediaEntity
import app.kiosksync.feature.pairing.data.PairedDeviceEntity
import app.kiosksync.feature.playlist.data.PlaylistEntity
import app.kiosksync.feature.playlist.data.PlaylistItemEntity
import app.kiosksync.feature.role.data.AppMetadataDao
import app.kiosksync.feature.role.data.AppMetadataEntity

data class LegacyRoomData(
    val playlists: List<PlaylistEntity>,
    val playlistItems: List<PlaylistItemEntity>,
    val pairedDevices: List<PairedDeviceEntity>,
    val kioskLock: KioskLockEntity?,
    val media: List<MediaEntity>
)

@Dao
interface LegacyRoomDao {
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
        upsertMetadata(AppMetadataEntity(AppMetadataDao.LEGACY_MIGRATION_KEY, AppMetadataDao.LEGACY_MIGRATION_COMPLETE))
    }

    @Query("SELECT value FROM app_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadata(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: AppMetadataEntity)

    @Upsert
    suspend fun upsertPlaylists(playlists: List<PlaylistEntity>)

    @Upsert
    suspend fun upsertPlaylistItems(items: List<PlaylistItemEntity>)

    @Upsert
    suspend fun upsertPairedDevices(devices: List<PairedDeviceEntity>)

    @Upsert
    suspend fun upsertKioskLock(lock: KioskLockEntity)

    @Upsert
    suspend fun upsertMedia(media: List<MediaEntity>)
}
