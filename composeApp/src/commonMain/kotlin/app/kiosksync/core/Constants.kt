package app.kiosksync.core

const val APP_NAME = "Override Kiosk"
const val APP_VERSION = "1.0.0"

/** Folder under %APPDATA% used when the user does not pass --data-dir. */
const val DATA_DIR_NAME = "OverrideKiosk"

const val DEFAULT_PORT = 8765
const val MDNS_SERVICE_TYPE = "_overridekiosk._tcp.local."
const val DEFAULT_DURATION_SEC = 10
const val MIN_DURATION_SEC = 3
const val MAX_DURATION_SEC = 600
const val MAX_IMAGE_SIZE_BYTES = 25L * 1024 * 1024 // 25 MB

const val HEADER_EMITTER_ID = "X-Emitter-Id"
const val HEADER_AUTH_TOKEN = "X-Auth-Token"
const val HEADER_CONTENT_SHA256 = "X-Content-Sha256"

const val STATUS_POLL_INTERVAL_MS = 5_000L
const val STATUS_FAILURE_THRESHOLD = 3
const val HTTP_TIMEOUT_MS = 5_000L
const val SYNC_TIMEOUT_MS = 60_000L
const val UPLOAD_TIMEOUT_MS = 120_000L

/**
 * Delay between the synchronisation command and the restart of the carousel.
 * A short lead gives every screen time to receive the command before the
 * common start, without relying on the clocks of the different PCs.
 */
const val PLAYBACK_SYNC_DELAY_MS = 2_000L
