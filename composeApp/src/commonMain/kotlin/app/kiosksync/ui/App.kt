package app.kiosksync.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.darkColors
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import app.kiosksync.AppContainer
import app.kiosksync.core.AppRole
import app.kiosksync.core.DEFAULT_PORT
import app.kiosksync.core.Strings
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

    // Determine initial screen based on CLI args and saved config
    val initialScreen = remember {
        val forcedRole = container.args.role
        if (forcedRole != null) {
            val config = AppConfig(
                role = forcedRole,
                deviceId = initialDeviceId,
                deviceName = container.deviceName,
                port = effectivePort
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
        if (currentScreen is Screen.ReceiverHome && !container.args.windowed) {
            container.kioskLockManager.lock()
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
    // receiver UI is shown, unless --windowed was passed for debugging.
    LaunchedEffect(currentScreen) {
        when (currentScreen) {
            is Screen.ReceiverHome -> {
                if (!container.args.windowed) {
                    container.kioskLockManager.lock()
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

    // Unlocking the receiver temporarily opens the admin dialog. Closing it
    // re-engages the lock.
    LaunchedEffect(lockState.isLocked) {
        val previous = wasLocked
        wasLocked = lockState.isLocked
        if (previous && !lockState.isLocked && currentScreen is Screen.ReceiverHome) {
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

    MaterialTheme(
        colors = darkColors(
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            primary = Color(0xFF3D7AF5),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (val screen = currentScreen) {
                    is Screen.RoleSelection -> {
                        RoleSelectionScreen(
                            stateHolder = roleStateHolder,
                            onRoleConfirmed = { confirmedConfig ->
                                if (confirmedConfig.role == AppRole.RECEIVER) {
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
                            onExitRequested = onExitRequested
                        )
                    }

                    is Screen.EmitterHome -> {
                        EmitterHomeScreen(
                            config = screen.config,
                            container = container,
                            onExitRequested = onExitRequested,
                            onRoleReset = { resetRole() }
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
    }

    if (showReceiverOptions) {
        AlertDialog(
            onDismissRequest = { closeReceiverOptions() },
            title = {
                Text(Strings.OPTIONS_TITLE, color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    if (currentScreen is Screen.ReceiverHome) {
                        Strings.RECEIVER_UNLOCKED_MESSAGE
                    } else {
                        Strings.OPTIONS_MESSAGE
                    },
                    color = Color(0xFFCCCCCC)
                )
            },
            confirmButton = {
                Row {
                    OutlinedButton(
                        onClick = { resetRole() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(Strings.CHANGE_ROLE)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            showReceiverOptions = false
                            onExitRequested()
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        )
                    ) {
                        Text(Strings.EXIT_APP)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { closeReceiverOptions() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9EC1FF))
                    ) {
                        Text(Strings.RELOCK_BUTTON)
                    }
                }
            },
            backgroundColor = Color(0xFF242424)
        )
    }

    if (showUnpairConfirm) {
        AlertDialog(
            onDismissRequest = {
                showUnpairConfirm = false
                relockReceiverIfNeeded()
            },
            title = {
                Text(Strings.UNPAIR_RECEIVER_TITLE, color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(Strings.UNPAIR_RECEIVER_CONFIRM, color = Color(0xFFCCCCCC))
            },
            confirmButton = {
                Button(
                    onClick = {
                        pairingManager.unpair()
                        showUnpairConfirm = false
                        relockReceiverIfNeeded()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text(Strings.UNPAIR_BUTTON)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showUnpairConfirm = false
                        relockReceiverIfNeeded()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(Strings.CANCEL_BUTTON)
                }
            },
            backgroundColor = Color(0xFF242424)
        )
    }
}
