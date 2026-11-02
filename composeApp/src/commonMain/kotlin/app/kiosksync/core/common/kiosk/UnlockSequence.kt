package app.kiosksync.core.common.kiosk

/** Shift, Shift, Ctrl, Ctrl, Alt, Shift. Modifier keys only. */
val DEFAULT_UNLOCK_SEQUENCE: List<String> = listOf("SHIFT", "SHIFT", "CTRL", "CTRL", "ALT", "SHIFT")

val ALLOWED_UNLOCK_KEYS: Set<String> = setOf("SHIFT", "CTRL", "ALT")

const val MIN_UNLOCK_SEQUENCE_LENGTH = 2
const val MAX_UNLOCK_SEQUENCE_LENGTH = 12

private val UNLOCK_KEY_LABELS = mapOf("SHIFT" to "Shift", "CTRL" to "Ctrl", "ALT" to "Alt")

fun isValidUnlockSequence(sequence: List<String>): Boolean =
    sequence.size in MIN_UNLOCK_SEQUENCE_LENGTH..MAX_UNLOCK_SEQUENCE_LENGTH &&
        sequence.all { it in ALLOWED_UNLOCK_KEYS }

/** Human readable form, for example "Shift + Shift + Ctrl". */
fun formatUnlockSequence(sequence: List<String>): String =
    sequence.joinToString(" + ") { key -> UNLOCK_KEY_LABELS[key] ?: key }
