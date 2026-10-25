package app.kiosksync.feature.guide.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import app.kiosksync.core.AppRole
import app.kiosksync.core.AppTheme
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.guide.domain.GuideStep
import app.kiosksync.feature.guide.presentation.components.GuideOverlay
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * Offscreen snapshots of the guide card. They protect the layout of the step
 * that overlaps the real application and make review possible without opening
 * a window.
 */
class GuideRenderTest {

    @Test
    fun rendersGuideCards() {
        val output = File("build/reports/ui").apply { mkdirs() }

        render(
            output = output,
            name = "guide-emitter-send-dark",
            theme = AppTheme.DARK,
            state = GuideUiState(
                isActive = true,
                role = AppRole.EMITTER,
                step = GuideStep.EMITTER_SEND,
                playlistItems = 3,
                pairedScreens = 1,
                selectedScreens = 1,
                hasPassword = true,
            ),
        )

        render(
            output = output,
            name = "guide-receiver-code-light",
            theme = AppTheme.LIGHT,
            state = GuideUiState(
                isActive = true,
                role = AppRole.RECEIVER,
                step = GuideStep.RECEIVER_CODE,
            ),
        )

        render(
            output = output,
            name = "guide-emitter-done-dark",
            theme = AppTheme.DARK,
            state = GuideUiState(
                isActive = true,
                role = AppRole.EMITTER,
                step = GuideStep.EMITTER_DONE,
                playlistItems = 3,
                pairedScreens = 1,
                selectedScreens = 1,
                hasPassword = true,
                sendSucceeded = true,
            ),
        )
    }

    private fun render(
        output: File,
        name: String,
        theme: AppTheme,
        state: GuideUiState,
    ) {
        val scene = ImageComposeScene(
            width = 1280,
            height = 800,
            density = Density(1f),
        ) {
            KioskTheme(theme = theme, reduceMotion = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    GuideOverlay(
                        state = state,
                        onChooseRole = {},
                        onAction = {},
                    )
                }
            }
        }
        try {
            var nanos = 0L
            repeat(3) {
                scene.render(nanos)
                nanos += 250_000_000L
            }
            val image = scene.render(nanos)
            val png = requireNotNull(image.encodeToData(EncodedImageFormat.PNG)) {
                "Could not encode $name"
            }
            File(output, "$name.png").writeBytes(png.bytes)
        } finally {
            scene.close()
        }
    }
}
