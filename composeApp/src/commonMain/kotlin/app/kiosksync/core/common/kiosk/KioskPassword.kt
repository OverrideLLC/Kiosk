package app.kiosksync.core.common.kiosk

/** Universal recovery phrase. It is compiled into every build by design. */
const val UNIVERSAL_RECOVERY_PASSWORD = "0137GenericPassword"

const val MIN_KIOSK_PASSWORD_LENGTH = 4
const val MAX_KIOSK_PASSWORD_LENGTH = 64
const val MAX_UNLOCK_ATTEMPTS = 5
const val UNLOCK_LOCKOUT_SECONDS = 30

const val MIN_RECOVERY_PHRASE_LENGTH = 8
const val MAX_RECOVERY_PHRASE_LENGTH = 64

/** Passwords stay ASCII so the Windows hook can map key codes without a layout. */
fun isValidKioskPassword(value: String): Boolean =
    value.length in MIN_KIOSK_PASSWORD_LENGTH..MAX_KIOSK_PASSWORD_LENGTH && value.isAsciiAlphanumeric()

/** Custom recovery phrases use the same character set as passwords. */
fun isValidRecoveryPhrase(value: String): Boolean =
    value.length in MIN_RECOVERY_PHRASE_LENGTH..MAX_RECOVERY_PHRASE_LENGTH && value.isAsciiAlphanumeric()

private fun String.isAsciiAlphanumeric(): Boolean = all { char ->
    char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9'
}
