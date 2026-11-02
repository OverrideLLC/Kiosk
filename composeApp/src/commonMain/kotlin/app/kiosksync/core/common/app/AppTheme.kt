package app.kiosksync.core.common.app

import kotlinx.serialization.Serializable

/**
 * Persisted appearance preference. Dark is the default because kiosk screens
 * usually run in low-light rooms.
 */
@Serializable
enum class AppTheme {
    DARK,
    LIGHT
}
