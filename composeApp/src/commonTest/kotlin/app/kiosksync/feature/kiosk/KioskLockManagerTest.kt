package app.kiosksync.feature.kiosk

import app.kiosksync.NoOpLogger
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.feature.kiosk.domain.KioskKeyEvent
import app.kiosksync.feature.kiosk.domain.KioskKeyKind
import app.kiosksync.feature.kiosk.domain.KioskLockFeedback
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.kiosk.domain.UNLOCK_SEQUENCE
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

    private fun KioskLockManager.pressSequence() {
        UNLOCK_SEQUENCE.forEach { onKey(KioskKeyEvent(it)) }
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

        // The card only appears once the Shift+Shift+Ctrl chord begins.
        manager.onKey(KioskKeyEvent(KioskKeyKind.SHIFT))
        assertTrue(manager.uiState.value.showUnlockHint)
    }

    @Test
    fun `unlock hint tracks the chord and the typed password`() {
        val manager = lockedManager()

        manager.pressSequence()
        assertTrue(manager.uiState.value.showUnlockHint)

        manager.onKey(KioskKeyEvent(KioskKeyKind.CHARACTER, 'a'))
        assertEquals(1, manager.uiState.value.typedLength)

        manager.onKey(KioskKeyEvent(KioskKeyKind.BACKSPACE))
        assertEquals(0, manager.uiState.value.typedLength)
    }

    @Test
    fun `the chord and the password unlock through the manager`() {
        val manager = lockedManager("abcd")

        manager.pressSequence()
        manager.onKey(KioskKeyEvent(KioskKeyKind.CHARACTER, 'a'))
        manager.onKey(KioskKeyEvent(KioskKeyKind.CHARACTER, 'b'))
        manager.onKey(KioskKeyEvent(KioskKeyKind.CHARACTER, 'c'))
        manager.onKey(KioskKeyEvent(KioskKeyKind.CHARACTER, 'd'))

        assertFalse(manager.uiState.value.isLocked)
    }

    @Test
    fun `the default sequence unlocks a locked screen with no password`() {
        val manager = KioskLockManager(
            logger = NoOpLogger(),
            passwordHasher = hasher,
            scope = scope,
            platformLockAvailable = false
        )
        manager.lock(force = true)
        assertTrue(manager.uiState.value.isLocked)

        manager.pressSequence()

        assertFalse(manager.uiState.value.isLocked)
    }

    @Test
    fun `incorrect password exposes typed feedback with attempts remaining`() {
        val manager = lockedManager("abcd")
        manager.pressSequence()
        "wxyz".forEach { character ->
            manager.onKey(KioskKeyEvent(KioskKeyKind.CHARACTER, character))
        }

        assertEquals(
            KioskLockFeedback.IncorrectPassword(attemptsRemaining = 4),
            manager.uiState.value.feedback,
        )
    }

    @Test
    fun `lockout exposes typed feedback while its countdown runs`() {
        val manager = lockedManager("abcd")

        repeat(5) {
            manager.pressSequence()
            "wxyz".forEach { character ->
                manager.onKey(KioskKeyEvent(KioskKeyKind.CHARACTER, character))
            }
        }

        assertEquals(KioskLockFeedback.LockedOut, manager.uiState.value.feedback)
        assertTrue(manager.uiState.value.lockoutRemainingSec > 0)
    }
}
