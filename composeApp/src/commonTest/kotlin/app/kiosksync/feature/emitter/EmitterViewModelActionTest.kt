package app.kiosksync.feature.emitter

import app.kiosksync.core.MAX_RECOVERY_PHRASE_LENGTH
import app.kiosksync.feature.emitter.presentation.EmitterAction
import app.kiosksync.feature.emitter.presentation.EmitterDeviceUi
import app.kiosksync.feature.emitter.presentation.SecurityDialogUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class EmitterViewModelActionTest {
    @Test
    fun pairingActionsUpdateStateAndSanitizeTheCode() = withViewModel { viewModel ->
        val device = EmitterDeviceUi(
            deviceId = "screen-1",
            deviceName = "Pantalla",
            host = "192.168.1.2",
            port = 8765,
            isPairedToThisEmitter = false,
            isPairedToAnotherEmitter = false,
            isOnline = true,
            isSelected = false,
        )

        viewModel.onAction(EmitterAction.OpenPairDialog(device))
        assertEquals(device, viewModel.state.value.pairingDevice)

        viewModel.onAction(EmitterAction.UpdatePairingCode("12a34567"))
        assertEquals("123456", viewModel.state.value.pairingCode)
        assertNull(viewModel.state.value.pairingError)

        viewModel.onAction(EmitterAction.DismissPairDialog)
        assertNull(viewModel.state.value.pairingDevice)
        assertEquals("", viewModel.state.value.pairingCode)
    }

    @Test
    fun manualAddressActionsOpenEditAndDismissDialog() = withViewModel { viewModel ->
        viewModel.onAction(EmitterAction.OpenManualIp)
        assertTrue(viewModel.state.value.showManualIpDialog)

        viewModel.onAction(EmitterAction.UpdateManualIp("127.0.0.1:8765"))
        assertEquals("127.0.0.1:8765", viewModel.state.value.manualIpInput)
        assertNull(viewModel.state.value.manualIpError)

        viewModel.onAction(EmitterAction.DismissManualIpDialog)
        assertFalse(viewModel.state.value.showManualIpDialog)
        assertEquals("", viewModel.state.value.manualIpInput)
    }

    @Test
    fun securityActionsUpdateFormStateThroughTheViewModel() = withViewModel { viewModel ->
        viewModel.onAction(EmitterAction.OpenSecurityDialog)
        assertNotNull(viewModel.state.value.securityDialog)

        viewModel.onAction(EmitterAction.AppendUnlockKey("SHIFT"))
        viewModel.onAction(EmitterAction.SetRecoveryAccepted(true))
        viewModel.onAction(
            EmitterAction.UpdateRecoveryDraft("a".repeat(MAX_RECOVERY_PHRASE_LENGTH + 10))
        )

        val edited = assertNotNull(viewModel.state.value.securityDialog)
        assertEquals(SecurityDialogUi().sequence + "SHIFT", edited.sequence)
        assertTrue(edited.recoveryAccepted)
        assertEquals(MAX_RECOVERY_PHRASE_LENGTH, edited.recoveryDraft.length)

        viewModel.onAction(EmitterAction.ResetUnlockSequence)
        assertEquals(SecurityDialogUi().sequence, viewModel.state.value.securityDialog?.sequence)
    }

    private fun withViewModel(block: (app.kiosksync.feature.emitter.presentation.EmitterViewModel) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = createEmitterViewModelForActionTests(scope)
        try {
            block(viewModel)
        } finally {
            viewModel.stop()
            scope.cancel()
        }
    }
}
