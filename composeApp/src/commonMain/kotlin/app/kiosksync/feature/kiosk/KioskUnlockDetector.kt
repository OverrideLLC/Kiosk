package app.kiosksync.feature.kiosk

import app.kiosksync.core.UNIVERSAL_RECOVERY_PASSWORD

enum class KioskKeyKind {
    SHIFT,
    CTRL,
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
    data object Accepted : KioskUnlockResult
    data object UniversalAccepted : KioskUnlockResult
    data object Rejected : KioskUnlockResult
}

/**
 * Pure unlock state machine: Shift, Shift, Ctrl, then the password.
 *
 * The universal recovery phrase can always be typed after the same chord.
 * It has no access to Android/Compose types, so it is fully unit tested.
 */
class KioskUnlockDetector(
    private val passwordHash: String?,
    private val passwordLength: Int,
    private val passwordHasher: (String) -> String
) {
    private var chordStage = 0
    private val typed = StringBuilder()

    val typedLength: Int
        get() = typed.length

    fun reset() {
        chordStage = 0
        typed.clear()
    }

    fun feed(event: KioskKeyEvent): KioskUnlockResult {
        return when (chordStage) {
            0 -> if (event.kind == KioskKeyKind.SHIFT) {
                chordStage = 1
                KioskUnlockResult.ChordProgress
            } else {
                KioskUnlockResult.Ignored
            }

            1 -> when (event.kind) {
                KioskKeyKind.SHIFT -> {
                    chordStage = 2
                    KioskUnlockResult.ChordProgress
                }

                else -> {
                    reset()
                    KioskUnlockResult.Ignored
                }
            }

            2 -> when (event.kind) {
                KioskKeyKind.CTRL -> {
                    chordStage = 3
                    KioskUnlockResult.ChordProgress
                }

                // A third Shift is harmless; keep waiting for Ctrl.
                KioskKeyKind.SHIFT -> KioskUnlockResult.ChordProgress

                else -> {
                    reset()
                    KioskUnlockResult.Ignored
                }
            }

            else -> handleTyping(event)
        }
    }

    private fun handleTyping(event: KioskKeyEvent): KioskUnlockResult {
        when (event.kind) {
            KioskKeyKind.SHIFT, KioskKeyKind.CTRL -> return KioskUnlockResult.Typing

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
