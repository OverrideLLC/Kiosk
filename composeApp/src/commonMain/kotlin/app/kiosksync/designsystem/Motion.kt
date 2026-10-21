package app.kiosksync.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Single switch to disable every spring/fade of the app. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/**
 * All motion specs live here. Short expressive springs (150-350 ms perceived)
 * for selection, rows, menus and dialogs; nothing decorative on the Screen role.
 */
object Motion {
    const val FAST_MS = 150
    const val MEDIUM_MS = 220
    const val SLOW_MS = 320

    @Composable
    fun <T> fast(): FiniteAnimationSpec<T> =
        if (LocalReduceMotion.current) snap() else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 900f,
        )

    @Composable
    fun <T> medium(): FiniteAnimationSpec<T> =
        if (LocalReduceMotion.current) snap() else spring(
            dampingRatio = 0.82f,
            stiffness = 600f,
        )

    @Composable
    fun <T> slow(): FiniteAnimationSpec<T> =
        if (LocalReduceMotion.current) snap() else spring(
            dampingRatio = 0.78f,
            stiffness = 320f,
        )

    /** Fade used by snackbars and content swaps. */
    @Composable
    fun fade(durationMs: Int = MEDIUM_MS): FiniteAnimationSpec<Float> =
        if (LocalReduceMotion.current) snap() else tween(
            durationMillis = durationMs,
            easing = FastOutSlowInEasing,
        )
}
