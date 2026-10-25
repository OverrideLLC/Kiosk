package app.kiosksync.feature.playlist

import app.kiosksync.NoOpLogger
import app.kiosksync.FakeRepositories
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.domain.PlaylistItem
import app.kiosksync.feature.playlist.presentation.PlaylistEditorAction
import app.kiosksync.feature.playlist.presentation.PlaylistEditorEffect
import app.kiosksync.feature.playlist.presentation.PlaylistEditorReducer
import app.kiosksync.feature.playlist.presentation.PlaylistEditorStateHolder
import app.kiosksync.feature.playlist.presentation.PlaylistEditorUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PlaylistEditorReducerTest {
    @Test
    fun removeItemDialogTargetLivesInPlaylistState() {
        val state = PlaylistEditorUiState(
            playlist = Playlist(
                items = listOf(
                    PlaylistItem("first", "one.png", "hash-1"),
                    PlaylistItem("second", "two.png", "hash-2"),
                ),
            ),
        )

        val dialog = PlaylistEditorReducer.reduce(state, PlaylistEditorAction.RequestRemoveItem("second"))
        assertEquals("second", dialog.removeItemId)
        assertEquals(null, PlaylistEditorReducer.reduce(dialog, PlaylistEditorAction.DismissRemoveItem).removeItemId)
        assertEquals(
            null,
            PlaylistEditorReducer.reduce(state, PlaylistEditorAction.RequestRemoveItem("missing")).removeItemId,
        )
    }

    @Test
    fun reducerHandlesDialogAndDeleteTransitions() {
        val onePlaylist = PlaylistEditorUiState(
            playlist = Playlist(id = "one", name = "Principal"),
            playlists = listOf(Playlist(id = "one", name = "Principal"))
        )
        val error = PlaylistEditorReducer.reduce(onePlaylist, PlaylistEditorAction.RequestDeletePlaylist)
        assertEquals("Debe quedar al menos una playlist.", error.message)
        assertFalse(error.showDeleteDialog)

        val twoPlaylists = onePlaylist.copy(
            playlists = onePlaylist.playlists + Playlist(id = "two", name = "Promos")
        )
        val dialog = PlaylistEditorReducer.reduce(twoPlaylists, PlaylistEditorAction.RequestDeletePlaylist)
        assertTrue(dialog.showDeleteDialog)
        assertFalse(PlaylistEditorReducer.reduce(dialog, PlaylistEditorAction.DismissDeleteDialog).showDeleteDialog)

        val rename = PlaylistEditorReducer.reduce(twoPlaylists, PlaylistEditorAction.OpenRenameDialog)
        assertEquals("Principal", rename.nameDraft)
        assertEquals("Nuevo nombre", PlaylistEditorReducer.reduce(
            rename,
            PlaylistEditorAction.UpdateNameDraft("Nuevo nombre")
        ).nameDraft)
    }

    @Test
    fun addImagesEmitsPickerEffectOnceAndCancellationClearsLoading() = runBlocking {
        val storage = FakeRepositories()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val holder = PlaylistEditorStateHolder(
            playlistRepository = storage,
            mediaRepository = storage,
            logger = NoOpLogger(),
            scope = scope,
            newPlaylistId = { "new" },
        )

        holder.dispatch(PlaylistEditorAction.AddImages)

        assertTrue(holder.uiState.value.isImporting)
        assertEquals(PlaylistEditorEffect.SelectImageFiles, holder.effects.first())
        holder.dispatch(PlaylistEditorAction.ImagesSelected(emptyList()))
        assertFalse(holder.uiState.value.isImporting)
    }
}
