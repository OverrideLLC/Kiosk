package app.kiosksync.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme

/** Extra semantic roles that Material 3 does not define. */
@Immutable
data class KioskExtraColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
)

private val SuccessSeed = Color(0xFF1B8A4A)
private val WarningSeed = Color(0xFFF5A524)

internal fun kioskExtraColors(isDark: Boolean, scheme: ColorScheme): KioskExtraColors {
    val success = semanticScheme(SuccessSeed, isDark)
    val warning = semanticScheme(WarningSeed, isDark)
    return KioskExtraColors(
        success = success.primary,
        onSuccess = success.onPrimary,
        successContainer = success.primaryContainer,
        onSuccessContainer = success.onPrimaryContainer,
        warning = warning.primary,
        onWarning = warning.onPrimary,
        warningContainer = warning.primaryContainer,
        onWarningContainer = warning.onPrimaryContainer,
        infoContainer = scheme.primaryContainer,
        onInfoContainer = scheme.onPrimaryContainer,
        disabledContainer = scheme.surfaceContainerHighest,
        disabledContent = scheme.onSurfaceVariant,
    )
}

private fun semanticScheme(seedColor: Color, isDark: Boolean): ColorScheme = dynamicColorScheme(
    seedColor = seedColor,
    isDark = isDark,
    style = PaletteStyle.TonalSpot,
    specVersion = ColorSpec.SpecVersion.SPEC_2025,
)

private val FallbackExtraColors = kioskExtraColors(
    isDark = false,
    scheme = semanticScheme(KioskSeedColor, isDark = false),
)

val LocalKioskExtraColors = staticCompositionLocalOf { FallbackExtraColors }

/** Access to Override Kiosk-specific colors that Material 3 does not provide. */
object KioskColors {
    val scheme: ColorScheme
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme

    val extra: KioskExtraColors
        @Composable @ReadOnlyComposable get() = LocalKioskExtraColors.current
}

/** Fixed colors for playback on the Screen role, regardless of the selected theme. */
object KioskScreenColors {
    val background: Color = Color(0xFF000000)
    val content: Color = Color(0xFFFFFFFF)

    /** Secondary text on black (contrast 9.5:1). */
    val muted: Color = Color(0xFFB8B8BE)

    /** Tertiary text and countdowns on black (contrast 6.2:1). */
    val subtle: Color = Color(0xFF8A8A90)

    /** Security failures and lockouts on black (contrast 9.9:1). */
    val error: Color = Color(0xFFFFB4AB)

    /** Inactive pairing code while the receiver rejects attempts. */
    val disabled: Color = Color(0xFF5C5C62)

    /** Subtle brand chip on the pure black screen. */
    val surfaceSubtle: Color = Color(0xFF1C1C20)
}
