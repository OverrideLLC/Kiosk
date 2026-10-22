package app.kiosksync.feature.emitter.presentation

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
import app.kiosksync.core.Strings
import app.kiosksync.core.formatUnlockSequence
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
    val colors = KioskColors.scheme
    val extra = KioskColors.extra
    var tab by remember { mutableStateOf(0) }

    AppDialog(
        title = Strings.SECURITY_TITLE,
        icon = AppIcon.LOCK,
        onDismissRequest = onDismiss,
        maxWidth = Sizes.dialogWideMaxWidth,
        content = {
            AppButtonGroup(
                options = listOf(
                    Strings.SECURITY_TAB_PASSWORD,
                    Strings.SECURITY_TAB_SEQUENCE,
                    Strings.SECURITY_TAB_RECOVERY,
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
                            Strings.SECURITY_PASSWORD_SECTION_HINT
                        } else {
                            "${Strings.SECURITY_NO_PASSWORD} ${Strings.SECURITY_PASSWORD_SECTION_HINT}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.md))
                    AppButton(
                        text = Strings.SECURITY_CHANGE_PASSWORD,
                        onClick = onChangePassword,
                        variant = AppButtonVariant.TONAL,
                        size = AppButtonSize.SMALL,
                        icon = AppIcon.LOCK,
                    )
                }

                1 -> {
                    Text(
                        text = Strings.SECURITY_SEQUENCE_HINT,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        text = Strings.SECURITY_SEQUENCE_CURRENT,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = if (dialog.sequence.isEmpty()) {
                            Strings.SECURITY_SEQUENCE_EMPTY
                        } else {
                            formatUnlockSequence(dialog.sequence)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                    )
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        text = Strings.SECURITY_SEQUENCE_ADD,
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
                            text = Strings.SECURITY_SEQUENCE_REMOVE,
                            onClick = onRemoveLast,
                            variant = AppButtonVariant.OUTLINED,
                            size = AppButtonSize.SMALL,
                            enabled = dialog.sequence.isNotEmpty(),
                        )
                        AppButton(
                            text = Strings.SECURITY_SEQUENCE_RESET,
                            onClick = onResetSequence,
                            variant = AppButtonVariant.TEXT,
                            size = AppButtonSize.SMALL,
                        )
                    }
                    Text(
                        text = Strings.SECURITY_SEQUENCE_MIN_HINT,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                    dialog.sequenceError?.let {
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error,
                        )
                    }
                    Spacer(Modifier.height(Space.sm))
                    AppButton(
                        text = Strings.SECURITY_SEQUENCE_SAVE,
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
                                Strings.SECURITY_RECOVERY_CUSTOM
                            } else {
                                Strings.SECURITY_RECOVERY_DEFAULT
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
                                    text = Strings.SECURITY_RECOVERY_CHANGE,
                                    onClick = onToggleRecovery,
                                    variant = AppButtonVariant.TONAL,
                                    size = AppButtonSize.SMALL,
                                    icon = AppIcon.EDIT,
                                )
                                if (dialog.hasCustomRecovery) {
                                    AppButton(
                                        text = Strings.SECURITY_RECOVERY_RESET,
                                        onClick = onResetRecovery,
                                        variant = AppButtonVariant.TEXT,
                                        size = AppButtonSize.SMALL,
                                        icon = AppIcon.UNDO,
                                    )
                                }
                            }
                        } else {
                            WarningNote(
                                title = Strings.SECURITY_RECOVERY_WARNING_TITLE,
                                body = Strings.SECURITY_RECOVERY_WARNING,
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
                                    text = Strings.SECURITY_RECOVERY_ACCEPT,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(Space.sm))
                            AppTextField(
                                value = dialog.recoveryDraft,
                                onValueChange = onRecoveryChange,
                                label = Strings.SECURITY_RECOVERY_FIELD,
                                enabled = dialog.recoveryAccepted,
                                visualTransformation = PasswordVisualTransformation(),
                            )
                            Spacer(Modifier.height(Space.sm))
                            AppTextField(
                                value = dialog.recoveryConfirmDraft,
                                onValueChange = onRecoveryConfirmChange,
                                label = Strings.SECURITY_RECOVERY_CONFIRM_FIELD,
                                enabled = dialog.recoveryAccepted,
                                visualTransformation = PasswordVisualTransformation(),
                                error = dialog.recoveryError,
                            )
                            Text(
                                text = Strings.SECURITY_RECOVERY_RULE,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(Space.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                                AppButton(
                                    text = Strings.SECURITY_RECOVERY_SAVE,
                                    onClick = onSaveRecovery,
                                    size = AppButtonSize.SMALL,
                                    icon = AppIcon.LOCK,
                                    enabled = dialog.recoveryAccepted,
                                    disabledReason = Strings.SECURITY_RECOVERY_MUST_ACCEPT,
                                )
                                AppButton(
                                    text = Strings.CANCEL_BUTTON,
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
                text = Strings.SECURITY_CLOSE,
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
        color = KioskColors.scheme.onSurface,
    )
}

@Composable
internal fun SectionDivider() {
    HorizontalDivider(
        thickness = Sizes.securityDivider,
        color = KioskColors.scheme.outlineVariant,
    )
}
