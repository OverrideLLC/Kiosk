package app.kiosksync.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.kiosksync.core.Strings

/**
 * Consistent dialog: icon + title + close, content and a right-aligned action
 * row. Wrap the dangerous action in [AppButton] with the error container only
 * when it actually deletes or blocks something.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: AppIcon? = null,
    maxWidth: Dp = Sizes.dialogMaxWidth,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = KioskColors.scheme

    BasicAlertDialog(
        onDismissRequest = { if (dismissible) onDismissRequest() },
        modifier = modifier.widthIn(
            min = Sizes.dialogMinWidth,
            max = maxWidth,
        ),
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            shape = KioskShape.dialog,
            color = colors.surfaceContainerHigh,
            contentColor = colors.onSurface,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Space.lg),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppIcon(
                                icon = icon,
                                contentDescription = null,
                                size = Sizes.iconCompact,
                                tint = colors.onPrimaryContainer,
                            )
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (dismissible) {
                        AppIconButton(
                            icon = AppIcon.CLOSE,
                            contentDescription = Strings.CLOSE_BUTTON,
                            onClick = onDismissRequest,
                            variant = AppIconButtonVariant.STANDARD,
                            size = AppIconButtonSize.SMALL,
                        )
                    }
                }
                Spacer(Modifier.height(Space.md))
                content()
                Spacer(Modifier.height(Space.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}
