package app.kiosksync.platform.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val playlistKey: String,
    val owner: String,
    val domainId: String,
    val name: String,
    val version: Int,
    val defaultDurationSec: Int,
    val position: Int,
    val active: Boolean
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistKey", "position"],
    indices = [Index(value = ["mediaId"])],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["playlistKey"],
            childColumns = ["playlistKey"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistItemEntity(
    val playlistKey: String,
    val position: Int,
    val mediaId: String,
    val fileName: String,
    val sha256: String,
    val durationSec: Int?
)

@Entity(
    tableName = "paired_devices",
    primaryKeys = ["kind", "deviceId"]
)
data class PairedDeviceEntity(
    val kind: String,
    val deviceId: String,
    val deviceName: String,
    val tokenCiphertext: String,
    val host: String?,
    val port: Int?
)

@Entity(tableName = "kiosk_lock")
data class KioskLockEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val passwordHashCiphertext: String?,
    val passwordLength: Int,
    val enabled: Boolean,
    val unlockSequence: String,
    val recoveryHashCiphertext: String?,
    val recoveryLength: Int
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val mediaId: String,
    val fileName: String,
    val sha256: String,
    val sizeBytes: Long
)

@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey val key: String,
    val value: String
)
