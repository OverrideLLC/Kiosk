package app.kiosksync.platform

import app.kiosksync.NoOpLogger
import app.kiosksync.core.AppRole
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.role.AppConfig
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReceiverDataWipeTest {

    private lateinit var dir: File
    private lateinit var storage: FileLocalStorage

    @BeforeTest
    fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "kiosksync-wipe-${System.nanoTime()}")
        dir.mkdirs()
        storage = FileLocalStorage(dir, NoOpLogger())
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun clearReceiverDataRemovesContentLockAndPairingButKeepsConfig() {
        storage.saveConfig(
            AppConfig(
                role = AppRole.RECEIVER,
                deviceId = "device-1",
                deviceName = "Pantalla",
                port = 8765
            )
        )
        storage.savePlaylist(Playlist(version = 3, defaultDurationSec = 10))
        storage.saveKioskLock(
            KioskLockConfig(passwordHash = "abc", passwordLength = 3, enabled = true)
        )
        storage.saveReceiverPairedInfo(
            ReceiverPairedInfo(emitterId = "emitter-1", emitterName = "Control", token = "token")
        )
        File(dir, "media").mkdirs()
        File(dir, "media/one.jpg").writeBytes(byteArrayOf(1, 2, 3))

        storage.clearReceiverData()

        assertNull(storage.loadPlaylist())
        assertNull(storage.loadKioskLock())
        assertNull(storage.loadReceiverPairedInfo())
        assertEquals(0, File(dir, "media").listFiles()?.size ?: 0)
        // The role must survive so the device still knows it is a screen.
        assertNotNull(storage.loadConfig())
        assertEquals(AppRole.RECEIVER, storage.loadConfig()?.role)
    }

    @Test
    fun clearReceiverDataIsSafeOnAFreshDirectory() {
        storage.clearReceiverData()

        assertNull(storage.loadPlaylist())
        assertNull(storage.loadKioskLock())
        assertNull(storage.loadReceiverPairedInfo())
    }
}
