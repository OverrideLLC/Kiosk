package app.kiosksync.core.ui

import androidx.compose.runtime.Composable
import app.kiosksync.resources.Res
import app.kiosksync.resources.pair_error_invalid_code
import app.kiosksync.resources.pair_error_invalid_code_attempts
import app.kiosksync.resources.playback_sync_completed_many
import app.kiosksync.resources.playback_sync_completed_mixed
import app.kiosksync.resources.playback_sync_completed_none
import app.kiosksync.resources.playback_sync_completed_one
import app.kiosksync.resources.playlist_import_added
import app.kiosksync.resources.playlist_import_mixed
import app.kiosksync.resources.playlist_import_none
import app.kiosksync.resources.selected_screens_many
import app.kiosksync.resources.selected_screens_one
import app.kiosksync.resources.send_completed_many
import app.kiosksync.resources.send_completed_mixed
import app.kiosksync.resources.send_completed_none
import app.kiosksync.resources.send_completed_one
import app.kiosksync.resources.send_no_screens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A UI string that is resolved from Compose Resources when it is drawn.
 * ViewModels keep the resource, so the label follows the system language.
 */
data class UiText(
    val resource: StringResource,
    val args: List<Any> = emptyList(),
) {
    constructor(resource: StringResource, vararg args: Any) : this(resource, args.toList())
}

@Composable
fun UiText.resolve(): String = if (args.isEmpty()) {
    stringResource(resource)
} else {
    stringResource(resource, *args.toTypedArray())
}

fun sendCompletedText(success: Int, failure: Int): UiText = when {
    success == 0 -> UiText(Res.string.send_completed_none)
    failure == 0 && success == 1 -> UiText(Res.string.send_completed_one)
    failure == 0 -> UiText(Res.string.send_completed_many, success)
    else -> UiText(Res.string.send_completed_mixed, success, failure)
}

fun playbackSyncCompletedText(success: Int, failure: Int): UiText = when {
    success == 0 -> UiText(Res.string.playback_sync_completed_none)
    failure == 0 && success == 1 -> UiText(Res.string.playback_sync_completed_one)
    failure == 0 -> UiText(Res.string.playback_sync_completed_many, success)
    else -> UiText(Res.string.playback_sync_completed_mixed, success, failure)
}

fun selectedScreensText(count: Int): UiText = when (count) {
    0 -> UiText(Res.string.send_no_screens)
    1 -> UiText(Res.string.selected_screens_one)
    else -> UiText(Res.string.selected_screens_many, count)
}

fun playlistImportSummaryText(added: Int, rejected: Int): UiText = when {
    added > 0 && rejected == 0 -> UiText(Res.string.playlist_import_added, added)
    added > 0 && rejected > 0 -> UiText(Res.string.playlist_import_mixed, added, rejected)
    else -> UiText(Res.string.playlist_import_none)
}

fun pairInvalidCodeText(remainingAttempts: Int?): UiText = if (remainingAttempts == null) {
    UiText(Res.string.pair_error_invalid_code)
} else {
    UiText(Res.string.pair_error_invalid_code_attempts, remainingAttempts)
}
