package app.kiosksync.feature.kiosk

import app.kiosksync.core.UNIVERSAL_RECOVERY_PASSWORD
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KioskUnlockDetectorTest {

    private val hasher: (String) -> String = { input -> "hash:$input" }

    private fun detector(password: String = "abcd"): KioskUnlockDetector =
        KioskUnlockDetector(
            passwordHash = hasher(password),
            passwordLength = password.length,
            passwordHasher = hasher
        )

    private fun KioskUnlockDetector.pressChord() {
        feed(KioskKeyEvent(KioskKeyKind.SHIFT))
        feed(KioskKeyEvent(KioskKeyKind.SHIFT))
        feed(KioskKeyEvent(KioskKeyKind.CTRL))
    }

    private fun KioskUnlockDetector.type(text: String): KioskUnlockResult {
        var last: KioskUnlockResult = KioskUnlockResult.Ignored
        text.forEach { char ->
            last = feed(KioskKeyEvent(KioskKeyKind.CHARACTER, char))
        }
        return last
    }

    @Test
    fun `chord followed by the configured password unlocks`() {
        val detector = detector()
        detector.pressChord()
        val result = detector.type("abcd")
        assertEquals(KioskUnlockResult.Accepted, result)
    }

    @Test
    fun `wrong password is rejected after the chord`() {
        val detector = detector()
        detector.pressChord()
        val result = detector.type("abce")
        assertEquals(KioskUnlockResult.Rejected, result)
    }

    @Test
    fun `universal recovery phrase always unlocks`() {
        val detector = detector("somethingElse")
        detector.pressChord()
        val result = detector.type(UNIVERSAL_RECOVERY_PASSWORD)
        assertEquals(KioskUnlockResult.UniversalAccepted, result)
    }

    @Test
    fun `universal phrase works even when the custom password is shorter`() {
        val detector = detector("ab")
        detector.pressChord()
        val result = detector.type(UNIVERSAL_RECOVERY_PASSWORD)
        assertEquals(KioskUnlockResult.UniversalAccepted, result)
    }

    @Test
    fun `typing before the chord completes does not unlock`() {
        val detector = detector()
        detector.feed(KioskKeyEvent(KioskKeyKind.SHIFT))
        detector.feed(KioskKeyEvent(KioskKeyKind.CHARACTER, 'a'))
        detector.feed(KioskKeyEvent(KioskKeyKind.SHIFT))
        detector.feed(KioskKeyEvent(KioskKeyKind.CTRL))
        val result = detector.type("bcd")
        assertFalse(result == KioskUnlockResult.Accepted)
    }

    @Test
    fun `backspace removes the last typed character`() {
        val detector = detector("abcd")
        detector.pressChord()
        detector.type("abc")
        detector.feed(KioskKeyEvent(KioskKeyKind.BACKSPACE))
        val result = detector.type("cd")
        assertEquals(KioskUnlockResult.Accepted, result)
    }

    @Test
    fun `enter verifies a password of unexpected length`() {
        val detector = detector("abcd")
        detector.pressChord()
        detector.type("abcd")
        // Already accepted at the expected length; a fresh detector with Enter
        // covers the manual confirmation path.
        val second = detector("abcd")
        second.pressChord()
        second.type("ab")
        val result = second.feed(KioskKeyEvent(KioskKeyKind.ENTER))
        assertEquals(KioskUnlockResult.Rejected, result)
    }

    @Test
    fun `password validation restricts to letters and digits`() {
        assertTrue(app.kiosksync.core.isValidKioskPassword("Anuncios2026"))
        assertFalse(app.kiosksync.core.isValidKioskPassword("abc"))
        assertFalse(app.kiosksync.core.isValidKioskPassword("abc def"))
        assertFalse(app.kiosksync.core.isValidKioskPassword("abc!"))
    }
}
