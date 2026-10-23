package app.kiosksync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.Text as M3Text
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
import app.kiosksync.core.Strings
import app.kiosksync.designsystem.AppButton
import app.kiosksync.designsystem.AppButtonSize
import app.kiosksync.designsystem.AppButtonVariant
import app.kiosksync.designsystem.AppDialog
import app.kiosksync.designsystem.AppIcon
import app.kiosksync.designsystem.KioskColors
import app.kiosksync.designsystem.KioskTheme
import app.kiosksync.feature.guide.GuideManager
import app.kiosksync.feature.guide.GuideOverlay
import app.kiosksync.feature.guide.GuideStep
import app.kiosksync.feature.kiosk.KioskUnlockOverlay
import app.kiosksync.feature.pairing.PairingManager
import app.kiosksync.feature.playback.PlaybackStateHolder
import app.kiosksync.feature.role.AppConfig
import app.kiosksync.feature.role.RoleSelectionScreen
import app.kiosksync.feature.role.RoleStateHolder
import app.kiosksync.feature.update.ReceiverUpdateOverlay

@Composable
fun App(
    container: AppContainer,
    onExitRequested: () -> Unit = {},
    registerEscHandler: ((() -> Unit)?) -> Unit = {},
    registerUnpairHandler: ((() -> Unit)?) -> Unit = {},
    onReceiverModeChanged: (Boolean) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val existingConfig = remember { container.localStorage.loadConfig() }
    val initialDeviceId = remember { existingConfig?.deviceId ?: container.newDeviceId() }
    val effectivePort = container.args.port ?: existingConfig?.port ?: DEFAULT_PORT
    val pairingManager: PairingManager = container.pairingManager
    val guideManager = remember { GuideManager(container.logger) }
    val guideState by guideManager.uiState.collectAsState()

    // Appearance is shared by every role and persisted in config.json.
    var appTheme by remember { mutableStateOf(existingConfig?.theme ?: AppTheme.DARK) }
    var reduceMotion by remember { mutableStateOf(existingConfig?.reduceMotion ?: false) }

    fun persistConfig(transform: (AppConfig) -> AppConfig) {
        container.localStorage.loadConfig()?.let { current ->
            container.localStorage.saveConfig(transform(current))
        }
    }

    fun updateTheme(theme: AppTheme) {
        appTheme = theme
        persistConfig { it.copy(theme = theme) }
    }

    fun updateReduceMotion(enabled: Boolean) {
        reduceMotion = enabled
        persistConfig { it.copy(reduceMotion = enabled) }
    }

    // Determine the initial screen from the CLI or the saved role. A first
    // normal launch shows the role picker; later launches go straight to the
    // saved role. --role=<role> still forces a role for automated or two-process
    // debugging. Once a Screen is paired and has its password, the kiosk lock
    // takes over.
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
            container.localStorage.saveConfig(config)
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

    val roleStateHolder = remember {
        RoleStateHolder(
            localStorage = container.localStorage,
            logger = container.logger,
            initialRole = container.args.role ?: existingConfig?.role
        )
    }

    val playbackStateHolder = remember {
        PlaybackStateHolder(
            localStorage = container.localStorage,
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
    var wasLocked by remember { mutableStateOf(lockState.isLocked) }

    // Only the very first normal launch (no config.json yet) opens the guide by
    // itself. After that it stays available from the role screen, the Controller
    // settings and the Screen options. Forced roles skip the role picker, so
    // they do not open it either.
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
            // Only a paired Screen that already received its blocking password
            // is a kiosk. The pairing code and the "waiting for password" state
            // stay usable with the keyboard and the mouse.
            container.kioskLockManager.lock(force = true)
        }
    }

    fun closeReceiverOptions() {
        showReceiverOptions = false
        relockReceiverIfNeeded()
    }

    fun handleRoleConfirmed(confirmedConfig: AppConfig) {
        if (confirmedConfig.role == AppRole.RECEIVER) {
            // Entering the Screen role clears the previous session, except when
            // this process just restarted after an update. In that case the
            // restart marker preserves pairing and content.
            if (!container.consumeUpdateRestartMarker()) {
                container.localStorage.clearReceiverData()
                // Drop the password that lived only on disk: the next lock
                // must use the password this Controller sends.
                container.kioskLockManager.setConfig(null)
            }
            pairingManager.checkInitialPairing()
        }
        currentScreen = when (confirmedConfig.role) {
            AppRole.EMITTER -> Screen.EmitterHome(confirmedConfig)
            AppRole.RECEIVER -> Screen.ReceiverHome(confirmedConfig)
        }
        guideManager.onRoleConfirmed(confirmedConfig.role)
    }

    fun confirmRoleFromGuide(role: AppRole) {
        roleStateHolder.selectRole(role)
        val config = roleStateHolder.confirmRole(
            deviceId = initialDeviceId,
            deviceName = container.deviceName,
            port = effectivePort,
            theme = appTheme,
            reduceMotion = reduceMotion,
        )
        if (config != null) {
            handleRoleConfirmed(config)
        }
    }

    fun closeGuide() {
        guideManager.stop()
        relockReceiverIfNeeded()
    }

    fun resetRole() {
        roleStateHolder.confirmChangeRole()
        container.kioskLockManager.setConfig(null)
        container.kioskLockManager.release()
        container.projectionController.stop()
        // Pairing is initialized only when the Receiver role is confirmed again,
        // so changing role does not generate an unused code on the UI thread.
        showReceiverOptions = false
        showUnpairConfirm = false
        guideManager.stop()
        currentScreen = Screen.RoleSelection
    }

    // The Screen only becomes a kiosk once it is paired AND the Controller has
    // sent the blocking password. While the screen waits for that password it
    // must stay usable with the keyboard and the mouse. Pairing, unpairing or
    // provisioning the password updates this effect immediately.
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

    LaunchedEffect(currentScreen) {
        onReceiverModeChanged(currentScreen is Screen.ReceiverHome)
    }

    // While locked, the screen must never show the admin menu. The menu only
    // appears after the correct password unlocks the device.
    LaunchedEffect(lockState.isLocked) {
        val previous = wasLocked
        wasLocked = lockState.isLocked
        if (lockState.isLocked) {
            showReceiverOptions = false
            showUnpairConfirm = false
            // The Screen is now a kiosk showing content. The setup guide has no
            // place on top of a locked carousel.
            if (currentScreen is Screen.ReceiverHome) {
                guideManager.stop()
            }
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
                                handleRoleConfirmed(confirmedConfig)
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
                        EmitterHomeScreen(
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
                            onChangeRoleRequested = { resetRole() }
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
                        onChooseRole = { role -> confirmRoleFromGuide(role) },
                        onNext = { guideManager.next() },
                        onBack = { guideManager.back() },
                        onSkip = { closeGuide() },
                        onAction = { action -> guideManager.requestAction(action) },
                        onFinish = { closeGuide() },
                        onRestart = { guideManager.restart() }
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
            AppDialog(
                title = Strings.OPTIONS_TITLE,
                icon = AppIcon.LOCK_OPEN,
                onDismissRequest = { closeReceiverOptions() },
                content = {
                    M3Text(
                        text = if (currentScreen is Screen.ReceiverHome) {
                            Strings.RECEIVER_UNLOCKED_MESSAGE
                        } else {
                            Strings.OPTIONS_MESSAGE
                        },
                        style = M3MaterialTheme.typography.bodyLarge,
                        color = KioskColors.scheme.onSurfaceVariant,
                    )
                },
                actions = {
                    AppButton(
                        text = Strings.GUIDE_BUTTON,
                        onClick = {
                            showReceiverOptions = false
                            guideManager.start(AppRole.RECEIVER)
                        },
                        variant = AppButtonVariant.TEXT,
                        size = AppButtonSize.SMALL,
                        icon = AppIcon.HELP,
                    )
                    AppButton(
                        text = Strings.CHANGE_ROLE,
                        onClick = { resetRole() },
                        variant = AppButtonVariant.OUTLINED,
                        size = AppButtonSize.SMALL,
                        icon = AppIcon.SWAP_HORIZ,
                    )
                    AppButton(
                        text = Strings.EXIT_APP,
                        onClick = {
                            showReceiverOptions = false
                            onExitRequested()
                        },
                        variant = AppButtonVariant.TEXT,
                        size = AppButtonSize.SMALL,
                        icon = AppIcon.LOGOUT,
                    )
                    AppButton(
                        text = Strings.RELOCK_BUTTON,
                        onClick = { closeReceiverOptions() },
                        size = AppButtonSize.SMALL,
                        icon = AppIcon.LOCK,
                    )
                },
            )
        }

        if (showUnpairConfirm) {
            AppDialog(
                title = Strings.UNPAIR_RECEIVER_TITLE,
                icon = AppIcon.LINK,
                onDismissRequest = {
                    showUnpairConfirm = false
                    relockReceiverIfNeeded()
                },
                content = {
                    M3Text(
                        text = Strings.UNPAIR_RECEIVER_CONFIRM,
                        style = M3MaterialTheme.typography.bodyLarge,
                        color = KioskColors.scheme.onSurfaceVariant,
                    )
                },
                actions = {
                    AppButton(
                        text = Strings.CANCEL_BUTTON,
                        onClick = {
                            showUnpairConfirm = false
                            relockReceiverIfNeeded()
                        },
                        variant = AppButtonVariant.TEXT,
                        size = AppButtonSize.SMALL,
                    )
                    AppButton(
                        text = Strings.UNPAIR_BUTTON,
                        onClick = {
                            pairingManager.unpair()
                            showUnpairConfirm = false
                            relockReceiverIfNeeded()
                        },
                        variant = AppButtonVariant.DANGER,
                        size = AppButtonSize.SMALL,
                        icon = AppIcon.LINK,
                    )
                },
            )
        }
    }
}
