package app.kiosksync.core.common.pairing

import kotlinx.serialization.Serializable

@Serializable
data class PairResponse(
    val token: String,
    val receiverId: String
)
