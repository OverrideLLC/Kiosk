package app.kiosksync.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppButtonVariant { FILLED, TONAL, OUTLINED, TEXT, DANGER }

enum class AppButtonSize(
    val height: Dp,
    val horizontalPadding: Dp,
    val iconSize: Dp,
) {
    LARGE(Sizes.buttonLarge, 28.dp, Sizes.icon),
    MEDIUM(Sizes.buttonMedium, 20.dp, Sizes.iconCompact),
    SMALL(Sizes.buttonSmall, 14.dp, Sizes.iconSmall),
}

/**
 * Primary action component. The disabled state keeps readable text and exposes
 * the reason through a tooltip and accessibility state.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.FILLED,
    size: AppButtonSize = AppButtonSize.MEDIUM,
    icon: AppIcon? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    disabledReason: String? = null,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra
    val effectiveEnabled = enabled && !loading
    val contentColor = when (variant) {
        AppButtonVariant.FILLED -> colors.onPrimary
        AppButtonVariant.TONAL -> colors.onSecondaryContainer
        AppButtonVariant.OUTLINED, AppButtonVariant.TEXT -> colors.primary
        AppButtonVariant.DANGER -> colors.onError
    }
    val textStyle = when (size) {
        AppButtonSize.LARGE -> MaterialTheme.typography.titleMedium
        AppButtonSize.MEDIUM -> MaterialTheme.typography.labelLarge
        AppButtonSize.SMALL -> MaterialTheme.typography.labelMedium
    }
    val contentPadding = PaddingValues(horizontal = size.horizontalPadding, vertical = 0.dp)
    val semanticsModifier = if (!effectiveEnabled && disabledReason != null) {
        Modifier.semantics { stateDescription = disabledReason }
    } else {
        Modifier
    }
    val buttonModifier = modifier
        .focusRing(KioskShape.button)
        .then(semanticsModifier)
        .height(size.height)

    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size.iconSize),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else if (icon != null) {
            AppIcon(icon, contentDescription = null, size = size.iconSize)
        }
        if (loading || icon != null) {
            Spacer(Modifier.width(Space.xs))
        }
        Text(
            text = text,
            style = textStyle,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }

    val button: @Composable () -> Unit = {
        when (variant) {
            AppButtonVariant.FILLED -> Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = effectiveEnabled,
                shape = KioskShape.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = contentColor,
                    disabledContainerColor = extra.disabledContainer,
                    disabledContentColor = extra.disabledContent,
                ),
                contentPadding = contentPadding,
                content = content,
            )

            AppButtonVariant.TONAL -> FilledTonalButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = effectiveEnabled,
                shape = KioskShape.button,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.secondaryContainer,
                    contentColor = contentColor,
                    disabledContainerColor = extra.disabledContainer,
                    disabledContentColor = extra.disabledContent,
                ),
                contentPadding = contentPadding,
                content = content,
            )

            AppButtonVariant.OUTLINED -> OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = effectiveEnabled,
                shape = KioskShape.button,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = contentColor,
                    disabledContentColor = extra.disabledContent,
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (effectiveEnabled) colors.outline else colors.outlineVariant,
                ),
                contentPadding = contentPadding,
                content = content,
            )

            AppButtonVariant.DANGER -> Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = effectiveEnabled,
                shape = KioskShape.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.error,
                    contentColor = contentColor,
                    disabledContainerColor = extra.disabledContainer,
                    disabledContentColor = extra.disabledContent,
                ),
                contentPadding = contentPadding,
                content = content,
            )

            AppButtonVariant.TEXT -> TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = effectiveEnabled,
                shape = KioskShape.button,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = contentColor,
                    disabledContentColor = extra.disabledContent,
                ),
                contentPadding = contentPadding,
                content = content,
            )
        }
    }

    if (disabledReason != null && !effectiveEnabled) {
        AppTooltip(disabledReason) { button() }
    } else {
        button()
    }
}

enum class AppIconButtonVariant { STANDARD, TONAL, FILLED }

enum class AppIconButtonSize(val box: Dp, val icon: Dp) {
    LARGE(48.dp, Sizes.icon),
    MEDIUM(Sizes.minTouchTarget, Sizes.iconCompact),
    SMALL(Sizes.minTouchTarget, Sizes.iconSmall),
}

@Composable
fun AppIconButton(
    icon: AppIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppIconButtonVariant = AppIconButtonVariant.STANDARD,
    size: AppIconButtonSize = AppIconButtonSize.MEDIUM,
    enabled: Boolean = true,
    tooltip: String? = null,
    tint: Color? = null,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra
    val background = when (variant) {
        AppIconButtonVariant.STANDARD -> Color.Transparent
        AppIconButtonVariant.TONAL -> colors.secondaryContainer
        AppIconButtonVariant.FILLED -> colors.primary
    }
    val foreground = when (variant) {
        AppIconButtonVariant.STANDARD -> colors.onSurfaceVariant
        AppIconButtonVariant.TONAL -> colors.onSecondaryContainer
        AppIconButtonVariant.FILLED -> colors.onPrimary
    }

    val button: @Composable () -> Unit = {
        Box(
            modifier = modifier
                .focusRing(CircleShape)
                .size(size.box)
                .clip(CircleShape)
                .background(if (enabled) background else extra.disabledContainer)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                icon = icon,
                contentDescription = contentDescription,
                size = size.icon,
                tint = when {
                    !enabled -> extra.disabledContent
                    tint != null -> tint
                    else -> foreground
                },
            )
        }
    }

    if (tooltip != null) {
        AppTooltip(tooltip) { button() }
    } else {
        button()
    }
}

/**
 * Pill-shaped segmented control: one selected option, flat tonal track.
 */
@Composable
fun AppButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = KioskColors.scheme
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = KioskShape.chip,
        color = colors.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .padding(Space.xxs)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Space.xxs),
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val container by animateColorAsState(
                    targetValue = if (selected) colors.primary else Color.Transparent,
                    animationSpec = Motion.fast(),
                    label = "buttonGroupContainer",
                )
                val content by animateColorAsState(
                    targetValue = when {
                        !enabled -> KioskColors.extra.disabledContent
                        selected -> colors.onPrimary
                        else -> colors.onSurfaceVariant
                    },
                    animationSpec = Motion.fast(),
                    label = "buttonGroupContent",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .focusRing(KioskShape.chip)
                        .height(Sizes.buttonSmall)
                        .clip(KioskShape.chip)
                        .background(container)
                        .selectable(
                            selected = selected,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onSelect(index) },
                        )
                        .padding(horizontal = Space.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
