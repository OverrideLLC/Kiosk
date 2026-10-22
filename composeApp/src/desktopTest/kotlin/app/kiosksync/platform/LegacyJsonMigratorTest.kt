package app.kiosksync.platform

import app.kiosksync.NoOpLogger
import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.platform.db.KioskDao
import app.kiosksync.platform.db.PairedDeviceEntity
import app.kiosksync.platform.db.buildKioskDatabase
import app.kiosksync.platform.secret.DevelopmentSecretProtector
import app.kiosksync.platform.secret.SecretProtector
import java.io.File
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class LegacyJsonMigratorTest {
    private lateinit var dir: File
    private var storage: RoomLocalStorage? = null

    @BeforeTest
    fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "kiosk-room-migration-${System.nanoTime()}")
        dir.mkdirs()
    }

    @AfterTest
    fun tearDown() {
        storage?.close()
        dir.deleteRecursively()
    }

    @Test
    fun migratesLegacyRowsBacksUpSecretsAndCanRestart() = runBlocking {
        val png = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nWQAAAAASUVORK5CYII="
        )
        val hash = Sha256.hex(png)
        val mediaDir = File(dir, "media").apply { mkdirs() }
        File(mediaDir, "media-1.png").writeBytes(png)
        File(dir, "playlist.json").writeText(
            """{"id":"receiver","name":"Pantalla","version":4,"defaultDurationSec":10,"items":[{"mediaId":"media-1","fileName":"ficha.png","sha256":"$hash","durationSec":15}]}"""
        )
        File(dir, "playlists.json").writeText(
            """{"playlists":[{"id":"library-1","name":"Promos","version":2,"defaultDurationSec":12,"items":[]}],"activePlaylistId":"library-1"}"""
        )
        File(dir, "paired.json").writeText(
            """[{"deviceId":"screen-1","deviceName":"Pantalla 1","token":"emitter-token","host":"192.168.1.20","port":8765}]"""
        )
        File(dir, "lock.json").writeText(
            """{"passwordHash":"password-hash","passwordLength":8,"enabled":true,"unlockSequence":["SHIFT","CTRL"],"recoveryHash":"recovery-hash","recoveryLength":10}"""
        )

        val database = openDatabase()
        val migrator = migrator(database)
        val localStorage = RoomLocalStorage(dir, NoOpLogger(), database, DevelopmentSecretProtector)
        storage = localStorage

        migrator.migrate()

        assertEquals("emitter-token", localStorage.loadEmitterPairedDevices().single().token)
        assertEquals("library-1", localStorage.loadPlaylistLibrary()?.activePlaylistId)
        assertEquals("receiver", localStorage.loadPlaylist()?.id)
        assertEquals(hash, localStorage.listStoredMedia().single().sha256)
        assertEquals("password-hash", localStorage.loadKioskLock()?.passwordHash)
        assertEquals(KioskDao.LEGACY_MIGRATION_COMPLETE, database.kioskDao().getMetadata(KioskDao.LEGACY_MIGRATION_KEY))

        val pairingBackup = File(dir, "legacy/paired.json.room-v1.dpapi")
        val lockBackup = File(dir, "legacy/lock.json.room-v1.dpapi")
        assertTrue(pairingBackup.isFile)
        assertTrue(lockBackup.isFile)
        assertFalse(pairingBackup.readText().contains("emitter-token"))
        assertFalse(lockBackup.readText().contains("password-hash"))
        assertEquals(
            File(dir, "legacy/playlists.json.room-v1").readText(),
            """{"playlists":[{"id":"library-1","name":"Promos","version":2,"defaultDurationSec":12,"items":[]}],"activePlaylistId":"library-1"}"""
        )
        assertFalse(File(dir, "paired.json").exists())

        localStorage.saveReceiverPairedInfo(ReceiverPairedInfo("controller", "Control", "receiver-token"))
        val receiverRow = database.kioskDao().getPairedDevices(KioskDao.PAIR_KIND_RECEIVER).single()
        assertNotEquals("receiver-token", receiverRow.tokenCiphertext)
        assertTrue(receiverRow.tokenCiphertext.startsWith("dev-only:v1:"))

        migrator.migrate()
        assertEquals(1, localStorage.loadEmitterPairedDevices().size)

        localStorage.close()
        val reopenedDatabase = openDatabase()
        val reopenedStorage = RoomLocalStorage(dir, NoOpLogger(), reopenedDatabase, DevelopmentSecretProtector)
        storage = reopenedStorage
        assertEquals("receiver-token", reopenedStorage.loadReceiverPairedInfo()?.token)
        assertEquals("receiver", reopenedStorage.loadPlaylist()?.id)
    }

    @Test
    fun invalidLegacyJsonDoesNotMarkMigrationOrDeleteSources() = runBlocking {
        val source = File(dir, "playlist.json").apply { writeText("{not-json") }
        val database = openDatabase()
        storage = RoomLocalStorage(dir, NoOpLogger(), database, DevelopmentSecretProtector)
        val migrator = migrator(database)

        val failure = runCatching { migrator.migrate() }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals("{not-json", source.readText())
        assertEquals("{not-json", File(dir, "legacy/playlist.json.room-v1").readText())
        assertNull(database.kioskDao().getMetadata(KioskDao.LEGACY_MIGRATION_KEY))
        assertNull(storage?.loadPlaylist())
    }

    @Test
    fun decryptionFailureKeepsStoredSecretForRecovery() = runBlocking {
        val database = openDatabase()
        val ciphertext = "dpapi:v1:unavailable-on-this-account"
        database.kioskDao().upsertPairedDevice(
            PairedDeviceEntity(
                kind = KioskDao.PAIR_KIND_RECEIVER,
                deviceId = "controller-1",
                deviceName = "Control",
                tokenCiphertext = ciphertext,
                host = null,
                port = null
            )
        )
        val failingProtector = object : SecretProtector {
            override fun protect(plainText: String): String = error("must not overwrite secrets")
            override fun unprotect(cipherText: String): String = error("DPAPI decryption failed")
        }
        val localStorage = RoomLocalStorage(dir, NoOpLogger(), database, failingProtector)
        storage = localStorage

        assertNull(localStorage.loadReceiverPairedInfo())
        assertEquals(ciphertext, database.kioskDao().getPairedDevices(KioskDao.PAIR_KIND_RECEIVER).single().tokenCiphertext)
    }

    private fun openDatabase() = buildKioskDatabase(dir)

    private fun migrator(database: app.kiosksync.platform.db.KioskDatabase) = LegacyJsonMigrator(
        dataDir = dir,
        database = database,
        mediaStore = FileMediaStore(dir, NoOpLogger()),
        secretProtector = DevelopmentSecretProtector,
        logger = NoOpLogger()
    )
}
