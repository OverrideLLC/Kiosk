package app.kiosksync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.kiosksync.AppContainer
import app.kiosksync.core.AppRole
import app.kiosksync.core.AppTheme
import app.kiosksync.core.DEFAULT_PORT
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.emitter.presentation.EmitterAction
import app.kiosksync.feature.emitter.presentation.EmitterPanel
import app.kiosksync.feature.emitter.presentation.EmitterScreen
import app.kiosksync.feature.emitter.presentation.EmitterScreenAction
import app.kiosksync.feature.emitter.presentation.components.EmitterDialogs
import app.kiosksync.feature.emitter.presentation.components.SettingsDialog
import app.kiosksync.feature.guide.presentation.GuideAction
import app.kiosksync.feature.guide.presentation.GuideEffect
import app.kiosksync.feature.guide.presentation.components.GuideOverlay
import app.kiosksync.feature.guide.presentation.GuideViewModel
import app.kiosksync.feature.guide.domain.GuideStep
import app.kiosksync.feature.emitter.presentation.EmitterViewModel
import app.kiosksync.feature.emitter.presentation.SyncUiStatus
import app.kiosksync.feature.kiosk.presentation.KioskUnlockOverlay
import app.kiosksync.feature.pairing.domain.PairingManager
import app.kiosksync.feature.playback.presentation.PlaybackStateHolder
import app.kiosksync.feature.playlist.presentation.PlaylistEditorAction
import app.kiosksync.feature.playlist.presentation.PlaylistEditorDialogs
import app.kiosksync.feature.playlist.presentation.PlaylistEditorEffect
import app.kiosksync.feature.playlist.presentation.PlaylistEditorViewModel
import app.kiosksync.feature.playlist.presentation.PlaylistPanel
import app.kiosksync.feature.role.domain.AppConfig
import app.kiosksync.feature.role.presentation.RoleAction
import app.kiosksync.feature.role.presentation.RoleEffect
import app.kiosksync.feature.role.presentation.RoleSelectionScreen
import app.kiosksync.feature.role.presentation.RoleViewModel
import app.kiosksync.feature.update.presentation.ReceiverUpdateOverlay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun App(
    container: AppContainer,
    onExitRequested: () -> Unit = {},
    registerEscHandler: ((() -> Unit)?) -> Unit = {},
    registerUnpairHandler: ((() -> Unit)?) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val existingConfig = container.initialConfig
    val initialDeviceId = remember { existingConfig?.deviceId ?: container.newDeviceId() }
    val effectivePort = container.args.port ?: existingConfig?.port ?: DEFAULT_PORT
    val pairingManager: PairingManager = container.pairingManager
    val guideViewModel = remember { GuideViewModel(container.logger) }
    val guideState by guideViewModel.state.collectAsState()

    // Determine initial screen based on CLI args and saved config
    val initialScreen = remember {
        val forcedRole = container.args.role
        if (forcedRole != null) {
            val config = AppConfig(
                role = forcedRole,
                deviceId = initialDeviceId,
                deviceName = container.deviceName,
                port = effectivePort,
                theme = existingConfig?.theme ?: AppTheme.DARK,
                reduceMotion = existingConfig?.reduceMotion ?: false
            )
            when (forcedRole) {
                AppRole.EMITTER -> Screen.EmitterHome(config)
                AppRole.RECEIVER -> Screen.ReceiverHome(config)
            }
        } else if (existingConfig != null) {
            when (existingConfig.role) {
                AppRole.EMITTER -> Screen.EmitterHome(existingConfig)
                AppRole.RECEIVER -> Screen.ReceiverHome(existingConfig)
            }
        } else {
            Screen.RoleSelection
        }
    }

    var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }
    val initialScreenConfig = when (val screen = initialScreen) {
        is Screen.EmitterHome -> screen.config
        is Screen.ReceiverHome -> screen.config
        Screen.RoleSelection -> existingConfig
    }
    var currentConfig by remember { mutableStateOf(initialScreenConfig) }
    var appTheme by remember { mutableStateOf(existingConfig?.theme ?: AppTheme.DARK) }
    var reduceMotion by remember { mutableStateOf(existingConfig?.reduceMotion ?: false) }

    fun persistConfig(config: AppConfig) {
        currentConfig = config
        coroutineScope.launch {
            try {
                container.configStore.save(config)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                container.logger.error("Could not save application preferences", error)
            }
        }
    }

    fun updateConfig(transform: (AppConfig) -> AppConfig) {
        currentConfig?.let { persistConfig(transform(it)) }
    }

    fun updateTheme(theme: AppTheme) {
        appTheme = theme
        updateConfig { it.copy(theme = theme) }
    }

    fun updateReduceMotion(enabled: Boolean) {
        reduceMotion = enabled
        updateConfig { it.copy(reduceMotion = enabled) }
    }

    LaunchedEffect(container.args.role) {
        if (container.args.role != null) {
            currentConfig?.let { config ->
                try {
                    container.configStore.save(config)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    container.logger.error("Could not persist the role forced by command line", error)
                }
            }
        }
    }

    val roleViewModel = remember {
        RoleViewModel(
            logger = container.logger,
            initialRole = container.args.role ?: existingConfig?.role
        )
    }
    val roleUiState by roleViewModel.state.collectAsState()

    LaunchedEffect(roleViewModel) {
        roleViewModel.effects.collect { effect ->
            when (effect) {
                is RoleEffect.RoleConfirmed -> {
                    val config = AppConfig(
                        role = effect.role,
                        deviceId = initialDeviceId,
                        deviceName = container.deviceName,
                        port = effectivePort,
                        theme = appTheme,
                        reduceMotion = reduceMotion,
                    )
                    persistConfig(config)
                    currentScreen = when (config.role) {
                        AppRole.EMITTER -> Screen.EmitterHome(config)
                        AppRole.RECEIVER -> Screen.ReceiverHome(config)
                    }
                    guideViewModel.onAction(GuideAction.RoleConfirmed(config.role))
                }
            }
        }
    }

    val playbackStateHolder = remember {
        PlaybackStateHolder(
            playlistRepository = container.playlistRepository,
            mediaRepository = container.mediaRepository,
            logger = container.logger,
            scope = coroutineScope,
            statusTracker = container.receiverStatusTracker,
            backgroundDispatcher = container.backgroundDispatcher
        )
    }

    DisposableEffect(playbackStateHolder) {
        container.receiverPlaylistBridge.setListener { playlist ->
            playbackStateHolder.updatePlaylist(playlist)
        }
        container.receiverPlaybackSyncBridge.setListener { delayMs ->
            playbackStateHolder.scheduleRestart(delayMs)
        }
        onDispose {
            container.receiverPlaylistBridge.setListener(null)
            container.receiverPlaybackSyncBridge.setListener(null)
            playbackStateHolder.stop()
        }
    }

    LaunchedEffect(currentScreen) {
        if (currentScreen is Screen.ReceiverHome) {
            playbackStateHolder.start()
        } else {
            playbackStateHolder.stop()
        }
    }

    val pairingState by pairingManager.uiState.collectAsState()
    val lockState by container.kioskLockManager.uiState.collectAsState()
    val receiverUpdateState by container.receiverUpdateManager.uiState.collectAsState()
    var showReceiverOptions by remember { mutableStateOf(false) }
    var showUnpairConfirm by remember { mutableStateOf(false) }
    var showChangeRoleConfirm by remember { mutableStateOf(false) }
    var isResettingRole by remember { mutableStateOf(false) }
    var roleResetFailed by remember { mutableStateOf(false) }
    var wasLocked by remember { mutableStateOf(lockState.isLocked) }

    LaunchedEffect(Unit) {
        if (existingConfig == null && container.args.role == null) {
            guideViewModel.onAction(GuideAction.Start())
        }
    }

    fun relockReceiverIfNeeded() {
        if (
            currentScreen is Screen.ReceiverHome &&
            pairingState.isPaired &&
            lockState.hasPassword
        ) {
            container.kioskLockManager.lock(force = true)
        }
    }

    fun closeReceiverOptions() {
        showReceiverOptions = false
        relockReceiverIfNeeded()
    }

    fun resetRole() {
        if (isResettingRole) return
        val changingFromReceiver = currentScreen is Screen.ReceiverHome
        isResettingRole = true
        roleResetFailed = false
        coroutineScope.launch {
            try {
                withContext(container.serviceDispatcher) {
                    container.discovery.unregisterService()
                    container.discovery.stopDiscovery()
                    container.receiverServer.stop()
                }
                if (changingFromReceiver) {
                    container.receiverDataResetter.clearReceiverData()
                } else {
                    container.screensRepository.clearPairedScreens()
                }
                // Refresh the in-memory pairing state as well as its persisted row.
                // A role change must return a Receiver to a newly generated pairing code.
                container.pairingManager.unpair()
                container.kioskLockRepository.clearLock()
                container.configStore.clear()
                guideViewModel.onAction(GuideAction.Stop)
                roleViewModel.onAction(RoleAction.ResetSelection)
                currentConfig = null
                container.kioskLockManager.release()
                container.projectionController.stop()
                showReceiverOptions = false
                showUnpairConfirm = false
                showChangeRoleConfirm = false
                currentScreen = Screen.RoleSelection
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                container.logger.error("Could not reset local data while changing role", error)
                roleResetFailed = true
                showChangeRoleConfirm = true
                // Keep the current role and its UI active when cleanup fails.
            } finally {
                isResettingRole = false
            }
        }
    }

    // Only a paired Screen with a blocking password is a kiosk. The pairing
    // screen and the waiting-for-password screen remain usable.
    LaunchedEffect(currentScreen) {
        container.updateActiveRole(
            when (currentScreen) {
                is Screen.EmitterHome -> AppRole.EMITTER
                is Screen.ReceiverHome -> AppRole.RECEIVER
                Screen.RoleSelection -> null
            },
        )
    }

    LaunchedEffect(currentScreen, pairingState.isPaired, lockState.hasPassword) {
        when (currentScreen) {
            is Screen.ReceiverHome -> {
                if (pairingState.isPaired && lockState.hasPassword) {
                    container.kioskLockManager.lock(force = true)
                } else {
                    container.kioskLockManager.release()
                }
            }

            is Screen.RoleSelection -> container.kioskLockManager.release()
            is Screen.EmitterHome -> {
                if (!container.projectionController.isProjecting) {
                    container.kioskLockManager.release()
                }
            }
        }
    }

    // While locked, the screen must never show the admin menu. The menu only
    // appears after the correct password unlocks the device.
    LaunchedEffect(lockState.isLocked) {
        val previous = wasLocked
        wasLocked = lockState.isLocked
        if (lockState.isLocked) {
            showReceiverOptions = false
            showUnpairConfirm = false
            if (currentScreen is Screen.ReceiverHome) guideViewModel.onAction(GuideAction.Stop)
        } else if (previous && currentScreen is Screen.ReceiverHome) {
            showReceiverOptions = true
        }
    }

    LaunchedEffect(receiverUpdateState.isVisible) {
        if (receiverUpdateState.isVisible) {
            showReceiverOptions = false
            showUnpairConfirm = false
            relockReceiverIfNeeded()
        }
    }

    LaunchedEffect(currentScreen, pairingState.isPaired) {
        if (currentScreen is Screen.ReceiverHome) {
            registerEscHandler {
                if (container.kioskLockManager.uiState.value.isLocked) {
                    // Keys are swallowed while locked; do not open dialogs.
                    return@registerEscHandler
                }
                showReceiverOptions = true
            }
        } else {
            registerEscHandler(null)
        }
    }

    LaunchedEffect(currentScreen, pairingState.isPaired) {
        if (currentScreen is Screen.ReceiverHome && pairingState.isPaired) {
            registerUnpairHandler { showUnpairConfirm = true }
        } else {
            registerUnpairHandler(null)
        }
    }

    KioskTheme(theme = appTheme, reduceMotion = reduceMotion) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (val screen = currentScreen) {
                    is Screen.RoleSelection -> {
                        RoleSelectionScreen(
                            state = roleUiState,
                            onAction = roleViewModel::onAction,
                            onExitRequested = onExitRequested,
                            onOpenGuide = { guideViewModel.onAction(GuideAction.Start()) }
                        )
                    }

                    is Screen.EmitterHome -> {
                        EmitterWorkspace(
                            config = screen.config,
                            container = container,
                            guideViewModel = guideViewModel,
                            onExitRequested = onExitRequested,
                            onRoleReset = { resetRole() },
                            theme = appTheme,
                            reduceMotion = reduceMotion,
                            onThemeChange = { updateTheme(it) },
                            onReduceMotionChange = { updateReduceMotion(it) }
                        )
                    }

                    is Screen.ReceiverHome -> {
                        ReceiverHomeScreen(
                            config = screen.config,
                            container = container,
                            pairingManager = pairingManager,
                            playbackStateHolder = playbackStateHolder,
                            guideViewModel = guideViewModel,
                            onOpenGuide = {
                                guideViewModel.onAction(GuideAction.Start(AppRole.RECEIVER))
                            },
                            onExitRequested = onExitRequested,
                            onChangeRoleRequested = { showChangeRoleConfirm = true }
                        )
                    }
                }

                KioskUnlockOverlay(lockState)

                val guideMatchesScreen = when (guideState.step) {
                    GuideStep.ROLE -> currentScreen is Screen.RoleSelection
                    else -> when (guideState.role) {
                        AppRole.EMITTER -> currentScreen is Screen.EmitterHome
                        AppRole.RECEIVER -> currentScreen is Screen.ReceiverHome
                        null -> false
                    }
                }
                if (
                    guideState.isActive &&
                    !lockState.isLocked &&
                    !receiverUpdateState.isVisible &&
                    guideMatchesScreen
                ) {
                    GuideOverlay(
                        state = guideState,
                        onChooseRole = { role ->
                            roleViewModel.onAction(RoleAction.SelectRole(role))
                            roleViewModel.onAction(RoleAction.ConfirmRole)
                        },
                        onAction = { action ->
                            guideViewModel.onAction(action)
                            if (action == GuideAction.Stop) relockReceiverIfNeeded()
                        },
                    )
                }

                if (currentScreen is Screen.ReceiverHome && receiverUpdateState.isVisible) {
                    ReceiverUpdateOverlay(
                        state = receiverUpdateState,
                        onUpdate = {
                            if (container.receiverUpdateManager.acceptUpdate()) {
                                onExitRequested()
                            }
                        },
                        onDecline = { container.receiverUpdateManager.declineUpdate() },
                        onDismissError = {
                            container.receiverUpdateManager.dismissError()
                        }
                    )
                }
            }
        }

        if (showReceiverOptions) {
            ReceiverOptionsDialog(
                isReceiver = currentScreen is Screen.ReceiverHome,
                onOpenGuide = {
                    showReceiverOptions = false
                    guideViewModel.onAction(GuideAction.Start(AppRole.RECEIVER))
                },
                onDismiss = { closeReceiverOptions() },
                onChangeRole = {
                    showReceiverOptions = false
                    showChangeRoleConfirm = true
                },
                onExit = {
                    showReceiverOptions = false
                    onExitRequested()
                },
                onRelock = { closeReceiverOptions() },
            )
        }

        if (showUnpairConfirm) {
            ReceiverUnpairDialog(
                onDismiss = {
                    showUnpairConfirm = false
                    relockReceiverIfNeeded()
                },
                onConfirm = {
                    coroutineScope.launch {
                        try {
                            pairingManager.unpair()
                            showUnpairConfirm = false
                            relockReceiverIfNeeded()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            container.logger.error("Could not unlink the Receiver", error)
                        }
                    }
                },
            )
        }

        if (showChangeRoleConfirm) {
            ChangeRoleDialog(
                isResetting = isResettingRole,
                hasError = roleResetFailed,
                onDismiss = {
                    if (!isResettingRole) {
                        showChangeRoleConfirm = false
                        relockReceiverIfNeeded()
                    }
                },
                onConfirm = { resetRole() },
            )
        }
    }
}

@Composable
private fun EmitterWorkspace(
    config: AppConfig,
    container: AppContainer,
    guideViewModel: GuideViewModel,
    onExitRequested: () -> Unit,
    onRoleReset: () -> Unit,
    theme: AppTheme,
    reduceMotion: Boolean,
    onThemeChange: (AppTheme) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(config.deviceId, container) {
        EmitterViewModel(
            service = container.emitterService,
            kioskLockConfigurationUseCases = container.kioskLockConfigurationUseCases,
            logger = container.logger,
            emitterId = config.deviceId,
            emitterName = container.deviceName,
            scope = scope,
            kioskLockManager = container.kioskLockManager,
            projectionController = container.projectionController,
            backgroundDispatcher = container.backgroundDispatcher,
        )
    }
    val emitterState by viewModel.state.collectAsState()
    val playlistViewModel = remember(config.deviceId, container) {
        PlaylistEditorViewModel(
            playlistRepository = container.playlistRepository,
            mediaRepository = container.mediaRepository,
            logger = container.logger,
            scope = scope,
            newPlaylistId = container.newDeviceId,
        )
    }
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
                        PlaylistEditorAction.ImagesSelected(container.pickImageFiles())
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
        container.projectionController.toggleRequests.collect {
            viewModel.onAction(EmitterAction.RequestProjection)
        }
    }
    LaunchedEffect(viewModel, container.projectionController) {
        container.projectionController.playlist.collect { playlist ->
            viewModel.onAction(EmitterAction.SetProjectionActive(playlist != null))
        }
    }

    val dispatch: (EmitterScreenAction) -> Unit = { action ->
        when (action) {
            is EmitterScreenAction.Emitter -> viewModel.onAction(action.action)
            EmitterScreenAction.ExitRequested -> onExitRequested()
            EmitterScreenAction.RoleResetConfirmed -> {
                viewModel.onAction(EmitterAction.DismissRoleReset)
                onRoleReset()
            }
        }
    }

    EmitterScreen(
        state = emitterState,
        onAction = dispatch,
        playlistPanel = { modifier ->
            PlaylistPanel(
                state = playlistEditorState,
                onAction = playlistViewModel::onAction,
                modifier = modifier,
            )
        },
    )
    EmitterDialogs(state = emitterState, onAction = dispatch)
    PlaylistEditorDialogs(state = playlistEditorState, onAction = playlistViewModel::onAction)
    if (emitterState.isSettingsOpen) {
        SettingsDialog(
            theme = theme,
            reduceMotion = reduceMotion,
            onThemeChange = onThemeChange,
            onReduceMotionChange = onReduceMotionChange,
            onOpenGuide = {
                viewModel.onAction(EmitterAction.DismissSettings)
                guideViewModel.onAction(GuideAction.Start(AppRole.EMITTER))
            },
            onDismiss = { viewModel.onAction(EmitterAction.DismissSettings) },
        )
    }
}
