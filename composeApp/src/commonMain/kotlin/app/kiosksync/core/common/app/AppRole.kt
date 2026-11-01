package app.kiosksync.core.common.app

import kotlinx.serialization.Serializable

@Serializable
enum class AppRole(val serializedName: String) {
    EMITTER("emitter"),
    RECEIVER("receiver");

    companion object {
        fun fromString(value: String): AppRole? = entries.firstOrNull { role ->
            role.serializedName.equals(value.trim(), ignoreCase = true)
        }
    }
}
