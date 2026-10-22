package app.kiosksync.feature.pairing

import app.kiosksync.core.AppLogger
import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.feature.pairing.domain.PairingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

sealed interface PairResult {
    data class Success(val token: String) : PairResult
    data class InvalidCode(val remainingAttempts: Int) : PairResult
    data class LockedOut(val remainingSeconds: Long) : PairResult
    data object AlreadyPaired : PairResult
}

data class PairingUiState(
    val isPaired: Boolean = false,
    val pairedInfo: ReceiverPairedInfo? = null,
    val currentCode: String = "",
    val codeExpiresInSeconds: Long = 0,
    val isLocked: Boolean = false,
    val lockRemainingSeconds: Long = 0
)

class PairingManager(
    private val repository: PairingRepository,
    private val logger: AppLogger,
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
    private val tokenGenerator: () -> String = { defaultGenerateToken() },
    private val codeGenerator: () -> String = { defaultGenerateCode() },
) {
    companion object {
        const val CODE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30 * 1000L // 30 seconds

        fun defaultGenerateCode(): String =
            Random.nextInt(100_000, 1_000_000).toString()

        fun defaultGenerateToken(): String {
            val hexChars = "0123456789abcdef"
            val sb = StringBuilder(64)
            for (i in 0 until 64) {
                sb.append(hexChars[Random.nextInt(hexChars.length)])
            }
            return sb.toString()
        }

        fun constantTimeEquals(a: String, b: String): Boolean {
            if (a.length != b.length) return false
            var result = 0
            for (i in a.indices) {
                result = result or (a[i].code xor b[i].code)
            }
            return result == 0
        }
    }

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    private var activeCode: String = ""
    private var codeCreatedAtMs: Long = 0
    private var failedAttempts: Int = 0
    private var lockedUntilMs: Long = 0

    private val pairingMutex = Mutex()
    private var isInitialized = false

    suspend fun initialize() = pairingMutex.withLock {
        if (isInitialized) return@withLock
        val pairedInfo = repository.loadPairedEmitter()
        if (pairedInfo != null) {
            _uiState.update {
                it.copy(
                    isPaired = true,
                    pairedInfo = pairedInfo,
                    currentCode = "",
                    codeExpiresInSeconds = 0,
                    isLocked = false
                )
            }
            logger.info("Receiver is already paired with emitter '${pairedInfo.emitterName}' (id: ${pairedInfo.emitterId})")
        } else {
            failedAttempts = 0
            lockedUntilMs = 0
            generateNewCode()
        }
        isInitialized = true
    }

    suspend fun getOrRefreshCode(): String = pairingMutex.withLock {
        if (_uiState.value.isPaired) {
            return@withLock ""
        }
        val now = timeProvider()
        if (activeCode.isEmpty() || now - codeCreatedAtMs >= CODE_VALIDITY_MS) {
            generateNewCode()
        }
        activeCode
    }

    /**
     * Updates countdowns and renews the code when it expires. Meant to be called
     * once per second by the receiver UI.
     */
    suspend fun tick() = pairingMutex.withLock {
        if (_uiState.value.isPaired) {
            return@withLock
        }

        val now = timeProvider()
        if (now < lockedUntilMs) {
            val remainingSec = ((lockedUntilMs - now) + 999L) / 1000L
            _uiState.update {
                it.copy(isLocked = true, lockRemainingSeconds = remainingSec)
            }
            return@withLock
        }

        if (_uiState.value.isLocked) {
            _uiState.update { it.copy(isLocked = false, lockRemainingSeconds = 0) }
        }

        if (activeCode.isEmpty() || now - codeCreatedAtMs >= CODE_VALIDITY_MS) {
            generateNewCode()
        } else {
            val expiresInSec = (CODE_VALIDITY_MS - (now - codeCreatedAtMs)) / 1000L
            _uiState.update {
                it.copy(
                    codeExpiresInSeconds = expiresInSec.coerceAtLeast(0),
                    isLocked = false,
                    lockRemainingSeconds = 0
                )
            }
        }
    }

    suspend fun attemptPair(emitterId: String, emitterName: String, code: String): PairResult = pairingMutex.withLock {
        if (_uiState.value.isPaired) {
            return@withLock PairResult.AlreadyPaired
        }

        val now = timeProvider()

        // Check if currently locked out
        if (now < lockedUntilMs) {
            val remainingSec = ((lockedUntilMs - now) / 1000L).coerceAtLeast(1)
            logger.warn("Pair attempt rejected: locked out for $remainingSec more seconds")
            return@withLock PairResult.LockedOut(remainingSec)
        }

        // Check if code has expired
        if (now - codeCreatedAtMs >= CODE_VALIDITY_MS) {
            logger.info("Pairing code expired. Generating a new code.")
            generateNewCode()
        }

        if (constantTimeEquals(activeCode, code.trim())) {
            // Success!
            val token = tokenGenerator()
            val pairedInfo = ReceiverPairedInfo(
                emitterId = emitterId,
                emitterName = emitterName,
                token = token
            )
            repository.savePairedEmitter(pairedInfo)
            failedAttempts = 0
            activeCode = ""

            _uiState.update {
                it.copy(
                    isPaired = true,
                    pairedInfo = pairedInfo,
                    currentCode = "",
                    codeExpiresInSeconds = 0,
                    isLocked = false
                )
            }
            logger.info("Pairing successful with emitter '$emitterName' ($emitterId)")
            return@withLock PairResult.Success(token)
        } else {
            // Failure
            failedAttempts++
            logger.warn("Failed pairing attempt $failedAttempts/$MAX_FAILED_ATTEMPTS for emitter '$emitterName'")

            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                lockedUntilMs = now + LOCKOUT_DURATION_MS
                failedAttempts = 0
                generateNewCode()
                val remainingSec = (LOCKOUT_DURATION_MS / 1000L)
                _uiState.update {
                    it.copy(
                        isLocked = true,
                        lockRemainingSeconds = remainingSec
                    )
                }
                return@withLock PairResult.LockedOut(remainingSec)
            } else {
                val remaining = MAX_FAILED_ATTEMPTS - failedAttempts
                return@withLock PairResult.InvalidCode(remaining)
            }
        }
    }

    suspend fun unpair(): Boolean = pairingMutex.withLock {
        logger.info("Unpairing receiver. Clearing paired info and returning to pairing mode.")
        repository.clearPairedEmitter()
        failedAttempts = 0
        lockedUntilMs = 0
        generateNewCode()
        isInitialized = true
        true
    }

    fun isAuthorized(emitterId: String?, token: String?): Boolean {
        if (emitterId == null || token == null) return false
        val paired = _uiState.value.pairedInfo ?: return false
        return constantTimeEquals(paired.emitterId, emitterId) && constantTimeEquals(paired.token, token)
    }

    private fun generateNewCode() {
        activeCode = codeGenerator()
        codeCreatedAtMs = timeProvider()
        val expiresInSec = CODE_VALIDITY_MS / 1000L

        _uiState.update {
            it.copy(
                isPaired = false,
                pairedInfo = null,
                currentCode = activeCode,
                codeExpiresInSeconds = expiresInSec,
                isLocked = false
            )
        }
        // Never write the code to the log: the log is a local file and support
        // bundles usually include it.
        logger.info("New pairing code generated (valid for 5 minutes)")
    }
}
