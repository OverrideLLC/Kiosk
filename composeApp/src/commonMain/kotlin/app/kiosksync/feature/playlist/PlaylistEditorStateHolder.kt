package app.kiosksync.feature.playlist

import app.kiosksync.core.AppLogger
import app.kiosksync.core.DEFAULT_DURATION_SEC
import app.kiosksync.core.MAX_DURATION_SEC
import app.kiosksync.core.MIN_DURATION_SEC
import app.kiosksync.core.Strings
import app.kiosksync.platform.LocalStorage
import app.kiosksync.platform.MediaImportError
import app.kiosksync.platform.MediaImportResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaylistEditorUiState(
    val playlist: Playlist = Playlist(),
    val durationDrafts: Map<String, String> = emptyMap(),
    val defaultDurationDraft: String = DEFAULT_DURATION_SEC.toString(),
    val isImporting: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false
)

class PlaylistEditorStateHolder(
    private val storage: LocalStorage,
    private val logger: AppLogger,
    private val scope: CoroutineScope,
    private val pickImageFiles: () -> List<String>
) {
    private val _uiState = MutableStateFlow(PlaylistEditorUiState())
    val uiState: StateFlow<PlaylistEditorUiState> = _uiState.asStateFlow()

    init {
        val loaded = storage.loadPlaylist()
        if (loaded != null) {
            _uiState.value = PlaylistEditorUiState(
                playlist = loaded,
                defaultDurationDraft = loaded.defaultDurationSec.toString()
            )
            logger.info("Emitter playlist loaded with ${loaded.items.size} items")
        }
    }

    fun currentPlaylist(): Playlist = _uiState.value.playlist

    fun addImages() {
        if (_uiState.value.isImporting) return

        scope.launch {
            _uiState.value = _uiState.value.copy(
                isImporting = true,
                message = null,
                messageIsError = false
            )

            val paths = try {
                pickImageFiles()
            } catch (error: Exception) {
                logger.error("Error opening the image picker", error)
                emptyList()
            }

            if (paths.isEmpty()) {
                _uiState.value = _uiState.value.copy(isImporting = false)
                return@launch
            }

            val results = withContext(Dispatchers.Default) {
                paths.map { path -> storage.importMedia(path) }
            }

            val imported = results.mapNotNull { result ->
                (result as? MediaImportResult.Success)?.media
            }.map { media ->
                PlaylistItem(
                    mediaId = media.mediaId,
                    fileName = media.fileName,
                    sha256 = media.sha256,
                    durationSec = null
                )
            }
            val rejected = results.count { it is MediaImportResult.Failure }

            results.filterIsInstance<MediaImportResult.Failure>().forEach { failure ->
                val reason = when (failure.reason) {
                    MediaImportError.UNSUPPORTED_FORMAT -> "unsupported format"
                    MediaImportError.TOO_LARGE -> "file larger than 25 MB"
                    MediaImportError.UNREADABLE -> "unreadable file"
                }
                logger.warn("Rejected image import: $reason")
            }

            if (imported.isNotEmpty()) {
                val updated = _uiState.value.playlist.copy(
                    items = _uiState.value.playlist.items + imported
                )
                storage.savePlaylist(updated)
                _uiState.value = _uiState.value.copy(
                    playlist = updated,
                    isImporting = false,
                    message = Strings.playlistImportSummary(imported.size, rejected),
                    messageIsError = rejected > 0
                )
                logger.info("Added ${imported.size} images to the emitter playlist")
            } else {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    message = Strings.PLAYLIST_IMPORT_NONE,
                    messageIsError = true
                )
            }
        }
    }

    fun updateDefaultDuration(value: String) {
        val digits = value.filter { it.isDigit() }.take(3)
        if (digits.isEmpty()) return

        val state = _uiState.value
        val parsed = digits.toIntOrNull()
        if (parsed == null || parsed !in MIN_DURATION_SEC..MAX_DURATION_SEC) {
            _uiState.value = state.copy(defaultDurationDraft = digits)
            return
        }

        val updated = state.playlist.copy(defaultDurationSec = parsed)
        _uiState.value = state.copy(
            playlist = updated,
            defaultDurationDraft = digits
        )
        storage.savePlaylist(updated)
    }

    fun updateItemDuration(mediaId: String, value: String) {
        val state = _uiState.value
        val item = state.playlist.items.firstOrNull { it.mediaId == mediaId } ?: return
        val digits = value.filter { it.isDigit() }.take(3)
        val parsed = digits.toIntOrNull()
        val newDuration = when {
            digits.isEmpty() -> null
            parsed != null && parsed in MIN_DURATION_SEC..MAX_DURATION_SEC -> parsed
            else -> item.durationSec
        }

        val newItems = state.playlist.items.map {
            if (it.mediaId == mediaId) it.copy(durationSec = newDuration) else it
        }
        val updated = state.playlist.copy(items = newItems)
        val drafts = if (digits.isEmpty()) {
            state.durationDrafts - mediaId
        } else {
            state.durationDrafts + (mediaId to digits)
        }

        _uiState.value = state.copy(
            playlist = updated,
            durationDrafts = drafts
        )
        if (newDuration != item.durationSec) {
            storage.savePlaylist(updated)
        }
    }

    fun moveUp(index: Int) = moveItem(index, -1)

    fun moveDown(index: Int) = moveItem(index, 1)

    fun removeItem(index: Int) {
        val state = _uiState.value
        val item = state.playlist.items.getOrNull(index) ?: return
        val updated = state.playlist.copy(
            items = state.playlist.items.filterNot { it.mediaId == item.mediaId }
        )
        storage.savePlaylist(updated)
        _uiState.value = state.copy(
            playlist = updated,
            durationDrafts = state.durationDrafts - item.mediaId
        )
        logger.info("Removed '${item.fileName}' from the emitter playlist")
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null, messageIsError = false)
    }

    private fun moveItem(index: Int, delta: Int) {
        val state = _uiState.value
        val target = index + delta
        val items = state.playlist.items
        if (index !in items.indices || target !in items.indices) return

        val mutable = items.toMutableList()
        val moved = mutable.removeAt(index)
        mutable.add(target, moved)
        val updated = state.playlist.copy(items = mutable)
        storage.savePlaylist(updated)
        _uiState.value = state.copy(playlist = updated)
    }
}
