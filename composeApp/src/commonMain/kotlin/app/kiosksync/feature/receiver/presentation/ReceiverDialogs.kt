package app.kiosksync.feature.receiver.presentation

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
import app.kiosksync.resources.exit_app
import app.kiosksync.resources.guide_button
import app.kiosksync.resources.options_message
import app.kiosksync.resources.options_title
import app.kiosksync.resources.receiver_unlocked_message
import app.kiosksync.resources.relock_button
import app.kiosksync.resources.unpair_button
import app.kiosksync.resources.unpair_receiver_confirm
import app.kiosksync.resources.unpair_receiver_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReceiverOptionsDialog(
    isReceiver: Boolean,
    onOpenGuide: () -> Unit = {},
    onDismiss: () -> Unit,
    onChangeRole: () -> Unit,
    onExit: () -> Unit,
    onRelock: () -> Unit,
) {
    AppDialog(
        title = stringResource(Res.string.options_title),
        icon = AppIcon.LOCK_OPEN,
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = if (isReceiver) stringResource(Res.string.receiver_unlocked_message) else stringResource(Res.string.options_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        actions = {
            AppButton(
                text = stringResource(Res.string.guide_button),
                onClick = onOpenGuide,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
                icon = AppIcon.HELP,
            )
            AppButton(
                text = stringResource(Res.string.change_role),
                onClick = onChangeRole,
                variant = AppButtonVariant.OUTLINED,
                size = AppButtonSize.SMALL,
                icon = AppIcon.SWAP_HORIZ,
            )
            AppButton(
                text = stringResource(Res.string.exit_app),
                onClick = onExit,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
                icon = AppIcon.LOGOUT,
            )
            AppButton(
                text = stringResource(Res.string.relock_button),
                onClick = onRelock,
                size = AppButtonSize.SMALL,
                icon = AppIcon.LOCK,
            )
        },
    )
}

@Composable
fun ReceiverUnpairDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppDialog(
        title = stringResource(Res.string.unpair_receiver_title),
        icon = AppIcon.LINK,
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = stringResource(Res.string.unpair_receiver_confirm),
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
            )
            AppButton(
                text = stringResource(Res.string.unpair_button),
                onClick = onConfirm,
                variant = AppButtonVariant.DANGER,
                size = AppButtonSize.SMALL,
                icon = AppIcon.LINK,
            )
        },
    )
}

