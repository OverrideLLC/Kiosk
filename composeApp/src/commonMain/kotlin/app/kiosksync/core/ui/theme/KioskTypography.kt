package app.kiosksync.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import app.kiosksync.resources.Res
import app.kiosksync.resources.inter_medium
import app.kiosksync.resources.inter_regular
import app.kiosksync.resources.inter_semibold
import app.kiosksync.resources.sora_bold
import app.kiosksync.resources.sora_extrabold
import app.kiosksync.resources.sora_semibold
import org.jetbrains.compose.resources.Font

/**
 * Typography tokens.
 *
 * Headings use Sora (700-800) and body/labels use Inter (400-600), both
 * bundled under the SIL Open Font License (see `docs/licenses/`).
 *
 * Controller scale: nothing below 13 sp. Screen (seen from several metres)
 * uses the [KioskDisplayType] helpers instead.
 */
@Composable
fun kioskTypography(): Typography {
    val heading = FontFamily(
        Font(Res.font.sora_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.sora_bold, weight = FontWeight.Bold),
        Font(Res.font.sora_extrabold, weight = FontWeight.ExtraBold),
    )
    val body = FontFamily(
        Font(Res.font.inter_regular, weight = FontWeight.Normal),
        Font(Res.font.inter_medium, weight = FontWeight.Medium),
        Font(Res.font.inter_semibold, weight = FontWeight.SemiBold),
    )

    return remember(heading, body) {
        Typography(
            displayLarge = TextStyle(
                fontFamily = heading, fontWeight = FontWeight.ExtraBold,
                fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp,
            ),
            displayMedium = TextStyle(
                fontFamily = heading, fontWeight = FontWeight.ExtraBold,
                fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp,
            ),
            displaySmall = TextStyle(
                fontFamily = heading, fontWeight = FontWeight.Bold,
                fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp,
            ),
            headlineLarge = TextStyle(
                fontFamily = heading, fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.2).sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = heading, fontWeight = FontWeight.Bold,
                fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.1).sp,
            ),
            headlineSmall = TextStyle(
                fontFamily = heading, fontWeight = FontWeight.Bold,
                fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp,
            ),
            titleLarge = TextStyle(
                fontFamily = heading, fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = 0.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = heading, fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp,
            ),
            titleSmall = TextStyle(
                fontFamily = heading, fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
            ),
            bodyLarge = TextStyle(
                fontFamily = body, fontWeight = FontWeight.Normal,
                fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = body, fontWeight = FontWeight.Normal,
                fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.15.sp,
            ),
            bodySmall = TextStyle(
                fontFamily = body, fontWeight = FontWeight.Normal,
                fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp,
            ),
            labelLarge = TextStyle(
                fontFamily = body, fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
            ),
            labelMedium = TextStyle(
                fontFamily = body, fontWeight = FontWeight.Medium,
                fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp,
            ),
            labelSmall = TextStyle(
                fontFamily = body, fontWeight = FontWeight.Medium,
                fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp,
            ),
        ).withOpticalLineHeight()
    }
}

private fun Typography.withOpticalLineHeight(): Typography = copy(
    bodyLarge = bodyLarge.optical(),
    bodyMedium = bodyMedium.optical(),
    bodySmall = bodySmall.optical(),
)

private fun TextStyle.optical(): TextStyle = copy(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

/**
 * Distance-reading scale for the Screen role. Kept in the design system so no
 * screen hardcodes a font size.
 */
object KioskDisplayType {
    /** Pairing code: at least 96 sp, monospaced and grouped by the screen. */
    @Composable
    fun pairingCode(): TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 104.sp,
        lineHeight = 112.sp,
        letterSpacing = 8.sp,
    )

    @Composable
    fun screenTitle(): TextStyle = MaterialTheme.typography.displaySmall.copy(
        // Minimum 40 sp for the Screen role, readable from several metres.
        fontSize = 40.sp,
        lineHeight = 48.sp,
    )

    @Composable
    fun screenBody(): TextStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 38.sp,
    )

    @Composable
    fun screenCaption(): TextStyle = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    )
}
