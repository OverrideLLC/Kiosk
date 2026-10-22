package app.kiosksync.data.local.room

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlinx.coroutines.Dispatchers

fun buildKioskDatabase(dataDir: File): KioskDatabase {
    dataDir.mkdirs()
    val databaseFile = File(dataDir, "kiosk.db")
    return Room.databaseBuilder<KioskDatabase>(name = databaseFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
