package app.kiosksync.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material 3 shape roles, rounded for the expressive look. */
val KioskShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Named shapes used by the components. A selected card "morphs" to a slightly
 * rounder shape, which is the only expressive shape trick the product needs.
 */
object KioskShape {
    val card: Shape = RoundedCornerShape(28.dp)
    val cardSelected: Shape = RoundedCornerShape(32.dp)
    val cardCorner: Dp get() = 28.dp
    val cardCornerSelected: Dp get() = 32.dp
    val button: Shape = RoundedCornerShape(percent = 50)
    val chip: Shape = RoundedCornerShape(percent = 50)
    val field: Shape = RoundedCornerShape(16.dp)
    val dialog: Shape = RoundedCornerShape(28.dp)
    val menu: Shape = RoundedCornerShape(20.dp)
    val thumbnail: Shape = RoundedCornerShape(12.dp)
    val small: Shape = RoundedCornerShape(12.dp)
}

/** 4/8 dp spacing scale. */
object Space {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
    val huge = 56.dp
}

/** Fixed sizes shared by the components. */
object Sizes {
    val minTouchTarget = 40.dp
    val contentMaxWidth = 960.dp
    val wideLayoutBreakpoint = 1100.dp
    val dialogMaxWidth = 480.dp
    val dialogWideMaxWidth = 600.dp
    val dialogMinWidth = 320.dp
    val dialogBodyMaxHeight = 440.dp
    val avatar = 44.dp
    val iconBubbleSmall = 40.dp
    val durationFieldWidth = 84.dp
    val stackedListMaxHeight = 320.dp
    val playlistRowMinHeight = 72.dp
    val securityDivider = 1.dp
    val selectionBorder = 2.dp
    val hairline = 1.dp
    val stepperWidth = 200.dp
    val emptyStateMinHeight = 220.dp
    val loadingDotSmall = 6.dp

    val iconSmall = 16.dp
    val iconCompact = 20.dp
    val icon = 24.dp
    val iconLarge = 28.dp
    val iconHero = 40.dp

    val buttonLarge = 56.dp
    val buttonMedium = 48.dp
    val buttonSmall = 40.dp

    val fieldHeight = 56.dp
    val listThumbnailWidth = 112.dp
    val listThumbnailHeight = 63.dp
    val emptyIconBubble = 88.dp
}
