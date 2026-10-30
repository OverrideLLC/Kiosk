package app.kiosksync.feature.playlist.data

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
