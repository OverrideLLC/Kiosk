package app.kiosksync.platform

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/**
 * Best-effort primary IPv4 address for the receiver pairing screen. We prefer
 * a physical Ethernet/Wi-Fi adapter and a site-local address, which is the one
 * a controller on the same LAN can reach. Virtual adapters (Hyper-V, Docker,
 * VPNs) are ranked last because they often have an address that is not reachable
 * from the other PC.
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
                .filter { address ->
                    !address.isLoopbackAddress && !address.isLinkLocalAddress
                }
                .map { address -> networkInterface to address }
        }
        .toList()

    val ranked = candidates.sortedWith(
        compareByDescending<Pair<NetworkInterface, Inet4Address>> { (adapter, _) ->
            isPreferredAdapterName(adapter.name)
        }.thenBy { (adapter, _) ->
            isVirtualAdapterName(adapter.name)
        }
    )

    return ranked
        .firstOrNull { (_, address) -> address.isSiteLocalAddress }
        ?.second
        ?.hostAddress
        ?: ranked.firstOrNull()?.second?.hostAddress
}

private fun isPreferredAdapterName(name: String): Boolean {
    val lower = name.lowercase()
    return lower.startsWith("ethernet") ||
        lower.startsWith("eth") ||
        lower.startsWith("wi-fi") ||
        lower.startsWith("wifi") ||
        lower.startsWith("wlan")
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
