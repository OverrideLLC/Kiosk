package app.kiosksync.platform

import app.kiosksync.core.DeviceInfo
import app.kiosksync.core.PairResponse
import app.kiosksync.core.ReceiverStatusResponse

interface EmitterClient {
    suspend fun getInfo(host: String, port: Int): Result<DeviceInfo>
    suspend fun pair(host: String, port: Int, emitterId: String, emitterName: String, code: String): Result<PairResponse>
    suspend fun unpair(host: String, port: Int, emitterId: String, token: String): Result<Boolean>
    suspend fun getStatus(host: String, port: Int, emitterId: String, token: String): Result<ReceiverStatusResponse>
}
