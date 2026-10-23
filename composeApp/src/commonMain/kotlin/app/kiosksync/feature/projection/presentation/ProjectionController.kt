package app.kiosksync.feature.projection.presentation

import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.projection.domain.ProjectionService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Keeps the controller's projection state outside the Compose tree so the
 * window layer (Main.kt) and the emitter UI can share it.
 */
class ProjectionController : ProjectionService {
    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()

    private val _toggleRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val toggleRequests: SharedFlow<Unit> = _toggleRequests.asSharedFlow()

    override val isProjecting: Boolean
        get() = _playlist.value != null

    override fun start(playlist: Playlist) {
        _playlist.value = playlist
    }

    fun stop() {
        _playlist.value = null
    }

    fun requestToggle() {
        _toggleRequests.tryEmit(Unit)
    }
}
