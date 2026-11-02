@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.kiosksync.core.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppIconButton
import app.kiosksync.core.ui.components.AppIconButtonSize
import app.kiosksync.core.ui.components.AppListItem
import app.kiosksync.core.ui.components.AppSelectableCard
import app.kiosksync.core.ui.components.AppStatusChip
import app.kiosksync.core.ui.components.AppStatusTone
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.resources.Res
import app.kiosksync.resources.design_card_controller_desc
import app.kiosksync.resources.design_card_screen_desc
import app.kiosksync.resources.design_chip_duration
import app.kiosksync.resources.design_chip_error
import app.kiosksync.resources.design_chip_linked
import app.kiosksync.resources.design_chip_neutral
import app.kiosksync.resources.design_chip_offline
import app.kiosksync.resources.design_chip_online
import app.kiosksync.resources.design_chip_projecting
import app.kiosksync.resources.design_chip_success
import app.kiosksync.resources.design_chip_warning
import app.kiosksync.resources.design_delete
import app.kiosksync.resources.design_playlist_row
import app.kiosksync.resources.design_playlist_row_sub
import app.kiosksync.resources.design_section_cards
import app.kiosksync.resources.design_section_chips
import app.kiosksync.resources.design_section_list
import app.kiosksync.resources.role_emitter_title
import app.kiosksync.resources.role_receiver_title
import org.jetbrains.compose.resources.stringResource



@Composable
internal fun ChipSection() {
    CatalogSection(stringResource(Res.string.design_section_chips)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            AppStatusChip(stringResource(Res.string.design_chip_neutral), AppStatusTone.NEUTRAL, icon = AppIcon.TV)
            AppStatusChip(stringResource(Res.string.design_chip_linked), AppStatusTone.PRIMARY, icon = AppIcon.LINK)
            AppStatusChip(stringResource(Res.string.design_chip_online), AppStatusTone.SUCCESS, icon = AppIcon.WIFI)
            AppStatusChip(stringResource(Res.string.design_chip_projecting), AppStatusTone.PRIMARY, icon = AppIcon.PLAY_ARROW)
            AppStatusChip(stringResource(Res.string.design_chip_success), AppStatusTone.SUCCESS, icon = AppIcon.CHECK_CIRCLE)
            AppStatusChip(stringResource(Res.string.design_chip_warning), AppStatusTone.WARNING, icon = AppIcon.WARNING)
            AppStatusChip(stringResource(Res.string.design_chip_error), AppStatusTone.ERROR, icon = AppIcon.ERROR)
            AppStatusChip(stringResource(Res.string.design_chip_offline), AppStatusTone.ERROR, icon = AppIcon.WIFI_OFF)
        }
    }
}

@Composable
internal fun CardSection() {
    var roleSelected by remember { mutableIntStateOf(0) }
    var screenChecked by remember { mutableStateOf(true) }

    CatalogSection(stringResource(Res.string.design_section_cards)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            AppSelectableCard(
                title = stringResource(Res.string.role_emitter_title),
                description = stringResource(Res.string.design_card_controller_desc),
                icon = AppIcon.CAST,
                selected = roleSelected == 0,
                onClick = { roleSelected = 0 },
                role = Role.RadioButton,
                modifier = Modifier.width(380.dp),
            )
            AppSelectableCard(
                title = stringResource(Res.string.role_receiver_title),
                description = stringResource(Res.string.design_card_screen_desc),
                icon = AppIcon.TV,
                selected = roleSelected == 1,
                onClick = { roleSelected = 1 },
                role = Role.RadioButton,
                modifier = Modifier.width(380.dp),
            )
            AppSelectableCard(
                title = stringResource(Res.string.design_chip_online),
                description = stringResource(Res.string.design_playlist_row_sub),
                icon = AppIcon.TV,
                selected = screenChecked,
                onClick = { screenChecked = !screenChecked },
                role = Role.Checkbox,
                modifier = Modifier.width(380.dp),
                trailing = {
                    AppStatusChip(stringResource(Res.string.design_chip_linked), AppStatusTone.PRIMARY)
                },
            )
        }
    }
}

@Composable
internal fun ListSection() {
    val colors = KioskColors.scheme

    CatalogSection(stringResource(Res.string.design_section_list)) {
        Surface(
            shape = KioskShape.card,
            color = colors.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(Space.xs)) {
                AppListItem(
                    title = stringResource(Res.string.design_playlist_row),
                    subtitle = stringResource(Res.string.design_playlist_row_sub),
                    leading = { ThumbnailPlaceholder() },
                    trailing = {
                        AppStatusChip(stringResource(Res.string.design_chip_duration), AppStatusTone.NEUTRAL)
                        AppIconButton(
                            icon = AppIcon.DRAG_INDICATOR,
                            contentDescription = stringResource(Res.string.design_section_list),
                            onClick = {},
                            size = AppIconButtonSize.SMALL,
                        )
                        AppIconButton(
                            icon = AppIcon.DELETE,
                            contentDescription = stringResource(Res.string.design_delete),
                            onClick = {},
                            size = AppIconButtonSize.SMALL,
                        )
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Space.sm),
                    color = colors.outlineVariant,
                )
                AppListItem(
                    title = stringResource(Res.string.design_playlist_row),
                    subtitle = stringResource(Res.string.design_playlist_row_sub),
                    leading = { ThumbnailPlaceholder() },
                    selected = true,
                    trailing = {
                        AppStatusChip(stringResource(Res.string.design_chip_duration), AppStatusTone.PRIMARY)
                        AppIconButton(
                            icon = AppIcon.DRAG_INDICATOR,
                            contentDescription = stringResource(Res.string.design_section_list),
                            onClick = {},
                            size = AppIconButtonSize.SMALL,
                        )
                    },
                )
            }
        }
    }
}

@Composable
internal fun ThumbnailPlaceholder() {
    val colors = KioskColors.scheme

    Box(
        modifier = Modifier
            .size(Sizes.listThumbnailWidth, Sizes.listThumbnailHeight)
            .clip(KioskShape.thumbnail)
            .background(colors.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            icon = AppIcon.IMAGE,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
        )
    }
}
