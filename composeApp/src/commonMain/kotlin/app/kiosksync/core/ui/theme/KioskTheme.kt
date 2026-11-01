package app.kiosksync.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import app.kiosksync.core.common.app.AppTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.DynamicMaterialTheme

/** Brand seed. Material Kolor builds the tonal palettes from this blue. */
val KioskSeedColor = Color(0xFF3D7AF5)

/**
 * Root theme. The Material theme is [DynamicMaterialTheme]: Material Kolor
 * builds the palette from [KioskSeedColor]. Screens still read typography,
 * shapes and spacing from this package instead of defining their own.
 *
 * Material 3 Expressive (`MotionScheme`) is internal in the Material 3 version
 * this project compiles against, so motion stays in [Motion].
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
    DynamicMaterialTheme(
        seedColor = KioskSeedColor,
        isDark = isDark,
        style = PaletteStyle.Expressive,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        shapes = KioskShapes,
        typography = kioskTypography(),
        animate = !reduceMotion,
    ) {
        CompositionLocalProvider(
            LocalKioskExtraColors provides kioskExtraColors(isDark, MaterialTheme.colorScheme),
            LocalReduceMotion provides reduceMotion,
        ) {
            content()
        }
    }
}
