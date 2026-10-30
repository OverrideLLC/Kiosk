package app.kiosksync.feature.role.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey val key: String,
    val value: String
)
