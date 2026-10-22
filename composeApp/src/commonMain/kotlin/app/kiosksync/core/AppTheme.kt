package app.kiosksync.core

import kotlinx.serialization.Serializable

/**
 * Persisted appearance preference. It lives in the app configuration so the app
 * remembers it between sessions. Dark is the default because kiosk screens
 * usually run in low-light rooms.
 */
@Serializable
enum class AppTheme {
    DARK,
    LIGHT
}
