package app.kiosksync.platform

import app.kiosksync.core.AppLogger
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FileAppLogger(
    private val logsDir: File,
    private val maxFileSizeBytes: Long = 1024 * 1024L, // 1 MB
    private val maxBackupIndex: Int = 5
) : AppLogger {

    private val logFile = File(logsDir, "app.log")
    private val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    init {
        logsDir.mkdirs()
    }

    override fun info(message: String) {
        log("INFO", message, null)
    }

    override fun warn(message: String, throwable: Throwable?) {
        log("WARN", message, throwable)
    }

    override fun error(message: String, throwable: Throwable?) {
        log("ERROR", message, throwable)
    }

    override fun debug(message: String) {
        log("DEBUG", message, null)
    }

    @Synchronized
    private fun log(level: String, message: String, throwable: Throwable?) {
        val timestamp = LocalDateTime.now().format(dtf)
        val threadName = Thread.currentThread().name
        val formatted = "[$timestamp] [$level] [$threadName] $message"

        println(formatted)
        throwable?.printStackTrace(System.err)

        try {
            checkRotation()
            logFile.appendText(formatted + "\n")
            if (throwable != null) {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                logFile.appendText(sw.toString() + "\n")
            }
        } catch (e: Exception) {
            System.err.println("Failed to write log to file: ${e.message}")
        }
    }

    private fun checkRotation() {
        if (!logFile.exists() || logFile.length() < maxFileSizeBytes) {
            return
        }

        // Delete the oldest backup if exists
        val oldest = File(logsDir, "app.log.$maxBackupIndex")
        if (oldest.exists()) {
            oldest.delete()
        }

        // Shift backups
        for (i in (maxBackupIndex - 1) downTo 1) {
            val src = File(logsDir, "app.log.$i")
            if (src.exists()) {
                val dst = File(logsDir, "app.log.${i + 1}")
                src.renameTo(dst)
            }
        }

        // Rename current app.log to app.log.1
        val firstBackup = File(logsDir, "app.log.1")
        logFile.renameTo(firstBackup)
    }
}
