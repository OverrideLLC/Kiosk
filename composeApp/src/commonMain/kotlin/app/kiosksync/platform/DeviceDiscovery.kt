package app.kiosksync.platform

import app.kiosksync.core.DiscoveredDevice
import kotlinx.coroutines.flow.StateFlow

interface DeviceDiscovery {
    val discoveredDevices: StateFlow<List<DiscoveredDevice>>
    fun startDiscovery()
    fun stopDiscovery()
    fun registerService(deviceName: String, port: Int, deviceId: String, isPaired: Boolean)
    fun unregisterService()
}
