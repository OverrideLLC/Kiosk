package app.kiosksync.feature.pairing.presentation

/**
 * Presentation helpers for the pairing code. Pure so they can be tested
 * without a display.
 */
fun formatPairingCode(code: String): String =
    code.filter { it.isDigit() }.chunked(3).joinToString(" ")

fun formatCountdown(totalSeconds: Long): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
