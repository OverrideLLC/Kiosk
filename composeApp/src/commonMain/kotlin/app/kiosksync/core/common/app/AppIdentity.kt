package app.kiosksync.core.common.app

const val APP_NAME = "Override Kiosk"
const val OVERRIDE_WEBSITE = "https://override.com.mx"
const val APP_VERSION = "1.1.3"

/** Name produced by jpackage; used to relaunch after an in-place update. */
const val APP_EXECUTABLE_NAME = "OverrideKiosk.exe"

/** Folder inside the installation that contains the replaceable application payload. */
const val APP_PAYLOAD_DIR_NAME = "app"

/** Folder under %APPDATA% used when the user does not pass --data-dir. */
const val DATA_DIR_NAME = "OverrideKiosk"
