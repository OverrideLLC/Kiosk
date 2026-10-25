package app.kiosksync.feature.guide.presentation.components

import androidx.compose.runtime.Composable
import app.kiosksync.core.AppRole
import app.kiosksync.feature.guide.presentation.GuideAction
import app.kiosksync.feature.guide.presentation.GuideUiState
import app.kiosksync.feature.guide.domain.GuideStep

/** Shows the role dialog or the current guide step over the application. */
@Composable
fun GuideOverlay(
    state: GuideUiState,
    onChooseRole: (AppRole) -> Unit,
    onAction: (GuideAction) -> Unit,
) {
    if (!state.isActive) return

    if (state.step == GuideStep.ROLE) {
        GuideRoleDialog(
            onChooseRole = onChooseRole,
            onSkip = { onAction(GuideAction.Stop) },
        )
    } else {
        GuideCoachCard(state = state, onAction = onAction)
    }
}
