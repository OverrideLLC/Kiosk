package app.kiosksync.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import app.kiosksync.core.AppTheme
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.core.ui.catalog.CatalogContent
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.math.pow
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the design-system catalog offscreen in both themes. It catches
 * broken draw calls (fonts, SVG icons, shapes) at build time and leaves PNG
 * snapshots in `composeApp/build/reports/ui` for visual review.
 */
class CatalogRenderTest {

    @Test
    fun rendersCatalogInBothThemes() {
        val output = File("build/reports/ui").apply { mkdirs() }

        for (theme in AppTheme.entries) {
            val contrastPairs = AtomicReference<List<Pair<String, Pair<Color, Color>>>>()
            val scene = ImageComposeScene(
                width = 1280,
                height = 5600,
                density = Density(1f),
            ) {
                KioskTheme(theme = theme, reduceMotion = true) {
                    val scheme = MaterialTheme.colorScheme
                    val extra = KioskColors.extra
                    contrastPairs.set(
                        listOf(
                            "primary" to (scheme.primary to scheme.onPrimary),
                            "primary container" to (scheme.primaryContainer to scheme.onPrimaryContainer),
                            "secondary" to (scheme.secondary to scheme.onSecondary),
                            "error" to (scheme.error to scheme.onError),
                            "error container" to (scheme.errorContainer to scheme.onErrorContainer),
                            "surface" to (scheme.surface to scheme.onSurface),
                            "success" to (extra.success to extra.onSuccess),
                            "success container" to (extra.successContainer to extra.onSuccessContainer),
                            "warning" to (extra.warning to extra.onWarning),
                            "warning container" to (extra.warningContainer to extra.onWarningContainer),
                            "info container" to (extra.infoContainer to extra.onInfoContainer),
                        ),
                    )
                    CatalogContent(theme = theme, reduceMotion = true)
                }
            }

            try {
                var nanos = 0L
                repeat(4) {
                    scene.render(nanos)
                    nanos += 250_000_000L
                }
                val image = scene.render(nanos)
                val pairs = requireNotNull(contrastPairs.get()) {
                    "Theme tokens were not captured for $theme"
                }
                pairs.forEach { (name, colors) ->
                    val ratio = contrastRatio(colors.first, colors.second)
                    assertTrue(
                        ratio >= 4.5,
                        "$theme $name token contrast was ${"%.2f".format(ratio)}:1; expected at least 4.5:1",
                    )
                }
                val png = requireNotNull(image.encodeToData(EncodedImageFormat.PNG)) {
                    "Could not encode the catalog screenshot"
                }
                File(output, "catalog-${theme.name.lowercase()}.png").writeBytes(png.bytes)
            } finally {
                scene.close()
            }
        }
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        val lighter = maxOf(firstLuminance, secondLuminance)
        val darker = minOf(firstLuminance, secondLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }

        return 0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)
    }
}
