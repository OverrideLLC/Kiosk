package app.kiosksync.app.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.kiosksync.app.di.AppSession
import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.core.common.app.AppTheme
import app.kiosksync.feature.role.domain.AppConfig
import app.kiosksync.feature.role.domain.AppConfigStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

internal class AppPreferences(
    initial: AppConfig?,
    private val configStore: AppConfigStore,
    private val logger: AppLogger,
    private val scope: CoroutineScope,
) {
    var current by mutableStateOf(initial)
        private set
    var theme by mutableStateOf(initial?.theme ?: AppTheme.DARK)
        private set
    var reduceMotion by mutableStateOf(initial?.reduceMotion ?: false)
        private set

    fun persist(config: AppConfig) {
        current = config
        scope.launch {
            try {
                configStore.save(config)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not save application preferences", error)
            }
        }
    }

    fun update(transform: (AppConfig) -> AppConfig) {
        current?.let { persist(transform(it)) }
    }

    fun updateTheme(theme: AppTheme) {
        this.theme = theme
        update { it.copy(theme = theme) }
    }

    fun updateReduceMotion(enabled: Boolean) {
        reduceMotion = enabled
        update { it.copy(reduceMotion = enabled) }
    }

    fun clear() {
        current = null
    }
}

@Composable
internal fun PersistForcedRole(config: AppConfig?) {
    val session = koinInject<AppSession>()
    val configStore = koinInject<AppConfigStore>()
    val logger = koinInject<AppLogger>()
    LaunchedEffect(session.args.role) {
        if (session.args.role != null) {
            config?.let { current ->
                try {
                    configStore.save(current)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    logger.error("Could not persist the role forced by command line", error)
                }
            }
        }
    }
}
