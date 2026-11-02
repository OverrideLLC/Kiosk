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
import app.kiosksync.resources.Res
import app.kiosksync.resources.design_add_ip
import app.kiosksync.resources.design_cancel
import app.kiosksync.resources.design_disabled_button
import app.kiosksync.resources.design_disabled_reason
import app.kiosksync.resources.design_group_all
import app.kiosksync.resources.design_group_offline
import app.kiosksync.resources.design_group_online
import app.kiosksync.resources.design_icon_button
import app.kiosksync.resources.design_project
import app.kiosksync.resources.design_section_buttons
import app.kiosksync.resources.design_section_buttons_desc
import app.kiosksync.resources.design_section_group
import app.kiosksync.resources.design_section_icons
import app.kiosksync.resources.design_section_icons_desc
import app.kiosksync.resources.design_send
import org.jetbrains.compose.resources.stringResource



@Composable
internal fun ButtonSection() {
    CatalogSection(stringResource(Res.string.design_section_buttons), stringResource(Res.string.design_section_buttons_desc)) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                AppButton(
                    text = stringResource(Res.string.design_send),
                    onClick = {},
                    variant = AppButtonVariant.FILLED,
                    size = AppButtonSize.LARGE,
                    icon = AppIcon.SEND,
                )
                AppButton(
                    text = stringResource(Res.string.design_project),
                    onClick = {},
                    variant = AppButtonVariant.TONAL,
                    size = AppButtonSize.LARGE,
                    icon = AppIcon.PRESENT_TO_ALL,
                )
                AppButton(
                    text = stringResource(Res.string.design_add_ip),
                    onClick = {},
                    variant = AppButtonVariant.OUTLINED,
                    icon = AppIcon.ADD_LINK,
                )
                AppButton(
                    text = stringResource(Res.string.design_cancel),
                    onClick = {},
                    variant = AppButtonVariant.TEXT,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                AppButton(stringResource(Res.string.design_send), {}, size = AppButtonSize.LARGE, icon = AppIcon.SEND)
                AppButton(stringResource(Res.string.design_send), {}, size = AppButtonSize.MEDIUM, icon = AppIcon.SEND)
                AppButton(stringResource(Res.string.design_send), {}, size = AppButtonSize.SMALL, icon = AppIcon.SEND)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                AppButton(
                    text = stringResource(Res.string.design_disabled_button),
                    onClick = {},
                    enabled = false,
                    disabledReason = stringResource(Res.string.design_disabled_reason),
                    icon = AppIcon.SEND,
                )
                AppButton(
                    text = stringResource(Res.string.design_send),
                    onClick = {},
                    loading = true,
                )
                AppIconButton(
                    icon = AppIcon.REFRESH,
                    contentDescription = stringResource(Res.string.design_icon_button),
                    onClick = {},
                    variant = AppIconButtonVariant.STANDARD,
                )
                AppIconButton(
                    icon = AppIcon.REFRESH,
                    contentDescription = stringResource(Res.string.design_icon_button),
                    onClick = {},
                    variant = AppIconButtonVariant.TONAL,
                )
                AppIconButton(
                    icon = AppIcon.REFRESH,
                    contentDescription = stringResource(Res.string.design_icon_button),
                    onClick = {},
                    variant = AppIconButtonVariant.FILLED,
                )
                AppIconButton(
                    icon = AppIcon.SETTINGS,
                    contentDescription = stringResource(Res.string.design_icon_button),
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

    CatalogSection(stringResource(Res.string.design_section_group)) {
        AppButtonGroup(
            options = listOf(
                stringResource(Res.string.design_group_all),
                stringResource(Res.string.design_group_online),
                stringResource(Res.string.design_group_offline),
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

    CatalogSection(stringResource(Res.string.design_section_icons), stringResource(Res.string.design_section_icons_desc)) {
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
