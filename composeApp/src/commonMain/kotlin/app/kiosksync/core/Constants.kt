package app.kiosksync.core

const val APP_NAME = "KioskSync"
const val APP_VERSION = "1.0.0"
const val DEFAULT_PORT = 8765
const val MDNS_SERVICE_TYPE = "_kiosksync._tcp.local."
const val DEFAULT_DURATION_SEC = 10
const val MIN_DURATION_SEC = 3
const val MAX_DURATION_SEC = 600
const val MAX_IMAGE_SIZE_BYTES = 25L * 1024 * 1024 // 25 MB

const val HEADER_EMITTER_ID = "X-Emitter-Id"
const val HEADER_AUTH_TOKEN = "X-Auth-Token"

const val STATUS_POLL_INTERVAL_MS = 5_000L
const val STATUS_FAILURE_THRESHOLD = 3
const val HTTP_TIMEOUT_MS = 5_000L
