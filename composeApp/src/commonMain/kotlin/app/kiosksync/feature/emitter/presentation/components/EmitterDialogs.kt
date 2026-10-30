package app.kiosksync.feature.emitter.presentation.components

import app.kiosksync.feature.emitter.domain.models.EmitterDeviceUi
import app.kiosksync.feature.emitter.domain.models.EmitterPendingAction
import app.kiosksync.feature.emitter.domain.models.KioskPasswordDialogUi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import app.kiosksync.core.AppTheme
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonGroup
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDialog
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppTextField
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.feature.emitter.presentation.EmitterAction
import app.kiosksync.feature.emitter.presentation.EmitterState

@Composable
internal fun SettingsDialog(
    theme: AppTheme,
    reduceMotion: Boolean,
    onThemeChange: (AppTheme) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onOpenGuide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    AppDialog(
        title = Strings.SETTINGS_TITLE,
        icon = AppIcon.SETTINGS,
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = Strings.SETTINGS_THEME,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            AppButtonGroup(
                options = listOf(Strings.THEME_DARK, Strings.THEME_LIGHT),
                selectedIndex = if (theme == AppTheme.DARK) 0 else 1,
                onSelect = { index ->
                    onThemeChange(if (index == 0) AppTheme.DARK else AppTheme.LIGHT)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = Strings.SETTINGS_THEME_HINT,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )

            Spacer(Modifier.height(Space.lg))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Strings.SETTINGS_REDUCE_MOTION,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                    )
                    Text(
                        text = Strings.SETTINGS_REDUCE_MOTION_HINT,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(Space.md))
                Switch(
                    checked = reduceMotion,
                    onCheckedChange = onReduceMotionChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onPrimary,
                        checkedTrackColor = colors.primary,
                        uncheckedThumbColor = colors.outline,
                        uncheckedTrackColor = colors.surfaceContainerHighest,
                    ),
                )
            }

            Spacer(Modifier.height(Space.lg))
            Text(
                text = Strings.GUIDE_TITLE,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = Strings.GUIDE_REPLAY_HINT,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.sm))
            AppButton(
                text = Strings.GUIDE_BUTTON,
                onClick = onOpenGuide,
                variant = AppButtonVariant.TONAL,
                size = AppButtonSize.SMALL,
                icon = AppIcon.HELP,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        actions = {
            AppButton(
                text = Strings.CLOSE_BUTTON,
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
            )
        },
    )
}

@Composable
internal fun PairScreenDialog(
    device: EmitterDeviceUi,
    code: String,
    error: String?,
    isPairing: Boolean,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    AppDialog(
        title = "${Strings.PAIR_DIALOG_TITLE}: ${device.deviceName}",
        icon = AppIcon.ADD_LINK,
        onDismissRequest = onDismiss,
        dismissible = !isPairing,
        content = {
            Text(
                text = Strings.PAIR_DIALOG_INSTRUCTION,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))
            AppTextField(
                value = code,
                onValueChange = onCodeChange,
                label = Strings.PAIR_CODE_FIELD,
                placeholder = Strings.PAIR_CODE_PLACEHOLDER,
                error = error,
                helper = if (error == null) Strings.PAIR_CODE_REQUIRED else null,
                enabled = !isPairing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        },
        actions = {
            AppButton(
                text = Strings.PAIR_DIALOG_CANCEL,
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
                enabled = !isPairing,
            )
            AppButton(
                text = if (isPairing) Strings.PAIRING_IN_PROGRESS else Strings.PAIR_DIALOG_CONFIRM,
                onClick = onConfirm,
                size = AppButtonSize.SMALL,
                icon = AppIcon.LINK,
                enabled = code.length == 6,
                loading = isPairing,
                disabledReason = Strings.PAIR_CODE_REQUIRED,
            )
        },
    )
}

@Composable
internal fun ManualIpDialog(
    value: String,
    error: String?,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    AppDialog(
        title = Strings.MANUAL_IP_DIALOG_TITLE,
        icon = AppIcon.WIFI,
        onDismissRequest = onDismiss,
        dismissible = !isLoading,
        content = {
            Text(
                text = Strings.MANUAL_IP_DIALOG_INSTRUCTION,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))
            AppTextField(
                value = value,
                onValueChange = onValueChange,
                label = Strings.MANUAL_IP_FIELD,
                placeholder = Strings.MANUAL_IP_HINT,
                error = error,
                enabled = !isLoading,
            )
        },
        actions = {
            AppButton(
                text = Strings.CANCEL_BUTTON,
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
                enabled = !isLoading,
            )
            AppButton(
                text = if (isLoading) Strings.PAIRING_IN_PROGRESS else Strings.ADD_BUTTON,
                onClick = onConfirm,
                size = AppButtonSize.SMALL,
                icon = AppIcon.ADD,
                enabled = !isLoading,
                loading = isLoading,
            )
        },
    )
}

@Composable
internal fun KioskPasswordDialog(
    dialog: KioskPasswordDialogUi,
    onDraftChange: (String) -> Unit,
    onConfirmDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val extra = KioskColors.extra
    val title = when (dialog.action) {
        EmitterPendingAction.CONFIGURE -> Strings.KIOSK_PASSWORD_CHANGE_TITLE
        EmitterPendingAction.PAIR_LOCK -> Strings.KIOSK_PASSWORD_PAIR_TITLE
        else -> Strings.KIOSK_PASSWORD_TITLE
    }

    AppDialog(
        title = title,
        icon = AppIcon.LOCK,
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .heightIn(max = Sizes.dialogBodyMaxHeight)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = Strings.KIOSK_PASSWORD_INSTRUCTION,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = Strings.KIOSK_PASSWORD_RULE,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = Strings.KIOSK_DEFAULT_SEQUENCE_HINT,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )

                Spacer(Modifier.height(Space.md))
                AppTextField(
                    value = dialog.draft,
                    onValueChange = onDraftChange,
                    label = Strings.KIOSK_PASSWORD_FIELD,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(Space.sm))
                AppTextField(
                    value = dialog.confirmDraft,
                    onValueChange = onConfirmDraftChange,
                    label = Strings.KIOSK_PASSWORD_CONFIRM_FIELD,
                    visualTransformation = PasswordVisualTransformation(),
                )
                dialog.error?.let {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                    )
                }

                Spacer(Modifier.height(Space.md))
                WarningNote(
                    title = Strings.KIOSK_LOCK_ACTIVE_HINT,
                    body = buildString {
                        if (dialog.action == EmitterPendingAction.CONFIGURE) {
                            append(Strings.KIOSK_PASSWORD_APPLIES_NOW)
                        }
                        if (dialog.action == EmitterPendingAction.PAIR_LOCK) {
                            if (isNotEmpty()) append('\n')
                            append(Strings.KIOSK_PASSWORD_PAIR_HINT)
                        }
                        if (isNotEmpty()) append('\n')
                        append(Strings.KIOSK_UNIVERSAL_HINT)
                    },
                )
            }
        },
        actions = {
            AppButton(
                text = Strings.CANCEL_BUTTON,
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
            )
            AppButton(
                text = Strings.SAVE_BUTTON,
                onClick = onConfirm,
                size = AppButtonSize.SMALL,
                icon = AppIcon.LOCK,
            )
        },
    )
}

@Composable
fun EmitterDialogs(
    state: EmitterState,
    onAction: (EmitterAction) -> Unit,
) {

    state.passwordDialog?.let { dialog ->
        KioskPasswordDialog(
            dialog = dialog,
            onDraftChange = { onAction(EmitterAction.UpdatePasswordDraft(it)) },
            onConfirmDraftChange = { onAction(EmitterAction.UpdatePasswordConfirmDraft(it)) },
            onConfirm = { onAction(EmitterAction.ConfirmPassword) },
            onDismiss = { onAction(EmitterAction.DismissPasswordDialog) },
        )
    }

    state.securityDialog?.let { dialog ->
        SecurityDialog(
            dialog = dialog,
            hasPassword = state.hasKioskPassword,
            onAddKey = { onAction(EmitterAction.AppendUnlockKey(it)) },
            onRemoveLast = { onAction(EmitterAction.RemoveLastUnlockKey) },
            onResetSequence = { onAction(EmitterAction.ResetUnlockSequence) },
            onSaveSequence = { onAction(EmitterAction.SaveUnlockSequence) },
            onToggleRecovery = { onAction(EmitterAction.ToggleRecoveryEditor) },
            onAcceptedChange = { onAction(EmitterAction.SetRecoveryAccepted(it)) },
            onRecoveryChange = { onAction(EmitterAction.UpdateRecoveryDraft(it)) },
            onRecoveryConfirmChange = { onAction(EmitterAction.UpdateRecoveryConfirmDraft(it)) },
            onSaveRecovery = { onAction(EmitterAction.SaveRecoveryPhrase) },
            onResetRecovery = { onAction(EmitterAction.ResetRecoveryPhrase) },
            onChangePassword = { onAction(EmitterAction.OpenChangePasswordDialog) },
            onDismiss = { onAction(EmitterAction.DismissSecurityDialog) },
        )
    }

    if (state.isRoleResetConfirmationOpen) {
        AppDialog(
            title = Strings.CHANGE_ROLE,
            icon = AppIcon.SWAP_HORIZ,
            onDismissRequest = { onAction(EmitterAction.DismissRoleReset) },
            content = {
                Text(
                    text = Strings.CHANGE_ROLE_CONFIRM,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            actions = {
                AppButton(
                    text = Strings.CANCEL_BUTTON,
                    onClick = { onAction(EmitterAction.DismissRoleReset) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = Strings.CHANGE_ROLE,
                    onClick = { onAction(EmitterAction.ConfirmRoleReset) },
                    variant = AppButtonVariant.TONAL,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.SWAP_HORIZ,
                )
            },
        )
    }

    state.pairingDevice?.let { device ->
        PairScreenDialog(
            device = device,
            code = state.pairingCode,
            error = state.pairingError,
            isPairing = state.isPairing,
            onCodeChange = { onAction(EmitterAction.UpdatePairingCode(it)) },
            onConfirm = { onAction(EmitterAction.ConfirmPair) },
            onDismiss = { onAction(EmitterAction.DismissPairDialog) },
        )
    }

    state.unpairDevice?.let { device ->
        AppDialog(
            title = Strings.UNPAIR_DIALOG_TITLE,
            icon = AppIcon.LINK,
            onDismissRequest = { onAction(EmitterAction.DismissUnpairDialog) },
            content = {
                Text(
                    text = "${Strings.UNPAIR_DIALOG_CONFIRM}\n\n${device.deviceName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            actions = {
                AppButton(
                    text = Strings.CANCEL_BUTTON,
                    onClick = { onAction(EmitterAction.DismissUnpairDialog) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = Strings.UNPAIR_BUTTON,
                    onClick = { onAction(EmitterAction.ConfirmUnpair) },
                    variant = AppButtonVariant.DANGER,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.LINK,
                )
            },
        )
    }

    if (state.showManualIpDialog) {
        ManualIpDialog(
            value = state.manualIpInput,
            error = state.manualIpError,
            isLoading = state.isManualIpLoading,
            onValueChange = { onAction(EmitterAction.UpdateManualIp(it)) },
            onConfirm = { onAction(EmitterAction.ConfirmManualIp) },
            onDismiss = { onAction(EmitterAction.DismissManualIpDialog) },
        )
    }
}
