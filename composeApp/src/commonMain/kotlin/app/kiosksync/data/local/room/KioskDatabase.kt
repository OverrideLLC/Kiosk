package app.kiosksync.data.local.room

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

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
    abstract fun kioskDao(): KioskDao
}

@Suppress("KotlinNoActualForExpect")
expect object KioskDatabaseConstructor : RoomDatabaseConstructor<KioskDatabase>
