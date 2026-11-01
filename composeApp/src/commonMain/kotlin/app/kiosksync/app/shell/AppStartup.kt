package app.kiosksync.app.shell

import app.kiosksync.app.di.AppSession
import app.kiosksync.core.common.app.AppRole
import app.kiosksync.core.common.app.AppTheme
import app.kiosksync.feature.role.domain.AppConfig

internal fun startupScreen(session: AppSession, deviceId: String, port: Int): Screen {
    val existing = session.initialConfig
    val forcedRole = session.args.role
    if (forcedRole != null) {
        return configScreen(
            AppConfig(
                role = forcedRole,
                deviceId = deviceId,
                deviceName = session.deviceName,
                port = port,
                theme = existing?.theme ?: AppTheme.DARK,
                reduceMotion = existing?.reduceMotion ?: false,
            ),
        )
    }
    if (existing != null) return configScreen(existing)
    return Screen.RoleSelection
}

internal fun configScreen(config: AppConfig): Screen = when (config.role) {
    AppRole.EMITTER -> Screen.EmitterHome(config)
    AppRole.RECEIVER -> Screen.ReceiverHome(config)
}

internal fun initialConfig(screen: Screen, existing: AppConfig?): AppConfig? = when (screen) {
    is Screen.EmitterHome -> screen.config
    is Screen.ReceiverHome -> screen.config
    Screen.RoleSelection -> existing
}
