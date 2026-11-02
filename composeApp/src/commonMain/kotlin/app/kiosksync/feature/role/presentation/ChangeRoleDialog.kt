package app.kiosksync.feature.role.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDialog
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.resources.Res
import app.kiosksync.resources.cancel_button
import app.kiosksync.resources.change_role
import app.kiosksync.resources.change_role_confirm
import app.kiosksync.resources.role_reset_error
import app.kiosksync.resources.role_resetting
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChangeRoleDialog(
    isResetting: Boolean,
    hasError: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppDialog(
        title = stringResource(Res.string.change_role),
        icon = AppIcon.SWAP_HORIZ,
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = when {
                    isResetting -> stringResource(Res.string.role_resetting)
                    hasError -> stringResource(Res.string.role_reset_error)
                    else -> stringResource(Res.string.change_role_confirm)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        actions = {
            AppButton(
                text = stringResource(Res.string.cancel_button),
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
                enabled = !isResetting,
            )
            AppButton(
                text = if (isResetting) stringResource(Res.string.role_resetting) else stringResource(Res.string.change_role),
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
