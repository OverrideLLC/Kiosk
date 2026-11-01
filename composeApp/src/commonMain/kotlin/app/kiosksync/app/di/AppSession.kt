package app.kiosksync.app.di

import app.kiosksync.core.common.app.AppArgs
import app.kiosksync.core.common.app.AppRole
import app.kiosksync.feature.role.domain.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process values resolved during startup and the role shown by the window host. */
class AppSession(
    val args: AppArgs,
    val dataDirPath: String,
    val deviceName: String,
    val platformLockAvailable: Boolean,
) {
    var initialConfig: AppConfig? = null
        private set
    var localIpAddress: String? = null
        private set

    private val _activeRole = MutableStateFlow<AppRole?>(null)
    val activeRole: StateFlow<AppRole?> = _activeRole.asStateFlow()

    fun updateActiveRole(role: AppRole?) {
        _activeRole.value = role
    }

    internal fun publishStartup(config: AppConfig?, address: String?) {
        initialConfig = config
        localIpAddress = address
        _activeRole.value = args.role ?: config?.role
    }
}
