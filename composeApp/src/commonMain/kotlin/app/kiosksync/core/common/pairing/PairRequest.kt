package app.kiosksync.core.common.pairing

import kotlinx.serialization.Serializable

@Serializable
data class PairRequest(
    val emitterId: String,
    val emitterName: String,
    val code: String
)
