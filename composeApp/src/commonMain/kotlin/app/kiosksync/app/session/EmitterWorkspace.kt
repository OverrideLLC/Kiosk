package app.kiosksync.app.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.kiosksync.app.di.ImageFilePicker
import app.kiosksync.app.navigation.session.sessionViewModel
import app.kiosksync.core.common.app.AppRole
import app.kiosksync.feature.emitter.domain.models.EmitterPanel
import app.kiosksync.feature.emitter.domain.models.SyncUiStatus
import app.kiosksync.feature.emitter.presentation.EmitterAction
import app.kiosksync.feature.emitter.presentation.EmitterRoot
import app.kiosksync.feature.emitter.presentation.EmitterViewModel
import app.kiosksync.feature.emitter.presentation.components.EmitterDialogs
import app.kiosksync.feature.emitter.presentation.components.SettingsDialog
import app.kiosksync.feature.guide.domain.GuideStep
import app.kiosksync.feature.guide.presentation.GuideAction
import app.kiosksync.feature.guide.presentation.GuideEffect
import app.kiosksync.feature.guide.presentation.GuideViewModel
import app.kiosksync.feature.playlist.presentation.PlaylistEditorAction
import app.kiosksync.feature.playlist.presentation.PlaylistEditorEffect
import app.kiosksync.feature.playlist.presentation.PlaylistEditorViewModel
import app.kiosksync.feature.playlist.presentation.components.PlaylistEditorDialogs
import app.kiosksync.feature.projection.domain.ProjectionController
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun EmitterWorkspace() {
    val guideViewModel = sessionViewModel<GuideViewModel>()
    val imagePicker = koinInject<ImageFilePicker>()
    val projectionController = koinInject<ProjectionController>()
    val viewModel = koinViewModel<EmitterViewModel>()
    val emitterState by viewModel.state.collectAsState()
    val playlistViewModel = koinViewModel<PlaylistEditorViewModel>()
    val playlistEditorState by playlistViewModel.state.collectAsState()
    val guideState by guideViewModel.state.collectAsState()
    val pairedCount = emitterState.devices.count { it.isPairedToThisEmitter }
    val selectedIds = emitterState.devices
        .filter { it.isSelected && it.isPairedToThisEmitter }
        .mapTo(mutableSetOf()) { it.deviceId }
    val selectedSendSucceeded = selectedIds.isNotEmpty() && selectedIds.all { deviceId ->
        emitterState.syncByDevice[deviceId]?.status == SyncUiStatus.SUCCESS
    }

    DisposableEffect(viewModel) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }

    LaunchedEffect(playlistViewModel) {
        playlistViewModel.effects.collect { effect ->
            when (effect) {
                PlaylistEditorEffect.SelectImageFiles -> try {
                    playlistViewModel.onAction(
                        PlaylistEditorAction.ImagesSelected(imagePicker.pick())
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    playlistViewModel.onAction(PlaylistEditorAction.ImagePickerFailed(error))
                }
            }
        }
    }
    LaunchedEffect(playlistEditorState.playlist) {
        viewModel.onAction(EmitterAction.PlaylistChanged(playlistEditorState.playlist))
    }

    LaunchedEffect(
        playlistEditorState.playlist.items.size,
        pairedCount,
        selectedIds.size,
        emitterState.hasKioskPassword,
        selectedSendSucceeded,
    ) {
        guideViewModel.onAction(
            GuideAction.ReportEmitterProgress(
                playlistItems = playlistEditorState.playlist.items.size,
                pairedScreens = pairedCount,
                selectedScreens = selectedIds.size,
                hasPassword = emitterState.hasKioskPassword,
                sendSucceeded = selectedSendSucceeded,
            ),
        )
    }
    LaunchedEffect(guideState.step) {
        when (guideState.step) {
            GuideStep.EMITTER_PLAYLIST -> viewModel.onAction(EmitterAction.SelectPanel(EmitterPanel.PLAYLIST))
            GuideStep.EMITTER_SCREENS,
            GuideStep.EMITTER_PASSWORD,
            GuideStep.EMITTER_SEND -> viewModel.onAction(EmitterAction.SelectPanel(EmitterPanel.SCREENS))
            else -> Unit
        }
    }
    LaunchedEffect(guideState.step, guideState.isActive, guideState.role, pairedCount, selectedIds.size) {
        if (
            guideState.isActive &&
            guideState.role == AppRole.EMITTER &&
            guideState.step == GuideStep.EMITTER_SEND &&
            selectedIds.isEmpty()
        ) {
            emitterState.devices
                .firstOrNull { it.isPairedToThisEmitter && !it.isSelected }
                ?.let { viewModel.onAction(EmitterAction.ToggleSelection(it.deviceId)) }
        }
    }
    LaunchedEffect(guideViewModel, viewModel, playlistViewModel) {
        guideViewModel.effects.collect { effect ->
            when (effect) {
                GuideEffect.AddImages -> playlistViewModel.onAction(PlaylistEditorAction.AddImages)
                GuideEffect.RefreshDevices -> viewModel.onAction(EmitterAction.RefreshDevices)
                GuideEffect.Send -> viewModel.onAction(EmitterAction.SendToSelected)
            }
        }
    }
    LaunchedEffect(viewModel, playlistViewModel) {
        projectionController.toggleRequests.collect {
            viewModel.onAction(EmitterAction.RequestProjection)
        }
    }
    LaunchedEffect(viewModel, projectionController) {
        projectionController.playlist.collect { playlist ->
            viewModel.onAction(EmitterAction.SetProjectionActive(playlist != null))
        }
    }

    EmitterRoot(
        viewModel = viewModel,
        playlistViewModel = playlistViewModel,
    )
    EmitterDialogs(state = emitterState, onAction = viewModel::onAction)
    PlaylistEditorDialogs(state = playlistEditorState, onAction = playlistViewModel::onAction)
    if (emitterState.isSettingsOpen) {
        SettingsDialog(
            onOpenGuide = {
                viewModel.onAction(EmitterAction.DismissSettings)
                guideViewModel.onAction(GuideAction.Start(AppRole.EMITTER))
            },
            onDismiss = { viewModel.onAction(EmitterAction.DismissSettings) },
        )
    }
}
