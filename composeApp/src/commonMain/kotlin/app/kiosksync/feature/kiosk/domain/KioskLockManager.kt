package app.kiosksync.feature.kiosk.domain

import app.kiosksync.core.AppLogger
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.MAX_UNLOCK_ATTEMPTS
import app.kiosksync.core.UNLOCK_LOCKOUT_SECONDS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KioskLockState(
    val isLocked: Boolean = false,
    val hasPassword: Boolean = false,
    val showUnlockHint: Boolean = false,
    val typedLength: Int = 0,
    val feedback: KioskLockFeedback? = null,
    val failedAttempts: Int = 0,
    val lockoutRemainingSec: Int = 0
)

sealed interface KioskLockFeedback {
    data class IncorrectPassword(val attemptsRemaining: Int) : KioskLockFeedback
    data object LockedOut : KioskLockFeedback
}

/**
 * Owns the kiosk lock state and the unlock attempts. The platform keyboard
 * guard feeds key events while the device is locked.
 */
class KioskLockManager(
    private val logger: AppLogger,
    private val passwordHasher: (String) -> String,
    private val scope: CoroutineScope,
    val platformLockAvailable: Boolean = true
) {
    private val _state = MutableStateFlow(KioskLockState())
    val state: StateFlow<KioskLockState> = _state.asStateFlow()

    private var config: KioskLockConfig? = null
    private var detector: KioskUnlockDetector? = null
    private var feedbackJob: Job? = null
    private var hintJob: Job? = null
    private var lockoutJob: Job? = null

    fun setConfig(newConfig: KioskLockConfig?) {
        config = newConfig?.takeIf { it.enabled }
        _state.update { it.copy(hasPassword = hasPassword()) }
        if (_state.value.isLocked) {
            detector = newDetector()
            _state.update {
                it.copy(typedLength = 0)
            }
        }
    }

    fun lock(force: Boolean = false) {
        if (_state.value.isLocked) return
        if (!platformLockAvailable && !force) return
        detector = newDetector()
        _state.update {
            it.copy(
                isLocked = true,
                hasPassword = hasPassword(),
                showUnlockHint = false,
                typedLength = 0,
                feedback = null,
                failedAttempts = 0,
                lockoutRemainingSec = 0
            )
        }
        logger.info(
            "Kiosk lock engaged (password configured: ${hasPassword()})"
        )
    }

    /** Releases the lock without a password (role change or shutdown). */
    fun release() {
        cancelJobs()
        detector = null
        _state.update {
            KioskLockState(hasPassword = hasPassword())
        }
    }

    fun onKey(event: KioskKeyEvent) {
        val state = _state.value
        if (!state.isLocked || state.lockoutRemainingSec > 0) return

        val activeDetector = detector ?: newDetector().also { detector = it }
        val result = activeDetector.feed(event)

        when (result) {
            KioskUnlockResult.Accepted ->
                unlock("Kiosk unlocked with the configured password")

            KioskUnlockResult.SequenceAccepted ->
                unlock("Kiosk unlocked with the configured sequence (no password)")

            KioskUnlockResult.UniversalAccepted ->
                unlock("Kiosk unlocked with the recovery phrase")

            KioskUnlockResult.Rejected -> registerFailure()

            KioskUnlockResult.ChordProgress, KioskUnlockResult.Typing -> {
                scheduleHintHide()
                _state.update {
                    it.copy(
                        showUnlockHint = true,
                        typedLength = activeDetector.typedLength,
                        feedback = null
                    )
                }
            }

            // Any unrelated key (Esc, arrows, letters before the chord...) must
            // do absolutely nothing while locked. The unlock card only appears
            // once the Shift+Shift+Ctrl sequence starts. If the card is already
            // visible, keep the typed-length indicator in sync with the detector.
            KioskUnlockResult.Ignored -> {
                if (_state.value.showUnlockHint) {
                    _state.update {
                        it.copy(typedLength = activeDetector.typedLength)
                    }
                }
            }
        }
    }

    private fun unlock(logMessage: String) {
        cancelJobs()
        detector = null
        _state.update {
            it.copy(
                isLocked = false,
                showUnlockHint = false,
                typedLength = 0,
                feedback = null,
                failedAttempts = 0,
                lockoutRemainingSec = 0
            )
        }
        logger.info(logMessage)
    }

    private fun registerFailure() {
        val failed = _state.value.failedAttempts + 1
        detector?.reset()

        if (failed >= MAX_UNLOCK_ATTEMPTS) {
            startLockout()
            return
        }

        _state.update {
            it.copy(
                showUnlockHint = true,
                typedLength = 0,
                failedAttempts = failed,
                feedback = KioskLockFeedback.IncorrectPassword(
                    attemptsRemaining = MAX_UNLOCK_ATTEMPTS - failed,
                )
            )
        }
        logger.warn("Failed kiosk unlock attempt $failed/$MAX_UNLOCK_ATTEMPTS")
        scheduleFeedbackClear()
    }

    private fun startLockout() {
        lockoutJob?.cancel()
        _state.update {
            it.copy(
                showUnlockHint = true,
                typedLength = 0,
                failedAttempts = MAX_UNLOCK_ATTEMPTS,
                feedback = KioskLockFeedback.LockedOut,
                lockoutRemainingSec = UNLOCK_LOCKOUT_SECONDS
            )
        }
        logger.warn("Kiosk unlock locked out for $UNLOCK_LOCKOUT_SECONDS seconds")
        lockoutJob = scope.launch {
            var remaining = UNLOCK_LOCKOUT_SECONDS
            while (remaining > 0) {
                _state.update { it.copy(lockoutRemainingSec = remaining) }
                delay(1000)
                remaining--
            }
            detector?.reset()
            _state.update {
                it.copy(
                    lockoutRemainingSec = 0,
                    failedAttempts = 0,
                    feedback = null,
                    showUnlockHint = false,
                    typedLength = 0,
                )
            }
        }
    }

    private fun scheduleFeedbackClear() {
        feedbackJob?.cancel()
        feedbackJob = scope.launch {
            delay(4000)
            _state.update {
                if (it.lockoutRemainingSec > 0) it else it.copy(feedback = null)
            }
        }
    }

    private fun scheduleHintHide() {
        hintJob?.cancel()
        hintJob = scope.launch {
            delay(6000)
            _state.update {
                if (it.lockoutRemainingSec > 0) {
                    it
                } else {
                    it.copy(showUnlockHint = false, typedLength = 0)
                }
            }
        }
    }

    private fun cancelJobs() {
        feedbackJob?.cancel()
        feedbackJob = null
        hintJob?.cancel()
        hintJob = null
        lockoutJob?.cancel()
        lockoutJob = null
    }

    private fun newDetector(): KioskUnlockDetector = KioskUnlockDetector(
        passwordHash = config?.passwordHash,
        passwordLength = config?.passwordLength ?: 0,
        passwordHasher = passwordHasher,
        sequence = unlockSequenceKinds(config),
        recoveryHash = config?.recoveryHash,
        recoveryLength = config?.recoveryLength ?: 0
    )

    private fun hasPassword(): Boolean = config?.passwordHash != null
}
