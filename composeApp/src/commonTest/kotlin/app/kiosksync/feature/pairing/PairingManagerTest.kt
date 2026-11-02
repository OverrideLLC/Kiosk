package app.kiosksync.feature.pairing

import app.kiosksync.FakeRepositories
import app.kiosksync.NoOpLogger
import app.kiosksync.feature.pairing.domain.PairingManager
import app.kiosksync.feature.pairing.domain.PairResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PairingManagerTest {
    private var now = 1_000_000L
    private var nextCode = 100_000
    private var repositories = FakeRepositories()
    private val fixedToken = "0123456789abcdef".repeat(4)

    private suspend fun createManager(): PairingManager =
        PairingManager(
            repository = repositories,
            logger = NoOpLogger(),
            timeProvider = { now },
            tokenGenerator = { fixedToken },
            codeGenerator = { (nextCode++).toString() },
        ).also { it.initialize() }

    @Test
    fun testInitialStateGeneratesCode() = runBlocking {
        val manager = createManager()
        val state = manager.state.value
        assertFalse(state.isPaired)
        assertEquals("100000", state.currentCode)
        assertEquals(PairingManager.CODE_VALIDITY_MS / 1000, state.codeExpiresInSeconds)
        assertFalse(state.isLocked)
    }

    @Test
    fun testSuccessfulPairingStoresTokenAndLeavesPairingMode() = runBlocking {
        val manager = createManager()
        val result = manager.attemptPair("emitter-1", "Controlador", manager.state.value.currentCode)

        assertTrue(result is PairResult.Success)
        assertEquals(fixedToken, (result as PairResult.Success).token)
        assertEquals(fixedToken, repositories.receiverPairedInfo?.token)
        assertEquals("emitter-1", repositories.receiverPairedInfo?.emitterId)
        assertTrue(manager.state.value.isPaired)
        assertEquals("", manager.state.value.currentCode)
        assertEquals("", manager.getOrRefreshCode())
    }

    @Test
    fun testWrongCodeDecrementsRemainingAttempts() = runBlocking {
        val manager = createManager()
        val result = manager.attemptPair("emitter-1", "Controlador", "000000")
        assertEquals(PairResult.InvalidCode(4), result)
        assertFalse(manager.state.value.isPaired)
        assertNull(repositories.receiverPairedInfo)
    }

    @Test
    fun testExpiredCodeIsRenewed() = runBlocking {
        val manager = createManager()
        val firstCode = manager.state.value.currentCode
        now += PairingManager.CODE_VALIDITY_MS
        val refreshedCode = manager.getOrRefreshCode()
        assertNotEquals(firstCode, refreshedCode)
        assertEquals("100001", refreshedCode)
        assertTrue(manager.attemptPair("emitter-1", "Controlador", firstCode) is PairResult.InvalidCode)
        assertTrue(manager.attemptPair("emitter-1", "Controlador", refreshedCode) is PairResult.Success)
    }

    @Test
    fun testFiveFailuresLockOutAndRenewCode() = runBlocking {
        val manager = createManager()
        val firstCode = manager.state.value.currentCode
        var lastResult: PairResult? = null
        repeat(PairingManager.MAX_FAILED_ATTEMPTS) {
            lastResult = manager.attemptPair("emitter-1", "Controlador", "999999")
        }
        assertEquals(PairResult.LockedOut(30), lastResult)
        assertTrue(manager.state.value.isLocked)
        assertNotEquals(firstCode, manager.state.value.currentCode)

        val blocked = manager.attemptPair(
            "emitter-1",
            "Controlador",
            manager.state.value.currentCode,
        )
        assertTrue(blocked is PairResult.LockedOut)
        now += PairingManager.LOCKOUT_DURATION_MS
        manager.tick()
        assertFalse(manager.state.value.isLocked)
        assertTrue(
            manager.attemptPair(
                "emitter-1",
                "Controlador",
                manager.state.value.currentCode,
            ) is PairResult.Success,
        )
    }

    @Test
    fun testAlreadyPairedRejectsFurtherPairing() = runBlocking {
        val manager = createManager()
        manager.attemptPair("emitter-1", "Controlador", manager.state.value.currentCode)
        val secondAttempt = manager.attemptPair(
            "emitter-2",
            "Otro Controlador",
            manager.state.value.currentCode.ifEmpty { "123456" },
        )
        assertEquals(PairResult.AlreadyPaired, secondAttempt)
        assertEquals("emitter-1", repositories.receiverPairedInfo?.emitterId)
    }

    @Test
    fun testUnpairClearsStorageAndShowsNewCode() = runBlocking {
        val manager = createManager()
        manager.attemptPair("emitter-1", "Controlador", manager.state.value.currentCode)
        manager.unpair()
        assertNull(repositories.receiverPairedInfo)
        assertTrue(repositories.clearPairingCalled)
        val state = manager.state.value
        assertFalse(state.isPaired)
        assertTrue(state.currentCode.isNotBlank())
        assertFalse(manager.isAuthorized("emitter-1", fixedToken))
    }

    @Test
    fun testAuthorizationChecksEmitterAndToken() = runBlocking {
        val manager = createManager()
        manager.attemptPair("emitter-1", "Controlador", manager.state.value.currentCode)
        assertTrue(manager.isAuthorized("emitter-1", fixedToken))
        assertFalse(manager.isAuthorized("emitter-2", fixedToken))
        assertFalse(manager.isAuthorized("emitter-1", "wrong-token"))
        assertFalse(manager.isAuthorized(null, fixedToken))
        assertFalse(manager.isAuthorized("emitter-1", null))
    }

    @Test
    fun testConstantTimeEquals() {
        assertTrue(PairingManager.constantTimeEquals("abc123", "abc123"))
        assertFalse(PairingManager.constantTimeEquals("abc123", "abc124"))
        assertFalse(PairingManager.constantTimeEquals("abc", "abcdef"))
    }

    @Test
    fun testDefaultGeneratorsHaveExpectedFormat() {
        repeat(50) {
            assertEquals(6, PairingManager.defaultGenerateCode().length)
            val token = PairingManager.defaultGenerateToken()
            assertEquals(64, token.length)
            assertTrue(token.all { it in "0123456789abcdef" })
        }
    }
}
