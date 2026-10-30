package app.kiosksync.feature.emitter.presentation.components

import app.kiosksync.feature.emitter.domain.EmitterUpdateStatus
import app.kiosksync.feature.emitter.domain.EmitterUpdateState
import app.kiosksync.feature.emitter.domain.models.DeviceSyncUi
import app.kiosksync.feature.emitter.domain.models.EmitterDeviceUi
import app.kiosksync.feature.emitter.domain.models.SyncUiStatus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.AppVersionPolicy
import app.kiosksync.core.Strings
import app.kiosksync.core.UpdateUrgency
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDropdownMenu
import app.kiosksync.core.ui.components.AppEmptyState
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppIconButton
import app.kiosksync.core.ui.components.AppIconButtonSize
import app.kiosksync.core.ui.components.AppLinearProgress
import app.kiosksync.core.ui.components.AppLoadingIndicator
import app.kiosksync.core.ui.components.AppPanelHeader
import app.kiosksync.core.ui.components.AppMenuItem
import app.kiosksync.core.ui.components.AppStatusChip
import app.kiosksync.core.ui.components.AppStatusTone
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.components.OverrideLogo
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space

@Composable
internal fun DeviceCard(
    device: EmitterDeviceUi,
    sync: DeviceSyncUi?,
    update: EmitterUpdateState?,
    onToggleSelection: () -> Unit,
    onPair: () -> Unit,
    onUnpair: () -> Unit,
    onUpdate: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val extra = KioskColors.extra
    val selected = device.isSelected && device.isPairedToThisEmitter
    val versionPlan = device.appVersion?.let { screenVersion ->
        AppVersionPolicy.plan(APP_VERSION, screenVersion)
    }
    val canRequestUpdate = device.isPairedToThisEmitter &&
        versionPlan != null &&
        versionPlan.urgency != UpdateUrgency.NONE &&
        update?.status in setOf(
            EmitterUpdateStatus.FAILED,
            EmitterUpdateStatus.SKIPPED,
            EmitterUpdateStatus.UNAVAILABLE,
            EmitterUpdateStatus.AWAITING_CONFIRMATION
        )

    val statusLabel: String
    val statusTone: AppStatusTone
    val statusIcon: AppIcon
    when {
        device.isPairedToThisEmitter && device.isOnline -> {
            statusLabel = Strings.STATUS_ONLINE
            statusTone = AppStatusTone.SUCCESS
            statusIcon = AppIcon.WIFI
        }

        device.isPairedToThisEmitter -> {
            statusLabel = Strings.STATUS_OFFLINE
            statusTone = AppStatusTone.WARNING
            statusIcon = AppIcon.WIFI_OFF
        }

        device.isPairedToAnotherEmitter -> {
            statusLabel = Strings.PAIRED_OTHER_CONTROLLER
            statusTone = AppStatusTone.WARNING
            statusIcon = AppIcon.LINK
        }

        else -> {
            statusLabel = Strings.STATUS_UNPAIRED
            statusTone = AppStatusTone.NEUTRAL
            statusIcon = AppIcon.LINK
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KioskShape.card,
        color = if (selected) colors.primaryContainer else colors.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) Sizes.selectionBorder else Sizes.hairline,
            color = if (selected) colors.primary else colors.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(Space.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(Sizes.iconBubbleSmall)
                        .clip(CircleShape)
                        .background(
                            if (selected) colors.primary else colors.surfaceContainerHigh,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        icon = AppIcon.TV,
                        contentDescription = null,
                        size = Sizes.iconCompact,
                        tint = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(Space.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${device.host}:${device.port} · " +
                            Strings.screenVersion(device.appVersion),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    )
                }
                if (device.isPairedToThisEmitter) {
                    Checkbox(
                        checked = device.isSelected,
                        onCheckedChange = { onToggleSelection() },
                        modifier = Modifier.semantics {
                            contentDescription = Strings.selectScreen(device.deviceName)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.primary,
                            uncheckedColor = colors.outline,
                            checkmarkColor = colors.onPrimary,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(Space.xs))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AppStatusChip(
                    label = statusLabel,
                    tone = statusTone,
                    icon = statusIcon,
                )
                Spacer(Modifier.weight(1f))
                when {
                    device.isPairedToThisEmitter -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.xxs),
                        ) {
                            if (canRequestUpdate) {
                                AppButton(
                                    text = Strings.UPDATE_BUTTON,
                                    onClick = onUpdate,
                                    variant = AppButtonVariant.TONAL,
                                    size = AppButtonSize.SMALL,
                                    icon = AppIcon.REFRESH,
                                )
                            }
                            AppButton(
                                text = Strings.UNPAIR_BUTTON,
                                onClick = onUnpair,
                                variant = AppButtonVariant.TEXT,
                                size = AppButtonSize.SMALL,
                                icon = AppIcon.LINK,
                            )
                        }
                    }

                    !device.isPairedToAnotherEmitter -> {
                        AppButton(
                            text = Strings.PAIR_BUTTON,
                            onClick = onPair,
                            variant = AppButtonVariant.TONAL,
                            size = AppButtonSize.SMALL,
                            icon = AppIcon.ADD_LINK,
                        )
                    }
                }
            }

            sync?.let {
                Spacer(Modifier.height(Space.xs))
                SyncStatusRow(it)
            }

            update?.let {
                Spacer(Modifier.height(Space.xs))
                UpdateStatusRow(it, device.appVersion)
            }
        }
    }
}

@Composable
internal fun UpdateStatusRow(update: EmitterUpdateState, deviceVersion: String?) {
    val colors = MaterialTheme.colorScheme
    val extra = KioskColors.extra

    when (update.status) {
        EmitterUpdateStatus.PREPARING,
        EmitterUpdateStatus.UPLOADING,
        EmitterUpdateStatus.APPLYING -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppLoadingIndicator(
                color = colors.primary,
                dotSize = Sizes.loadingDotSmall,
            )
            Text(
                text = when (update.status) {
                    EmitterUpdateStatus.PREPARING -> Strings.UPDATE_SEND_PREPARING
                    EmitterUpdateStatus.UPLOADING -> Strings.UPDATE_SEND_UPLOADING
                    else -> Strings.UPDATE_SEND_APPLYING
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        EmitterUpdateStatus.AWAITING_CONFIRMATION -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppIcon(
                icon = AppIcon.CHECK_CIRCLE,
                contentDescription = null,
                size = Sizes.iconSmall,
                tint = colors.primary,
            )
            Text(
                text = Strings.UPDATE_SEND_WAITING,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        EmitterUpdateStatus.SKIPPED -> Text(
            text = deviceVersion?.let(Strings::updateSkipped) ?: Strings.updateSkipped(update.version),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        EmitterUpdateStatus.FAILED -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppIcon(
                icon = AppIcon.ERROR,
                contentDescription = null,
                size = Sizes.iconSmall,
                tint = colors.error,
            )
            Text(
                text = Strings.UPDATE_SEND_FAILED,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
            )
        }

        EmitterUpdateStatus.UNAVAILABLE -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppIcon(
                icon = AppIcon.WARNING,
                contentDescription = null,
                size = Sizes.iconSmall,
                tint = extra.warning,
            )
            Text(
                text = Strings.UPDATE_SEND_UNAVAILABLE,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SyncStatusRow(sync: DeviceSyncUi) {
    val colors = MaterialTheme.colorScheme
    val extra = KioskColors.extra

    when (sync.status) {
        SyncUiStatus.PENDING -> Text(
            text = Strings.SEND_DEVICE_PENDING,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        SyncUiStatus.UPLOADING -> {
            AppLinearProgress(
                progress = if (sync.total > 0) {
                    sync.uploaded.toFloat() / sync.total.toFloat()
                } else {
                    0f
                },
                label = if (sync.total > 0) {
                    Strings.syncUploading(sync.uploaded, sync.total)
                } else {
                    Strings.SEND_DEVICE_UPLOADING
                },
            )
        }

        SyncUiStatus.APPLYING -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppLoadingIndicator(
                color = colors.primary,
                dotSize = Sizes.loadingDotSmall,
            )
            Text(
                text = Strings.SEND_DEVICE_APPLYING,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        SyncUiStatus.SUCCESS -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppIcon(
                icon = AppIcon.CHECK_CIRCLE,
                contentDescription = null,
                size = Sizes.iconSmall,
                tint = extra.success,
            )
            Text(
                text = "${sync.message ?: Strings.SEND_DEVICE_SUCCESS} (${Strings.versionTag(sync.version)})",
                style = MaterialTheme.typography.bodySmall,
                color = extra.success,
            )
        }

        SyncUiStatus.FAILED -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppIcon(
                icon = AppIcon.ERROR,
                contentDescription = null,
                size = Sizes.iconSmall,
                tint = colors.error,
            )
            Text(
                text = sync.message ?: Strings.SEND_DEVICE_FAILED,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
            )
        }
    }
}
