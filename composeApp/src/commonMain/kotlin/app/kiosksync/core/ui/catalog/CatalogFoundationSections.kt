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
import app.kiosksync.core.common.app.AppTheme
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
import app.kiosksync.resources.Res
import app.kiosksync.resources.app_title
import app.kiosksync.resources.design_card_controller_desc
import app.kiosksync.resources.design_color_error
import app.kiosksync.resources.design_color_error_container
import app.kiosksync.resources.design_color_on_primary
import app.kiosksync.resources.design_color_on_surface_variant
import app.kiosksync.resources.design_color_outline
import app.kiosksync.resources.design_color_primary
import app.kiosksync.resources.design_color_primary_container
import app.kiosksync.resources.design_color_secondary
import app.kiosksync.resources.design_color_secondary_container
import app.kiosksync.resources.design_color_success
import app.kiosksync.resources.design_color_surface
import app.kiosksync.resources.design_color_surface_high
import app.kiosksync.resources.design_color_surface_highest
import app.kiosksync.resources.design_color_tertiary
import app.kiosksync.resources.design_color_warning
import app.kiosksync.resources.design_display_code
import app.kiosksync.resources.design_section_color
import app.kiosksync.resources.design_section_color_desc
import app.kiosksync.resources.design_section_display
import app.kiosksync.resources.design_section_display_desc
import app.kiosksync.resources.design_section_shapes
import app.kiosksync.resources.design_section_typography
import app.kiosksync.resources.design_section_typography_desc
import app.kiosksync.resources.design_shape_extra_large
import app.kiosksync.resources.design_shape_extra_small
import app.kiosksync.resources.design_shape_large
import app.kiosksync.resources.design_shape_medium
import app.kiosksync.resources.design_shape_pill
import app.kiosksync.resources.design_shape_small
import app.kiosksync.resources.design_typo_body
import app.kiosksync.resources.design_typo_display
import app.kiosksync.resources.design_typo_headline
import app.kiosksync.resources.design_typo_headline_medium
import app.kiosksync.resources.design_typo_label
import app.kiosksync.resources.design_typo_sample_body
import app.kiosksync.resources.design_typo_sample_label
import app.kiosksync.resources.design_typo_sample_title
import app.kiosksync.resources.design_typo_title
import app.kiosksync.resources.kiosk_locked_title
import org.jetbrains.compose.resources.stringResource

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

    CatalogSection(stringResource(Res.string.design_section_color), stringResource(Res.string.design_section_color_desc)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            ColorSwatch(stringResource(Res.string.design_color_primary), colors.primary)
            ColorSwatch(stringResource(Res.string.design_color_on_primary), colors.onPrimary)
            ColorSwatch(stringResource(Res.string.design_color_primary_container), colors.primaryContainer)
            ColorSwatch(stringResource(Res.string.design_color_secondary), colors.secondary)
            ColorSwatch(stringResource(Res.string.design_color_secondary_container), colors.secondaryContainer)
            ColorSwatch(stringResource(Res.string.design_color_tertiary), colors.tertiary)
            ColorSwatch(stringResource(Res.string.design_color_surface), colors.surface)
            ColorSwatch(stringResource(Res.string.design_color_surface_high), colors.surfaceContainerHigh)
            ColorSwatch(stringResource(Res.string.design_color_surface_highest), colors.surfaceContainerHighest)
            ColorSwatch(stringResource(Res.string.design_color_on_surface_variant), colors.onSurfaceVariant)
            ColorSwatch(stringResource(Res.string.design_color_outline), colors.outline)
            ColorSwatch(stringResource(Res.string.design_color_error), colors.error)
            ColorSwatch(stringResource(Res.string.design_color_error_container), colors.errorContainer)
            ColorSwatch(stringResource(Res.string.design_color_success), extra.success)
            ColorSwatch(stringResource(Res.string.design_color_warning), extra.warning)
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

    CatalogSection(stringResource(Res.string.design_section_typography), stringResource(Res.string.design_section_typography_desc)) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            TypographySample(stringResource(Res.string.design_typo_display), typography.displaySmall)
            TypographySample(stringResource(Res.string.design_typo_headline), typography.headlineLarge)
            TypographySample(stringResource(Res.string.design_typo_headline_medium), typography.headlineMedium)
            TypographySample(stringResource(Res.string.design_typo_title), typography.titleLarge)
            TypographySample(stringResource(Res.string.design_typo_body), typography.bodyLarge)
            TypographySample(stringResource(Res.string.design_typo_label), typography.labelLarge)
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            text = stringResource(Res.string.design_typo_sample_body),
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
                stringResource(Res.string.design_typo_label) -> stringResource(Res.string.design_typo_sample_label)
                stringResource(Res.string.design_typo_body) -> stringResource(Res.string.design_typo_sample_body)
                stringResource(Res.string.design_typo_title) -> stringResource(Res.string.design_typo_sample_title)
                else -> stringResource(Res.string.app_title)
            },
            style = style,
            color = colors.onSurface,
        )
    }
}

@Composable
internal fun DisplaySection() {
    val colors = KioskColors.scheme

    CatalogSection(stringResource(Res.string.design_section_display), stringResource(Res.string.design_section_display_desc)) {
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
                    text = stringResource(Res.string.design_display_code),
                    style = KioskDisplayType.pairingCode(),
                    color = KioskScreenColors.content,
                )
                Text(
                    text = stringResource(Res.string.kiosk_locked_title),
                    style = KioskDisplayType.screenTitle(),
                    color = KioskScreenColors.content,
                )
                Text(
                    text = stringResource(Res.string.design_card_controller_desc),
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

    CatalogSection(stringResource(Res.string.design_section_shapes)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            ShapeTile(stringResource(Res.string.design_shape_extra_small), KioskShapes.extraSmall)
            ShapeTile(stringResource(Res.string.design_shape_small), KioskShapes.small)
            ShapeTile(stringResource(Res.string.design_shape_medium), KioskShapes.medium)
            ShapeTile(stringResource(Res.string.design_shape_large), KioskShapes.large)
            ShapeTile(stringResource(Res.string.design_shape_extra_large), KioskShapes.extraLarge)
            ShapeTile(stringResource(Res.string.design_shape_pill), KioskShape.chip)
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

