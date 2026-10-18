package app.kiosksync.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.kiosksync.AppContainer
import app.kiosksync.core.AppRole
import app.kiosksync.core.DEFAULT_PORT
import app.kiosksync.feature.playback.CarouselScreen
import app.kiosksync.feature.playback.PlaybackStateHolder
import app.kiosksync.feature.role.AppConfig
import app.kiosksync.feature.role.RoleSelectionScreen
import app.kiosksync.feature.role.RoleStateHolder
import java.util.UUID

@Composable
fun App(
    container: AppContainer,
    onExitRequested: () -> Unit = {},
    registerEscHandler: ((() -> Unit)?) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val existingConfig = remember { container.localStorage.loadConfig() }
    val initialDeviceId = remember { existingConfig?.deviceId ?: UUID.randomUUID().toString() }
    val effectivePort = container.args.port ?: existingConfig?.port ?: DEFAULT_PORT

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
            scope = coroutineScope
        )
    }

    // Register Esc handler for receiver mode
    remember(currentScreen) {
        if (currentScreen is Screen.ReceiverHome) {
            registerEscHandler {
                playbackStateHolder.requestExitDialog()
            }
        } else {
            registerEscHandler(null)
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
                    RoleHomeScreen(
                        config = screen.config,
                        stateHolder = roleStateHolder,
                        onRoleReset = {
                            currentScreen = Screen.RoleSelection
                        },
                        onExitRequested = onExitRequested
                    )
                }

                is Screen.ReceiverHome -> {
                    CarouselScreen(
                        stateHolder = playbackStateHolder,
                        onExitRequested = onExitRequested,
                        onChangeRoleRequested = {
                            roleStateHolder.confirmChangeRole()
                            currentScreen = Screen.RoleSelection
                        }
                    )
                }
            }
        }
    }
}
