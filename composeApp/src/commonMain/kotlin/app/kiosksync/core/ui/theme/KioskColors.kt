package app.kiosksync.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

internal fun kioskExtraColors(isDark: Boolean, scheme: ColorScheme) =
    if (isDark) {
        KioskExtraColors(
            success = Color(0xFF7DDBA0),
            onSuccess = Color(0xFF00391D),
            successContainer = Color(0xFF00522C),
            onSuccessContainer = Color(0xFF9AF8BC),
            warning = Color(0xFFF5C048),
            onWarning = Color(0xFF3F2E00),
            warningContainer = Color(0xFF5A4300),
            onWarningContainer = Color(0xFFFFDF9A),
            infoContainer = scheme.primaryContainer,
            onInfoContainer = scheme.onPrimaryContainer,
            disabledContainer = scheme.surfaceContainerHighest,
            disabledContent = scheme.onSurfaceVariant,
        )
    } else {
        KioskExtraColors(
            success = Color(0xFF006D3C),
            onSuccess = Color(0xFFFFFFFF),
            successContainer = Color(0xFF9AF8BC),
            onSuccessContainer = Color(0xFF00210F),
            warning = Color(0xFF7A5900),
            onWarning = Color(0xFFFFFFFF),
            warningContainer = Color(0xFFFFDF9A),
            onWarningContainer = Color(0xFF261A00),
            infoContainer = scheme.primaryContainer,
            onInfoContainer = scheme.onPrimaryContainer,
            disabledContainer = scheme.surfaceContainerHighest,
            disabledContent = scheme.onSurfaceVariant,
        )
    }

private val FallbackExtraColors = KioskExtraColors(
    success = Color(0xFF006D3C),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFF9AF8BC),
    onSuccessContainer = Color(0xFF00210F),
    warning = Color(0xFF7A5900),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFDF9A),
    onWarningContainer = Color(0xFF261A00),
    infoContainer = Color(0xFFDEE0FF),
    onInfoContainer = Color(0xFF001A43),
    disabledContainer = Color(0xFFE2E2E7),
    disabledContent = Color(0xFF444655),
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
