package app.kiosksync.feature.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.kiosksync.core.APP_NAME
import app.kiosksync.designsystem.AppIcon
import app.kiosksync.designsystem.KioskDisplayType
import app.kiosksync.designsystem.KioskScreenColors
import app.kiosksync.designsystem.Sizes
import app.kiosksync.designsystem.Space

/**
 * Idle state for a Screen without content: a static, high-contrast brand mark
 * on pure black. The old animated gradients were removed on purpose: the Screen
 * role must not depend on decorative motion, and flat black is easier to read
 * from a distance.
 */
@Composable
fun DefaultIdleCarousel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KioskScreenColors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(Sizes.emptyIconBubble)
                    .clip(CircleShape)
                    .border(Sizes.selectionBorder, KioskScreenColors.subtle, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    icon = AppIcon.CAST,
                    contentDescription = null,
                    size = Sizes.iconHero,
                    tint = KioskScreenColors.muted,
                )
            }
            Spacer(Modifier.height(Space.lg))
            Text(
                text = APP_NAME,
                style = KioskDisplayType.screenBody(),
                color = KioskScreenColors.content,
            )
        }
    }
}
