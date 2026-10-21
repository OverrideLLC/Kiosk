package app.kiosksync.platform

import app.kiosksync.core.AppLogger
import app.kiosksync.core.DiscoveredDevice
import app.kiosksync.core.MDNS_SERVICE_TYPE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

class JmDnsDiscovery(
    private val logger: AppLogger
) : DeviceDiscovery {

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private var jmDns: JmDNS? = null
    private var registeredService: ServiceInfo? = null
    private var listenerRegistered = false

    private val devices = mutableMapOf<String, DiscoveredDevice>()
    private val serviceNames = mutableMapOf<String, String>()

    private val listener = object : ServiceListener {
        override fun serviceAdded(event: ServiceEvent) {
            jmDns?.requestServiceInfo(event.type, event.name, true)
        }

        override fun serviceRemoved(event: ServiceEvent) {
            removeService(event.name)
        }

        override fun serviceResolved(event: ServiceEvent) {
            handleResolved(event.info)
        }
    }

    @Synchronized
    override fun startDiscovery() {
        val instance = ensureJmDns() ?: return
        if (listenerRegistered) return
        try {
            instance.addServiceListener(MDNS_SERVICE_TYPE, listener)
            listenerRegistered = true
            logger.info("mDNS discovery started for $MDNS_SERVICE_TYPE")
        } catch (error: Exception) {
            logger.error("Could not start mDNS discovery", error)
        }
    }

    @Synchronized
    override fun stopDiscovery() {
        val instance = jmDns ?: return
        if (listenerRegistered) {
            runCatching { instance.removeServiceListener(MDNS_SERVICE_TYPE, listener) }
            listenerRegistered = false
        }
        runCatching { instance.close() }
            .onFailure { logger.warn("Error closing JmDNS: ${it.message}") }
        jmDns = null
        registeredService = null
        devices.clear()
        serviceNames.clear()
        publish()
        logger.info("mDNS discovery stopped")
    }

    /**
     * Re-browses the network. mDNS announces services asynchronously, so this
     * lets the person force a scan instead of waiting for the next announcement.
     * Re-adding the listener triggers a fresh browse; the known names are asked
     * again so their address and port are refreshed.
     */
    @Synchronized
    override fun refresh() {
        val instance = jmDns
        if (instance == null || !listenerRegistered) {
            startDiscovery()
            return
        }
        logger.info("Manual mDNS refresh requested")
        runCatching { instance.removeServiceListener(MDNS_SERVICE_TYPE, listener) }
        listenerRegistered = false
        runCatching {
            instance.addServiceListener(MDNS_SERVICE_TYPE, listener)
            listenerRegistered = true
        }
        serviceNames.keys.toList().forEach { name ->
            runCatching { instance.requestServiceInfo(MDNS_SERVICE_TYPE, name, true) }
        }
    }

    @Synchronized
    override fun registerService(deviceName: String, port: Int, deviceId: String, isPaired: Boolean) {
        val instance = ensureJmDns() ?: return
        try {
            registeredService?.let { previous ->
                runCatching { instance.unregisterService(previous) }
            }
            val properties = mapOf(
                "deviceId" to deviceId,
                "deviceName" to deviceName,
                "role" to "receiver",
                "paired" to isPaired.toString()
            )
            val info = ServiceInfo.create(
                MDNS_SERVICE_TYPE,
                "KioskSync-${deviceId.take(8)}",
                port,
                0,
                0,
                properties
            )
            instance.registerService(info)
            registeredService = info
            logger.info("mDNS service registered: $deviceName on port $port (paired=$isPaired)")
        } catch (error: Exception) {
            logger.error("Could not register the mDNS service", error)
        }
    }

    @Synchronized
    override fun unregisterService() {
        val instance = jmDns ?: return
        registeredService?.let { info ->
            runCatching { instance.unregisterService(info) }
                .onFailure { logger.warn("Could not unregister mDNS service: ${it.message}") }
        }
        registeredService = null
        logger.info("mDNS service unregistered")
    }

    private fun ensureJmDns(): JmDNS? {
        jmDns?.let { return it }
        return try {
            JmDNS.create().also {
                jmDns = it
                logger.info("JmDNS initialized")
            }
        } catch (error: Exception) {
            logger.error("Could not initialize JmDNS", error)
            null
        }
    }

    @Synchronized
    private fun handleResolved(info: ServiceInfo) {
        if (info.getPropertyString("role") != "receiver") {
            return
        }
        val deviceId = info.getPropertyString("deviceId") ?: return

        val addresses = info.inetAddresses
        val address = addresses?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?: addresses?.firstOrNull { it is Inet4Address }
            ?: addresses?.firstOrNull { !it.isLinkLocalAddress && !it.isLoopbackAddress }
            ?: return
        val host = address.hostAddress ?: return

        val device = DiscoveredDevice(
            deviceId = deviceId,
            deviceName = info.getPropertyString("deviceName") ?: info.name,
            host = host,
            port = info.port,
            paired = info.getPropertyString("paired") == "true"
        )

        devices[deviceId] = device
        serviceNames[info.name] = deviceId
        publish()
        logger.info("Discovered screen '${device.deviceName}' at $host:${device.port} (paired=${device.paired})")
    }

    @Synchronized
    private fun removeService(serviceName: String) {
        val deviceId = serviceNames.remove(serviceName) ?: return
        val removed = devices.remove(deviceId) ?: return
        publish()
        logger.info("Screen '${removed.deviceName}' is no longer visible")
    }

    private fun publish() {
        _discoveredDevices.value = devices.values.sortedBy { it.deviceName.lowercase() }
    }
}
