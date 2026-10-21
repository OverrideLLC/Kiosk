package app.kiosksync.core

import kotlinx.serialization.Serializable

/**
 * Lock configuration shared by the controller and every screen.
 *
 * Only a SHA-256 hash of the password travels over the LAN; the plain password
 * never leaves the controller. [passwordLength] is stored so the unlock
 * detector knows when the typed password is complete.
 */
@Serializable
data class KioskLockConfig(
    val passwordHash: String,
    val passwordLength: Int,
    val enabled: Boolean = true
)

/** Universal recovery phrase. It is compiled into every build by design. */
const val UNIVERSAL_RECOVERY_PASSWORD = "0137GenericPassword"

const val MIN_KIOSK_PASSWORD_LENGTH = 4
const val MAX_KIOSK_PASSWORD_LENGTH = 64
const val MAX_UNLOCK_ATTEMPTS = 5
const val UNLOCK_LOCKOUT_SECONDS = 30

/**
 * Passwords are restricted to ASCII letters and digits so the low-level
 * Windows hook can map key codes without depending on the keyboard layout.
 */
fun isValidKioskPassword(value: String): Boolean {
    if (value.length !in MIN_KIOSK_PASSWORD_LENGTH..MAX_KIOSK_PASSWORD_LENGTH) return false
    return value.all { char ->
        char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9'
    }
}
