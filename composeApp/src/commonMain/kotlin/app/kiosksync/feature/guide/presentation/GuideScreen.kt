package app.kiosksync.feature.guide.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.guide.domain.GuideStep
import app.kiosksync.feature.guide.presentation.components.GuideOverlay
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Connects the guide ViewModel to the overlay drawn over the active screen. */
@Composable
fun GuideRoot(viewModel: GuideViewModel) {
    val state by viewModel.state.collectAsState()
    GuideScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun GuideScreen(
    state: GuideState,
    onAction: (GuideAction) -> Unit,
) {
    GuideOverlay(
        state = state,
        onAction = onAction,
    )
}

@Preview
@Composable
private fun GuideScreenPreview() {
    KioskTheme {
        GuideScreen(
            state = GuideState(isActive = true, step = GuideStep.ROLE),
            onAction = {},
        )
    }
}
