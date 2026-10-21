package app.kiosksync.designsystem

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import app.kiosksync.core.AppTheme
import app.kiosksync.designsystem.catalog.CatalogContent
import java.io.File
import kotlin.test.Test
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
            val scene = ImageComposeScene(
                width = 1280,
                height = 5600,
                density = Density(1f),
            ) {
                KioskTheme(theme = theme, reduceMotion = true) {
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
                val png = requireNotNull(image.encodeToData(EncodedImageFormat.PNG)) {
                    "Could not encode the catalog screenshot"
                }
                File(output, "catalog-${theme.name.lowercase()}.png").writeBytes(png.bytes)
            } finally {
                scene.close()
            }
        }
    }
}
