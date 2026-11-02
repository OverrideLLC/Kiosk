package app.kiosksync.core.ui.components

import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.Motion
import app.kiosksync.core.ui.theme.Space

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Generic list row: thumbnail/avatar on the left, title and subtitle, actions
 * on the right. Hover is expressed with a tonal overlay, not elevation.
 */
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val container by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            selected -> colors.primaryContainer
            hovered -> colors.surfaceContainerHigh
            else -> Color.Transparent
        },
        animationSpec = Motion.fast(),
        label = "listItemContainer",
    )

    Row(
        modifier = modifier
            .focusRing(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .hoverable(interaction, enabled = enabled)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        enabled = enabled,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = Space.sm, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        if (leading != null) {
            leading()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    if (selected) colors.onPrimaryContainer else colors.onSurface
                } else {
                    extra.disabledContent
                },
                maxLines = 1,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xxs),
                content = trailing,
            )
        }
    }
}
