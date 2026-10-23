package app.kiosksync.feature.playlist.data

import app.kiosksync.core.AppLogger
import app.kiosksync.core.Strings
import java.awt.FileDialog
import java.awt.Frame
import javax.swing.SwingUtilities

internal object DesktopFilePicker {
    suspend fun pickImages(logger: AppLogger): List<String> = try {
        showImagePicker()
    } catch (error: Exception) {
        logger.warn("Could not open the image picker: ${error.message}")
        emptyList()
    }

    private fun showImagePicker(): List<String> {
        // Do not open a modal picker from Compose's flush dispatcher: its nested
        // event loop can re-enter rendering and corrupt snapshot continuations.
        if (SwingUtilities.isEventDispatchThread()) return showImagePickerOnEdt()

        var files: List<String> = emptyList()
        var failure: Throwable? = null
        SwingUtilities.invokeAndWait {
            try {
                files = showImagePickerOnEdt()
            } catch (error: Throwable) {
                failure = error
            }
        }
        failure?.let { throw it }
        return files
    }

    private fun showImagePickerOnEdt(): List<String> {
        val dialog = FileDialog(null as Frame?, Strings.ADD_IMAGES, FileDialog.LOAD)
        dialog.isMultipleMode = true
        dialog.file = "*.jpg;*.jpeg;*.png;*.webp"
        dialog.isVisible = true
        return dialog.files?.map { it.absolutePath }.orEmpty()
    }
}

