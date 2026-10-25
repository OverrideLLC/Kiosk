package app.kiosksync.feature.guide.presentation

import androidx.compose.runtime.Composable
import app.kiosksync.core.AppRole
import app.kiosksync.feature.guide.presentation.components.GuideOverlay

/** Presentation entry point for the guide overlay shown over the active screen. */
@Composable
fun GuideScreen(
    state: GuideUiState,
    onChooseRole: (AppRole) -> Unit,
    onAction: (GuideAction) -> Unit,
) {
    GuideOverlay(
        state = state,
        onChooseRole = onChooseRole,
        onAction = onAction,
    )
}
