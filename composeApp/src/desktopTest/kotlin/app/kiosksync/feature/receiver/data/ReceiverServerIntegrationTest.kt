package app.kiosksync.feature.receiver.data

import app.kiosksync.FakeRepositories
import app.kiosksync.FakeReceiverUpdateManager
import app.kiosksync.NoOpLogger
import app.kiosksync.data.security.Sha256
import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.ReceiverStatusResponse
import app.kiosksync.feature.emitter.domain.PlaylistRejectedException
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.pairing.domain.PairingManager
import app.kiosksync.feature.emitter.data.KtorEmitterClient
import app.kiosksync.feature.playback.domain.ReceiverPlaybackSyncBridge
import app.kiosksync.feature.playback.domain.ReceiverPlaylistBridge
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.domain.PlaylistItem
import java.net.ServerSocket
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

/**
 * End-to-end test of the real Ktor server and client on loopback: discovery
 * info, pairing, auth, upload with hash verification, sync state, atomic
 * playlist apply and unpair.
 */
class ReceiverServerIntegrationTest {

    @Test
    fun fullPairingAndSyncFlow() = runBlocking {
        val storage = FakeRepositories(hashProvider = { Sha256.hex(it) })
        val pairing = PairingManager(
            repository = storage,
            logger = NoOpLogger(),
            codeGenerator = { "123456" },
            tokenGenerator = { "a".repeat(64) },
        )
        pairing.initialize()
        val bridge = ReceiverPlaylistBridge()
        var appliedPlaylist: Playlist? = null
        bridge.setListener { appliedPlaylist = it }
        val playbackSyncBridge = ReceiverPlaybackSyncBridge()
        var playbackSyncDelay: Long? = null
        playbackSyncBridge.setListener { delayMs ->
            playbackSyncDelay = delayMs
            true
        }
        val lockScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val lock = KioskLockManager(
            logger = NoOpLogger(),
            passwordHasher = { it },
            scope = lockScope,
            platformLockAvailable = false,
        )
        val updateManager = FakeReceiverUpdateManager()
        val server = KtorReceiverServer(
            pairingManager = pairing,
            logger = NoOpLogger(),
            playlistRepository = storage,
            mediaRepository = storage,
            kioskLockRepository = storage,
            playlistBridge = bridge,
            playbackSyncBridge = playbackSyncBridge,
            kioskLockManager = lock,
            statusProvider = {
                ReceiverStatusResponse(
                    playlistVersion = storage.playlist?.version ?: 0,
                )
            },
            updateManager = updateManager,
        )
        val port = ServerSocket(0).use { it.localPort }
        server.start(port, deviceId = "rx-1", deviceName = "Pantalla")
        val client = KtorEmitterClient(NoOpLogger())

        try {
            val info = client.getInfo("127.0.0.1", port).getOrThrow()
            assertTrue(info.isReceiver)
            assertEquals("Pantalla", info.deviceName)
            assertEquals(APP_VERSION, info.appVersion)
            assertFalse(info.isPaired)

            // A wrong code is rejected and does not pair the screen.
            assertTrue(
                client.pair("127.0.0.1", port, "tx-1", "Caja", "000000").isFailure,
            )
            assertFalse(pairing.uiState.value.isPaired)

            val paired = client.pair("127.0.0.1", port, "tx-1", "Caja", "123456").getOrThrow()
            assertEquals("a".repeat(64), paired.token)

            // A second emitter cannot take over the screen.
            val second = client.pair("127.0.0.1", port, "tx-2", "Otra caja", "123456")
            assertTrue(second.isFailure)

            val bytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nWQAAAAASUVORK5CYII="
            )
            val hash = Sha256.hex(bytes)

            // A wrong hash must be rejected before the file is stored.
            assertTrue(
                client.uploadMedia(
                    host = "127.0.0.1",
                    port = port,
                    emitterId = "tx-1",
                    token = paired.token,
                    mediaId = "m1",
                    sha256 = "0".repeat(64),
                    bytes = bytes,
                ).isFailure,
            )
            assertTrue(storage.listStored().isEmpty())

            client.uploadMedia(
                host = "127.0.0.1",
                port = port,
                emitterId = "tx-1",
                token = paired.token,
                mediaId = "m1",
                sha256 = hash,
                bytes = bytes,
            ).getOrThrow()

            val state = client.getSyncState("127.0.0.1", port, "tx-1", paired.token).getOrThrow()
            assertEquals(listOf("m1"), state.media.map { it.mediaId })
            assertEquals(hash, state.media.single().sha256)

            val playlist = Playlist(
                id = "p1",
                name = "Promociones",
                version = 1,
                defaultDurationSec = 10,
                items = listOf(
                    PlaylistItem(
                        mediaId = "m1",
                        fileName = "pixel.png",
                        sha256 = hash,
                        durationSec = null,
                    ),
                ),
            )

            // A playlist that references missing media is rejected atomically.
            val missing = playlist.copy(
                version = 2,
                items = listOf(
                    PlaylistItem("m2", "missing.png", "0".repeat(64)),
                ),
            )
            val rejected = client.putPlaylist(
                "127.0.0.1", port, "tx-1", paired.token, missing, null,
            )
            assertTrue(rejected.isFailure)
            val rejection = rejected.exceptionOrNull()
            assertTrue(rejection is PlaylistRejectedException)
            assertEquals(listOf("m2"), (rejection as PlaylistRejectedException).missingMediaIds)
            assertEquals(null, storage.loadReceiverPlaylist())

            val applied = client.putPlaylist(
                "127.0.0.1", port, "tx-1", paired.token, playlist, null,
            ).getOrThrow()
            assertTrue(applied.applied)
            assertEquals(1, applied.version)
            assertEquals(playlist, storage.loadReceiverPlaylist())
            assertEquals(playlist, appliedPlaylist)

            val status = client.getStatus("127.0.0.1", port, "tx-1", paired.token).getOrThrow()
            assertEquals(APP_VERSION, status.appVersion)

            val updateBytes = "update-package-for-test".toByteArray()
            assertTrue(
                client.uploadUpdate(
                    "127.0.0.1", port, "tx-1", paired.token, "1.1.0",
                    "0".repeat(64), updateBytes,
                ).isFailure,
            )
            assertNull(updateManager.stagedVersion)
            val update = client.uploadUpdate(
                "127.0.0.1", port, "tx-1", paired.token, "1.1.0",
                Sha256.hex(updateBytes), updateBytes,
            ).getOrThrow()
            assertTrue(update.accepted)
            assertEquals("1.1.0", update.version)
            assertFalse(update.forced)
            assertEquals("1.1.0", updateManager.stagedVersion)

            assertTrue(
                client.synchronizePlayback(
                    "127.0.0.1", port, "tx-1", paired.token, delayMs = 2_000,
                ).getOrThrow(),
            )
            assertEquals(2_000L, playbackSyncDelay)

            // Wrong token -> 401 -> failure, not data.
            assertTrue(
                client.getStatus("127.0.0.1", port, "tx-1", "b".repeat(64)).isFailure,
            )

            assertTrue(client.unpair("127.0.0.1", port, "tx-1", paired.token).getOrThrow())
            assertTrue(
                client.getStatus("127.0.0.1", port, "tx-1", paired.token).isFailure,
            )
            val after = client.getInfo("127.0.0.1", port).getOrThrow()
            assertFalse(after.isPaired)
        } finally {
            client.close()
            server.stop()
            lockScope.cancel()
        }
    }

    @Test
    fun rejectsInvalidMediaIdsAndFormats() = runBlocking {
        // Keep a separate server so the happy-path test stays independent.
        val storage = FakeRepositories(hashProvider = { Sha256.hex(it) })
        val pairing = PairingManager(
            repository = storage,
            logger = NoOpLogger(),
            codeGenerator = { "123456" },
            tokenGenerator = { "a".repeat(64) },
        )
        pairing.initialize()
        val lockScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val server = KtorReceiverServer(
            pairingManager = pairing,
            logger = NoOpLogger(),
            playlistRepository = storage,
            mediaRepository = storage,
            kioskLockRepository = storage,
            playlistBridge = ReceiverPlaylistBridge(),
            playbackSyncBridge = ReceiverPlaybackSyncBridge(),
            kioskLockManager = KioskLockManager(
                logger = NoOpLogger(),
                passwordHasher = { it },
                scope = lockScope,
                platformLockAvailable = false,
            ),
            statusProvider = { ReceiverStatusResponse(playlistVersion = 0) },
            updateManager = FakeReceiverUpdateManager(),
        )
        val port = ServerSocket(0).use { it.localPort }
        server.start(port, "rx-2", "Pantalla 2")
        val client = KtorEmitterClient(NoOpLogger())
        try {
            val token = client.pair("127.0.0.1", port, "tx-1", "Caja", "123456").getOrThrow().token
            val garbage = "not-an-image".toByteArray()
            assertTrue(
                client.uploadMedia(
                    "127.0.0.1", port, "tx-1", token, "m1", Sha256.hex(garbage), garbage,
                ).isFailure,
            )
            assertTrue(
                client.uploadMedia(
                    "127.0.0.1", port, "tx-1", token, "../escape", Sha256.hex(garbage), garbage,
                ).isFailure,
            )
            assertTrue(storage.listStored().isEmpty())
        } finally {
            client.close()
            server.stop()
            lockScope.cancel()
        }
    }
}
