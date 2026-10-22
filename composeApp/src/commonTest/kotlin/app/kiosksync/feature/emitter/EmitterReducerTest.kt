package app.kiosksync.feature.emitter

import app.kiosksync.core.MAX_RECOVERY_PHRASE_LENGTH
import app.kiosksync.feature.emitter.presentation.EmitterAction
import app.kiosksync.feature.emitter.presentation.EmitterDeviceUi
import app.kiosksync.feature.emitter.presentation.EmitterReducer
import app.kiosksync.feature.emitter.presentation.EmitterUiState
import app.kiosksync.feature.emitter.presentation.SecurityDialogUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmitterReducerTest {
    @Test
    fun pairingDialogTransitionsArePureAndSanitizeTheCode() {
        val device = EmitterDeviceUi(
            deviceId = "screen-1",
            deviceName = "Pantalla",
            host = "192.168.1.2",
            port = 8765,
            isPairedToThisEmitter = false,
            isPairedToAnotherEmitter = false,
            isOnline = true,
            isSelected = false
        )
        val opened = EmitterReducer.reduce(EmitterUiState(), EmitterAction.OpenPairDialog(device))
        assertEquals(device, opened.pairingDevice)

        val edited = EmitterReducer.reduce(opened, EmitterAction.UpdatePairingCode("12a34567"))
        assertEquals("123456", edited.pairingCode)
        assertNull(edited.pairingError)

        val dismissed = EmitterReducer.reduce(edited, EmitterAction.DismissPairDialog)
        assertNull(dismissed.pairingDevice)
        assertEquals("", dismissed.pairingCode)
    }

    @Test
    fun manualAddressAndStatusTransitionsClearStaleErrors() {
        val failed = EmitterUiState(
            showManualIpDialog = true,
            manualIpError = "bad address",
            statusMessage = "failed",
            statusIsError = true
        )
        val edited = EmitterReducer.reduce(failed, EmitterAction.UpdateManualIp("127.0.0.1:8765"))
        assertEquals("127.0.0.1:8765", edited.manualIpInput)
        assertNull(edited.manualIpError)

        val dismissed = EmitterReducer.reduce(edited, EmitterAction.DismissStatusMessage)
        assertNull(dismissed.statusMessage)
        assertFalse(dismissed.statusIsError)
        assertTrue(dismissed.showManualIpDialog)
    }

    @Test
    fun securityFormActionsAreReducedWithoutSideEffects() {
        val initial = EmitterUiState(securityDialog = SecurityDialogUi(sequence = listOf("CTRL")))
        val appended = EmitterReducer.reduce(initial, EmitterAction.AppendUnlockKey("SHIFT"))
        assertEquals(listOf("CTRL", "SHIFT"), appended.securityDialog?.sequence)

        val accepted = EmitterReducer.reduce(appended, EmitterAction.SetRecoveryAccepted(true))
        val edited = EmitterReducer.reduce(
            accepted,
            EmitterAction.UpdateRecoveryDraft("a".repeat(MAX_RECOVERY_PHRASE_LENGTH + 10))
        )
        assertTrue(edited.securityDialog?.recoveryAccepted == true)
        assertEquals(MAX_RECOVERY_PHRASE_LENGTH, edited.securityDialog.recoveryDraft.length)

        val reset = EmitterReducer.reduce(edited, EmitterAction.ResetUnlockSequence)
        assertEquals(SecurityDialogUi().sequence, reset.securityDialog?.sequence)
    }
}
