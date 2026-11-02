package app.kiosksync.feature.emitter.presentation.components

import app.kiosksync.feature.emitter.domain.models.SecurityDialogUi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.PasswordVisualTransformation
import app.kiosksync.core.common.kiosk.formatUnlockSequence
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonGroup
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDialog
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppTextField
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.resources.Res
import app.kiosksync.resources.cancel_button
import app.kiosksync.resources.security_change_password
import app.kiosksync.resources.security_close
import app.kiosksync.resources.security_no_password
import app.kiosksync.resources.security_password_section_hint
import app.kiosksync.resources.security_recovery_accept
import app.kiosksync.resources.security_recovery_change
import app.kiosksync.resources.security_recovery_confirm_field
import app.kiosksync.resources.security_recovery_custom
import app.kiosksync.resources.security_recovery_default
import app.kiosksync.resources.security_recovery_field
import app.kiosksync.resources.security_recovery_must_accept
import app.kiosksync.resources.security_recovery_reset
import app.kiosksync.resources.security_recovery_rule
import app.kiosksync.resources.security_recovery_save
import app.kiosksync.resources.security_recovery_warning
import app.kiosksync.resources.security_recovery_warning_title
import app.kiosksync.resources.security_sequence_add
import app.kiosksync.resources.security_sequence_current
import app.kiosksync.resources.security_sequence_empty
import app.kiosksync.resources.security_sequence_hint
import app.kiosksync.resources.security_sequence_min_hint
import app.kiosksync.resources.security_sequence_remove
import app.kiosksync.resources.security_sequence_reset
import app.kiosksync.resources.security_sequence_save
import app.kiosksync.resources.security_tab_password
import app.kiosksync.resources.security_tab_recovery
import app.kiosksync.resources.security_tab_sequence
import app.kiosksync.resources.security_title
import org.jetbrains.compose.resources.stringResource
import app.kiosksync.core.ui.resolve

@Composable
internal fun SecurityDialog(
    dialog: SecurityDialogUi,
    hasPassword: Boolean,
    onAddKey: (String) -> Unit,
    onRemoveLast: () -> Unit,
    onResetSequence: () -> Unit,
    onSaveSequence: () -> Unit,
    onToggleRecovery: () -> Unit,
    onAcceptedChange: (Boolean) -> Unit,
    onRecoveryChange: (String) -> Unit,
    onRecoveryConfirmChange: (String) -> Unit,
    onSaveRecovery: () -> Unit,
    onResetRecovery: () -> Unit,
    onChangePassword: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val extra = KioskColors.extra
    var tab by remember { mutableStateOf(0) }

    AppDialog(
        title = stringResource(Res.string.security_title),
        icon = AppIcon.LOCK,
        onDismissRequest = onDismiss,
        maxWidth = Sizes.dialogWideMaxWidth,
        content = {
            AppButtonGroup(
                options = listOf(
                    stringResource(Res.string.security_tab_password),
                    stringResource(Res.string.security_tab_sequence),
                    stringResource(Res.string.security_tab_recovery),
                ),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Space.md))

            when (tab) {
                0 -> {
                    Text(
                        text = if (hasPassword) {
                            stringResource(Res.string.security_password_section_hint)
                        } else {
                            "${stringResource(Res.string.security_no_password)} ${stringResource(Res.string.security_password_section_hint)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.md))
                    AppButton(
                        text = stringResource(Res.string.security_change_password),
                        onClick = onChangePassword,
                        variant = AppButtonVariant.TONAL,
                        size = AppButtonSize.SMALL,
                        icon = AppIcon.LOCK,
                    )
                }

                1 -> {
                    Text(
                        text = stringResource(Res.string.security_sequence_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        text = stringResource(Res.string.security_sequence_current),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = if (dialog.sequence.isEmpty()) {
                            stringResource(Res.string.security_sequence_empty)
                        } else {
                            formatUnlockSequence(dialog.sequence)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                    )
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        text = stringResource(Res.string.security_sequence_add),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        listOf("SHIFT", "CTRL", "ALT").forEach { key ->
                            AppButton(
                                text = formatUnlockSequence(listOf(key)),
                                onClick = { onAddKey(key) },
                                variant = AppButtonVariant.TONAL,
                                size = AppButtonSize.SMALL,
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        AppButton(
                            text = stringResource(Res.string.security_sequence_remove),
                            onClick = onRemoveLast,
                            variant = AppButtonVariant.OUTLINED,
                            size = AppButtonSize.SMALL,
                            enabled = dialog.sequence.isNotEmpty(),
                        )
                        AppButton(
                            text = stringResource(Res.string.security_sequence_reset),
                            onClick = onResetSequence,
                            variant = AppButtonVariant.TEXT,
                            size = AppButtonSize.SMALL,
                        )
                    }
                    Text(
                        text = stringResource(Res.string.security_sequence_min_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                    dialog.sequenceError?.let {
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            text = it.resolve(),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error,
                        )
                    }
                    Spacer(Modifier.height(Space.sm))
                    AppButton(
                        text = stringResource(Res.string.security_sequence_save),
                        onClick = onSaveSequence,
                        size = AppButtonSize.SMALL,
                        icon = AppIcon.CHECK,
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .heightIn(max = Sizes.dialogBodyMaxHeight)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = if (dialog.hasCustomRecovery) {
                                stringResource(Res.string.security_recovery_custom)
                            } else {
                                stringResource(Res.string.security_recovery_default)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (dialog.hasCustomRecovery) {
                                extra.warning
                            } else {
                                colors.onSurfaceVariant
                            },
                        )
                        Spacer(Modifier.height(Space.sm))

                        if (!dialog.showRecoveryEditor) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                                AppButton(
                                    text = stringResource(Res.string.security_recovery_change),
                                    onClick = onToggleRecovery,
                                    variant = AppButtonVariant.TONAL,
                                    size = AppButtonSize.SMALL,
                                    icon = AppIcon.EDIT,
                                )
                                if (dialog.hasCustomRecovery) {
                                    AppButton(
                                        text = stringResource(Res.string.security_recovery_reset),
                                        onClick = onResetRecovery,
                                        variant = AppButtonVariant.TEXT,
                                        size = AppButtonSize.SMALL,
                                        icon = AppIcon.UNDO,
                                    )
                                }
                            }
                        } else {
                            WarningNote(
                                title = stringResource(Res.string.security_recovery_warning_title),
                                body = stringResource(Res.string.security_recovery_warning),
                            )
                            Spacer(Modifier.height(Space.sm))
                            Row(
                                modifier = Modifier.clickable {
                                    onAcceptedChange(!dialog.recoveryAccepted)
                                },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = dialog.recoveryAccepted,
                                    onCheckedChange = { onAcceptedChange(it) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = extra.warning,
                                        uncheckedColor = colors.outline,
                                        checkmarkColor = extra.onWarning,
                                    ),
                                )
                                Text(
                                    text = stringResource(Res.string.security_recovery_accept),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(Space.sm))
                            AppTextField(
                                value = dialog.recoveryDraft,
                                onValueChange = onRecoveryChange,
                                label = stringResource(Res.string.security_recovery_field),
                                enabled = dialog.recoveryAccepted,
                                visualTransformation = PasswordVisualTransformation(),
                            )
                            Spacer(Modifier.height(Space.sm))
                            AppTextField(
                                value = dialog.recoveryConfirmDraft,
                                onValueChange = onRecoveryConfirmChange,
                                label = stringResource(Res.string.security_recovery_confirm_field),
                                enabled = dialog.recoveryAccepted,
                                visualTransformation = PasswordVisualTransformation(),
                                error = dialog.recoveryError?.resolve(),
                            )
                            Text(
                                text = stringResource(Res.string.security_recovery_rule),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(Space.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                                AppButton(
                                    text = stringResource(Res.string.security_recovery_save),
                                    onClick = onSaveRecovery,
                                    size = AppButtonSize.SMALL,
                                    icon = AppIcon.LOCK,
                                    enabled = dialog.recoveryAccepted,
                                    disabledReason = stringResource(Res.string.security_recovery_must_accept),
                                )
                                AppButton(
                                    text = stringResource(Res.string.cancel_button),
                                    onClick = onToggleRecovery,
                                    variant = AppButtonVariant.TEXT,
                                    size = AppButtonSize.SMALL,
                                )
                            }
                        }
                    }
                }
            }
        },
        actions = {
            AppButton(
                text = stringResource(Res.string.security_close),
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
            )
        },
    )
}

@Composable
internal fun WarningNote(title: String, body: String) {
    val extra = KioskColors.extra

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KioskShape.small,
        color = extra.warningContainer,
        contentColor = extra.onWarningContainer,
    ) {
        Row(
            modifier = Modifier.padding(Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppIcon(
                icon = AppIcon.WARNING,
                contentDescription = null,
                size = Sizes.iconCompact,
                tint = extra.onWarningContainer,
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = extra.onWarningContainer,
                )
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = extra.onWarningContainer,
                )
            }
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
internal fun SectionDivider() {
    HorizontalDivider(
        thickness = Sizes.securityDivider,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
