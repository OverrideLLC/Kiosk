package app.kiosksync.feature.playlist.presentation

import app.kiosksync.core.AppLogger
import app.kiosksync.core.DEFAULT_DURATION_SEC
import app.kiosksync.core.MAX_DURATION_SEC
import app.kiosksync.core.MIN_DURATION_SEC
import app.kiosksync.core.Strings
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.domain.PlaylistItem
import app.kiosksync.feature.playlist.domain.PlaylistLibrary
import app.kiosksync.feature.playlist.domain.PlaylistRepository
import app.kiosksync.feature.media.domain.MediaRepository
import app.kiosksync.feature.media.domain.MediaImportError
import app.kiosksync.feature.media.domain.MediaImportResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistEditorStateHolder(
    private val playlistRepository: PlaylistRepository,
    private val mediaRepository: MediaRepository,
    private val logger: AppLogger,
    private val scope: CoroutineScope,
    private val newPlaylistId: () -> String,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    private val _uiState = MutableStateFlow(PlaylistEditorUiState())
    val uiState: StateFlow<PlaylistEditorUiState> = _uiState.asStateFlow()
    private val loadingThumbnails = mutableSetOf<String>()
    private val effectChannel = Channel<PlaylistEditorEffect>(Channel.BUFFERED)
    val effects: Flow<PlaylistEditorEffect> = effectChannel.receiveAsFlow()

    init {
        scope.launch {
            try {
                val library = withContext(ioDispatcher) { loadOrMigrateLibrary() }
                val active = library.activePlaylist()!!
                _uiState.update {
                    it.copy(
                        playlist = active,
                        playlists = library.playlists,
                        activePlaylistId = active.id,
                        defaultDurationDraft = active.defaultDurationSec.toString(),
                        isLoading = false,
                    )
                }
                logger.info(
                    "Emitter playlist library loaded: ${library.playlists.size} playlists, " +
                        "active='${active.name}'"
                )
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not load the Emitter playlist library", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = Strings.PLAYLIST_SAVE_ERROR,
                        messageIsError = true,
                    )
                }
            }
        }
    }

    fun dispatch(action: PlaylistEditorAction) {
        when (action) {
            is PlaylistEditorAction.SelectPlaylist -> selectPlaylist(action.playlistId)
            PlaylistEditorAction.OpenCreateDialog,
            PlaylistEditorAction.OpenRenameDialog,
            is PlaylistEditorAction.UpdateNameDraft,
            PlaylistEditorAction.DismissNameDialog,
            PlaylistEditorAction.RequestDeletePlaylist,
            PlaylistEditorAction.DismissDeleteDialog,
            PlaylistEditorAction.DismissRemoveItem,
            PlaylistEditorAction.DismissMessage,
            is PlaylistEditorAction.RequestRemoveItem -> _uiState.update {
                PlaylistEditorReducer.reduce(it, action)
            }
            PlaylistEditorAction.AddImages -> {
                val previous = _uiState.value
                val next = PlaylistEditorReducer.reduce(previous, action)
                if (next !== previous) {
                    _uiState.value = next
                    effectChannel.trySend(PlaylistEditorEffect.SelectImageFiles)
                }
            }
            is PlaylistEditorAction.LoadThumbnail -> loadThumbnail(action.mediaId, action.fileName)
            is PlaylistEditorAction.ImagesSelected -> importImages(action.paths)
            is PlaylistEditorAction.ImagePickerFailed -> {
                logger.error("Error opening the image picker", action.error)
                _uiState.update { PlaylistEditorReducer.reduce(it, action) }
            }
            PlaylistEditorAction.ConfirmNameDialog -> confirmNameDialog()
            PlaylistEditorAction.ConfirmDeletePlaylist -> confirmDeletePlaylist()
            PlaylistEditorAction.ConfirmRemoveItem -> confirmRemoveItem()
            is PlaylistEditorAction.UpdateDefaultDuration -> updateDefaultDuration(action.value)
            is PlaylistEditorAction.UpdateItemDuration -> updateItemDuration(action.mediaId, action.value)
            is PlaylistEditorAction.MoveItemUp -> moveUp(action.index)
            is PlaylistEditorAction.MoveItemDown -> moveDown(action.index)
            is PlaylistEditorAction.RemoveItem -> removeItem(action.index)
        }
    }

    fun selectPlaylist(playlistId: String) {
        val state = _uiState.value
        if (state.activePlaylistId == playlistId) return
        val selected = state.playlists.firstOrNull { it.id == playlistId } ?: return
        persistLibrary(PlaylistLibrary(state.playlists, playlistId)) { current ->
            current.copy(
                playlist = selected,
                activePlaylistId = playlistId,
                defaultDurationDraft = selected.defaultDurationSec.toString(),
                durationDrafts = emptyMap(),
                thumbnailByMediaId = emptyMap(),
                message = null,
            )
        }
        logger.info("Active playlist changed to '${selected.name}'")
    }

    fun openCreatePlaylistDialog() {
        _uiState.value = _uiState.value.copy(
            nameDialogMode = PlaylistNameDialogMode.CREATE,
            nameDraft = "",
            nameError = null
        )
    }

    fun openRenamePlaylistDialog() {
        _uiState.value = _uiState.value.copy(
            nameDialogMode = PlaylistNameDialogMode.RENAME,
            nameDraft = _uiState.value.playlist.name,
            nameError = null
        )
    }

    fun updateNameDraft(value: String) {
        _uiState.value = _uiState.value.copy(nameDraft = value, nameError = null)
    }

    fun dismissNameDialog() {
        _uiState.value = _uiState.value.copy(
            nameDialogMode = null,
            nameDraft = "",
            nameError = null
        )
    }

    fun confirmNameDialog() {
        val state = _uiState.value
        val mode = state.nameDialogMode ?: return
        val name = state.nameDraft.trim()
        if (name.isEmpty()) {
            _uiState.value = state.copy(nameError = Strings.PLAYLIST_NAME_REQUIRED)
            return
        }
        val duplicate = state.playlists.any {
            it.id != state.playlist.id && it.name.equals(name, ignoreCase = true)
        }
        if (duplicate) {
            _uiState.value = state.copy(nameError = Strings.PLAYLIST_NAME_EXISTS)
            return
        }

        when (mode) {
            PlaylistNameDialogMode.CREATE -> {
                val created = Playlist(
                    id = newPlaylistId(),
                    name = name,
                    defaultDurationSec = state.playlist.defaultDurationSec
                )
                val library = PlaylistLibrary(state.playlists, state.activePlaylistId)
                    .upsert(created)
                    .select(created.id)
                persistLibrary(library) { current ->
                    current.copy(
                        playlist = created,
                        playlists = library.playlists,
                        activePlaylistId = created.id,
                        defaultDurationDraft = created.defaultDurationSec.toString(),
                        durationDrafts = emptyMap(),
                        thumbnailByMediaId = emptyMap(),
                        nameDialogMode = null,
                        nameDraft = "",
                        nameError = null,
                        message = null,
                    )
                }
                logger.info("Created playlist '$name'")
            }

            PlaylistNameDialogMode.RENAME -> {
                val renamed = state.playlist.copy(name = name)
                val library = PlaylistLibrary(state.playlists, state.activePlaylistId)
                    .upsert(renamed)
                persistLibrary(library) { current ->
                    current.copy(
                        playlist = renamed,
                        playlists = library.playlists,
                        nameDialogMode = null,
                        nameDraft = "",
                        nameError = null,
                    )
                }
                logger.info("Renamed playlist to '$name'")
            }
        }
    }

    fun requestDeletePlaylist() {
        if (_uiState.value.playlists.size <= 1) {
            _uiState.value = _uiState.value.copy(
                message = Strings.PLAYLIST_DELETE_LAST_ERROR,
                messageIsError = true
            )
            return
        }
        _uiState.value = _uiState.value.copy(showDeleteDialog = true)
    }

    fun dismissDeletePlaylist() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
    }

    fun confirmDeletePlaylist() {
        val state = _uiState.value
        val library = PlaylistLibrary(state.playlists, state.activePlaylistId)
            .remove(state.playlist.id)
        val active = library.activePlaylist() ?: return
        persistLibrary(library) { current ->
            current.copy(
                playlist = active,
                playlists = library.playlists,
                activePlaylistId = active.id,
                defaultDurationDraft = active.defaultDurationSec.toString(),
                durationDrafts = emptyMap(),
                thumbnailByMediaId = emptyMap(),
                showDeleteDialog = false,
                message = null,
            )
        }
        logger.info("Deleted playlist '${state.playlist.name}'")
    }

    fun addImages() {
        dispatch(PlaylistEditorAction.AddImages)
    }

    private fun importImages(paths: List<String>) {
        if (paths.isEmpty()) {
            _uiState.update { it.copy(isImporting = false) }
            return
        }
        scope.launch {
            val results = withContext(ioDispatcher) {
                paths.map { path -> mediaRepository.import(path) }
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
                _uiState.update {
                    it.copy(
                        playlist = updated,
                        isImporting = false,
                        message = Strings.playlistImportSummary(imported.size, rejected),
                        messageIsError = rejected > 0
                    )
                }
                saveActivePlaylist(updated)
                logger.info("Added ${imported.size} images to playlist '${updated.name}'")
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
        saveActivePlaylist(updated)
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
            saveActivePlaylist(updated)
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
        _uiState.value = state.copy(
            playlist = updated,
            durationDrafts = state.durationDrafts - item.mediaId,
            thumbnailByMediaId = state.thumbnailByMediaId - item.mediaId,
        )
        saveActivePlaylist(updated)
        logger.info("Removed '${item.fileName}' from playlist '${updated.name}'")
    }

    private fun confirmRemoveItem() {
        val state = _uiState.value
        val mediaId = state.removeItemId ?: return
        _uiState.value = state.copy(removeItemId = null)
        val index = state.playlist.items.indexOfFirst { it.mediaId == mediaId }
        if (index >= 0) removeItem(index)
    }

    private fun loadThumbnail(mediaId: String, fileName: String) {
        val state = _uiState.value
        if (mediaId in state.thumbnailByMediaId || !loadingThumbnails.add(mediaId)) return
        if (state.playlist.items.none { it.mediaId == mediaId && it.fileName == fileName }) {
            loadingThumbnails.remove(mediaId)
            return
        }

        scope.launch {
            try {
                val bitmap = withContext(ioDispatcher) {
                    mediaRepository.loadBitmap(mediaId, fileName)
                }
                _uiState.update { current ->
                    if (current.playlist.items.any { it.mediaId == mediaId && it.fileName == fileName }) {
                        current.copy(thumbnailByMediaId = current.thumbnailByMediaId + (mediaId to bitmap))
                    } else {
                        current
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.warn("Could not load playlist thumbnail for '$fileName': ${error.message}")
                _uiState.update { current ->
                    if (current.playlist.items.any { it.mediaId == mediaId && it.fileName == fileName }) {
                        current.copy(thumbnailByMediaId = current.thumbnailByMediaId + (mediaId to null))
                    } else {
                        current
                    }
                }
            } finally {
                loadingThumbnails.remove(mediaId)
            }
        }
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
        _uiState.value = state.copy(playlist = updated)
        saveActivePlaylist(updated)
    }

    private fun saveActivePlaylist(updated: Playlist) {
        val state = _uiState.value
        val library = PlaylistLibrary(state.playlists, state.activePlaylistId).upsert(updated)
        persistLibrary(library) { it.copy(playlists = library.playlists) }
    }

    private fun persistLibrary(
        library: PlaylistLibrary,
        onSuccess: (PlaylistEditorUiState) -> PlaylistEditorUiState = { it },
    ) {
        _uiState.update { it.copy(isSaving = true) }
        scope.launch {
            try {
                withContext(ioDispatcher) { playlistRepository.saveEmitterPlaylistLibrary(library) }
                _uiState.update { state -> onSuccess(state).copy(isSaving = false) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not save the Emitter playlist library", error)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = Strings.PLAYLIST_SAVE_ERROR,
                        messageIsError = true,
                    )
                }
            }
        }
    }

    private suspend fun loadOrMigrateLibrary(): PlaylistLibrary {
        val stored = playlistRepository.loadEmitterPlaylistLibrary()
        if (stored != null && stored.playlists.isNotEmpty()) {
            val activeId = stored.activePlaylist()?.id
            if (stored.activePlaylistId != activeId) {
                playlistRepository.saveEmitterPlaylistLibrary(stored.copy(activePlaylistId = activeId))
            }
            return stored.copy(activePlaylistId = activeId)
        }

        val fresh = Playlist(
            id = newPlaylistId(),
            name = Strings.PLAYLIST_DEFAULT_NAME
        )
        val library = PlaylistLibrary(
            playlists = listOf(fresh),
            activePlaylistId = fresh.id
        )
        playlistRepository.saveEmitterPlaylistLibrary(library)
        return library
    }
}
