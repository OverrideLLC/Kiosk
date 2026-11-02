package app.kiosksync.app.shell

import app.kiosksync.app.di.AppSession
import app.kiosksync.core.common.app.AppTheme
import app.kiosksync.feature.role.domain.AppConfig

/**
 * Config present at launch. A forced role builds a config; otherwise the saved
 * one is returned, or null when the person still has to choose a role.
 */
internal fun startupConfig(session: AppSession, deviceId: String, port: Int): AppConfig? {
    val existing = session.initialConfig
    val forcedRole = session.args.role ?: return existing
    return AppConfig(
        role = forcedRole,
        deviceId = deviceId,
        deviceName = session.deviceName,
        port = port,
        theme = existing?.theme ?: AppTheme.DARK,
        reduceMotion = existing?.reduceMotion ?: false,
    )
}
