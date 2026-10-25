package app.kiosksync.feature.playlist.presentation

sealed interface PlaylistEditorAction {
    data object OpenCreateDialog : PlaylistEditorAction
    data object OpenRenameDialog : PlaylistEditorAction
    data class UpdateNameDraft(val value: String) : PlaylistEditorAction
    data object DismissNameDialog : PlaylistEditorAction
    data object RequestDeletePlaylist : PlaylistEditorAction
    data object DismissDeleteDialog : PlaylistEditorAction
    data object DismissMessage : PlaylistEditorAction
    data object AddImages : PlaylistEditorAction
    data class LoadThumbnail(val mediaId: String, val fileName: String) : PlaylistEditorAction
    data class ImagesSelected(val paths: List<String>) : PlaylistEditorAction
    data class ImagePickerFailed(val error: Throwable) : PlaylistEditorAction
    data class SelectPlaylist(val playlistId: String) : PlaylistEditorAction
    data object ConfirmNameDialog : PlaylistEditorAction
    data object ConfirmDeletePlaylist : PlaylistEditorAction
    data class RequestRemoveItem(val mediaId: String) : PlaylistEditorAction
    data object DismissRemoveItem : PlaylistEditorAction
    data object ConfirmRemoveItem : PlaylistEditorAction
    data class UpdateDefaultDuration(val value: String) : PlaylistEditorAction
    data class UpdateItemDuration(val mediaId: String, val value: String) : PlaylistEditorAction
    data class MoveItemUp(val index: Int) : PlaylistEditorAction
    data class MoveItemDown(val index: Int) : PlaylistEditorAction
}

sealed interface PlaylistEditorEffect {
    data object SelectImageFiles : PlaylistEditorEffect
}
