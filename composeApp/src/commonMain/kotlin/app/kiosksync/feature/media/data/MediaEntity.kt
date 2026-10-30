package app.kiosksync.feature.media.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val mediaId: String,
    val fileName: String,
    val sha256: String,
    val sizeBytes: Long
)
