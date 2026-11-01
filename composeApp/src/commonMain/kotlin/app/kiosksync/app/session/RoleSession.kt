package app.kiosksync.app.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.kiosksync.app.di.AppQualifiers
import app.kiosksync.app.di.AppSession
import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.session.sessionViewModel
import app.kiosksync.app.shell.AppPreferences
import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.feature.emitter.domain.EmitterScreensRepository
import app.kiosksync.feature.guide.presentation.GuideAction
import app.kiosksync.feature.guide.presentation.GuideViewModel
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.kiosk.presentation.KioskAction
import app.kiosksync.feature.kiosk.presentation.KioskViewModel
import app.kiosksync.feature.pairing.domain.DeviceDiscovery
import app.kiosksync.feature.pairing.domain.PairingManager
import app.kiosksync.feature.projection.domain.ProjectionController
import app.kiosksync.feature.receiver.domain.ReceiverDataResetter
import app.kiosksync.feature.receiver.domain.ReceiverServer
import app.kiosksync.feature.role.domain.AppConfig
import app.kiosksync.feature.role.domain.AppConfigStore
import app.kiosksync.feature.role.presentation.RoleAction
import app.kiosksync.feature.role.presentation.RoleEffect
import app.kiosksync.feature.role.presentation.RoleViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

internal class RoleSession(
    val viewModel: RoleViewModel,
    private val scope: CoroutineScope,
    private val serviceDispatcher: CoroutineDispatcher,
    private val discovery: DeviceDiscovery,
    private val receiverServer: ReceiverServer,
    private val receiverDataResetter: ReceiverDataResetter,
    private val screensRepository: EmitterScreensRepository,
    private val pairingManager: PairingManager,
    private val kioskLockRepository: KioskLockRepository,
    private val configStore: AppConfigStore,
    private val projectionController: ProjectionController,
    private val logger: AppLogger,
    private val guideViewModel: GuideViewModel,
    private val kioskViewModel: KioskViewModel,
    private val preferences: AppPreferences,
    private val dialogs: ReceiverDialogs,
) {
    var isResetting by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set

    fun reset(route: AppRoute, onNavigate: (AppRoute) -> Unit) {
        if (isResetting) return
        val changingFromReceiver = route is AppRoute.ReceiverHome
        isResetting = true
        failed = false
        scope.launch {
            try {
                withContext(serviceDispatcher) {
                    discovery.unregisterService()
                    discovery.stopDiscovery()
                    receiverServer.stop()
                }
                if (changingFromReceiver) {
                    receiverDataResetter.clearReceiverData()
                } else {
                    screensRepository.clearPairedScreens()
                }
                pairingManager.unpair()
                kioskLockRepository.clearLock()
                configStore.clear()
                guideViewModel.onAction(GuideAction.Stop)
                viewModel.onAction(RoleAction.ResetSelection)
                preferences.clear()
                kioskViewModel.onAction(KioskAction.Release)
                projectionController.stop()
                dialogs.dismissAll()
                onNavigate(AppRoute.RoleSelection)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not reset local data while changing role", error)
                failed = true
                dialogs.showChangeRoleConfirm = true
            } finally {
                isResetting = false
            }
        }
    }
}

@Composable
internal fun rememberRoleSession(
    scope: CoroutineScope,
    preferences: AppPreferences,
    dialogs: ReceiverDialogs,
    deviceId: String,
    port: Int,
    onRoleConfirmed: (AppConfig) -> Unit,
): RoleSession {
    val session = koinInject<AppSession>()
    val viewModel = sessionViewModel<RoleViewModel>()
    val guideViewModel = sessionViewModel<GuideViewModel>()
    val kioskViewModel = sessionViewModel<KioskViewModel>()
    val serviceDispatcher = koinInject<CoroutineDispatcher>(AppQualifiers.serviceDispatcher)
    val discovery = koinInject<DeviceDiscovery>()
    val receiverServer = koinInject<ReceiverServer>()
    val receiverDataResetter = koinInject<ReceiverDataResetter>()
    val screensRepository = koinInject<EmitterScreensRepository>()
    val pairingManager = koinInject<PairingManager>()
    val kioskLockRepository = koinInject<KioskLockRepository>()
    val configStore = koinInject<AppConfigStore>()
    val projectionController = koinInject<ProjectionController>()
    val logger = koinInject<AppLogger>()
    val roleSession = remember(viewModel) {
        RoleSession(
            viewModel = viewModel,
            scope = scope,
            serviceDispatcher = serviceDispatcher,
            discovery = discovery,
            receiverServer = receiverServer,
            receiverDataResetter = receiverDataResetter,
            screensRepository = screensRepository,
            pairingManager = pairingManager,
            kioskLockRepository = kioskLockRepository,
            configStore = configStore,
            projectionController = projectionController,
            logger = logger,
            guideViewModel = guideViewModel,
            kioskViewModel = kioskViewModel,
            preferences = preferences,
            dialogs = dialogs,
        )
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RoleEffect.RoleConfirmed -> {
                    val config = AppConfig(
                        role = effect.role,
                        deviceId = deviceId,
                        deviceName = session.deviceName,
                        port = port,
                        theme = preferences.theme,
                        reduceMotion = preferences.reduceMotion,
                    )
                    preferences.persist(config)
                    onRoleConfirmed(config)
                    guideViewModel.onAction(GuideAction.RoleConfirmed(config.role))
                }
            }
        }
    }
    return roleSession
}
