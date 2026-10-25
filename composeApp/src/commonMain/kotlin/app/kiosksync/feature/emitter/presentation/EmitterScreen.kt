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
import androidx.compose.ui.Modifier
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppButtonGroup
import app.kiosksync.core.ui.components.OverrideFooter
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.feature.emitter.presentation.components.ControllerHeader
import app.kiosksync.feature.emitter.presentation.components.ScreensPanel
import app.kiosksync.feature.emitter.presentation.components.SendPanel
import app.kiosksync.feature.emitter.presentation.components.StatusBanner

@Composable
fun EmitterScreen(
    state: EmitterUiState,
    onAction: (EmitterScreenAction) -> Unit,
    playlistPanel: @Composable (Modifier) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val emit: (EmitterAction) -> Unit = { onAction(EmitterScreenAction.Emitter(it)) }

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
                onOpenSecurity = { emit(EmitterAction.OpenSecurityDialog) },
                onOpenSettings = { emit(EmitterAction.OpenSettings) },
                onChangeRole = { emit(EmitterAction.RequestRoleReset) },
                onExit = { onAction(EmitterScreenAction.ExitRequested) },
            )

            state.statusMessage?.let { message ->
                Spacer(Modifier.height(Space.sm))
                StatusBanner(
                    message = message,
                    isError = state.statusIsError,
                    onDismiss = { emit(EmitterAction.DismissStatusMessage) },
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
                        onAction = emit,
                        state = state,
                        modifier = Modifier.weight(0.42f).fillMaxHeight(),
                    )
                    Spacer(Modifier.width(Space.lg))
                    playlistPanel(Modifier.weight(0.58f).fillMaxHeight())
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
                            emit(EmitterAction.SelectPanel(if (it == 0) EmitterPanel.SCREENS else EmitterPanel.PLAYLIST))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Space.sm))
                    if (state.selectedPanel == EmitterPanel.SCREENS) {
                        ScreensPanel(
                            onAction = emit,
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        playlistPanel(Modifier.fillMaxSize())
                    }
                }
            }

            Spacer(Modifier.height(Space.md))

            SendPanel(
                state = state,
                onAction = emit,
            )

            Spacer(Modifier.height(Space.xs))
            OverrideFooter()
        }
    }
}
