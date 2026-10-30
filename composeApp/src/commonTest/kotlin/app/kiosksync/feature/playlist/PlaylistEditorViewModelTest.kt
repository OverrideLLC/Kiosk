package app.kiosksync.feature.playlist

import app.kiosksync.FakeRepositories
import app.kiosksync.NoOpLogger
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.domain.PlaylistItem
import app.kiosksync.feature.playlist.domain.PlaylistLibrary
import app.kiosksync.feature.playlist.presentation.PlaylistEditorAction
import app.kiosksync.feature.playlist.presentation.PlaylistEditorEffect
import app.kiosksync.feature.playlist.presentation.PlaylistEditorViewModel
import app.kiosksync.feature.playlist.domain.models.PlaylistNameDialogMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PlaylistEditorViewModelTest {
    @Test
    fun actionsUpdateDialogStateAndValidatePlaylistRemoval() {
        val active = Playlist(
            id = "one",
            name = "Principal",
            items = listOf(
                PlaylistItem("first", "one.png", "hash-1"),
                PlaylistItem("second", "two.png", "hash-2"),
            ),
        )
        val storage = FakeRepositories().apply {
            playlistLibrary = PlaylistLibrary(
                playlists = listOf(active, Playlist(id = "two", name = "Promos")),
                activePlaylistId = active.id,
            )
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val viewModel = PlaylistEditorViewModel(
                playlistRepository = storage,
                mediaRepository = storage,
                logger = NoOpLogger(),
                scope = scope,
                newPlaylistId = { "new" },
                ioDispatcher = Dispatchers.Unconfined,
            )

            viewModel.onAction(PlaylistEditorAction.OpenRenameDialog)
            assertEquals(PlaylistNameDialogMode.RENAME, viewModel.state.value.nameDialogMode)
            assertEquals("Principal", viewModel.state.value.nameDraft)
            viewModel.onAction(PlaylistEditorAction.UpdateNameDraft("Nuevo nombre"))
            assertEquals("Nuevo nombre", viewModel.state.value.nameDraft)

            viewModel.onAction(PlaylistEditorAction.RequestRemoveItem("second"))
            assertEquals("second", viewModel.state.value.removeItemId)
            viewModel.onAction(PlaylistEditorAction.DismissRemoveItem)
            assertEquals(null, viewModel.state.value.removeItemId)
            viewModel.onAction(PlaylistEditorAction.RequestRemoveItem("missing"))
            assertEquals(null, viewModel.state.value.removeItemId)

            viewModel.onAction(PlaylistEditorAction.RequestDeletePlaylist)
            assertTrue(viewModel.state.value.showDeleteDialog)
            viewModel.onAction(PlaylistEditorAction.DismissDeleteDialog)
            assertFalse(viewModel.state.value.showDeleteDialog)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun lastPlaylistCannotBeDeleted() {
        val onlyPlaylist = Playlist(id = "one", name = "Principal")
        val storage = FakeRepositories().apply {
            playlistLibrary = PlaylistLibrary(listOf(onlyPlaylist), onlyPlaylist.id)
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val viewModel = PlaylistEditorViewModel(
                playlistRepository = storage,
                mediaRepository = storage,
                logger = NoOpLogger(),
                scope = scope,
                newPlaylistId = { "new" },
                ioDispatcher = Dispatchers.Unconfined,
            )

            viewModel.onAction(PlaylistEditorAction.RequestDeletePlaylist)

            assertEquals("Debe quedar al menos una playlist.", viewModel.state.value.message)
            assertFalse(viewModel.state.value.showDeleteDialog)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun addImagesEmitsPickerEffectAndCancellationClearsLoading() = runBlocking {
        val storage = FakeRepositories()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val viewModel = PlaylistEditorViewModel(
                playlistRepository = storage,
                mediaRepository = storage,
                logger = NoOpLogger(),
                scope = scope,
                newPlaylistId = { "new" },
                ioDispatcher = Dispatchers.Unconfined,
            )

            viewModel.onAction(PlaylistEditorAction.AddImages)

            assertTrue(viewModel.state.value.isImporting)
            assertEquals(PlaylistEditorEffect.SelectImageFiles, viewModel.effects.first())
            viewModel.onAction(PlaylistEditorAction.ImagesSelected(emptyList()))
            assertFalse(viewModel.state.value.isImporting)
        } finally {
            scope.cancel()
        }
    }
}
