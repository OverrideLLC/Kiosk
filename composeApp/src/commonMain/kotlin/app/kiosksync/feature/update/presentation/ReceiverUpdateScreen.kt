package app.kiosksync.feature.update.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.update.presentation.components.ReceiverUpdateOverlay
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ReceiverUpdateRoot(viewModel: ReceiverUpdateViewModel) {
    val state by viewModel.state.collectAsState()
    ReceiverUpdateScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun ReceiverUpdateScreen(
    state: ReceiverUpdateState,
    onAction: (ReceiverUpdateAction) -> Unit,
) {
    ReceiverUpdateOverlay(
        state = state,
        onAction = onAction,
    )
}

@Preview
@Composable
private fun ReceiverUpdateScreenPreview() {
    KioskTheme {
        ReceiverUpdateScreen(
            state = ReceiverUpdateState(version = "1.2.0", currentVersion = "1.1.0"),
            onAction = {},
        )
    }
}
