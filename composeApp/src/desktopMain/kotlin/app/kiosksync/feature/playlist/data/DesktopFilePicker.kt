package app.kiosksync.feature.playlist.data

import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.resources.Res
import app.kiosksync.resources.add_images
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.FileKitDialogParent
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import java.awt.Window
import java.util.concurrent.atomic.AtomicReference
import org.jetbrains.compose.resources.getString

internal object DesktopFilePicker {
    private val parentWindow = AtomicReference<Window?>(null)

    fun bindWindow(window: Window) {
        parentWindow.set(window)
    }

    fun unbindWindow(window: Window) {
        parentWindow.compareAndSet(window, null)
    }

    suspend fun pickImages(logger: AppLogger): List<String> {
        val title = getString(Res.string.add_images)
        val parent = parentWindow.get()
        return try {
            open(title, parent)
        } catch (error: IllegalArgumentException) {
            if (parent == null) throw error
            logger.warn("Could not parent the image picker to the app window: ${error.message}")
            open(title, null)
        }
    }

    private suspend fun open(title: String, parent: Window?): List<String> {
        val files = FileKit.openFilePicker(
            type = FileKitType.File("jpg", "jpeg", "png", "webp"),
            mode = FileKitMode.Multiple(),
            dialogSettings = FileKitDialogSettings(
                title = title,
                parent = parent?.let(FileKitDialogParent::awt),
            ),
        )
        return files.orEmpty().map { it.absolutePath() }
    }
}
