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
import app.kiosksync.core.Strings
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



@Composable
internal fun ChipSection() {
    CatalogSection(Strings.DESIGN_SECTION_CHIPS) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            AppStatusChip(Strings.DESIGN_CHIP_NEUTRAL, AppStatusTone.NEUTRAL, icon = AppIcon.TV)
            AppStatusChip(Strings.DESIGN_CHIP_LINKED, AppStatusTone.PRIMARY, icon = AppIcon.LINK)
            AppStatusChip(Strings.DESIGN_CHIP_ONLINE, AppStatusTone.SUCCESS, icon = AppIcon.WIFI)
            AppStatusChip(Strings.DESIGN_CHIP_PROJECTING, AppStatusTone.PRIMARY, icon = AppIcon.PLAY_ARROW)
            AppStatusChip(Strings.DESIGN_CHIP_SUCCESS, AppStatusTone.SUCCESS, icon = AppIcon.CHECK_CIRCLE)
            AppStatusChip(Strings.DESIGN_CHIP_WARNING, AppStatusTone.WARNING, icon = AppIcon.WARNING)
            AppStatusChip(Strings.DESIGN_CHIP_ERROR, AppStatusTone.ERROR, icon = AppIcon.ERROR)
            AppStatusChip(Strings.DESIGN_CHIP_OFFLINE, AppStatusTone.ERROR, icon = AppIcon.WIFI_OFF)
        }
    }
}

@Composable
internal fun CardSection() {
    var roleSelected by remember { mutableIntStateOf(0) }
    var screenChecked by remember { mutableStateOf(true) }

    CatalogSection(Strings.DESIGN_SECTION_CARDS) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            AppSelectableCard(
                title = Strings.ROLE_EMITTER_TITLE,
                description = Strings.DESIGN_CARD_CONTROLLER_DESC,
                icon = AppIcon.CAST,
                selected = roleSelected == 0,
                onClick = { roleSelected = 0 },
                role = Role.RadioButton,
                modifier = Modifier.width(380.dp),
            )
            AppSelectableCard(
                title = Strings.ROLE_RECEIVER_TITLE,
                description = Strings.DESIGN_CARD_SCREEN_DESC,
                icon = AppIcon.TV,
                selected = roleSelected == 1,
                onClick = { roleSelected = 1 },
                role = Role.RadioButton,
                modifier = Modifier.width(380.dp),
            )
            AppSelectableCard(
                title = Strings.DESIGN_CHIP_ONLINE,
                description = Strings.DESIGN_PLAYLIST_ROW_SUB,
                icon = AppIcon.TV,
                selected = screenChecked,
                onClick = { screenChecked = !screenChecked },
                role = Role.Checkbox,
                modifier = Modifier.width(380.dp),
                trailing = {
                    AppStatusChip(Strings.DESIGN_CHIP_LINKED, AppStatusTone.PRIMARY)
                },
            )
        }
    }
}

@Composable
internal fun ListSection() {
    val colors = KioskColors.scheme

    CatalogSection(Strings.DESIGN_SECTION_LIST) {
        Surface(
            shape = KioskShape.card,
            color = colors.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(Space.xs)) {
                AppListItem(
                    title = Strings.DESIGN_PLAYLIST_ROW,
                    subtitle = Strings.DESIGN_PLAYLIST_ROW_SUB,
                    leading = { ThumbnailPlaceholder() },
                    trailing = {
                        AppStatusChip(Strings.DESIGN_CHIP_DURATION, AppStatusTone.NEUTRAL)
                        AppIconButton(
                            icon = AppIcon.DRAG_INDICATOR,
                            contentDescription = Strings.DESIGN_SECTION_LIST,
                            onClick = {},
                            size = AppIconButtonSize.SMALL,
                        )
                        AppIconButton(
                            icon = AppIcon.DELETE,
                            contentDescription = Strings.DESIGN_DELETE,
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
                    title = Strings.DESIGN_PLAYLIST_ROW,
                    subtitle = Strings.DESIGN_PLAYLIST_ROW_SUB,
                    leading = { ThumbnailPlaceholder() },
                    selected = true,
                    trailing = {
                        AppStatusChip(Strings.DESIGN_CHIP_DURATION, AppStatusTone.PRIMARY)
                        AppIconButton(
                            icon = AppIcon.DRAG_INDICATOR,
                            contentDescription = Strings.DESIGN_SECTION_LIST,
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
