@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.kiosksync.core.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.kiosksync.core.AppTheme
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppCircularProgress
import app.kiosksync.core.ui.components.AppButtonGroup
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDialog
import app.kiosksync.core.ui.components.AppDropdownMenu
import app.kiosksync.core.ui.components.AppEmptyState
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppIconButton
import app.kiosksync.core.ui.components.AppIconButtonSize
import app.kiosksync.core.ui.components.AppIconButtonVariant
import app.kiosksync.core.ui.components.AppLinearProgress
import app.kiosksync.core.ui.components.AppListItem
import app.kiosksync.core.ui.components.AppLoadingIndicator
import app.kiosksync.core.ui.components.AppMenuItem
import app.kiosksync.core.ui.components.AppSelectableCard
import app.kiosksync.core.ui.components.AppSnackbarHost
import app.kiosksync.core.ui.components.AppStatusChip
import app.kiosksync.core.ui.components.AppStatusTone
import app.kiosksync.core.ui.components.AppStepper
import app.kiosksync.core.ui.components.AppTextField
import app.kiosksync.core.ui.components.AppTooltip
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.KioskDisplayType
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.KioskShapes
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import kotlinx.coroutines.launch

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

@Composable
internal fun FieldSection() {
    var name by remember { mutableStateOf("Promociones de octubre") }
    var invalidName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("kiosco-2026") }
    var passwordVisible by remember { mutableStateOf(false) }

    CatalogSection(Strings.DESIGN_SECTION_FIELDS) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Box(modifier = Modifier.width(300.dp)) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = Strings.DESIGN_NAME_LABEL,
                    helper = Strings.DESIGN_NAME_HELPER,
                )
            }
            Box(modifier = Modifier.width(300.dp)) {
                AppTextField(
                    value = invalidName,
                    onValueChange = { invalidName = it },
                    label = Strings.DESIGN_NAME_LABEL,
                    error = Strings.DESIGN_NAME_ERROR,
                )
            }
            Box(modifier = Modifier.width(300.dp)) {
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = Strings.DESIGN_PASSWORD_LABEL,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        AppIconButton(
                            icon = if (passwordVisible) AppIcon.VISIBILITY_OFF else AppIcon.VISIBILITY,
                            contentDescription = if (passwordVisible) {
                                Strings.DESIGN_HIDE_PASSWORD
                            } else {
                                Strings.DESIGN_SHOW_PASSWORD
                            },
                            onClick = { passwordVisible = !passwordVisible },
                            size = AppIconButtonSize.SMALL,
                        )
                    },
                )
            }
            Box(modifier = Modifier.width(300.dp)) {
                AppTextField(
                    value = name,
                    onValueChange = {},
                    label = Strings.DESIGN_NAME_LABEL,
                    enabled = false,
                )
            }
        }
    }
}

@Composable
internal fun StepperSection() {
    var duration by remember { mutableIntStateOf(10) }
    var invalidDuration by remember { mutableIntStateOf(1) }

    CatalogSection(Strings.DESIGN_SECTION_STEPPER) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            AppStepper(
                value = duration,
                onValueChange = { duration = it },
                label = Strings.DESIGN_DURATION,
                range = 3..600,
                unit = Strings.DESIGN_DURATION_UNIT,
                helper = Strings.DESIGN_DURATION_HELP,
                modifier = Modifier.width(260.dp),
            )
            AppStepper(
                value = invalidDuration,
                onValueChange = { invalidDuration = it },
                label = Strings.DESIGN_DURATION,
                range = 3..600,
                unit = Strings.DESIGN_DURATION_UNIT,
                error = Strings.DESIGN_DURATION_ERROR,
                modifier = Modifier.width(260.dp),
            )
        }
    }
}

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

