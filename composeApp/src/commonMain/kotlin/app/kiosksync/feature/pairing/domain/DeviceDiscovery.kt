package app.kiosksync.feature.pairing.domain

import app.kiosksync.core.DiscoveredDevice
import kotlinx.coroutines.flow.StateFlow

interface DeviceDiscovery {
    val discoveredDevices: StateFlow<List<DiscoveredDevice>>
    fun startDiscovery()
    fun stopDiscovery()

    /** Forces a new scan without waiting for the periodic mDNS announcements. */
    fun refresh()

    fun registerService(deviceName: String, port: Int, deviceId: String, isPaired: Boolean)
    fun unregisterService()
}
