package app.kiosksync.feature.emitter.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.emitter.domain.models.EmitterPanel
import org.jetbrains.compose.ui.tooling.preview.Preview
import app.kiosksync.core.ui.components.AppButtonGroup
import app.kiosksync.core.ui.components.OverrideFooter
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.feature.emitter.presentation.components.ControllerHeader
import app.kiosksync.feature.emitter.presentation.components.ScreensPanel
import app.kiosksync.feature.emitter.presentation.components.SendPanel
import app.kiosksync.feature.emitter.presentation.components.StatusBanner
import app.kiosksync.feature.playlist.presentation.PlaylistEditorAction
import app.kiosksync.feature.playlist.presentation.PlaylistEditorState
import app.kiosksync.feature.playlist.presentation.PlaylistEditorViewModel
import app.kiosksync.feature.playlist.presentation.components.PlaylistPanel

@Composable
fun EmitterRoot(
    viewModel: EmitterViewModel,
    playlistViewModel: PlaylistEditorViewModel,
) {
    val state by viewModel.state.collectAsState()
    val playlistState by playlistViewModel.state.collectAsState()
    EmitterScreen(
        state = state,
        onAction = viewModel::onAction,
        playlistState = playlistState,
        onPlaylistAction = playlistViewModel::onAction,
    )
}

@Composable
fun EmitterScreen(
    state: EmitterState,
    onAction: (EmitterAction) -> Unit,
    playlistState: PlaylistEditorState,
    onPlaylistAction: (PlaylistEditorAction) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        val wide = maxWidth >= Sizes.wideLayoutBreakpoint

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Space.lg, vertical = Space.md),
        ) {
            ControllerHeader(
                hasPassword = state.hasKioskPassword,
                onOpenSecurity = { onAction(EmitterAction.OpenSecurityDialog) },
                onOpenSettings = { onAction(EmitterAction.OpenSettings) },
                onChangeRole = { onAction(EmitterAction.RequestRoleReset) },
                onExit = { onAction(EmitterAction.Exit) },
            )

            state.statusMessage?.let { message ->
                Spacer(Modifier.height(Space.sm))
                StatusBanner(
                    message = message,
                    isError = state.statusIsError,
                    onDismiss = { onAction(EmitterAction.DismissStatusMessage) },
                )
            }

            Spacer(Modifier.height(Space.md))

            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    ScreensPanel(
                        onAction = onAction,
                        state = state,
                        modifier = Modifier.weight(0.42f).fillMaxHeight(),
                    )
                    Spacer(Modifier.width(Space.lg))
                    PlaylistPanel(
                        state = playlistState,
                        onAction = onPlaylistAction,
                        modifier = Modifier.weight(0.58f).fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    AppButtonGroup(
                        options = listOf(
                            Strings.EMITTER_SCREENS_TITLE,
                            Strings.PLAYLIST_TITLE,
                        ),
                        selectedIndex = if (state.selectedPanel == EmitterPanel.SCREENS) 0 else 1,
                        onSelect = {
                            onAction(EmitterAction.SelectPanel(if (it == 0) EmitterPanel.SCREENS else EmitterPanel.PLAYLIST))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Space.sm))
                    if (state.selectedPanel == EmitterPanel.SCREENS) {
                        ScreensPanel(
                            onAction = onAction,
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        PlaylistPanel(
                            state = playlistState,
                            onAction = onPlaylistAction,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.md))

            SendPanel(
                state = state,
                onAction = onAction,
            )

            Spacer(Modifier.height(Space.xs))
            OverrideFooter()
        }
    }
}

@Preview
@Composable
private fun EmitterScreenPreview() {
    KioskTheme {
        EmitterScreen(
            state = EmitterState(),
            onAction = {},
            playlistState = PlaylistEditorState(),
            onPlaylistAction = {},
        )
    }
}
