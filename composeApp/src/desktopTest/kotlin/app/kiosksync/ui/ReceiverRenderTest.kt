package app.kiosksync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import app.kiosksync.FakeLocalStorage
import app.kiosksync.NoOpLogger
import app.kiosksync.core.AppTheme
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.kiosk.KioskLockUiState
import app.kiosksync.feature.kiosk.KioskUnlockOverlay
import app.kiosksync.feature.pairing.PairingCodeScreen
import app.kiosksync.feature.pairing.PairingManager
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * Offscreen snapshots of the Screen role states. They catch crashes in fonts,
 * icons and layout and leave PNGs in `composeApp/build/reports/ui`.
 */
class ReceiverRenderTest {

    @Test
    fun rendersReceiverStates() {
        val output = File("build/reports/ui").apply { mkdirs() }
        val pairingManager = PairingManager(
            localStorage = FakeLocalStorage(),
            logger = NoOpLogger(),
            codeGenerator = { "685853" },
        )

        render(output, "receiver-pairing") {
            PairingCodeScreen(
                pairingManager = pairingManager,
                serviceError = null,
                onExitRequested = {},
                onChangeRoleRequested = {},
                receiverIp = "192.168.1.20",
                receiverPort = 8765,
            )
        }
        render(output, "receiver-waiting") {
            WaitingForPasswordScreen()
        }
        render(output, "receiver-unlock") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KioskScreenColors.background),
            ) {
                KioskUnlockOverlay(
                    KioskLockUiState(
                        isLocked = true,
                        showUnlockHint = true,
                        typedLength = 4,
                    )
                )
            }
        }
    }

    private fun render(output: File, name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        val scene = ImageComposeScene(
            width = 1280,
            height = 720,
            density = Density(1f),
        ) {
            KioskTheme(theme = AppTheme.DARK, reduceMotion = true) {
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
}
