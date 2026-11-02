package app.kiosksync.core.common.kiosk

import kotlinx.serialization.Serializable

/**
 * Lock configuration shared by the controller and every screen.
 *
 * Only a SHA-256 hash of the password travels over the LAN. [passwordLength]
 * tells the unlock detector when the typed password is complete.
 * [unlockSequence] works with or without a password and uses modifier keys so
 * it does not depend on the keyboard layout. [recoveryHash] replaces the
 * compiled [UNIVERSAL_RECOVERY_PASSWORD] when it is set.
 */
@Serializable
data class KioskLockConfig(
    val passwordHash: String? = null,
    val passwordLength: Int = 0,
    val enabled: Boolean = true,
    val unlockSequence: List<String> = DEFAULT_UNLOCK_SEQUENCE,
    val recoveryHash: String? = null,
    val recoveryLength: Int = 0
)
