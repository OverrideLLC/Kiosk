@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.kiosksync.designsystem.catalog

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
import app.kiosksync.designsystem.AppButton
import app.kiosksync.designsystem.AppCircularProgress
import app.kiosksync.designsystem.AppButtonGroup
import app.kiosksync.designsystem.AppButtonSize
import app.kiosksync.designsystem.AppButtonVariant
import app.kiosksync.designsystem.AppDialog
import app.kiosksync.designsystem.AppDropdownMenu
import app.kiosksync.designsystem.AppEmptyState
import app.kiosksync.designsystem.AppIcon
import app.kiosksync.designsystem.AppIconButton
import app.kiosksync.designsystem.AppIconButtonSize
import app.kiosksync.designsystem.AppIconButtonVariant
import app.kiosksync.designsystem.AppLinearProgress
import app.kiosksync.designsystem.AppListItem
import app.kiosksync.designsystem.AppLoadingIndicator
import app.kiosksync.designsystem.AppMenuItem
import app.kiosksync.designsystem.AppSelectableCard
import app.kiosksync.designsystem.AppSnackbarHost
import app.kiosksync.designsystem.AppStatusChip
import app.kiosksync.designsystem.AppStatusTone
import app.kiosksync.designsystem.AppStepper
import app.kiosksync.designsystem.AppTextField
import app.kiosksync.designsystem.AppTooltip
import app.kiosksync.designsystem.KioskColors
import app.kiosksync.designsystem.KioskDisplayType
import app.kiosksync.designsystem.KioskScreenColors
import app.kiosksync.designsystem.KioskShape
import app.kiosksync.designsystem.KioskShapes
import app.kiosksync.designsystem.KioskTheme
import app.kiosksync.designsystem.Sizes
import app.kiosksync.designsystem.Space
import kotlinx.coroutines.launch

/**
 * Development-only gallery of every token and component. It is opened with
 * `--ui-catalog` and never part of a user flow.
 */
@Composable
fun CatalogScreen() {
    var theme by remember { mutableStateOf(AppTheme.DARK) }
    var reduceMotion by remember { mutableStateOf(false) }

    KioskTheme(theme = theme, reduceMotion = reduceMotion) {
        CatalogContent(
            theme = theme,
            reduceMotion = reduceMotion,
            onThemeChange = { theme = it },
            onReduceMotionChange = { reduceMotion = it },
        )
    }
}

@Composable
fun CatalogContent(
    theme: AppTheme,
    reduceMotion: Boolean,
    onThemeChange: (AppTheme) -> Unit = {},
    onReduceMotionChange: (Boolean) -> Unit = {},
) {
    val colors = KioskColors.scheme
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CatalogTopBar(
                theme = theme,
                reduceMotion = reduceMotion,
                onThemeChange = onThemeChange,
                onReduceMotionChange = onReduceMotionChange,
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 1200.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(Space.xl),
                    verticalArrangement = Arrangement.spacedBy(Space.xxl),
                ) {
                    ColorSection()
                    TypographySection()
                    DisplaySection()
                    ShapeSection()
                    ButtonSection()
                    ButtonGroupSection()
                    IconSection()
                    FieldSection()
                    StepperSection()
                    ChipSection()
                    CardSection()
                    ListSection()
                    FeedbackSection(showSnackbar = {
                        scope.launch {
                            hostState.showSnackbar(
                                message = Strings.DESIGN_SNACKBAR_MESSAGE,
                                actionLabel = Strings.DESIGN_UNDO,
                                withDismissAction = true,
                            )
                        }
                    })
                    LoadingSection()
                    EmptySection()
                    Spacer(Modifier.height(Space.huge))
                }
            }
        }
        AppSnackbarHost(
            hostState = hostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Space.lg),
        )
    }
}

@Composable
private fun CatalogTopBar(
    theme: AppTheme,
    reduceMotion: Boolean,
    onThemeChange: (AppTheme) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
) {
    val colors = KioskColors.scheme

    Surface(color = colors.surfaceContainerLow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xl, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.APP_TITLE,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = Strings.DESIGN_TITLE,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = Strings.DESIGN_SUBTITLE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            AppButtonGroup(
                options = listOf(Strings.DESIGN_THEME_DARK, Strings.DESIGN_THEME_LIGHT),
                selectedIndex = if (theme == AppTheme.DARK) 0 else 1,
                onSelect = { index ->
                    onThemeChange(if (index == 0) AppTheme.DARK else AppTheme.LIGHT)
                },
                modifier = Modifier.width(240.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                Text(
                    text = Strings.DESIGN_REDUCE_MOTION,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Switch(
                    checked = reduceMotion,
                    onCheckedChange = onReduceMotionChange,
                )
            }
        }
    }
}

@Composable
private fun CatalogSection(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = KioskColors.scheme

    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        content()
    }
}

@Composable
private fun ColorSection() {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra

    CatalogSection(Strings.DESIGN_SECTION_COLOR, Strings.DESIGN_SECTION_COLOR_DESC) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            ColorSwatch(Strings.DESIGN_COLOR_PRIMARY, colors.primary)
            ColorSwatch(Strings.DESIGN_COLOR_ON_PRIMARY, colors.onPrimary)
            ColorSwatch(Strings.DESIGN_COLOR_PRIMARY_CONTAINER, colors.primaryContainer)
            ColorSwatch(Strings.DESIGN_COLOR_SECONDARY, colors.secondary)
            ColorSwatch(Strings.DESIGN_COLOR_SECONDARY_CONTAINER, colors.secondaryContainer)
            ColorSwatch(Strings.DESIGN_COLOR_TERTIARY, colors.tertiary)
            ColorSwatch(Strings.DESIGN_COLOR_SURFACE, colors.surface)
            ColorSwatch(Strings.DESIGN_COLOR_SURFACE_HIGH, colors.surfaceContainerHigh)
            ColorSwatch(Strings.DESIGN_COLOR_SURFACE_HIGHEST, colors.surfaceContainerHighest)
            ColorSwatch(Strings.DESIGN_COLOR_ON_SURFACE_VARIANT, colors.onSurfaceVariant)
            ColorSwatch(Strings.DESIGN_COLOR_OUTLINE, colors.outline)
            ColorSwatch(Strings.DESIGN_COLOR_ERROR, colors.error)
            ColorSwatch(Strings.DESIGN_COLOR_ERROR_CONTAINER, colors.errorContainer)
            ColorSwatch(Strings.DESIGN_COLOR_SUCCESS, extra.success)
            ColorSwatch(Strings.DESIGN_COLOR_WARNING, extra.warning)
        }
    }
}

@Composable
private fun ColorSwatch(label: String, color: Color) {
    val colors = KioskColors.scheme

    Column(modifier = Modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(KioskShape.small)
                .background(color)
                .border(1.dp, colors.outlineVariant, KioskShape.small),
        )
        Spacer(Modifier.height(Space.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

@Composable
private fun TypographySection() {
    val colors = KioskColors.scheme
    val typography = MaterialTheme.typography

    CatalogSection(Strings.DESIGN_SECTION_TYPOGRAPHY, Strings.DESIGN_SECTION_TYPOGRAPHY_DESC) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            TypographySample(Strings.DESIGN_TYPO_DISPLAY, typography.displaySmall)
            TypographySample(Strings.DESIGN_TYPO_HEADLINE, typography.headlineLarge)
            TypographySample(Strings.DESIGN_TYPO_HEADLINE_MEDIUM, typography.headlineMedium)
            TypographySample(Strings.DESIGN_TYPO_TITLE, typography.titleLarge)
            TypographySample(Strings.DESIGN_TYPO_BODY, typography.bodyLarge)
            TypographySample(Strings.DESIGN_TYPO_LABEL, typography.labelLarge)
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            text = Strings.DESIGN_TYPO_SAMPLE_BODY,
            style = typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun TypographySample(label: String, style: TextStyle) {
    val colors = KioskColors.scheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.width(140.dp),
        )
        Text(
            text = when (label) {
                Strings.DESIGN_TYPO_LABEL -> Strings.DESIGN_TYPO_SAMPLE_LABEL
                Strings.DESIGN_TYPO_BODY -> Strings.DESIGN_TYPO_SAMPLE_BODY
                Strings.DESIGN_TYPO_TITLE -> Strings.DESIGN_TYPO_SAMPLE_TITLE
                else -> Strings.APP_TITLE
            },
            style = style,
            color = colors.onSurface,
        )
    }
}

@Composable
private fun DisplaySection() {
    val colors = KioskColors.scheme

    CatalogSection(Strings.DESIGN_SECTION_DISPLAY, Strings.DESIGN_SECTION_DISPLAY_DESC) {
        Surface(
            shape = KioskShape.card,
            color = KioskScreenColors.background,
            contentColor = KioskScreenColors.content,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Space.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                Text(
                    text = Strings.DESIGN_DISPLAY_CODE,
                    style = KioskDisplayType.pairingCode(),
                    color = KioskScreenColors.content,
                )
                Text(
                    text = Strings.KIOSK_LOCKED_TITLE,
                    style = KioskDisplayType.screenTitle(),
                    color = KioskScreenColors.content,
                )
                Text(
                    text = Strings.DESIGN_CARD_CONTROLLER_DESC,
                    style = KioskDisplayType.screenBody(),
                    color = KioskScreenColors.content,
                )
            }
        }
    }
}

@Composable
private fun ShapeSection() {
    val colors = KioskColors.scheme

    CatalogSection(Strings.DESIGN_SECTION_SHAPES) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            ShapeTile(Strings.DESIGN_SHAPE_EXTRA_SMALL, KioskShapes.extraSmall)
            ShapeTile(Strings.DESIGN_SHAPE_SMALL, KioskShapes.small)
            ShapeTile(Strings.DESIGN_SHAPE_MEDIUM, KioskShapes.medium)
            ShapeTile(Strings.DESIGN_SHAPE_LARGE, KioskShapes.large)
            ShapeTile(Strings.DESIGN_SHAPE_EXTRA_LARGE, KioskShapes.extraLarge)
            ShapeTile(Strings.DESIGN_SHAPE_PILL, KioskShape.chip)
        }
    }
}

@Composable
private fun ShapeTile(label: String, shape: Shape) {
    val colors = KioskColors.scheme

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .border(1.dp, colors.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun ButtonSection() {
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
private fun ButtonGroupSection() {
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
private fun IconSection() {
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
private fun FieldSection() {
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
private fun StepperSection() {
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
private fun ChipSection() {
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
private fun CardSection() {
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
private fun ListSection() {
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
private fun ThumbnailPlaceholder() {
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

@Composable
private fun FeedbackSection(
    showSnackbar: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    CatalogSection(Strings.DESIGN_SECTION_FEEDBACK) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            AppButton(
                text = Strings.DESIGN_SHOW_DIALOG,
                onClick = { showDialog = true },
                variant = AppButtonVariant.OUTLINED,
                icon = AppIcon.EDIT,
            )
            Box {
                AppButton(
                    text = Strings.DESIGN_SHOW_MENU,
                    onClick = { showMenu = true },
                    variant = AppButtonVariant.OUTLINED,
                    icon = AppIcon.MORE_VERT,
                )
                AppDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    items = listOf(
                        AppMenuItem(
                            label = Strings.DESIGN_MENU_NEW,
                            onClick = {},
                            icon = AppIcon.PLAYLIST_ADD,
                        ),
                        AppMenuItem(
                            label = Strings.DESIGN_MENU_RENAME,
                            onClick = {},
                            icon = AppIcon.EDIT,
                        ),
                        AppMenuItem(
                            label = Strings.DESIGN_MENU_DELETE,
                            onClick = {},
                            icon = AppIcon.DELETE,
                            destructive = true,
                        ),
                    ),
                )
            }
            AppButton(
                text = Strings.DESIGN_SHOW_SNACKBAR,
                onClick = showSnackbar,
                variant = AppButtonVariant.TONAL,
                icon = AppIcon.INFO,
            )
            AppTooltip(Strings.DESIGN_TOOLTIP_TEXT) {
                AppButton(
                    text = Strings.DESIGN_TOOLTIP_TARGET,
                    onClick = {},
                    variant = AppButtonVariant.TEXT,
                    icon = AppIcon.HELP,
                )
            }
        }
    }

    if (showDialog) {
        AppDialog(
            title = Strings.DESIGN_DIALOG_TITLE,
            onDismissRequest = { showDialog = false },
            icon = AppIcon.PLAYLIST_ADD,
            actions = {
                AppButton(
                    text = Strings.DESIGN_CANCEL,
                    onClick = { showDialog = false },
                    variant = AppButtonVariant.TEXT,
                )
                AppButton(
                    text = Strings.DESIGN_SAVE,
                    onClick = { showDialog = false },
                    icon = AppIcon.CHECK,
                )
            },
            content = {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = Strings.DESIGN_NAME_LABEL,
                    helper = Strings.DESIGN_DIALOG_TEXT,
                )
            },
        )
    }
}

@Composable
private fun LoadingSection() {
    val colors = KioskColors.scheme

    CatalogSection(Strings.DESIGN_SECTION_LOADING) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                AppLoadingIndicator()
                Text(
                    text = Strings.WAITING_CONTENT,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            AppCircularProgress()
            AppLinearProgress(
                progress = 0.42f,
                label = Strings.DESIGN_PROGRESS_LABEL,
                modifier = Modifier.width(360.dp),
            )
        }
    }
}

@Composable
private fun EmptySection() {
    CatalogSection(Strings.DESIGN_SECTION_EMPTY) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Surface(
                shape = KioskShape.card,
                color = KioskColors.scheme.surfaceContainerLow,
                modifier = Modifier.width(460.dp),
            ) {
                AppEmptyState(
                    icon = AppIcon.TV,
                    title = Strings.DESIGN_EMPTY_SCREENS_TITLE,
                    description = Strings.DESIGN_EMPTY_SCREENS_DESC,
                    action = {
                        AppButton(
                            text = Strings.DESIGN_ADD_IP,
                            onClick = {},
                            icon = AppIcon.ADD_LINK,
                        )
                    },
                )
            }
            Surface(
                shape = KioskShape.card,
                color = KioskColors.scheme.surfaceContainerLow,
                modifier = Modifier.width(460.dp),
            ) {
                AppEmptyState(
                    icon = AppIcon.PHOTO_LIBRARY,
                    title = Strings.DESIGN_EMPTY_PLAYLIST_TITLE,
                    description = Strings.DESIGN_EMPTY_PLAYLIST_DESC,
                    action = {
                        AppButton(
                            text = Strings.DESIGN_ADD_IMAGES,
                            onClick = {},
                            icon = AppIcon.ADD_PHOTO_ALTERNATE,
                        )
                    },
                )
            }
        }
    }
}
