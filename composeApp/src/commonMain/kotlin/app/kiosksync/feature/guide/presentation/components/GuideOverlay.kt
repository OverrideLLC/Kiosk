package app.kiosksync.feature.guide.presentation.components

import androidx.compose.runtime.Composable
import app.kiosksync.feature.guide.presentation.GuideAction
import app.kiosksync.feature.guide.presentation.GuideState
import app.kiosksync.feature.guide.domain.GuideStep

/** Shows the role dialog or the current guide step over the application. */
@Composable
fun GuideOverlay(
    state: GuideState,
    onAction: (GuideAction) -> Unit,
) {
    if (!state.isActive) return

    if (state.step == GuideStep.ROLE) {
        GuideRoleDialog(
            onChooseRole = { onAction(GuideAction.ChooseRole(it)) },
            onSkip = { onAction(GuideAction.Stop) },
        )
    } else {
        GuideCoachCard(state = state, onAction = onAction)
    }
}
