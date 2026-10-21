package app.kiosksync.feature.kiosk

import app.kiosksync.core.AppLogger
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.MAX_UNLOCK_ATTEMPTS
import app.kiosksync.core.Strings
import app.kiosksync.core.UNLOCK_LOCKOUT_SECONDS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KioskLockUiState(
    val isLocked: Boolean = false,
    val hasPassword: Boolean = false,
    val showUnlockHint: Boolean = false,
    val typedLength: Int = 0,
    val chordStage: Int = 0,
    val lastKey: String? = null,
    val message: String? = null,
    val failedAttempts: Int = 0,
    val lockoutRemainingSec: Int = 0
)

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
    private val _uiState = MutableStateFlow(KioskLockUiState())
    val uiState: StateFlow<KioskLockUiState> = _uiState.asStateFlow()

    private var config: KioskLockConfig? = null
    private var detector: KioskUnlockDetector? = null
    private var messageJob: Job? = null
    private var hintJob: Job? = null
    private var lockoutJob: Job? = null

    fun setConfig(newConfig: KioskLockConfig?) {
        config = newConfig?.takeIf { it.enabled }
        _uiState.update { it.copy(hasPassword = hasPassword()) }
        if (_uiState.value.isLocked) {
            detector = newDetector()
            _uiState.update {
                it.copy(typedLength = 0, chordStage = 0, lastKey = null)
            }
        }
    }

    fun lock(force: Boolean = false) {
        if (_uiState.value.isLocked) return
        if (!platformLockAvailable && !force) return
        detector = newDetector()
        _uiState.update {
            it.copy(
                isLocked = true,
                hasPassword = hasPassword(),
                showUnlockHint = false,
                typedLength = 0,
                chordStage = 0,
                lastKey = null,
                message = null,
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
        _uiState.update {
            KioskLockUiState(hasPassword = hasPassword())
        }
    }

    fun onKey(event: KioskKeyEvent) {
        val state = _uiState.value
        if (!state.isLocked || state.lockoutRemainingSec > 0) return

        val activeDetector = detector ?: newDetector().also { detector = it }
        val result = activeDetector.feed(event)
        val lastKey = keyLabel(event)

        when (result) {
            KioskUnlockResult.Accepted ->
                unlock("Kiosk unlocked with the configured password")

            KioskUnlockResult.SequenceAccepted ->
                unlock("Kiosk unlocked with the configured sequence (no password)")

            KioskUnlockResult.UniversalAccepted ->
                unlock("Kiosk unlocked with the recovery phrase")

            KioskUnlockResult.Rejected -> registerFailure(lastKey)

            KioskUnlockResult.ChordProgress, KioskUnlockResult.Typing -> {
                scheduleHintHide()
                _uiState.update {
                    it.copy(
                        showUnlockHint = true,
                        chordStage = activeDetector.chordStage,
                        typedLength = activeDetector.typedLength,
                        lastKey = lastKey ?: it.lastKey,
                        message = null
                    )
                }
            }

            // Any unrelated key (Esc, arrows, letters before the chord...) must
            // do absolutely nothing while locked. The unlock card only appears
            // once the Shift+Shift+Ctrl sequence starts. If the card is already
            // visible, keep the chord progress in sync with the detector.
            KioskUnlockResult.Ignored -> {
                if (_uiState.value.showUnlockHint) {
                    _uiState.update {
                        it.copy(
                            chordStage = activeDetector.chordStage,
                            typedLength = activeDetector.typedLength
                        )
                    }
                }
            }
        }
    }

    private fun keyLabel(event: KioskKeyEvent): String? = when (event.kind) {
        KioskKeyKind.SHIFT -> "Shift"
        KioskKeyKind.CTRL -> "Ctrl"
        KioskKeyKind.ALT -> "Alt"
        KioskKeyKind.ENTER -> "Enter"
        KioskKeyKind.BACKSPACE -> Strings.KIOSK_KEY_BACKSPACE
        KioskKeyKind.CHARACTER -> Strings.KIOSK_KEY_CHAR
        KioskKeyKind.OTHER -> null
    }

    private fun unlock(logMessage: String) {
        cancelJobs()
        detector = null
        _uiState.update {
            it.copy(
                isLocked = false,
                showUnlockHint = false,
                typedLength = 0,
                chordStage = 0,
                lastKey = null,
                message = null,
                failedAttempts = 0,
                lockoutRemainingSec = 0
            )
        }
        logger.info(logMessage)
    }

    private fun registerFailure(lastKey: String?) {
        val failed = _uiState.value.failedAttempts + 1
        detector?.reset()

        if (failed >= MAX_UNLOCK_ATTEMPTS) {
            startLockout()
            return
        }

        _uiState.update {
            it.copy(
                showUnlockHint = true,
                typedLength = 0,
                chordStage = 0,
                lastKey = lastKey ?: it.lastKey,
                failedAttempts = failed,
                message = "${Strings.KIOSK_WRONG_PASSWORD} " +
                    "${Strings.KIOSK_ATTEMPTS_LEFT} ${MAX_UNLOCK_ATTEMPTS - failed}"
            )
        }
        logger.warn("Failed kiosk unlock attempt $failed/$MAX_UNLOCK_ATTEMPTS")
        scheduleMessageClear()
    }

    private fun startLockout() {
        lockoutJob?.cancel()
        _uiState.update {
            it.copy(
                showUnlockHint = true,
                typedLength = 0,
                chordStage = 0,
                lastKey = null,
                failedAttempts = MAX_UNLOCK_ATTEMPTS,
                message = Strings.KIOSK_LOCKED_OUT,
                lockoutRemainingSec = UNLOCK_LOCKOUT_SECONDS
            )
        }
        logger.warn("Kiosk unlock locked out for $UNLOCK_LOCKOUT_SECONDS seconds")
        lockoutJob = scope.launch {
            var remaining = UNLOCK_LOCKOUT_SECONDS
            while (remaining > 0) {
                _uiState.update { it.copy(lockoutRemainingSec = remaining) }
                delay(1000)
                remaining--
            }
            detector?.reset()
            _uiState.update {
                it.copy(
                    lockoutRemainingSec = 0,
                    failedAttempts = 0,
                    message = null,
                    showUnlockHint = false,
                    typedLength = 0,
                    chordStage = 0,
                    lastKey = null
                )
            }
        }
    }

    private fun scheduleMessageClear() {
        messageJob?.cancel()
        messageJob = scope.launch {
            delay(4000)
            _uiState.update {
                if (it.lockoutRemainingSec > 0) it else it.copy(message = null)
            }
        }
    }

    private fun scheduleHintHide() {
        hintJob?.cancel()
        hintJob = scope.launch {
            delay(6000)
            _uiState.update {
                if (it.lockoutRemainingSec > 0) {
                    it
                } else {
                    it.copy(showUnlockHint = false, typedLength = 0)
                }
            }
        }
    }

    private fun cancelJobs() {
        messageJob?.cancel()
        messageJob = null
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
