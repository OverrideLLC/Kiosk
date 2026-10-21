package app.kiosksync.core

import kotlinx.serialization.Serializable

/**
 * Persisted appearance preference. It lives in `config.json` next to the role
 * so the app remembers it between sessions. Dark is the default because kiosk
 * screens usually run in low-light rooms.
 */
@Serializable
enum class AppTheme {
    DARK,
    LIGHT
}
