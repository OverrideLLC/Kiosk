package app.kiosksync.feature.playlist.presentation

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.core.DEFAULT_DURATION_SEC
import app.kiosksync.feature.playlist.domain.Playlist

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
    val showDeleteDialog: Boolean = false,
    val removeItemId: String? = null,
    val thumbnailByMediaId: Map<String, ImageBitmap?> = emptyMap(),
)
