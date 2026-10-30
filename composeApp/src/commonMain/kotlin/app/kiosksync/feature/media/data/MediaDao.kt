package app.kiosksync.feature.media.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface MediaDao {
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
}
