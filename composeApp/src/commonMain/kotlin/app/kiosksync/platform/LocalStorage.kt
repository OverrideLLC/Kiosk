package app.kiosksync.platform

import app.kiosksync.feature.role.AppConfig

interface LocalStorage {
    fun loadConfig(): AppConfig?
    fun saveConfig(config: AppConfig)
    fun clearConfig()
    fun clearPairing()
}
