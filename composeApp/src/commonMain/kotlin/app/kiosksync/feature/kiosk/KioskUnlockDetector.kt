package app.kiosksync.feature.kiosk

import app.kiosksync.core.UNIVERSAL_RECOVERY_PASSWORD

enum class KioskKeyKind {
    SHIFT,
    CTRL,
    ALT,
    CHARACTER,
    ENTER,
    BACKSPACE,
    OTHER
}

data class KioskKeyEvent(
    val kind: KioskKeyKind,
    val char: Char? = null
)

sealed interface KioskUnlockResult {
    data object Ignored : KioskUnlockResult
    data object ChordProgress : KioskUnlockResult
    data object Typing : KioskUnlockResult

    /** The configured password was typed correctly. */
    data object Accepted : KioskUnlockResult

    /** The default sequence unlocked a screen that has no password. */
    data object SequenceAccepted : KioskUnlockResult

    /** The universal recovery phrase unlocked the device. */
    data object UniversalAccepted : KioskUnlockResult

    data object Rejected : KioskUnlockResult
}

/**
 * The default unlock sequence: Shift, Shift, Ctrl, Ctrl, Alt, Shift.
 *
 * It is compiled into every build, is intentionally never shown on a locked
 * screen, and only appears in the project documentation and in the controller's
 * "Seguridad" dialog. When a screen has no password configured, completing this
 * sequence unlocks it. When a password is configured, the sequence is followed
 * by the password.
 */
val UNLOCK_SEQUENCE: List<KioskKeyKind> = listOf(
    KioskKeyKind.SHIFT,
    KioskKeyKind.SHIFT,
    KioskKeyKind.CTRL,
    KioskKeyKind.CTRL,
    KioskKeyKind.ALT,
    KioskKeyKind.SHIFT
)

/**
 * Pure unlock state machine. It first reads the default key sequence and, when
 * a password is configured, the password typed after it. The universal recovery
 * phrase can always be typed after the same sequence.
 *
 * It has no access to Android/Compose types, so it is fully unit tested.
 */
class KioskUnlockDetector(
    private val passwordHash: String?,
    private val passwordLength: Int,
    private val passwordHasher: (String) -> String,
    private val sequence: List<KioskKeyKind> = UNLOCK_SEQUENCE
) {
    private var stage = 0
    private val typed = StringBuilder()

    val typedLength: Int
        get() = typed.length

    /**
     * How many keys of the default sequence have been matched so far. The UI
     * never reveals it; it only drives the internal state.
     */
    val chordStage: Int
        get() = stage

    val isSequenceComplete: Boolean
        get() = stage >= sequence.size

    fun reset() {
        stage = 0
        typed.clear()
    }

    fun feed(event: KioskKeyEvent): KioskUnlockResult {
        if (stage < sequence.size) {
            return feedSequence(event)
        }
        return handleTyping(event)
    }

    private fun feedSequence(event: KioskKeyEvent): KioskUnlockResult {
        if (event.kind == sequence[stage]) {
            stage++
            if (stage >= sequence.size && passwordHash == null) {
                // No password configured: the default sequence alone unlocks.
                reset()
                return KioskUnlockResult.SequenceAccepted
            }
            return KioskUnlockResult.ChordProgress
        }

        // A repeat of the key that was just accepted is harmless (auto-repeat).
        if (stage > 0 && event.kind == sequence[stage - 1]) {
            return KioskUnlockResult.ChordProgress
        }

        reset()
        return KioskUnlockResult.Ignored
    }

    private fun handleTyping(event: KioskKeyEvent): KioskUnlockResult {
        when (event.kind) {
            KioskKeyKind.SHIFT, KioskKeyKind.CTRL, KioskKeyKind.ALT -> return KioskUnlockResult.Typing

            KioskKeyKind.BACKSPACE -> {
                if (typed.isNotEmpty()) typed.deleteAt(typed.length - 1)
                return KioskUnlockResult.Typing
            }

            KioskKeyKind.ENTER -> return verify()

            KioskKeyKind.CHARACTER -> {
                val char = event.char ?: return KioskUnlockResult.Ignored
                if (char.code < 32) return KioskUnlockResult.Ignored

                typed.append(char)
                val candidate = typed.toString()

                if (
                    candidate.length == UNIVERSAL_RECOVERY_PASSWORD.length &&
                    candidate == UNIVERSAL_RECOVERY_PASSWORD
                ) {
                    reset()
                    return KioskUnlockResult.UniversalAccepted
                }

                val expectedLength = passwordLength.takeIf { passwordHash != null && it > 0 }
                if (expectedLength != null && candidate.length == expectedLength) {
                    if (hashesMatch(candidate)) {
                        reset()
                        return KioskUnlockResult.Accepted
                    }
                }

                val customCanStillMatch =
                    expectedLength != null && candidate.length < expectedLength
                val universalCanStillMatch =
                    candidate.length < UNIVERSAL_RECOVERY_PASSWORD.length &&
                        UNIVERSAL_RECOVERY_PASSWORD.startsWith(candidate)

                if (!customCanStillMatch && !universalCanStillMatch) {
                    reset()
                    return KioskUnlockResult.Rejected
                }
                return KioskUnlockResult.Typing
            }

            KioskKeyKind.OTHER -> return KioskUnlockResult.Ignored
        }
    }

    private fun verify(): KioskUnlockResult {
        if (typed.isEmpty()) return KioskUnlockResult.Ignored
        val candidate = typed.toString()
        reset()
        return when {
            candidate == UNIVERSAL_RECOVERY_PASSWORD -> KioskUnlockResult.UniversalAccepted
            passwordHash != null && candidate.length == passwordLength && hashesMatch(candidate) ->
                KioskUnlockResult.Accepted

            else -> KioskUnlockResult.Rejected
        }
    }

    private fun hashesMatch(candidate: String): Boolean =
        passwordHash != null && passwordHasher(candidate).equals(passwordHash, ignoreCase = true)
}
