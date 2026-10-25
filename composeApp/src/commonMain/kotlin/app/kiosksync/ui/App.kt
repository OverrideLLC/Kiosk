package app.kiosksync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.emitter.presentation.actions.EmitterAction
import app.kiosksync.feature.guide.domain.GuideManager
import app.kiosksync.feature.guide.domain.GuideStep
import app.kiosksync.feature.guide.presentation.GuideOverlay
import app.kiosksync.feature.emitter.presentation.EmitterRoute
import app.kiosksync.feature.emitter.presentation.EmitterViewModel
import app.kiosksync.feature.kiosk.presentation.KioskUnlockOverlay
import app.kiosksync.feature.pairing.domain.PairingManager
import app.kiosksync.feature.playback.presentation.PlaybackStateHolder
import app.kiosksync.feature.playlist.presentation.PlaylistEditorAction
import app.kiosksync.feature.playlist.presentation.PlaylistEditorDialogs
import app.kiosksync.feature.playlist.presentation.PlaylistEditorEffect
import app.kiosksync.feature.playlist.presentation.PlaylistEditorStateHolder
import app.kiosksync.feature.playlist.presentation.PlaylistPanel
import app.kiosksync.feature.role.domain.AppConfig
import app.kiosksync.feature.role.presentation.RoleSelectionScreen
import app.kiosksync.feature.role.presentation.RoleStateHolder
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
    val guideManager = remember { GuideManager(container.logger) }
    val guideState by guideManager.uiState.collectAsState()

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

    val roleStateHolder = remember {
        RoleStateHolder(
            logger = container.logger,
            initialRole = container.args.role ?: existingConfig?.role
        )
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
            guideManager.start()
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
                guideManager.stop()
                roleStateHolder.confirmChangeRole()
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
            if (currentScreen is Screen.ReceiverHome) guideManager.stop()
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
                .background(KioskColors.scheme.surface),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (val screen = currentScreen) {
                    is Screen.RoleSelection -> {
                        RoleSelectionScreen(
                            stateHolder = roleStateHolder,
                            onRoleConfirmed = { confirmedConfig ->
                                persistConfig(confirmedConfig)
                                currentScreen = when (confirmedConfig.role) {
                                    AppRole.EMITTER -> Screen.EmitterHome(confirmedConfig)
                                    AppRole.RECEIVER -> Screen.ReceiverHome(confirmedConfig)
                                }
                                guideManager.onRoleConfirmed(confirmedConfig.role)
                            },
                            deviceId = initialDeviceId,
                            deviceName = container.deviceName,
                            port = effectivePort,
                            onExitRequested = onExitRequested,
                            theme = appTheme,
                            reduceMotion = reduceMotion,
                            onOpenGuide = { guideManager.start() }
                        )
                    }

                    is Screen.EmitterHome -> {
                        EmitterWorkspace(
                            config = screen.config,
                            container = container,
                            guideManager = guideManager,
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
                            guideManager = guideManager,
                            onOpenGuide = { guideManager.start(AppRole.RECEIVER) },
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
                            roleStateHolder.selectRole(role)
                            val config = roleStateHolder.confirmRole(
                                deviceId = initialDeviceId,
                                deviceName = container.deviceName,
                                port = effectivePort,
                                theme = appTheme,
                                reduceMotion = reduceMotion,
                            )
                            config?.let { confirmed ->
                                persistConfig(confirmed)
                                currentScreen = when (confirmed.role) {
                                    AppRole.EMITTER -> Screen.EmitterHome(confirmed)
                                    AppRole.RECEIVER -> Screen.ReceiverHome(confirmed)
                                }
                                guideManager.onRoleConfirmed(confirmed.role)
                            }
                        },
                        onNext = { guideManager.next() },
                        onBack = { guideManager.back() },
                        onSkip = {
                            guideManager.stop()
                            relockReceiverIfNeeded()
                        },
                        onAction = { guideManager.requestAction(it) },
                        onFinish = {
                            guideManager.stop()
                            relockReceiverIfNeeded()
                        },
                        onRestart = { guideManager.restart() },
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
                    guideManager.start(AppRole.RECEIVER)
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
    guideManager: GuideManager,
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
    val playlistStateHolder = remember(config.deviceId, container) {
        PlaylistEditorStateHolder(
            playlistRepository = container.playlistRepository,
            mediaRepository = container.mediaRepository,
            logger = container.logger,
            scope = scope,
            newPlaylistId = container.newDeviceId,
        )
    }
    val playlistEditorState by playlistStateHolder.uiState.collectAsState()

    LaunchedEffect(playlistStateHolder) {
        playlistStateHolder.effects.collect { effect ->
            when (effect) {
                PlaylistEditorEffect.SelectImageFiles -> try {
                    playlistStateHolder.dispatch(
                        PlaylistEditorAction.ImagesSelected(container.pickImageFiles())
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    playlistStateHolder.dispatch(PlaylistEditorAction.ImagePickerFailed(error))
                }
            }
        }
    }
    LaunchedEffect(viewModel, playlistStateHolder) {
        container.projectionController.toggleRequests.collect {
            viewModel.onAction(EmitterAction.RequestProjection(playlistStateHolder.currentPlaylist()))
        }
    }

    EmitterRoute(
        viewModel = viewModel,
        playlist = playlistEditorState.playlist,
        onAddImages = playlistStateHolder::addImages,
        playlistPanel = { modifier, fillHeight, onRequestRemove ->
            PlaylistPanel(
                stateHolder = playlistStateHolder,
                state = playlistEditorState,
                mediaRepository = container.mediaRepository,
                fillHeight = fillHeight,
                onRequestRemove = onRequestRemove,
                modifier = modifier,
            )
        },
        playlistDialogs = { removeTarget, onDismissRemove ->
            PlaylistEditorDialogs(
                stateHolder = playlistStateHolder,
                state = playlistEditorState,
                removeTarget = removeTarget,
                onDismissRemove = onDismissRemove,
            )
        },
        guideManager = guideManager,
        onExitRequested = onExitRequested,
        onRoleReset = onRoleReset,
        theme = theme,
        reduceMotion = reduceMotion,
        onThemeChange = onThemeChange,
        onReduceMotionChange = onReduceMotionChange,
    )
}
