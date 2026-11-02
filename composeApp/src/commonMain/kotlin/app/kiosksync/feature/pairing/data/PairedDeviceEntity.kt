package app.kiosksync.feature.pairing.data

import androidx.room3.Entity

@Entity(
    tableName = "paired_devices",
    primaryKeys = ["kind", "deviceId"]
)
data class PairedDeviceEntity(
    val kind: String,
    val deviceId: String,
    val deviceName: String,
    val tokenCiphertext: String,
    val host: String?,
    val port: Int?
)
