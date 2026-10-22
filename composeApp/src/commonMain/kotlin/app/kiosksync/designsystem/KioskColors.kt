package app.kiosksync.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tonal palettes generated from the Override Kiosk seed blue `#3D7AF5` using CIE
 * L*a*b* tones (see `docs/DESIGN_SYSTEM.md` for the contrast report).
 *
 * Every role maps to a Material 3 tonal step, so light and dark themes are
 * guaranteed to be coherent.
 */
private object Palette {
    // Primary (hue 288, seed #3D7AF5)
    const val P0 = 0xFF000003
    const val P10 = 0xFF001A43
    const val P20 = 0xFF002E6B
    const val P30 = 0xFF21438B
    const val P40 = 0xFF295AB9
    const val P70 = 0xFF95A6FA
    const val P80 = 0xFFBBC3FF
    const val P90 = 0xFFDEE0FF
    const val P95 = 0xFFEFF0FF
    const val P99 = 0xFFFCFCFF

    // Secondary (same hue, low chroma)
    const val S10 = 0xFF151A30
    const val S20 = 0xFF2B2F46
    const val S30 = 0xFF42455E
    const val S40 = 0xFF5A5D77
    const val S80 = 0xFFC2C5E3
    const val S90 = 0xFFDEE0FF

    // Tertiary (complementary rose)
    const val T10 = 0xFF380820
    const val T20 = 0xFF511F35
    const val T30 = 0xFF6A364C
    const val T40 = 0xFF844E64
    const val T80 = 0xFFF3B5CD
    const val T90 = 0xFFFFD8E6

    // Neutral
    const val N4 = 0xFF0E0E12
    const val N6 = 0xFF131317
    const val N10 = 0xFF1B1B1F
    const val N12 = 0xFF1F1F23
    const val N17 = 0xFF292A2D
    const val N20 = 0xFF303034
    const val N22 = 0xFF343438
    const val N24 = 0xFF39393D
    const val N30 = 0xFF46464A
    const val N50 = 0xFF76777B
    const val N60 = 0xFF909095
    const val N80 = 0xFFC6C6CB
    const val N87 = 0xFFD9DADE
    const val N90 = 0xFFE2E2E7
    const val N92 = 0xFFE7E8ED
    const val N94 = 0xFFEDEDF2
    const val N95 = 0xFFF0F0F5
    const val N96 = 0xFFF3F3F8
    const val N98 = 0xFFF9F9FE
    const val N100 = 0xFFFFFFFF

    // Neutral variant (slight blue tint)
    const val NV30 = 0xFF444655
    const val NV50 = 0xFF747687
    const val NV60 = 0xFF8E8FA1
    const val NV80 = 0xFFC4C5D8
    const val NV90 = 0xFFE0E1F4

    // Error
    const val E10 = 0xFF3F0400
    const val E20 = 0xFF660611
    const val E30 = 0xFF822525
    const val E40 = 0xFF9F3F3B
    const val E80 = 0xFFFFB4AB
    const val E90 = 0xFFFFDAD6
}

val KioskDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(Palette.P80),
    onPrimary = Color(Palette.P20),
    primaryContainer = Color(Palette.P30),
    onPrimaryContainer = Color(Palette.P90),
    inversePrimary = Color(Palette.P40),
    secondary = Color(Palette.S80),
    onSecondary = Color(Palette.S20),
    secondaryContainer = Color(Palette.S30),
    onSecondaryContainer = Color(Palette.S90),
    tertiary = Color(Palette.T80),
    onTertiary = Color(Palette.T20),
    tertiaryContainer = Color(Palette.T30),
    onTertiaryContainer = Color(Palette.T90),
    background = Color(Palette.N6),
    onBackground = Color(Palette.N90),
    surface = Color(Palette.N6),
    onSurface = Color(Palette.N90),
    surfaceVariant = Color(Palette.NV30),
    onSurfaceVariant = Color(Palette.NV80),
    surfaceTint = Color(Palette.P80),
    inverseSurface = Color(Palette.N90),
    inverseOnSurface = Color(Palette.N20),
    error = Color(Palette.E80),
    onError = Color(Palette.E20),
    errorContainer = Color(Palette.E30),
    onErrorContainer = Color(Palette.E90),
    outline = Color(Palette.NV60),
    outlineVariant = Color(Palette.NV30),
    scrim = Color(0xFF000000),
    surfaceBright = Color(Palette.N24),
    surfaceDim = Color(Palette.N6),
    surfaceContainerLowest = Color(Palette.N4),
    surfaceContainerLow = Color(Palette.N10),
    surfaceContainer = Color(Palette.N12),
    surfaceContainerHigh = Color(Palette.N17),
    surfaceContainerHighest = Color(Palette.N22),
)

val KioskLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(Palette.P40),
    onPrimary = Color(Palette.N100),
    primaryContainer = Color(Palette.P90),
    onPrimaryContainer = Color(Palette.P10),
    inversePrimary = Color(Palette.P80),
    secondary = Color(Palette.S40),
    onSecondary = Color(Palette.N100),
    secondaryContainer = Color(Palette.N90),
    onSecondaryContainer = Color(Palette.N10),
    tertiary = Color(Palette.T40),
    onTertiary = Color(Palette.N100),
    tertiaryContainer = Color(Palette.T90),
    onTertiaryContainer = Color(Palette.T10),
    background = Color(Palette.N98),
    onBackground = Color(Palette.N10),
    surface = Color(Palette.N98),
    onSurface = Color(Palette.N10),
    surfaceVariant = Color(Palette.NV90),
    onSurfaceVariant = Color(Palette.NV30),
    surfaceTint = Color(Palette.P40),
    inverseSurface = Color(Palette.N20),
    inverseOnSurface = Color(Palette.N95),
    error = Color(Palette.E40),
    onError = Color(Palette.N100),
    errorContainer = Color(Palette.E90),
    onErrorContainer = Color(Palette.E10),
    outline = Color(Palette.NV50),
    outlineVariant = Color(Palette.NV80),
    scrim = Color(0xFF000000),
    surfaceBright = Color(Palette.N98),
    surfaceDim = Color(Palette.N87),
    surfaceContainerLowest = Color(Palette.N100),
    surfaceContainerLow = Color(Palette.N96),
    surfaceContainer = Color(Palette.N94),
    surfaceContainerHigh = Color(Palette.N92),
    surfaceContainerHighest = Color(Palette.N90),
)

/**
 * Roles that Material 3 does not model: success, warning, disabled and
 * informational containers. They are the only extra colours the product needs;
 * error stays reserved for destructive actions and security failures.
 */
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

val DarkExtraColors = KioskExtraColors(
    success = Color(0xFF7DDBA0),
    onSuccess = Color(0xFF00391D),
    successContainer = Color(0xFF00522C),
    onSuccessContainer = Color(0xFF9AF8BC),
    warning = Color(0xFFF5C048),
    onWarning = Color(0xFF3F2E00),
    warningContainer = Color(0xFF5A4300),
    onWarningContainer = Color(0xFFFFDF9A),
    infoContainer = Color(Palette.P30),
    onInfoContainer = Color(Palette.P90),
    disabledContainer = Color(Palette.N22),
    disabledContent = Color(Palette.NV80),
)

val LightExtraColors = KioskExtraColors(
    success = Color(0xFF006D3C),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFF9AF8BC),
    onSuccessContainer = Color(0xFF00210F),
    warning = Color(0xFF7A5900),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFDF9A),
    onWarningContainer = Color(0xFF261A00),
    infoContainer = Color(Palette.P90),
    onInfoContainer = Color(Palette.P10),
    disabledContainer = Color(Palette.N90),
    disabledContent = Color(Palette.NV30),
)

val LocalKioskExtraColors = staticCompositionLocalOf { DarkExtraColors }

/** Access to the Override Kiosk-specific colours that Material 3 does not provide. */
object KioskColors {
    val scheme: ColorScheme
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme

    val extra: KioskExtraColors
        @Composable @ReadOnlyComposable get() = LocalKioskExtraColors.current
}


/**
 * Fixed colours of the Screen role while it plays content: pure black and pure
 * white, regardless of the selected theme, so a TV never shows a tinted
 * background and the image keeps its real colours.
 */
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
