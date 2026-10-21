package app.kiosksync.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Semantic tones for status chips. Error is reserved for real failures. */
enum class AppStatusTone { NEUTRAL, PRIMARY, SUCCESS, WARNING, ERROR }

/**
 * Compact status pill: linked, online, projecting, offline, success, failure.
 * The tone is informative, never the only signal: the label always says what
 * happened.
 */
@Composable
fun AppStatusChip(
    label: String,
    tone: AppStatusTone,
    modifier: Modifier = Modifier,
    icon: AppIcon? = null,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra
    val container = when (tone) {
        AppStatusTone.NEUTRAL -> colors.surfaceContainerHighest
        AppStatusTone.PRIMARY -> colors.primaryContainer
        AppStatusTone.SUCCESS -> extra.successContainer
        AppStatusTone.WARNING -> extra.warningContainer
        AppStatusTone.ERROR -> colors.errorContainer
    }
    val content = when (tone) {
        AppStatusTone.NEUTRAL -> colors.onSurfaceVariant
        AppStatusTone.PRIMARY -> colors.onPrimaryContainer
        AppStatusTone.SUCCESS -> extra.onSuccessContainer
        AppStatusTone.WARNING -> extra.onWarningContainer
        AppStatusTone.ERROR -> colors.onErrorContainer
    }

    Row(
        modifier = modifier
            .height(28.dp)
            .clip(KioskShape.chip)
            .background(container)
            .padding(horizontal = 10.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xxs),
    ) {
        if (icon != null) {
            AppIcon(
                icon = icon,
                contentDescription = null,
                size = Sizes.iconSmall,
                tint = content,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
        )
    }
}
