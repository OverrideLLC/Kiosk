package app.kiosksync.feature.playback

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.core.AppLogger
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.playlist.PlaylistItem
import app.kiosksync.platform.LocalStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaybackUiState(
    val playlist: Playlist? = null,
    val currentIndex: Int = 0,
    val currentItem: PlaylistItem? = null,
    val currentBitmap: ImageBitmap? = null,
    val hasItems: Boolean = false,
    val isExitDialogOpen: Boolean = false
)

class PlaybackStateHolder(
    private val localStorage: LocalStorage,
    private val logger: AppLogger,
    private val scope: CoroutineScope,
    private val statusTracker: ReceiverStatusTracker? = null,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private val activePlaylist = MutableStateFlow<Playlist?>(null)

    fun start() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            runPlaybackLoop()
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
    }

    fun reloadPlaylist() {
        val loaded = localStorage.loadPlaylist()
        activePlaylist.value = loaded
        _uiState.update { it.copy(playlist = loaded) }
        logger.info("Playlist reloaded: ${loaded?.items?.size ?: 0} items")
    }

    fun updatePlaylist(newPlaylist: Playlist) {
        activePlaylist.value = newPlaylist
        _uiState.update { it.copy(playlist = newPlaylist) }
        statusTracker?.update(
            playlistVersion = newPlaylist.version,
            mediaId = _uiState.value.currentItem?.mediaId,
            fileName = _uiState.value.currentItem?.fileName
        )
        logger.info("Playlist updated dynamically to version ${newPlaylist.version}")
    }

    private suspend fun runPlaybackLoop() {
        activePlaylist.value = withContext(backgroundDispatcher) { localStorage.loadPlaylist() }
        var index = 0

        while (scope.isActive) {
            val playlist = activePlaylist.value
                ?: withContext(backgroundDispatcher) { localStorage.loadPlaylist() }
            if (playlist == null || playlist.items.isEmpty()) {
                _uiState.update {
                    it.copy(
                        playlist = null,
                        currentItem = null,
                        currentBitmap = null,
                        hasItems = false
                    )
                }
                statusTracker?.update(playlistVersion = 0, mediaId = null, fileName = null)
                delay(1000)
                continue
            }

            activePlaylist.value = playlist
            val items = playlist.items
            if (index >= items.size) {
                index = 0
            }

            val previousItem = _uiState.value.currentItem
            val previousBitmap = _uiState.value.currentBitmap
            var attempts = 0
            var loadedBitmap: ImageBitmap? = null
            var validItem: PlaylistItem? = null

            // Find next valid item, skipping corrupted or missing ones. Reuse the
            // bitmap already on screen when the same image content comes up again.
            while (attempts < items.size && loadedBitmap == null) {
                val candidateItem = items[index]
                val bitmap = if (
                    previousBitmap != null &&
                    previousItem != null &&
                    previousItem.sha256.equals(candidateItem.sha256, ignoreCase = true)
                ) {
                    previousBitmap
                } else {
                    withContext(backgroundDispatcher) {
                        localStorage.loadMediaBitmap(candidateItem.mediaId, candidateItem.fileName)
                    }
                }
                if (bitmap != null) {
                    loadedBitmap = bitmap
                    validItem = candidateItem
                } else {
                    logger.warn("Skipping unreadable item '${candidateItem.fileName}' (id: ${candidateItem.mediaId})")
                    index = (index + 1) % items.size
                    attempts++
                }
            }

            if (loadedBitmap != null && validItem != null) {
                _uiState.update {
                    it.copy(
                        playlist = playlist,
                        currentIndex = index,
                        currentItem = validItem,
                        currentBitmap = loadedBitmap,
                        hasItems = true
                    )
                }
                statusTracker?.update(
                    playlistVersion = playlist.version,
                    mediaId = validItem.mediaId,
                    fileName = validItem.fileName
                )

                val durationSec = playlist.getEffectiveDurationSec(validItem)
                logger.debug("Displaying image '${validItem.fileName}' for $durationSec seconds")

                try {
                    delay(durationSec * 1000L)
                } catch (_: CancellationException) {
                    break
                }

                // Advance to next index for subsequent cycle
                index = (index + 1) % items.size
            } else {
                // All items in playlist were unreadable or damaged
                _uiState.update {
                    it.copy(
                        playlist = playlist,
                        currentItem = null,
                        currentBitmap = null,
                        hasItems = false
                    )
                }
                statusTracker?.update(playlistVersion = playlist.version, mediaId = null, fileName = null)
                delay(3000)
            }
        }
    }

    fun requestExitDialog() {
        _uiState.update { it.copy(isExitDialogOpen = true) }
    }

    fun dismissExitDialog() {
        _uiState.update { it.copy(isExitDialogOpen = false) }
    }
}
