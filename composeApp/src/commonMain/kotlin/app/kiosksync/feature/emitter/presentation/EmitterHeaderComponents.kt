package app.kiosksync.feature.emitter.presentation

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
internal fun ControllerHeader(
    hasPassword: Boolean,
    onOpenSecurity: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangeRole: () -> Unit,
    onExit: () -> Unit,
) {
    val colors = KioskColors.scheme
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.avatar)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            OverrideLogo(
                size = Sizes.iconCompact,
                tint = colors.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(Space.sm))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = APP_NAME,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface,
                )
                Spacer(Modifier.width(Space.xs))
                AppStatusChip(
                    label = Strings.ROLE_EMITTER_TITLE,
                    tone = AppStatusTone.PRIMARY,
                    icon = AppIcon.CAST,
                )
            }
            Text(
                text = Strings.APP_TAGLINE,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        AppStatusChip(
            label = if (hasPassword) {
                Strings.SECURITY_STATUS_PROTECTED
            } else {
                Strings.SECURITY_STATUS_NONE
            },
            tone = if (hasPassword) AppStatusTone.SUCCESS else AppStatusTone.NEUTRAL,
            icon = if (hasPassword) AppIcon.LOCK else AppIcon.LOCK_OPEN,
        )
        Spacer(Modifier.width(Space.xs))

        Box {
            AppIconButton(
                icon = AppIcon.MORE_VERT,
                contentDescription = Strings.MORE_OPTIONS,
                onClick = { menuExpanded = true },
                tooltip = Strings.MORE_OPTIONS,
            )
            AppDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                items = listOf(
                    AppMenuItem(
                        label = Strings.KIOSK_SECURITY_BUTTON,
                        icon = AppIcon.LOCK,
                        onClick = onOpenSecurity,
                    ),
                    AppMenuItem(
                        label = Strings.SETTINGS_TITLE,
                        icon = AppIcon.SETTINGS,
                        onClick = onOpenSettings,
                    ),
                    AppMenuItem(
                        label = Strings.CHANGE_ROLE,
                        icon = AppIcon.SWAP_HORIZ,
                        onClick = onChangeRole,
                    ),
                    AppMenuItem(
                        label = Strings.EXIT_APP,
                        icon = AppIcon.LOGOUT,
                        onClick = onExit,
                    ),
                ),
            )
        }
    }
}

@Composable
internal fun StatusBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra
    val container = if (isError) colors.errorContainer else extra.successContainer
    val content = if (isError) colors.onErrorContainer else extra.onSuccessContainer

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KioskShape.small,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppIcon(
                icon = if (isError) AppIcon.ERROR else AppIcon.CHECK_CIRCLE,
                contentDescription = null,
                size = Sizes.iconCompact,
                tint = content,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                modifier = Modifier.weight(1f),
            )
            AppIconButton(
                icon = AppIcon.CLOSE,
                contentDescription = Strings.CLOSE_BUTTON,
                onClick = onDismiss,
                size = AppIconButtonSize.SMALL,
                tint = content,
            )
        }
    }
}

