package app.kiosksync.feature.role.domain

import app.kiosksync.core.common.app.AppRole
import app.kiosksync.core.common.app.AppTheme
import app.kiosksync.core.common.discovery.DEFAULT_PORT
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val role: AppRole,
    val deviceId: String,
    val deviceName: String,
    val port: Int = DEFAULT_PORT,
    val theme: AppTheme = AppTheme.DARK,
    val reduceMotion: Boolean = false
)
