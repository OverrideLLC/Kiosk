package app.kiosksync.feature.role.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.kiosksync.core.ui.Strings
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDialog
import app.kiosksync.core.ui.components.AppIcon

@Composable
fun ChangeRoleDialog(
    isResetting: Boolean,
    hasError: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppDialog(
        title = Strings.CHANGE_ROLE,
        icon = AppIcon.SWAP_HORIZ,
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = when {
                    isResetting -> Strings.ROLE_RESETTING
                    hasError -> Strings.ROLE_RESET_ERROR
                    else -> Strings.CHANGE_ROLE_CONFIRM
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        actions = {
            AppButton(
                text = Strings.CANCEL_BUTTON,
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
                enabled = !isResetting,
            )
            AppButton(
                text = if (isResetting) Strings.ROLE_RESETTING else Strings.CHANGE_ROLE,
                onClick = onConfirm,
                variant = AppButtonVariant.OUTLINED,
                size = AppButtonSize.SMALL,
                icon = AppIcon.SWAP_HORIZ,
                loading = isResetting,
                enabled = !isResetting,
            )
        },
    )
}
