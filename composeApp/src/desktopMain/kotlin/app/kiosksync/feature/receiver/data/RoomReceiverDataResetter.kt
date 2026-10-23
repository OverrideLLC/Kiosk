package app.kiosksync.feature.receiver.data

import app.kiosksync.data.local.room.KioskDao
import app.kiosksync.feature.media.data.FileMediaStore
import app.kiosksync.feature.receiver.domain.ReceiverDataResetter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class RoomReceiverDataResetter(
    private val dao: KioskDao,
    private val mediaStore: FileMediaStore,
    private val dataDir: File,
) : ReceiverDataResetter {
    override suspend fun clearReceiverData() = withContext(Dispatchers.IO) {
        dao.clearReceiverData()
        mediaStore.clear()
        File(dataDir, "playlist.json.tmp").delete()
        File(dataDir, "lock.json.tmp").delete()
        File(dataDir, "skipped-update.txt").delete()
        File(dataDir, "updates").deleteRecursively()
        Unit
    }
}
