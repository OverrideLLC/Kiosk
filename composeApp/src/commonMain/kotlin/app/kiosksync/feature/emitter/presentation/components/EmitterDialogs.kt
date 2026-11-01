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
import app.kiosksync.app.shell.LocalAppPreferences
import app.kiosksync.core.common.app.AppTheme
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
import app.kiosksync.resources.Res
import app.kiosksync.resources.add_button
import app.kiosksync.resources.cancel_button
import app.kiosksync.resources.change_role
import app.kiosksync.resources.change_role_confirm
import app.kiosksync.resources.close_button
import app.kiosksync.resources.guide_button
import app.kiosksync.resources.guide_replay_hint
import app.kiosksync.resources.guide_title
import app.kiosksync.resources.kiosk_default_sequence_hint
import app.kiosksync.resources.kiosk_lock_active_hint
import app.kiosksync.resources.kiosk_password_applies_now
import app.kiosksync.resources.kiosk_password_change_title
import app.kiosksync.resources.kiosk_password_confirm_field
import app.kiosksync.resources.kiosk_password_field
import app.kiosksync.resources.kiosk_password_instruction
import app.kiosksync.resources.kiosk_password_pair_hint
import app.kiosksync.resources.kiosk_password_pair_title
import app.kiosksync.resources.kiosk_password_rule
import app.kiosksync.resources.kiosk_password_title
import app.kiosksync.resources.kiosk_universal_hint
import app.kiosksync.resources.manual_ip_dialog_instruction
import app.kiosksync.resources.manual_ip_dialog_title
import app.kiosksync.resources.manual_ip_field
import app.kiosksync.resources.manual_ip_hint
import app.kiosksync.resources.pair_code_field
import app.kiosksync.resources.pair_code_placeholder
import app.kiosksync.resources.pair_code_required
import app.kiosksync.resources.pair_dialog_cancel
import app.kiosksync.resources.pair_dialog_confirm
import app.kiosksync.resources.pair_dialog_instruction
import app.kiosksync.resources.pair_dialog_title
import app.kiosksync.resources.pairing_in_progress
import app.kiosksync.resources.save_button
import app.kiosksync.resources.settings_reduce_motion
import app.kiosksync.resources.settings_reduce_motion_hint
import app.kiosksync.resources.settings_theme
import app.kiosksync.resources.settings_theme_hint
import app.kiosksync.resources.settings_title
import app.kiosksync.resources.theme_dark
import app.kiosksync.resources.theme_light
import app.kiosksync.resources.unpair_button
import app.kiosksync.resources.unpair_dialog_confirm
import app.kiosksync.resources.unpair_dialog_title
import org.jetbrains.compose.resources.stringResource
import app.kiosksync.core.ui.resolve

@Composable
internal fun SettingsDialog(
    onOpenGuide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val preferences = LocalAppPreferences.current
    val theme = preferences.theme

    AppDialog(
        title = stringResource(Res.string.settings_title),
        icon = AppIcon.SETTINGS,
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = stringResource(Res.string.settings_theme),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            AppButtonGroup(
                options = listOf(stringResource(Res.string.theme_dark), stringResource(Res.string.theme_light)),
                selectedIndex = if (theme == AppTheme.DARK) 0 else 1,
                onSelect = { index ->
                    preferences.updateTheme(if (index == 0) AppTheme.DARK else AppTheme.LIGHT)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = stringResource(Res.string.settings_theme_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )

            Spacer(Modifier.height(Space.lg))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.settings_reduce_motion),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.settings_reduce_motion_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(Space.md))
                Switch(
                    checked = preferences.reduceMotion,
                    onCheckedChange = preferences::updateReduceMotion,
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
                text = stringResource(Res.string.guide_title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = stringResource(Res.string.guide_replay_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.sm))
            AppButton(
                text = stringResource(Res.string.guide_button),
                onClick = onOpenGuide,
                variant = AppButtonVariant.TONAL,
                size = AppButtonSize.SMALL,
                icon = AppIcon.HELP,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        actions = {
            AppButton(
                text = stringResource(Res.string.close_button),
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
        title = "${stringResource(Res.string.pair_dialog_title)}: ${device.deviceName}",
        icon = AppIcon.ADD_LINK,
        onDismissRequest = onDismiss,
        dismissible = !isPairing,
        content = {
            Text(
                text = stringResource(Res.string.pair_dialog_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))
            AppTextField(
                value = code,
                onValueChange = onCodeChange,
                label = stringResource(Res.string.pair_code_field),
                placeholder = stringResource(Res.string.pair_code_placeholder),
                error = error,
                helper = if (error == null) stringResource(Res.string.pair_code_required) else null,
                enabled = !isPairing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        },
        actions = {
            AppButton(
                text = stringResource(Res.string.pair_dialog_cancel),
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
                enabled = !isPairing,
            )
            AppButton(
                text = if (isPairing) stringResource(Res.string.pairing_in_progress) else stringResource(Res.string.pair_dialog_confirm),
                onClick = onConfirm,
                size = AppButtonSize.SMALL,
                icon = AppIcon.LINK,
                enabled = code.length == 6,
                loading = isPairing,
                disabledReason = stringResource(Res.string.pair_code_required),
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
        title = stringResource(Res.string.manual_ip_dialog_title),
        icon = AppIcon.WIFI,
        onDismissRequest = onDismiss,
        dismissible = !isLoading,
        content = {
            Text(
                text = stringResource(Res.string.manual_ip_dialog_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))
            AppTextField(
                value = value,
                onValueChange = onValueChange,
                label = stringResource(Res.string.manual_ip_field),
                placeholder = stringResource(Res.string.manual_ip_hint),
                error = error,
                enabled = !isLoading,
            )
        },
        actions = {
            AppButton(
                text = stringResource(Res.string.cancel_button),
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
                enabled = !isLoading,
            )
            AppButton(
                text = if (isLoading) stringResource(Res.string.pairing_in_progress) else stringResource(Res.string.add_button),
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
        EmitterPendingAction.CONFIGURE -> stringResource(Res.string.kiosk_password_change_title)
        EmitterPendingAction.PAIR_LOCK -> stringResource(Res.string.kiosk_password_pair_title)
        else -> stringResource(Res.string.kiosk_password_title)
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
                    text = stringResource(Res.string.kiosk_password_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = stringResource(Res.string.kiosk_password_rule),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = stringResource(Res.string.kiosk_default_sequence_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )

                Spacer(Modifier.height(Space.md))
                AppTextField(
                    value = dialog.draft,
                    onValueChange = onDraftChange,
                    label = stringResource(Res.string.kiosk_password_field),
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(Space.sm))
                AppTextField(
                    value = dialog.confirmDraft,
                    onValueChange = onConfirmDraftChange,
                    label = stringResource(Res.string.kiosk_password_confirm_field),
                    visualTransformation = PasswordVisualTransformation(),
                )
                dialog.error?.let {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = it.resolve(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                    )
                }

                Spacer(Modifier.height(Space.md))
                WarningNote(
                    title = stringResource(Res.string.kiosk_lock_active_hint),
                    body = buildString {
                        if (dialog.action == EmitterPendingAction.CONFIGURE) {
                            append(stringResource(Res.string.kiosk_password_applies_now))
                        }
                        if (dialog.action == EmitterPendingAction.PAIR_LOCK) {
                            if (isNotEmpty()) append('\n')
                            append(stringResource(Res.string.kiosk_password_pair_hint))
                        }
                        if (isNotEmpty()) append('\n')
                        append(stringResource(Res.string.kiosk_universal_hint))
                    },
                )
            }
        },
        actions = {
            AppButton(
                text = stringResource(Res.string.cancel_button),
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
            )
            AppButton(
                text = stringResource(Res.string.save_button),
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
            title = stringResource(Res.string.change_role),
            icon = AppIcon.SWAP_HORIZ,
            onDismissRequest = { onAction(EmitterAction.DismissRoleReset) },
            content = {
                Text(
                    text = stringResource(Res.string.change_role_confirm),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            actions = {
                AppButton(
                    text = stringResource(Res.string.cancel_button),
                    onClick = { onAction(EmitterAction.DismissRoleReset) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = stringResource(Res.string.change_role),
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
            error = state.pairingError?.resolve(),
            isPairing = state.isPairing,
            onCodeChange = { onAction(EmitterAction.UpdatePairingCode(it)) },
            onConfirm = { onAction(EmitterAction.ConfirmPair) },
            onDismiss = { onAction(EmitterAction.DismissPairDialog) },
        )
    }

    state.unpairDevice?.let { device ->
        AppDialog(
            title = stringResource(Res.string.unpair_dialog_title),
            icon = AppIcon.LINK,
            onDismissRequest = { onAction(EmitterAction.DismissUnpairDialog) },
            content = {
                Text(
                    text = "${stringResource(Res.string.unpair_dialog_confirm)}\n\n${device.deviceName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            actions = {
                AppButton(
                    text = stringResource(Res.string.cancel_button),
                    onClick = { onAction(EmitterAction.DismissUnpairDialog) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = stringResource(Res.string.unpair_button),
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
            error = state.manualIpError?.resolve(),
            isLoading = state.isManualIpLoading,
            onValueChange = { onAction(EmitterAction.UpdateManualIp(it)) },
            onConfirm = { onAction(EmitterAction.ConfirmManualIp) },
            onDismiss = { onAction(EmitterAction.DismissManualIpDialog) },
        )
    }
}
