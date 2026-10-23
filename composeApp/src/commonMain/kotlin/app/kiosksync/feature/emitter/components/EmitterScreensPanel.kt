package app.kiosksync.feature.emitter.components

import app.kiosksync.feature.emitter.actions.EmitterAction
import app.kiosksync.feature.emitter.state.EmitterUiState

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
internal fun ScreensPanel(
    onAction: (EmitterAction) -> Unit,
    state: EmitterUiState,
    fillHeight: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AppPanelHeader(
            title = Strings.EMITTER_SCREENS_TITLE,
            subtitle = Strings.EMITTER_SCREENS_SUBTITLE,
            actions = {
                AppIconButton(
                    icon = AppIcon.REFRESH,
                    contentDescription = Strings.DISCOVERY_REFRESH,
                    onClick = { onAction(EmitterAction.RefreshDevices) },
                    enabled = !state.isRefreshing,
                    tooltip = if (state.isRefreshing) {
                        Strings.DISCOVERY_REFRESHING
                    } else {
                        Strings.DISCOVERY_REFRESH
                    },
                )
                AppButton(
                    text = Strings.ADD_BY_IP,
                    onClick = { onAction(EmitterAction.OpenManualIp) },
                    variant = AppButtonVariant.OUTLINED,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.ADD,
                )
            },
        )

        Spacer(Modifier.height(Space.xs))

        val listModifier = if (fillHeight) {
            Modifier.fillMaxWidth().weight(1f)
        } else {
            Modifier.fillMaxWidth().heightIn(min = Sizes.emptyStateMinHeight)
        }
        if (state.devices.isEmpty()) {
            Box(modifier = listModifier, contentAlignment = Alignment.Center) {
                AppEmptyState(
                    icon = AppIcon.TV,
                    title = if (state.isSearching) Strings.DISCOVERY_SEARCHING else Strings.DISCOVERY_EMPTY,
                )
            }
        } else {
            LazyColumn(
                modifier = listModifier,
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                itemsIndexed(state.devices, key = { _, device -> device.deviceId }) { _, device ->
                    DeviceCard(
                        device = device,
                        sync = state.syncByDevice[device.deviceId],
                        update = state.updateByDevice[device.deviceId],
                        onToggleSelection = {
                            onAction(EmitterAction.ToggleSelection(device.deviceId))
                        },
                        onPair = { onAction(EmitterAction.OpenPairDialog(device)) },
                        onUnpair = { onAction(EmitterAction.RequestUnpair(device)) },
                        onUpdate = { onAction(EmitterAction.RequestUpdate(device.deviceId)) },
                    )
                }
            }
        }
    }
}

