package app.kiosksync.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import app.kiosksync.feature.kiosk.data.KioskLockDao
import app.kiosksync.feature.kiosk.data.KioskLockEntity
import app.kiosksync.feature.media.data.MediaDao
import app.kiosksync.feature.media.data.MediaEntity
import app.kiosksync.feature.pairing.data.PairedDeviceDao
import app.kiosksync.feature.pairing.data.PairedDeviceEntity
import app.kiosksync.feature.playlist.data.PlaylistDao
import app.kiosksync.feature.playlist.data.PlaylistEntity
import app.kiosksync.feature.playlist.data.PlaylistItemEntity
import app.kiosksync.feature.receiver.data.ReceiverDataDao
import app.kiosksync.feature.role.data.AppMetadataDao
import app.kiosksync.feature.role.data.AppMetadataEntity
import app.kiosksync.migration.LegacyRoomDao

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        PairedDeviceEntity::class,
        KioskLockEntity::class,
        MediaEntity::class,
        AppMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(KioskDatabaseConstructor::class)
abstract class KioskDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun mediaDao(): MediaDao
    abstract fun pairedDeviceDao(): PairedDeviceDao
    abstract fun kioskLockDao(): KioskLockDao
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun legacyRoomDao(): LegacyRoomDao
    abstract fun receiverDataDao(): ReceiverDataDao
}

@Suppress("KotlinNoActualForExpect")
expect object KioskDatabaseConstructor : RoomDatabaseConstructor<KioskDatabase>
