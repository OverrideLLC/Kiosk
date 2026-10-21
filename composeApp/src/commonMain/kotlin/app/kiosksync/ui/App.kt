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
import app.kiosksync.feature.kiosk.KioskUnlockOverlay
import app.kiosksync.feature.pairing.PairingManager
import app.kiosksync.feature.playback.PlaybackStateHolder
import app.kiosksync.feature.role.AppConfig
import app.kiosksync.feature.role.RoleSelectionScreen
import app.kiosksync.feature.role.RoleStateHolder

@Composable
fun App(
    container: AppContainer,
    onExitRequested: () -> Unit = {},
    registerEscHandler: ((() -> Unit)?) -> Unit = {},
    registerUnpairHandler: ((() -> Unit)?) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val existingConfig = remember { container.localStorage.loadConfig() }
    val initialDeviceId = remember { existingConfig?.deviceId ?: container.newDeviceId() }
    val effectivePort = container.args.port ?: existingConfig?.port ?: DEFAULT_PORT
    val pairingManager: PairingManager = container.pairingManager

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
        onDispose {
            container.receiverPlaylistBridge.setListener(null)
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
    var showReceiverOptions by remember { mutableStateOf(false) }
    var showUnpairConfirm by remember { mutableStateOf(false) }
    var wasLocked by remember { mutableStateOf(lockState.isLocked) }

    fun relockReceiverIfNeeded() {
        if (currentScreen is Screen.ReceiverHome) {
            // The screen role is always locked, including --windowed/Linux where
            // the Compose key fallback handles the unlock sequence.
            container.kioskLockManager.lock(force = true)
        }
    }

    fun closeReceiverOptions() {
        showReceiverOptions = false
        relockReceiverIfNeeded()
    }

    fun resetRole() {
        roleStateHolder.confirmChangeRole()
        container.kioskLockManager.release()
        container.projectionController.stop()
        // Pairing is initialized only when the Receiver role is confirmed again,
        // so changing role does not generate an unused code on the UI thread.
        showReceiverOptions = false
        showUnpairConfirm = false
        currentScreen = Screen.RoleSelection
    }

    // The screen role is always a kiosk: lock the keyboard as soon as the
    // receiver UI is shown, including --windowed (Compose feeds the keys).
    LaunchedEffect(currentScreen) {
        when (currentScreen) {
            is Screen.ReceiverHome -> container.kioskLockManager.lock(force = true)

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
        } else if (previous && currentScreen is Screen.ReceiverHome) {
            showReceiverOptions = true
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

    LaunchedEffect(currentScreen) {
        if (currentScreen is Screen.ReceiverHome) {
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
                                if (confirmedConfig.role == AppRole.RECEIVER) {
                                    // Entering the Screen role always starts blank.
                                    container.localStorage.clearReceiverData()
                                    pairingManager.checkInitialPairing()
                                }
                                currentScreen = when (confirmedConfig.role) {
                                    AppRole.EMITTER -> Screen.EmitterHome(confirmedConfig)
                                    AppRole.RECEIVER -> Screen.ReceiverHome(confirmedConfig)
                                }
                            },
                            deviceId = initialDeviceId,
                            deviceName = container.deviceName,
                            port = effectivePort,
                            onExitRequested = onExitRequested,
                            theme = appTheme,
                            reduceMotion = reduceMotion
                        )
                    }

                    is Screen.EmitterHome -> {
                        EmitterHomeScreen(
                            config = screen.config,
                            container = container,
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
                            onExitRequested = onExitRequested,
                            onChangeRoleRequested = { resetRole() }
                        )
                    }
                }

                KioskUnlockOverlay(lockState)
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
