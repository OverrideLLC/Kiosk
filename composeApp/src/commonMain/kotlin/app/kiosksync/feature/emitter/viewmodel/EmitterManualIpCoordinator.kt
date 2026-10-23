package app.kiosksync.feature.emitter.viewmodel

import app.kiosksync.feature.emitter.state.EmitterUiState

import app.kiosksync.core.AppLogger
import app.kiosksync.core.DiscoveredDevice
import app.kiosksync.core.Strings
import app.kiosksync.platform.EmitterClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns the manual IP validation flow for adding a Receiver to discovery results. */
internal class EmitterManualIpCoordinator(
    private val uiState: MutableStateFlow<EmitterUiState>,
    private val client: EmitterClient,
    private val logger: AppLogger,
    private val scope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val onDeviceInfo: (deviceId: String, appVersion: String, skippedVersion: String?) -> Unit,
    private val onDeviceAdded: (DiscoveredDevice) -> Unit,
    private val parseHostPort: (String) -> Pair<String, Int>?,
) {
    fun confirm() {
        val state = uiState.value
        if (state.isManualIpLoading) return
        val parsed = parseHostPort(state.manualIpInput)
        if (parsed == null) {
            uiState.update { it.copy(manualIpError = Strings.MANUAL_IP_INVALID) }
            return
        }

        val (host, port) = parsed
        scope.launch {
            uiState.update { it.copy(isManualIpLoading = true, manualIpError = null) }
            val result = withContext(backgroundDispatcher) { client.getInfo(host, port) }
            result.fold(
                onSuccess = { info ->
                    if (info.role != "receiver") {
                        uiState.update {
                            it.copy(
                                isManualIpLoading = false,
                                manualIpError = Strings.MANUAL_IP_NOT_RECEIVER,
                            )
                        }
                        return@fold
                    }
                    onDeviceInfo(info.deviceId, info.appVersion, info.skippedUpdateVersion)
                    onDeviceAdded(
                        DiscoveredDevice(
                            deviceId = info.deviceId,
                            deviceName = info.deviceName,
                            host = host,
                            port = port,
                            paired = info.paired,
                            appVersion = info.appVersion,
                        ),
                    )
                    logger.info("Screen '${info.deviceName}' added manually at $host:$port")
                    uiState.update {
                        it.copy(
                            showManualIpDialog = false,
                            manualIpInput = "",
                            manualIpError = null,
                            isManualIpLoading = false,
                        )
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    logger.warn("Manual screen $host:$port unreachable: ${error.message}")
                    uiState.update {
                        it.copy(
                            isManualIpLoading = false,
                            manualIpError = Strings.MANUAL_IP_UNREACHABLE,
                        )
                    }
                },
            )
        }
    }
}
