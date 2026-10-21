package app.kiosksync.playback

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import app.kiosksync.feature.playback.DefaultIdleCarousel
import kotlin.test.Test

/**
 * Smoke test for the idle animation: renders every slide (and the crossfade
 * between them) offscreen so a broken draw call fails the build instead of
 * crashing a screen at run time.
 */
class IdleCarouselRenderTest {

    @Test
    fun rendersEverySlideWithoutThrowing() {
        val scene = ImageComposeScene(
            width = 1280,
            height = 720,
            density = Density(1f)
        ) {
            DefaultIdleCarousel()
        }

        try {
            // Five slides of 7.5 s each plus their 1.6 s crossfades.
            var nanos = 0L
            val step = 500_000_000L
            while (nanos <= 45_000_000_000L) {
                scene.render(nanos)
                nanos += step
            }
        } finally {
            scene.close()
        }
    }
}
