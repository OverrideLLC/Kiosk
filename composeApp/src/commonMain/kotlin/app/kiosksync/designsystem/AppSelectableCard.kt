package app.kiosksync.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Selectable card used for roles and screens. Selected cards use the primary
 * container, a focus ring and a check badge; the corner radius morphs slightly
 * with a spring.
 */
@Composable
fun AppSelectableCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: AppIcon? = null,
    role: Role = Role.Checkbox,
    enabled: Boolean = true,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra
    var focused by remember { mutableStateOf(false) }

    val container by animateColorAsState(
        targetValue = when {
            selected -> colors.primaryContainer
            else -> colors.surfaceContainerLow
        },
        animationSpec = Motion.medium(),
        label = "cardContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimaryContainer else colors.onSurface,
        animationSpec = Motion.medium(),
        label = "cardContent",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            focused -> colors.primary
            selected -> colors.primary
            else -> colors.outlineVariant
        },
        animationSpec = Motion.fast(),
        label = "cardBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (focused || selected) 2.dp else 1.dp,
        animationSpec = Motion.fast(),
        label = "cardBorderWidth",
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (selected) KioskShape.cardCornerSelected else KioskShape.cardCorner,
        animationSpec = Motion.medium(),
        label = "cardCorner",
    )

    Surface(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .selectable(
                selected = selected,
                enabled = enabled,
                role = role,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = if (enabled) container else extra.disabledContainer,
        contentColor = if (enabled) contentColor else extra.disabledContent,
        border = BorderStroke(
            width = if (focused) 3.dp else borderWidth,
            color = borderColor,
        ),
    ) {
        Column(modifier = Modifier.padding(Space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) colors.primary else colors.surfaceContainerHigh,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(
                            icon = icon,
                            contentDescription = null,
                            size = Sizes.iconLarge,
                            tint = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(Space.md))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (description != null) {
                        Spacer(Modifier.size(Space.xxs))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) {
                                colors.onPrimaryContainer
                            } else {
                                colors.onSurfaceVariant
                            },
                        )
                    }
                }
                if (trailing != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.xs),
                    ) {
                        trailing()
                    }
                }
                AnimatedVisibility(
                    visible = selected,
                    enter = scaleIn(animationSpec = Motion.medium()) + fadeIn(animationSpec = Motion.fade()),
                    exit = scaleOut(animationSpec = Motion.fast()) + fadeOut(animationSpec = Motion.fade()),
                ) {
                    AppIcon(
                        icon = AppIcon.CHECK_CIRCLE,
                        contentDescription = null,
                        size = Sizes.iconLarge,
                        tint = colors.primary,
                    )
                }
            }
            if (content != null) {
                Spacer(Modifier.size(Space.md))
                content()
            }
        }
    }
}
