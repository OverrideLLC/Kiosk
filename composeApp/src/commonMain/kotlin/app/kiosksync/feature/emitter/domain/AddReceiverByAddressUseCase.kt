package app.kiosksync.feature.emitter.domain

import app.kiosksync.core.DiscoveredDevice
import kotlinx.coroutines.CancellationException

data class ReceiverAddressDetails(
    val device: DiscoveredDevice,
    val skippedUpdateVersion: String?,
)

enum class ReceiverAddressFailure {
    INVALID_ADDRESS,
    NOT_A_RECEIVER,
    UNREACHABLE,
}

class ReceiverAddressException(
    val reason: ReceiverAddressFailure,
    cause: Throwable? = null,
) : Exception(reason.name, cause)

/** Resolves a manually entered address and verifies that it identifies a Receiver. */
class AddReceiverByAddressUseCase(
    private val client: EmitterClient,
) {
    suspend operator fun invoke(input: String): ReceiverAddressDetails {
        val (host, port) = parseAddress(input)
            ?: throw ReceiverAddressException(ReceiverAddressFailure.INVALID_ADDRESS)

        val info = client.getInfo(host, port).getOrElse { error ->
            if (error is CancellationException) throw error
            throw ReceiverAddressException(ReceiverAddressFailure.UNREACHABLE, error)
        }
        if (!info.isReceiver) {
            throw ReceiverAddressException(ReceiverAddressFailure.NOT_A_RECEIVER)
        }

        return ReceiverAddressDetails(
            device = DiscoveredDevice(
                deviceId = info.deviceId,
                deviceName = info.deviceName,
                host = host,
                port = port,
                paired = info.isPaired,
                appVersion = info.appVersion,
            ),
            skippedUpdateVersion = info.skippedUpdateVersion,
        )
    }

    private fun parseAddress(input: String): Pair<String, Int>? {
        val trimmed = input.trim()
            .removePrefix("http://")
            .removePrefix("https://")
        val separator = trimmed.lastIndexOf(':')
        if (separator <= 0) return null
        val host = trimmed.substring(0, separator).trim().trim('[', ']')
        val port = trimmed.substring(separator + 1).trim().toIntOrNull() ?: return null
        if (host.isBlank() || port !in 1..65535) return null
        return host to port
    }
}
