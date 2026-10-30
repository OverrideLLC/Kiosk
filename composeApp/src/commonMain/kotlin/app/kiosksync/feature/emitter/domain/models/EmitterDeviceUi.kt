package app.kiosksync.feature.emitter.domain.models

data class EmitterDeviceUi(
    val deviceId: String,
    val deviceName: String,
    val host: String,
    val port: Int,
    val isPairedToThisEmitter: Boolean,
    val isPairedToAnotherEmitter: Boolean,
    val isOnline: Boolean,
    val isSelected: Boolean,
    val appVersion: String? = null
)
