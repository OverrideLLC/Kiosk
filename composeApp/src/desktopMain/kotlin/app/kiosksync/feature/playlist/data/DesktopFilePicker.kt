package app.kiosksync.feature.playlist.data

import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.resources.Res
import app.kiosksync.resources.add_images
import java.awt.FileDialog
import java.awt.Frame
import javax.swing.SwingUtilities
import org.jetbrains.compose.resources.getString

internal object DesktopFilePicker {
    suspend fun pickImages(logger: AppLogger): List<String> = try {
        showImagePicker(getString(Res.string.add_images))
    } catch (error: Exception) {
        logger.warn("Could not open the image picker: ${error.message}")
        emptyList()
    }

    private fun showImagePicker(title: String): List<String> {
        // Do not open a modal picker from Compose's flush dispatcher: its nested
        // event loop can re-enter rendering and corrupt snapshot continuations.
        if (SwingUtilities.isEventDispatchThread()) return showImagePickerOnEdt(title)

        var files: List<String> = emptyList()
        var failure: Throwable? = null
        SwingUtilities.invokeAndWait {
            try {
                files = showImagePickerOnEdt(title)
            } catch (error: Throwable) {
                failure = error
            }
        }
        failure?.let { throw it }
        return files
    }

    private fun showImagePickerOnEdt(title: String): List<String> {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.isMultipleMode = true
        dialog.file = "*.jpg;*.jpeg;*.png;*.webp"
        dialog.isVisible = true
        return dialog.files?.map { it.absolutePath }.orEmpty()
    }
}

