package app.kiosksync.feature.guide.domain

import app.kiosksync.NoOpLogger
import app.kiosksync.core.AppRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuideManagerTest {

    private fun manager() = GuideManager(NoOpLogger())

    @Test
    fun emitterGuideAdvancesWithRealState() {
        val guide = manager()

        guide.start(AppRole.EMITTER)
        assertEquals(GuideStep.EMITTER_PLAYLIST, guide.uiState.value.step)

        guide.updateEmitter(
            playlistItems = 1,
            pairedScreens = 0,
            selectedScreens = 0,
            hasPassword = false,
            sendSucceeded = false,
        )
        assertEquals(GuideStep.EMITTER_SCREENS, guide.uiState.value.step)

        guide.updateEmitter(
            playlistItems = 1,
            pairedScreens = 1,
            selectedScreens = 1,
            hasPassword = false,
            sendSucceeded = false,
        )
        assertEquals(GuideStep.EMITTER_PASSWORD, guide.uiState.value.step)

        guide.updateEmitter(
            playlistItems = 1,
            pairedScreens = 1,
            selectedScreens = 1,
            hasPassword = true,
            sendSucceeded = false,
        )
        assertEquals(GuideStep.EMITTER_SEND, guide.uiState.value.step)

        guide.updateEmitter(
            playlistItems = 1,
            pairedScreens = 1,
            selectedScreens = 1,
            hasPassword = true,
            sendSucceeded = true,
        )
        assertEquals(GuideStep.EMITTER_DONE, guide.uiState.value.step)
    }

    @Test
    fun receiverGuideAdvancesWithPairingPasswordAndContent() {
        val guide = manager()

        guide.start(AppRole.RECEIVER)
        assertEquals(GuideStep.RECEIVER_CODE, guide.uiState.value.step)

        guide.updateReceiver(paired = true, hasPassword = false, hasContent = false)
        assertEquals(GuideStep.RECEIVER_PASSWORD, guide.uiState.value.step)

        guide.updateReceiver(paired = true, hasPassword = true, hasContent = false)
        assertEquals(GuideStep.RECEIVER_CONTENT, guide.uiState.value.step)

        guide.updateReceiver(paired = true, hasPassword = true, hasContent = true)
        assertEquals(GuideStep.RECEIVER_DONE, guide.uiState.value.step)
    }

    @Test
    fun reopeningFromSettingsStartsAtTheFirstPendingStep() {
        val guide = manager()

        guide.updateEmitter(
            playlistItems = 2,
            pairedScreens = 1,
            selectedScreens = 1,
            hasPassword = true,
            sendSucceeded = false,
        )
        guide.start(AppRole.EMITTER)

        assertTrue(guide.uiState.value.isActive)
        assertEquals(GuideStep.EMITTER_SEND, guide.uiState.value.step)
    }

    @Test
    fun choosingRoleAgainResetsTheSnapshotForTheNewDemo() {
        val guide = manager()

        guide.updateEmitter(
            playlistItems = 2,
            pairedScreens = 1,
            selectedScreens = 1,
            hasPassword = true,
            sendSucceeded = true,
        )
        guide.start()
        guide.onRoleConfirmed(AppRole.EMITTER)

        assertEquals(GuideStep.EMITTER_PLAYLIST, guide.uiState.value.step)
        assertEquals(0, guide.uiState.value.playlistItems)
    }

    @Test
    fun restartGoesBackToTheFirstStepAndStopDeactivates() {
        val guide = manager()

        guide.start(AppRole.EMITTER)
        guide.updateEmitter(1, 0, 0, false, false)
        assertEquals(GuideStep.EMITTER_SCREENS, guide.uiState.value.step)

        guide.restart()
        assertEquals(GuideStep.EMITTER_PLAYLIST, guide.uiState.value.step)
        assertFalse(guide.uiState.value.autoAdvanceEnabled)

        // A replay is manual: an already configured demo must not jump to the
        // end just because the real screen still reports an old successful send.
        guide.updateEmitter(1, 1, 1, true, true)
        assertEquals(GuideStep.EMITTER_PLAYLIST, guide.uiState.value.step)

        guide.next()
        assertEquals(GuideStep.EMITTER_SCREENS, guide.uiState.value.step)

        guide.stop()
        assertFalse(guide.uiState.value.isActive)
    }
}
