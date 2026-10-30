package app.kiosksync.feature.kiosk.presentation

import app.kiosksync.feature.kiosk.domain.KioskLockFeedback
import app.kiosksync.feature.kiosk.domain.KioskLockState

data class KioskState(
    val isLocked: Boolean = false,
    val hasPassword: Boolean = false,
    val showUnlockHint: Boolean = false,
    val typedLength: Int = 0,
    val feedback: KioskLockFeedback? = null,
    val failedAttempts: Int = 0,
    val lockoutRemainingSec: Int = 0,
)

internal fun KioskLockState.toState() = KioskState(
    isLocked = isLocked,
    hasPassword = hasPassword,
    showUnlockHint = showUnlockHint,
    typedLength = typedLength,
    feedback = feedback,
    failedAttempts = failedAttempts,
    lockoutRemainingSec = lockoutRemainingSec,
)
