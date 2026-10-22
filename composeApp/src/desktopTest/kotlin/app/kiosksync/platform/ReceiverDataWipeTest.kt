package app.kiosksync.platform

import app.kiosksync.NoOpLogger
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.data.local.RoomRepositories
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.data.local.room.buildKioskDatabase
import app.kiosksync.platform.secret.DevelopmentSecretProtector
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class ReceiverDataWipeTest {
    private lateinit var dir: File
    private lateinit var repositories: RoomRepositories

    @BeforeTest
    fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "kiosksync-wipe-${System.nanoTime()}")
        dir.mkdirs()
        repositories = RoomRepositories(
            dir,
            NoOpLogger(),
            buildKioskDatabase(dir),
            DevelopmentSecretProtector,
        )
    }

    @AfterTest
    fun tearDown() {
        repositories.close()
        dir.deleteRecursively()
    }

    @Test
    fun clearReceiverDataRemovesContentLockAndPairing() = runBlocking {
        repositories.playlists.saveReceiverPlaylist(Playlist(version = 3, defaultDurationSec = 10))
        repositories.kioskLock.save(
            KioskLockConfig(passwordHash = "abc", passwordLength = 3, enabled = true),
        )
        repositories.pairing.savePairedEmitter(
            ReceiverPairedInfo(emitterId = "emitter-1", emitterName = "Control", token = "token"),
        )
        File(dir, "media").mkdirs()
        File(dir, "media/one.jpg").writeBytes(byteArrayOf(1, 2, 3))

        repositories.receiverDataResetter.clearReceiverData()

        assertNull(repositories.playlists.loadReceiverPlaylist())
        assertNull(repositories.kioskLock.load())
        assertNull(repositories.pairing.loadPairedEmitter())
        assertEquals(0, File(dir, "media").listFiles()?.size ?: 0)
    }

    @Test
    fun clearReceiverDataIsSafeOnAFreshDirectory() = runBlocking {
        repositories.receiverDataResetter.clearReceiverData()

        assertNull(repositories.playlists.loadReceiverPlaylist())
        assertNull(repositories.kioskLock.load())
        assertNull(repositories.pairing.loadPairedEmitter())
    }
}
