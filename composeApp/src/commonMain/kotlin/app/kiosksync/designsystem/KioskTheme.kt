package app.kiosksync.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.kiosksync.core.AppTheme

/**
 * Root theme. It only provides tokens; screens must not define colours,
 * typography, shapes or motion on their own.
 *
 * @param theme persisted appearance (dark by default, light fully supported).
 * @param reduceMotion disables springs and decorative fades in one place.
 */
@Composable
fun KioskTheme(
    theme: AppTheme = AppTheme.DARK,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = theme == AppTheme.DARK
    CompositionLocalProvider(
        LocalKioskExtraColors provides if (isDark) DarkExtraColors else LightExtraColors,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = if (isDark) KioskDarkColorScheme else KioskLightColorScheme,
            typography = kioskTypography(),
            shapes = KioskShapes,
            content = content,
        )
    }
}
