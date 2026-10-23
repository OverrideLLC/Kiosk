package app.kiosksync.platform.secret

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WindowsDpapiSecretProtectorTest {

    @Test
    fun protectsAndUnprotectsWithCurrentWindowsUser() {
        if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return

        val plainText = "kiosk-secret-for-dpapi-test"
        val protector = WindowsDpapiSecretProtector()

        val encrypted = protector.protect(plainText)

        assertTrue(encrypted.startsWith("dpapi:v1:"))
        assertNotEquals(plainText, encrypted)
        assertFalse(encrypted.contains(plainText))
        assertEquals(plainText, protector.unprotect(encrypted))
    }
}
