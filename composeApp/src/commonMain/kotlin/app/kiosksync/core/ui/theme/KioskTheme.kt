package app.kiosksync.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.rememberDynamicColorScheme
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
    val colorScheme = rememberDynamicColorScheme(
        seedColor = Color(0xFF3D7AF5),
        isDark = isDark,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )
    CompositionLocalProvider(
        LocalKioskExtraColors provides kioskExtraColors(isDark, colorScheme),
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = kioskTypography(),
            shapes = KioskShapes,
            content = content,
        )
    }
}
