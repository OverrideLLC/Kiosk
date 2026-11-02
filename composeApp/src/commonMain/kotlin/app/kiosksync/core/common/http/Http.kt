package app.kiosksync.core.common.http

const val HEADER_EMITTER_ID = "X-Emitter-Id"
const val HEADER_AUTH_TOKEN = "X-Auth-Token"
const val HEADER_CONTENT_SHA256 = "X-Content-Sha256"

const val STATUS_POLL_INTERVAL_MS = 5_000L
const val STATUS_FAILURE_THRESHOLD = 3
const val HTTP_TIMEOUT_MS = 5_000L
const val SYNC_TIMEOUT_MS = 60_000L
const val UPLOAD_TIMEOUT_MS = 120_000L
const val UPDATE_UPLOAD_TIMEOUT_MS = 300_000L
