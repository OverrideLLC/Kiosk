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
internal fun CatalogSection(
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
internal fun ColorSection() {
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
internal fun ColorSwatch(label: String, color: Color) {
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
internal fun TypographySection() {
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
internal fun TypographySample(label: String, style: TextStyle) {
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
internal fun DisplaySection() {
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
internal fun ShapeSection() {
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
internal fun ShapeTile(label: String, shape: Shape) {
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

