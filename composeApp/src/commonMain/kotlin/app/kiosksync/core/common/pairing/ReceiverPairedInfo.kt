package app.kiosksync.core.common.pairing

import kotlinx.serialization.Serializable

@Serializable
data class ReceiverPairedInfo(
    val emitterId: String,
    val emitterName: String,
    val token: String
)
