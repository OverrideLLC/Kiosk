package app.kiosksync.core

import kotlinx.serialization.Serializable

/**
 * Lock configuration shared by the controller and every screen.
 *
 * Only a SHA-256 hash of the password travels over the LAN; the plain password
 * never leaves the controller. [passwordLength] is stored so the unlock
 * detector knows when the typed password is complete.
 *
 * [unlockSequence] is the default key sequence used to unlock the device. It is
 * configurable from the controller and applies both when a password is set and
 * when the device is blank (no password).
 *
 * [recoveryHash] is the SHA-256 hash of a custom "I forgot the password" phrase.
 * When it is null, the compiled [UNIVERSAL_RECOVERY_PASSWORD] is used instead.
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

/** Universal recovery phrase. It is compiled into every build by design. */
const val UNIVERSAL_RECOVERY_PASSWORD = "0137GenericPassword"

const val MIN_KIOSK_PASSWORD_LENGTH = 4
const val MAX_KIOSK_PASSWORD_LENGTH = 64
const val MAX_UNLOCK_ATTEMPTS = 5
const val UNLOCK_LOCKOUT_SECONDS = 30

/**
 * Default unlock sequence: Shift, Shift, Ctrl, Ctrl, Alt, Shift.
 * Only modifier keys are allowed so it works with any keyboard layout.
 */
val DEFAULT_UNLOCK_SEQUENCE: List<String> = listOf("SHIFT", "SHIFT", "CTRL", "CTRL", "ALT", "SHIFT")

val ALLOWED_UNLOCK_KEYS: Set<String> = setOf("SHIFT", "CTRL", "ALT")

const val MIN_UNLOCK_SEQUENCE_LENGTH = 2
const val MAX_UNLOCK_SEQUENCE_LENGTH = 12

const val MIN_RECOVERY_PHRASE_LENGTH = 8
const val MAX_RECOVERY_PHRASE_LENGTH = 64

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

/** Human readable form, e.g. "Shift + Shift + Ctrl". */
fun formatUnlockSequence(sequence: List<String>): String =
    sequence.joinToString(" + ") { key ->
        when (key) {
            "SHIFT" -> "Shift"
            "CTRL" -> "Ctrl"
            "ALT" -> "Alt"
            else -> key
        }
    }

fun isValidUnlockSequence(sequence: List<String>): Boolean {
    if (sequence.size !in MIN_UNLOCK_SEQUENCE_LENGTH..MAX_UNLOCK_SEQUENCE_LENGTH) return false
    return sequence.all { it in ALLOWED_UNLOCK_KEYS }
}

/** Custom recovery phrases use the same safe character set as passwords. */
fun isValidRecoveryPhrase(value: String): Boolean {
    if (value.length !in MIN_RECOVERY_PHRASE_LENGTH..MAX_RECOVERY_PHRASE_LENGTH) return false
    return value.all { char ->
        char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9'
    }
}
