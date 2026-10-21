package app.kiosksync.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One entry of an overflow or playlist menu. */
data class AppMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val icon: AppIcon? = null,
    val enabled: Boolean = true,
    /** Only delete/block actions use the error colour. */
    val destructive: Boolean = false,
)

/**
 * Dropdown used for the playlist selector, the playlist actions and the
 * controller overflow menu. It replaces the clusters of small buttons.
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<AppMenuItem>,
    modifier: Modifier = Modifier,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = KioskShape.menu,
        containerColor = colors.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        items.forEach { item ->
            val tint = when {
                !item.enabled -> extra.disabledContent
                item.destructive -> colors.error
                else -> colors.onSurface
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                onClick = {
                    onDismissRequest()
                    item.onClick()
                },
                enabled = item.enabled,
                leadingIcon = item.icon?.let { icon ->
                    {
                        AppIcon(
                            icon = icon,
                            contentDescription = null,
                            size = Sizes.iconCompact,
                            tint = tint,
                        )
                    }
                },
                colors = MenuDefaults.itemColors(
                    textColor = tint,
                    leadingIconColor = tint,
                    disabledTextColor = extra.disabledContent,
                    disabledLeadingIconColor = extra.disabledContent,
                ),
            )
        }
    }
}
