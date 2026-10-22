package app.kiosksync.feature.playback.presentation

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.core.AppLogger
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.domain.PlaylistItem
import app.kiosksync.feature.playlist.domain.PlaylistRepository
import app.kiosksync.feature.playback.domain.ReceiverStatusTracker
import app.kiosksync.platform.MediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaybackUiState(
    val playlist: Playlist? = null,
    val currentIndex: Int = 0,
    val currentItem: PlaylistItem? = null,
    val currentBitmap: ImageBitmap? = null,
    val hasItems: Boolean = false
)

private data class PlaybackRestartRequest(
    val id: Long,
    val delayMs: Long
)

class PlaybackStateHolder(
    private val playlistRepository: PlaylistRepository,
    private val mediaRepository: MediaRepository,
    private val logger: AppLogger,
    private val scope: CoroutineScope,
    private val statusTracker: ReceiverStatusTracker? = null,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val loadFromStorage: Boolean = true
) {
    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var syncJob: Job? = null
    private var playlistJob: Job? = null
    private var nextRestartId = 0L
    private val activePlaylist = MutableStateFlow<Playlist?>(null)
    private val playbackStarted = MutableStateFlow(false)

    /**
     * Latest requested restart. The id makes every request distinct even when
     * the delay is the same, so a second synchronisation resets the countdown.
     * `collectLatest` cancels the previous countdown when a newer one arrives.
     */
    private val restartRequests = MutableStateFlow<PlaybackRestartRequest?>(null)

    fun start() {
        playbackStarted.value = true
        restartRequests.value = null

        playlistJob?.cancel()
        if (loadFromStorage) {
            playlistJob = scope.launch {
                playlistRepository.observeReceiverPlaylist()
                    .catch { error ->
                        if (error is CancellationException) throw error
                        logger.error("Could not observe the Receiver playlist", error)
                    }
                    .collect { playlist ->
                        activePlaylist.value = playlist
                        _uiState.update { it.copy(playlist = playlist) }
                    }
            }
        }

        playbackJob?.cancel()
        playbackJob = scope.launch {
            runPlaybackLoop(startIndex = 0)
        }

        syncJob?.cancel()
        syncJob = scope.launch {
            restartRequests
                .filterNotNull()
                .collectLatest { request ->
                    delay(request.delayMs)
                    if (!playbackStarted.value) return@collectLatest
                    playbackJob?.cancelAndJoin()
                    playbackJob = scope.launch {
                        runPlaybackLoop(startIndex = 0)
                    }
                }
        }
    }

    fun stop() {
        playbackStarted.value = false
        restartRequests.value = null
        syncJob?.cancel()
        syncJob = null
        playlistJob?.cancel()
        playlistJob = null
        playbackJob?.cancel()
        playbackJob = null
    }

    /**
     * Schedules a restart of the carousel from its first image after [delayMs].
     * Returns false when this receiver has no content to restart yet.
     */
    fun scheduleRestart(delayMs: Long): Boolean {
        if (!playbackStarted.value) return false
        val playlist = activePlaylist.value ?: return false
        if (playlist.items.isEmpty()) return false
        val safeDelay = delayMs.coerceAtLeast(0L)
        nextRestartId += 1
        restartRequests.value = PlaybackRestartRequest(nextRestartId, safeDelay)
        logger.info("Playback restart scheduled in $safeDelay ms")
        return true
    }

    fun reloadPlaylist() {
        scope.launch {
            try {
                val loaded = withContext(backgroundDispatcher) {
                    playlistRepository.loadReceiverPlaylist()
                }
                activePlaylist.value = loaded
                _uiState.update { it.copy(playlist = loaded) }
                logger.info("Playlist reloaded: ${loaded?.items?.size ?: 0} items")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not reload the Receiver playlist", error)
            }
        }
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

    private suspend fun runPlaybackLoop(startIndex: Int = 0) {
        var index = startIndex.coerceAtLeast(0)

        while (scope.isActive) {
            val playlist = activePlaylist.value
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
                    mediaRepository.loadBitmap(candidateItem.mediaId, candidateItem.fileName)
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

}
