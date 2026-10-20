package app.kiosksync.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImageFormatTest {

    @Test
    fun `detects jpeg signature`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        assertEquals(ImageFormat.JPEG, ImageFormat.detect(bytes))
    }

    @Test
    fun `detects png signature`() {
        val bytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
        assertEquals(ImageFormat.PNG, ImageFormat.detect(bytes))
    }

    @Test
    fun `detects webp signature`() {
        val bytes = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0x20, 0x00, 0x00, 0x00,
            'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte()
        )
        assertEquals(ImageFormat.WEBP, ImageFormat.detect(bytes))
    }

    @Test
    fun `rejects unknown formats`() {
        assertNull(ImageFormat.detect(byteArrayOf(0x47, 0x49, 0x46, 0x38)))
        assertNull(ImageFormat.detect(ByteArray(0)))
    }
}
