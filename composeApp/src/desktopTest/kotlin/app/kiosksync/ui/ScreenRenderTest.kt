package app.kiosksync.ui

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.Density
import app.kiosksync.FakeLocalStorage
import app.kiosksync.NoOpLogger
import app.kiosksync.core.AppRole
import app.kiosksync.core.AppTheme
import app.kiosksync.designsystem.KioskTheme
import app.kiosksync.feature.playback.CarouselScreen
import app.kiosksync.feature.playback.PlaybackStateHolder
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.playlist.PlaylistItem
import app.kiosksync.feature.role.RoleSelectionScreen
import app.kiosksync.feature.role.RoleStateHolder
import java.io.File
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.jetbrains.skia.EncodedImageFormat

/**
 * Offscreen snapshots of the first-run role picker and the Screen playback.
 * They verify type scale, spacing and the idle fallback without a window.
 */
class ScreenRenderTest {

    @Test
    fun rendersRoleSelection() {
        val output = File("build/reports/ui").apply { mkdirs() }
        val holder = RoleStateHolder(
            localStorage = FakeLocalStorage(),
            logger = NoOpLogger(),
            initialRole = AppRole.RECEIVER,
        )
        for (theme in AppTheme.entries) {
            render(
                output = output,
                name = "role-${theme.name.lowercase()}",
                theme = theme,
                width = 900,
                height = 650,
            ) {
                RoleSelectionScreen(
                    stateHolder = holder,
                    onRoleConfirmed = {},
                    deviceId = "device",
                    deviceName = "PC",
                    port = 8765,
                    theme = theme,
                )
            }
        }
    }

    @Test
    fun rendersPlayback() {
        val output = File("build/reports/ui").apply { mkdirs() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val bitmap = solidBitmap(Color(0xFF21438B))
            val storage = FakeLocalStorage(
                playlist = Playlist(
                    id = "p1",
                    name = "Promociones",
                    version = 3,
                    defaultDurationSec = 10,
                    items = listOf(
                        PlaylistItem("m1", "ficha-verano.png", "h1"),
                    ),
                ),
                availableBitmaps = mutableMapOf("m1" to bitmap),
            )
            val holder = PlaybackStateHolder(
                localStorage = storage,
                logger = NoOpLogger(),
                scope = scope,
                backgroundDispatcher = Dispatchers.Unconfined,
            )
            holder.start()
            check(holder.uiState.value.currentBitmap != null) {
                "Playback did not load the fake bitmap"
            }
            render(
                output = output,
                name = "screen-playback",
                theme = AppTheme.DARK,
                width = 1280,
                height = 720,
            ) {
                CarouselScreen(stateHolder = holder)
            }
            holder.stop()
        } finally {
            scope.cancel()
        }
    }

    private fun render(
        output: File,
        name: String,
        theme: AppTheme,
        width: Int,
        height: Int,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        val scene = ImageComposeScene(
            width = width,
            height = height,
            density = Density(1f),
        ) {
            KioskTheme(theme = theme, reduceMotion = true) {
                content()
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

    private fun solidBitmap(color: Color): ImageBitmap {
        val bitmap = ImageBitmap(320, 180)
        Canvas(bitmap).drawRect(
            Rect(0f, 0f, 320f, 180f),
            Paint().apply { this.color = color },
        )
        return bitmap
    }
}
