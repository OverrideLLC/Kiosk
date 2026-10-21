package app.kiosksync.feature.kiosk

import app.kiosksync.NoOpLogger
import app.kiosksync.core.KioskLockConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KioskLockManagerTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val hasher: (String) -> String = { input -> "hash:$input" }

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    private fun lockedManager(password: String = "abcdefgh"): KioskLockManager =
        KioskLockManager(
            logger = NoOpLogger(),
            passwordHasher = hasher,
            scope = scope,
            platformLockAvailable = false
        ).apply {
            setConfig(KioskLockConfig(hasher(password), password.length))
            lock(force = true)
        }

    @Test
    fun `force lock engages even when the platform hook is unavailable`() {
        val manager = KioskLockManager(
            logger = NoOpLogger(),
            passwordHasher = hasher,
            scope = scope,
            platformLockAvailable = false
        )

        manager.lock()
        assertFalse(manager.uiState.value.isLocked)

        manager.lock(force = true)
        assertTrue(manager.uiState.value.isLocked)
        // A freshly locked screen must not show anything until the user starts
        // the unlock sequence.
        assertFalse(manager.uiState.value.showUnlockHint)
    }

    @Test
    fun `unrelated keys do nothing while locked`() {
        val manager = lockedManager()

        manager.onKey(KioskKeyEvent(KioskKeyKind.OTHER))
        assertFalse(manager.uiState.value.showUnlockHint)
        assertEquals(0, manager.uiState.value.chordStage)

        // The card only appears once the Shift+Shift+Ctrl chord begins.
        manager.onKey(KioskKeyEvent(KioskKeyKind.SHIFT))
        assertTrue(manager.uiState.value.showUnlockHint)
        assertEquals(1, manager.uiState.value.chordStage)
    }

    @Test
    fun `key feedback tracks the chord and the typed password`() {
        val manager = lockedManager()

        manager.onKey(KioskKeyEvent(KioskKeyKind.SHIFT))
        assertEquals(1, manager.uiState.value.chordStage)
        manager.onKey(KioskKeyEvent(KioskKeyKind.SHIFT))
        assertEquals(2, manager.uiState.value.chordStage)
        manager.onKey(KioskKeyEvent(KioskKeyKind.CTRL))
        assertEquals(3, manager.uiState.value.chordStage)

        manager.onKey(KioskKeyEvent(KioskKeyKind.CHARACTER, 'a'))
        assertEquals(1, manager.uiState.value.typedLength)
        assertEquals("•", manager.uiState.value.lastKey)

        manager.onKey(KioskKeyEvent(KioskKeyKind.BACKSPACE))
        assertEquals(0, manager.uiState.value.typedLength)
    }

    @Test
    fun `the chord and the password unlock through the manager`() {
        val manager = lockedManager("abcd")

        manager.onKey(KioskKeyEvent(KioskKeyKind.SHIFT))
        manager.onKey(KioskKeyEvent(KioskKeyKind.SHIFT))
        manager.onKey(KioskKeyEvent(KioskKeyKind.CTRL))
        manager.onKey(KioskKeyEvent(KioskKeyKind.CHARACTER, 'a'))
        manager.onKey(KioskKeyEvent(KioskKeyKind.CHARACTER, 'b'))
        manager.onKey(KioskKeyEvent(KioskKeyKind.CHARACTER, 'c'))
        manager.onKey(KioskKeyEvent(KioskKeyKind.CHARACTER, 'd'))

        assertFalse(manager.uiState.value.isLocked)
    }
}
