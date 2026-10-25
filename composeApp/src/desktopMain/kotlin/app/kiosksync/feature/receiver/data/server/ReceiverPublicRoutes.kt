package app.kiosksync.feature.receiver.data.server

import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.DeviceInfo
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

internal fun Route.registerReceiverPublicRoutes(context: ReceiverRouteContext) {
    get("/info") {
        call.respond(
            DeviceInfo(
                deviceId = context.deviceId(),
                deviceName = context.deviceName(),
                role = "receiver",
                paired = context.pairingManager.state.value.isPaired,
                appVersion = APP_VERSION,
                skippedUpdateVersion = context.updateManager.skippedVersion,
            ),
        )
    }
}
