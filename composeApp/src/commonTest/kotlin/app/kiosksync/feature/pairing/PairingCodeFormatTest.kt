package app.kiosksync.feature.pairing

import app.kiosksync.feature.pairing.presentation.formatCountdown
import app.kiosksync.feature.pairing.presentation.formatPairingCode
import kotlin.test.Test
import kotlin.test.assertEquals

class PairingCodeFormatTest {

    @Test
    fun groupsSixDigitsInTwoBlocks() {
        assertEquals("685 853", formatPairingCode("685853"))
    }

    @Test
    fun keepsShortCodesReadable() {
        assertEquals("12", formatPairingCode("12"))
        assertEquals("123", formatPairingCode("123"))
    }

    @Test
    fun ignoresNonDigits() {
        assertEquals("123 456", formatPairingCode("123-456"))
    }

    @Test
    fun formatsCountdown() {
        assertEquals("00:00", formatCountdown(0))
        assertEquals("04:59", formatCountdown(299))
        assertEquals("05:00", formatCountdown(300))
        assertEquals("00:00", formatCountdown(-3))
    }
}
