package app.kiosksync.feature.media.data

import app.kiosksync.NoOpLogger
import app.kiosksync.core.ImageFormat
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileMediaStoreTest {
    private lateinit var dir: File
    private lateinit var mediaStore: FileMediaStore

    @BeforeTest
    fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "kiosk-media-store-${System.nanoTime()}")
        dir.mkdirs()
        mediaStore = FileMediaStore(dir, NoOpLogger())
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun receivedMediaIsRenamedIntoPlaceAndTemporaryFileIsRemoved() {
        val bytes = byteArrayOf(1, 2, 3, 4)

        val stored = mediaStore.writeReceived("media-1", ImageFormat.PNG, bytes)

        assertEquals("media-1.png", stored?.fileName)
        assertTrue(File(dir, "media/media-1.png").isFile)
        assertFalse(File(dir, "media").listFiles().orEmpty().any { it.extension == "part" })
    }

    @Test
    fun receivedMediaRejectsPathLikeIds() {
        val stored = mediaStore.writeReceived("../outside", ImageFormat.PNG, byteArrayOf(1))

        assertNull(stored)
        assertFalse(File(dir, "outside.png").exists())
        assertTrue(File(dir, "media").listFiles().orEmpty().isEmpty())
    }
}
