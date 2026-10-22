package app.kiosksync.feature.role.domain

import app.kiosksync.core.AppRole
import app.kiosksync.core.AppTheme
import app.kiosksync.core.DEFAULT_PORT
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
