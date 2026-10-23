package app.kiosksync.feature.pairing.data

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/**
 * Best-effort primary IPv4 address for the receiver pairing screen. Wi-Fi is
 * preferred because that is the address the person normally writes on the
 * Controller. Link-local (169.254.x.x) addresses are kept as a last resort so
 * the screen can still show the Wi-Fi IP when DHCP failed.
 */
fun detectLocalIpv4Address(): String? {
    val interfaces = runCatching {
        Collections.list(NetworkInterface.getNetworkInterfaces())
    }.getOrDefault(emptyList())

    val candidates = interfaces
        .asSequence()
        .filter { networkInterface ->
            runCatching {
                networkInterface.isUp && !networkInterface.isLoopback
            }.getOrDefault(false)
        }
        .flatMap { networkInterface ->
            Collections.list(networkInterface.inetAddresses)
                .asSequence()
                .filterIsInstance<Inet4Address>()
                .filter { address -> !address.isLoopbackAddress }
                .map { address -> networkInterface to address }
        }
        .toList()

    val ranked = candidates.sortedWith(
        compareBy<Pair<NetworkInterface, Inet4Address>> { (adapter, _) ->
            adapterPriority(adapter.name)
        }.thenByDescending { (_, address) -> address.isSiteLocalAddress }
    )

    // Always show the Wi-Fi address when a Wi-Fi adapter exists, even if it is
    // link-local; that is exactly the address the person needs to diagnose.
    return ranked
        .firstOrNull { (adapter, _) -> isWifiAdapterName(adapter.name) }
        ?.second
        ?.hostAddress
        ?: ranked.firstOrNull { (_, address) -> address.isSiteLocalAddress }
            ?.second
            ?.hostAddress
        ?: ranked.firstOrNull()?.second?.hostAddress
}

private fun adapterPriority(name: String): Int {
    val lower = name.lowercase()
    return when {
        isWifiAdapterName(lower) -> 0
        lower.startsWith("ethernet") || lower.startsWith("eth") -> 1
        isVirtualAdapterName(lower) -> 3
        else -> 2
    }
}

private fun isWifiAdapterName(name: String): Boolean {
    val lower = name.lowercase()
    return lower.startsWith("wi-fi") ||
        lower.startsWith("wifi") ||
        lower.startsWith("wlan") ||
        lower.contains("wireless")
}

private fun isVirtualAdapterName(name: String): Boolean {
    val lower = name.lowercase()
    return listOf(
        "vethernet",
        "virtual",
        "vmware",
        "virtualbox",
        "docker",
        "wsl",
        "tailscale",
        "zerotier",
        "loopback",
        "teredo",
        "isatap",
        "bluetooth",
        "vpn"
    ).any { hint -> lower.contains(hint) }
}
