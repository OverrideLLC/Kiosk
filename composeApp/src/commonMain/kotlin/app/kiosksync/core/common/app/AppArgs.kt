package app.kiosksync.core.common.app

data class AppArgs(
    val dataDir: String? = null,
    val role: AppRole? = null,
    val port: Int? = null,
    val windowed: Boolean = false,
    val uiCatalog: Boolean = false
) {
    companion object {
        fun parse(args: Array<String>): AppArgs = args.fold(AppArgs()) { current, arg ->
            when {
                arg.startsWith("--data-dir=") ->
                    current.copy(dataDir = arg.removePrefix("--data-dir=").trim('"', '\''))
                arg.startsWith("--role=") ->
                    current.copy(role = AppRole.fromString(arg.removePrefix("--role=")))
                arg.startsWith("--port=") ->
                    current.copy(port = arg.removePrefix("--port=").trim().toIntOrNull())
                arg == "--windowed" -> current.copy(windowed = true)
                arg == "--ui-catalog" -> current.copy(uiCatalog = true)
                else -> current
            }
        }
    }
}
