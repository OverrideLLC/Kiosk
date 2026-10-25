package app.kiosksync.feature.guide.presentation

import app.kiosksync.feature.guide.domain.GuideStep
import app.kiosksync.NoOpLogger
import app.kiosksync.core.AppRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuideViewModelTest {

    private fun viewModel() = GuideViewModel(NoOpLogger())

    @Test
    fun emitterGuideAdvancesWithRealState() {
        val guide = viewModel()

        guide.onAction(GuideAction.Start(AppRole.EMITTER))
        assertEquals(GuideStep.EMITTER_PLAYLIST, guide.state.value.step)

        guide.onAction(GuideAction.ReportEmitterProgress(1, 0, 0, false, false))
        assertEquals(GuideStep.EMITTER_SCREENS, guide.state.value.step)

        guide.onAction(GuideAction.ReportEmitterProgress(1, 1, 1, false, false))
        assertEquals(GuideStep.EMITTER_PASSWORD, guide.state.value.step)

        guide.onAction(GuideAction.ReportEmitterProgress(1, 1, 1, true, false))
        assertEquals(GuideStep.EMITTER_SEND, guide.state.value.step)

        guide.onAction(GuideAction.ReportEmitterProgress(1, 1, 1, true, true))
        assertEquals(GuideStep.EMITTER_DONE, guide.state.value.step)
    }

    @Test
    fun receiverGuideAdvancesWithPairingPasswordAndContent() {
        val guide = viewModel()

        guide.onAction(GuideAction.Start(AppRole.RECEIVER))
        assertEquals(GuideStep.RECEIVER_CODE, guide.state.value.step)

        guide.onAction(GuideAction.ReportReceiverProgress(true, false, false))
        assertEquals(GuideStep.RECEIVER_PASSWORD, guide.state.value.step)

        guide.onAction(GuideAction.ReportReceiverProgress(true, true, false))
        assertEquals(GuideStep.RECEIVER_CONTENT, guide.state.value.step)

        guide.onAction(GuideAction.ReportReceiverProgress(true, true, true))
        assertEquals(GuideStep.RECEIVER_DONE, guide.state.value.step)
    }

    @Test
    fun reopeningFromSettingsStartsAtTheFirstPendingStep() {
        val guide = viewModel()

        guide.onAction(GuideAction.ReportEmitterProgress(2, 1, 1, true, false))
        guide.onAction(GuideAction.Start(AppRole.EMITTER))

        assertTrue(guide.state.value.isActive)
        assertEquals(GuideStep.EMITTER_SEND, guide.state.value.step)
    }

    @Test
    fun choosingRoleAgainResetsTheSnapshotForTheNewDemo() {
        val guide = viewModel()

        guide.onAction(GuideAction.ReportEmitterProgress(2, 1, 1, true, true))
        guide.onAction(GuideAction.Start())
        guide.onAction(GuideAction.RoleConfirmed(AppRole.EMITTER))

        assertEquals(GuideStep.EMITTER_PLAYLIST, guide.state.value.step)
        assertEquals(0, guide.state.value.playlistItems)
    }

    @Test
    fun restartGoesBackToTheFirstStepAndStopDeactivates() {
        val guide = viewModel()

        guide.onAction(GuideAction.Start(AppRole.EMITTER))
        guide.onAction(GuideAction.ReportEmitterProgress(1, 0, 0, false, false))
        assertEquals(GuideStep.EMITTER_SCREENS, guide.state.value.step)

        guide.onAction(GuideAction.Restart)
        assertEquals(GuideStep.EMITTER_PLAYLIST, guide.state.value.step)
        assertFalse(guide.state.value.autoAdvanceEnabled)

        // A replay is manual: an already configured demo must not jump to the
        // end just because the real screen still reports an old successful send.
        guide.onAction(GuideAction.ReportEmitterProgress(1, 1, 1, true, true))
        assertEquals(GuideStep.EMITTER_PLAYLIST, guide.state.value.step)

        guide.onAction(GuideAction.Next)
        assertEquals(GuideStep.EMITTER_SCREENS, guide.state.value.step)

        guide.onAction(GuideAction.Stop)
        assertFalse(guide.state.value.isActive)
    }
}
