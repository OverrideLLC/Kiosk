package app.kiosksync.playback

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import app.kiosksync.feature.playback.presentation.DefaultIdleCarousel
import kotlin.test.Test

/**
 * Smoke test for the idle state: it is a static brand mark now, so a couple of
 * frames are enough to catch a broken draw call or a missing icon/font.
 */
class IdleCarouselRenderTest {

    @Test
    fun rendersIdleStateWithoutThrowing() {
        val scene = ImageComposeScene(
            width = 1280,
            height = 720,
            density = Density(1f)
        ) {
            DefaultIdleCarousel()
        }

        try {
            var nanos = 0L
            repeat(3) {
                scene.render(nanos)
                nanos += 250_000_000L
            }
        } finally {
            scene.close()
        }
    }
}
