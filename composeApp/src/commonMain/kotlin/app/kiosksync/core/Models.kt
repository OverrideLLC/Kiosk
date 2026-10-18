package app.kiosksync.core

import kotlinx.serialization.Serializable

@Serializable
enum class AppRole(val serializedName: String) {
    EMITTER("emitter"),
    RECEIVER("receiver");

    companion object {
        fun fromString(value: String): AppRole? = entries.firstOrNull {
            it.serializedName.equals(value.trim(), ignoreCase = true)
        }
    }
}

data class AppArgs(
    val dataDir: String? = null,
    val role: AppRole? = null,
    val port: Int? = null,
    val windowed: Boolean = false
) {
    companion object {
        fun parse(args: Array<String>): AppArgs {
            var dataDir: String? = null
            var role: AppRole? = null
            var port: Int? = null
            var windowed = false

            for (arg in args) {
                when {
                    arg.startsWith("--data-dir=") -> {
                        dataDir = arg.removePrefix("--data-dir=").trim('"', '\'')
                    }
                    arg.startsWith("--role=") -> {
                        val roleStr = arg.removePrefix("--role=").trim()
                        role = AppRole.fromString(roleStr)
                    }
                    arg.startsWith("--port=") -> {
                        port = arg.removePrefix("--port=").trim().toIntOrNull()
                    }
                    arg == "--windowed" -> {
                        windowed = true
                    }
                }
            }

            return AppArgs(
                dataDir = dataDir,
                role = role,
                port = port,
                windowed = windowed
            )
        }
    }
}
