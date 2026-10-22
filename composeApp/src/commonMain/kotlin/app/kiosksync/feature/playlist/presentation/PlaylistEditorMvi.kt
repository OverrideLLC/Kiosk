package app.kiosksync.feature.playlist.presentation

import app.kiosksync.core.DEFAULT_DURATION_SEC
import app.kiosksync.core.Strings
import app.kiosksync.feature.playlist.domain.Playlist

enum class PlaylistNameDialogMode {
    CREATE,
    RENAME
}

data class PlaylistEditorUiState(
    val playlist: Playlist = Playlist(),
    val playlists: List<Playlist> = emptyList(),
    val activePlaylistId: String? = null,
    val durationDrafts: Map<String, String> = emptyMap(),
    val defaultDurationDraft: String = DEFAULT_DURATION_SEC.toString(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val nameDialogMode: PlaylistNameDialogMode? = null,
    val nameDraft: String = "",
    val nameError: String? = null,
    val showDeleteDialog: Boolean = false
)

sealed interface PlaylistEditorAction {
    data object OpenCreateDialog : PlaylistEditorAction
    data object OpenRenameDialog : PlaylistEditorAction
    data class UpdateNameDraft(val value: String) : PlaylistEditorAction
    data object DismissNameDialog : PlaylistEditorAction
    data object RequestDeletePlaylist : PlaylistEditorAction
    data object DismissDeleteDialog : PlaylistEditorAction
    data object DismissMessage : PlaylistEditorAction
    data object AddImages : PlaylistEditorAction
    data class ImagesSelected(val paths: List<String>) : PlaylistEditorAction
    data class ImagePickerFailed(val error: Throwable) : PlaylistEditorAction
    data class SelectPlaylist(val playlistId: String) : PlaylistEditorAction
    data object ConfirmNameDialog : PlaylistEditorAction
    data object ConfirmDeletePlaylist : PlaylistEditorAction
    data class UpdateDefaultDuration(val value: String) : PlaylistEditorAction
    data class UpdateItemDuration(val mediaId: String, val value: String) : PlaylistEditorAction
    data class MoveItemUp(val index: Int) : PlaylistEditorAction
    data class MoveItemDown(val index: Int) : PlaylistEditorAction
    data class RemoveItem(val index: Int) : PlaylistEditorAction
}

sealed interface PlaylistEditorEffect {
    data object SelectImageFiles : PlaylistEditorEffect
}

object PlaylistEditorReducer {
    fun reduce(state: PlaylistEditorUiState, action: PlaylistEditorAction): PlaylistEditorUiState = when (action) {
        PlaylistEditorAction.OpenCreateDialog -> state.copy(
            nameDialogMode = PlaylistNameDialogMode.CREATE,
            nameDraft = "",
            nameError = null
        )
        PlaylistEditorAction.OpenRenameDialog -> state.copy(
            nameDialogMode = PlaylistNameDialogMode.RENAME,
            nameDraft = state.playlist.name,
            nameError = null
        )
        is PlaylistEditorAction.UpdateNameDraft -> state.copy(nameDraft = action.value, nameError = null)
        PlaylistEditorAction.DismissNameDialog -> state.copy(
            nameDialogMode = null,
            nameDraft = "",
            nameError = null
        )
        PlaylistEditorAction.RequestDeletePlaylist -> if (state.playlists.size <= 1) {
            state.copy(message = Strings.PLAYLIST_DELETE_LAST_ERROR, messageIsError = true)
        } else {
            state.copy(showDeleteDialog = true)
        }
        PlaylistEditorAction.DismissDeleteDialog -> state.copy(showDeleteDialog = false)
        PlaylistEditorAction.DismissMessage -> state.copy(message = null, messageIsError = false)
        PlaylistEditorAction.AddImages -> if (state.isImporting) state else state.copy(
            isImporting = true,
            message = null,
            messageIsError = false
        )
        is PlaylistEditorAction.ImagePickerFailed -> state.copy(
            isImporting = false,
            message = Strings.PLAYLIST_IMPORT_ERROR,
            messageIsError = true
        )
        is PlaylistEditorAction.SelectPlaylist,
        is PlaylistEditorAction.ImagesSelected,
        PlaylistEditorAction.ConfirmNameDialog,
        PlaylistEditorAction.ConfirmDeletePlaylist,
        is PlaylistEditorAction.UpdateDefaultDuration,
        is PlaylistEditorAction.UpdateItemDuration,
        is PlaylistEditorAction.MoveItemUp,
        is PlaylistEditorAction.MoveItemDown,
        is PlaylistEditorAction.RemoveItem -> state
    }
}

