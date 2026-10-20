package app.kiosksync.ui

import androidx.compose.foundation.layout.fillMaxSize
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
    var showReceiverOptions by remember { mutableStateOf(false) }
    var showUnpairConfirm by remember { mutableStateOf(false) }

    fun resetRole() {
        roleStateHolder.confirmChangeRole()
        // Pairing is initialized only when the Receiver role is confirmed again,
        // so changing role does not generate an unused code on the UI thread.
        showReceiverOptions = false
        showUnpairConfirm = false
        currentScreen = Screen.RoleSelection
    }

    LaunchedEffect(currentScreen, pairingState.isPaired) {
        if (currentScreen is Screen.ReceiverHome) {
            registerEscHandler {
                if (pairingState.isPaired) {
                    playbackStateHolder.requestExitDialog()
                } else {
                    showReceiverOptions = true
                }
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
        }
    }

    if (showReceiverOptions) {
        AlertDialog(
            onDismissRequest = { showReceiverOptions = false },
            title = {
                Text(Strings.OPTIONS_TITLE, color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(Strings.OPTIONS_MESSAGE, color = Color(0xFFCCCCCC))
            },
            confirmButton = {
                Button(
                    onClick = { resetRole() },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF333333),
                        contentColor = Color.White
                    )
                ) {
                    Text(Strings.CHANGE_ROLE)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showReceiverOptions = false
                        onExitRequested()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
                ) {
                    Text(Strings.EXIT_APP)
                }
            },
            backgroundColor = Color(0xFF242424)
        )
    }

    if (showUnpairConfirm) {
        AlertDialog(
            onDismissRequest = { showUnpairConfirm = false },
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
                    onClick = { showUnpairConfirm = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(Strings.CANCEL_BUTTON)
                }
            },
            backgroundColor = Color(0xFF242424)
        )
    }
}
