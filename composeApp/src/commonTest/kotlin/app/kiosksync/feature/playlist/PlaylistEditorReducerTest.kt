package app.kiosksync.feature.playlist

import app.kiosksync.NoOpLogger
import app.kiosksync.FakeLocalStorage
import app.kiosksync.feature.playlist.domain.Playlist
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
        val storage = FakeLocalStorage()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val holder = PlaylistEditorStateHolder(storage, NoOpLogger(), scope, newPlaylistId = { "new" })

        holder.dispatch(PlaylistEditorAction.AddImages)

        assertTrue(holder.uiState.value.isImporting)
        assertEquals(PlaylistEditorEffect.SelectImageFiles, holder.effects.first())
        holder.dispatch(PlaylistEditorAction.ImagesSelected(emptyList()))
        assertFalse(holder.uiState.value.isImporting)
    }
}
