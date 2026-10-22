package app.kiosksync.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import app.kiosksync.core.OVERRIDE_WEBSITE
import app.kiosksync.core.Strings
import app.kiosksync.resources.Res
import app.kiosksync.resources.override_logo
import org.jetbrains.compose.resources.painterResource

/**
 * The Override mark. It is decorative by default because the surrounding text
 * already names the brand.
 */
@Composable
fun OverrideLogo(
    modifier: Modifier = Modifier,
    size: Dp = Sizes.icon,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(Res.drawable.override_logo),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

/**
 * Small branding footer for windowed screens. Receivers and the projection
 * window deliberately do not use it: those screens are pure image playback.
 */
@Composable
fun OverrideFooter(modifier: Modifier = Modifier) {
    val color = KioskColors.scheme.onSurfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Strings.DEVELOPED_WITH,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
        AppIcon(
            icon = AppIcon.FAVORITE,
            contentDescription = null,
            modifier = Modifier.padding(horizontal = Space.xxs),
            size = Sizes.iconSmall,
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = Strings.DEVELOPED_BY,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
        Row(
            modifier = Modifier
                .clip(KioskShape.chip)
                .clickable { uriHandler.openUri(OVERRIDE_WEBSITE) }
                .padding(
                    start = Space.xs,
                    end = Space.xxs,
                    top = Space.xxs,
                    bottom = Space.xxs,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OverrideLogo(
                size = Sizes.iconSmall,
                tint = linkColor,
            )
            Text(
                text = Strings.OVERRIDE_NAME,
                modifier = Modifier.padding(start = Space.xxs),
                style = MaterialTheme.typography.labelSmall,
                color = linkColor,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}
