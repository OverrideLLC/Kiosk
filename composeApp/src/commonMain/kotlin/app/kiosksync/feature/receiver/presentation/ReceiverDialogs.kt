package app.kiosksync.feature.receiver.presentation

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
fun ReceiverOptionsDialog(
    isReceiver: Boolean,
    onOpenGuide: () -> Unit = {},
    onDismiss: () -> Unit,
    onChangeRole: () -> Unit,
    onExit: () -> Unit,
    onRelock: () -> Unit,
) {
    AppDialog(
        title = Strings.OPTIONS_TITLE,
        icon = AppIcon.LOCK_OPEN,
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = if (isReceiver) Strings.RECEIVER_UNLOCKED_MESSAGE else Strings.OPTIONS_MESSAGE,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        actions = {
            AppButton(
                text = Strings.GUIDE_BUTTON,
                onClick = onOpenGuide,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
                icon = AppIcon.HELP,
            )
            AppButton(
                text = Strings.CHANGE_ROLE,
                onClick = onChangeRole,
                variant = AppButtonVariant.OUTLINED,
                size = AppButtonSize.SMALL,
                icon = AppIcon.SWAP_HORIZ,
            )
            AppButton(
                text = Strings.EXIT_APP,
                onClick = onExit,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
                icon = AppIcon.LOGOUT,
            )
            AppButton(
                text = Strings.RELOCK_BUTTON,
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
        title = Strings.UNPAIR_RECEIVER_TITLE,
        icon = AppIcon.LINK,
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = Strings.UNPAIR_RECEIVER_CONFIRM,
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
            )
            AppButton(
                text = Strings.UNPAIR_BUTTON,
                onClick = onConfirm,
                variant = AppButtonVariant.DANGER,
                size = AppButtonSize.SMALL,
                icon = AppIcon.LINK,
            )
        },
    )
}

