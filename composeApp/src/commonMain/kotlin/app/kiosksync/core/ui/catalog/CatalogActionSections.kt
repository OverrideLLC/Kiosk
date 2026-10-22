@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.kiosksync.core.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonGroup
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppIconButton
import app.kiosksync.core.ui.components.AppIconButtonSize
import app.kiosksync.core.ui.components.AppIconButtonVariant
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.Space



@Composable
internal fun ButtonSection() {
    CatalogSection(Strings.DESIGN_SECTION_BUTTONS, Strings.DESIGN_SECTION_BUTTONS_DESC) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                AppButton(
                    text = Strings.DESIGN_SEND,
                    onClick = {},
                    variant = AppButtonVariant.FILLED,
                    size = AppButtonSize.LARGE,
                    icon = AppIcon.SEND,
                )
                AppButton(
                    text = Strings.DESIGN_PROJECT,
                    onClick = {},
                    variant = AppButtonVariant.TONAL,
                    size = AppButtonSize.LARGE,
                    icon = AppIcon.PRESENT_TO_ALL,
                )
                AppButton(
                    text = Strings.DESIGN_ADD_IP,
                    onClick = {},
                    variant = AppButtonVariant.OUTLINED,
                    icon = AppIcon.ADD_LINK,
                )
                AppButton(
                    text = Strings.DESIGN_CANCEL,
                    onClick = {},
                    variant = AppButtonVariant.TEXT,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                AppButton(Strings.DESIGN_SEND, {}, size = AppButtonSize.LARGE, icon = AppIcon.SEND)
                AppButton(Strings.DESIGN_SEND, {}, size = AppButtonSize.MEDIUM, icon = AppIcon.SEND)
                AppButton(Strings.DESIGN_SEND, {}, size = AppButtonSize.SMALL, icon = AppIcon.SEND)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                AppButton(
                    text = Strings.DESIGN_DISABLED_BUTTON,
                    onClick = {},
                    enabled = false,
                    disabledReason = Strings.DESIGN_DISABLED_REASON,
                    icon = AppIcon.SEND,
                )
                AppButton(
                    text = Strings.DESIGN_SEND,
                    onClick = {},
                    loading = true,
                )
                AppIconButton(
                    icon = AppIcon.REFRESH,
                    contentDescription = Strings.DESIGN_ICON_BUTTON,
                    onClick = {},
                    variant = AppIconButtonVariant.STANDARD,
                )
                AppIconButton(
                    icon = AppIcon.REFRESH,
                    contentDescription = Strings.DESIGN_ICON_BUTTON,
                    onClick = {},
                    variant = AppIconButtonVariant.TONAL,
                )
                AppIconButton(
                    icon = AppIcon.REFRESH,
                    contentDescription = Strings.DESIGN_ICON_BUTTON,
                    onClick = {},
                    variant = AppIconButtonVariant.FILLED,
                )
                AppIconButton(
                    icon = AppIcon.SETTINGS,
                    contentDescription = Strings.DESIGN_ICON_BUTTON,
                    onClick = {},
                    size = AppIconButtonSize.SMALL,
                )
            }
        }
    }
}

@Composable
internal fun ButtonGroupSection() {
    var selected by remember { mutableIntStateOf(0) }

    CatalogSection(Strings.DESIGN_SECTION_GROUP) {
        AppButtonGroup(
            options = listOf(
                Strings.DESIGN_GROUP_ALL,
                Strings.DESIGN_GROUP_ONLINE,
                Strings.DESIGN_GROUP_OFFLINE,
            ),
            selectedIndex = selected,
            onSelect = { selected = it },
            modifier = Modifier.width(420.dp),
        )
    }
}

@Composable
internal fun IconSection() {
    val colors = KioskColors.scheme

    CatalogSection(Strings.DESIGN_SECTION_ICONS, Strings.DESIGN_SECTION_ICONS_DESC) {
        FlowRow(
            maxItemsInEachRow = 8,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            AppIcon.entries.forEach { icon ->
                Column(
                    modifier = Modifier.width(120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(
                            icon = icon,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(Space.xxs))
                    Text(
                        text = icon.name.lowercase().replace('_', ' '),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
