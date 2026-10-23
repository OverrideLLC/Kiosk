package app.kiosksync.feature.projection.domain

import app.kiosksync.feature.playlist.domain.Playlist
import kotlinx.coroutines.flow.Flow

/** Projection operations exposed to the features that request local playback projection. */
interface ProjectionService {
    val toggleRequests: Flow<Unit>
    val isProjecting: Boolean

    fun start(playlist: Playlist)
}
