package app.kiosksync.core.ui.components

import app.kiosksync.core.ui.theme.Sizes

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import app.kiosksync.resources.Res
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import app.kiosksync.resources.ic_add
import app.kiosksync.resources.ic_add_link
import app.kiosksync.resources.ic_add_photo_alternate
import app.kiosksync.resources.ic_arrow_downward
import app.kiosksync.resources.ic_arrow_upward
import app.kiosksync.resources.ic_cast
import app.kiosksync.resources.ic_check
import app.kiosksync.resources.ic_check_circle
import app.kiosksync.resources.ic_close
import app.kiosksync.resources.ic_cloud_off
import app.kiosksync.resources.ic_delete
import app.kiosksync.resources.ic_drag_indicator
import app.kiosksync.resources.ic_edit
import app.kiosksync.resources.ic_error
import app.kiosksync.resources.ic_expand_more
import app.kiosksync.resources.ic_favorite
import app.kiosksync.resources.ic_folder_open
import app.kiosksync.resources.ic_help
import app.kiosksync.resources.ic_hourglass_empty
import app.kiosksync.resources.ic_image
import app.kiosksync.resources.ic_image_not_supported
import app.kiosksync.resources.ic_info
import app.kiosksync.resources.ic_link
import app.kiosksync.resources.ic_lock
import app.kiosksync.resources.ic_lock_open
import app.kiosksync.resources.ic_logout
import app.kiosksync.resources.ic_more_vert
import app.kiosksync.resources.ic_pause
import app.kiosksync.resources.ic_photo_library
import app.kiosksync.resources.ic_play_arrow
import app.kiosksync.resources.ic_playlist_add
import app.kiosksync.resources.ic_playlist_play
import app.kiosksync.resources.ic_present_to_all
import app.kiosksync.resources.ic_refresh
import app.kiosksync.resources.ic_remove
import app.kiosksync.resources.ic_select_all
import app.kiosksync.resources.ic_send
import app.kiosksync.resources.ic_settings
import app.kiosksync.resources.ic_stop_circle
import app.kiosksync.resources.ic_swap_horiz
import app.kiosksync.resources.ic_sync
import app.kiosksync.resources.ic_sync_problem
import app.kiosksync.resources.ic_timer
import app.kiosksync.resources.ic_tv
import app.kiosksync.resources.ic_undo
import app.kiosksync.resources.ic_upload
import app.kiosksync.resources.ic_visibility
import app.kiosksync.resources.ic_visibility_off
import app.kiosksync.resources.ic_warning
import app.kiosksync.resources.ic_wifi
import app.kiosksync.resources.ic_wifi_off

/**
 * Material Symbols Rounded (FILL = 1, weight 400, optical size 24), converted
 * to Compose vector resources. Only the icons the product uses are bundled;
 * see `docs/icons.md` to add another one.
 */
enum class AppIcon(val resource: DrawableResource) {
    ADD(Res.drawable.ic_add),
    ADD_LINK(Res.drawable.ic_add_link),
    ADD_PHOTO_ALTERNATE(Res.drawable.ic_add_photo_alternate),
    ARROW_DOWNWARD(Res.drawable.ic_arrow_downward),
    ARROW_UPWARD(Res.drawable.ic_arrow_upward),
    CAST(Res.drawable.ic_cast),
    CHECK(Res.drawable.ic_check),
    CHECK_CIRCLE(Res.drawable.ic_check_circle),
    CLOSE(Res.drawable.ic_close),
    CLOUD_OFF(Res.drawable.ic_cloud_off),
    DELETE(Res.drawable.ic_delete),
    DRAG_INDICATOR(Res.drawable.ic_drag_indicator),
    EDIT(Res.drawable.ic_edit),
    ERROR(Res.drawable.ic_error),
    EXPAND_MORE(Res.drawable.ic_expand_more),
    FAVORITE(Res.drawable.ic_favorite),
    FOLDER_OPEN(Res.drawable.ic_folder_open),
    HELP(Res.drawable.ic_help),
    HOURGLASS_EMPTY(Res.drawable.ic_hourglass_empty),
    IMAGE(Res.drawable.ic_image),
    IMAGE_NOT_SUPPORTED(Res.drawable.ic_image_not_supported),
    INFO(Res.drawable.ic_info),
    LINK(Res.drawable.ic_link),
    LOCK(Res.drawable.ic_lock),
    LOCK_OPEN(Res.drawable.ic_lock_open),
    LOGOUT(Res.drawable.ic_logout),
    MORE_VERT(Res.drawable.ic_more_vert),
    PAUSE(Res.drawable.ic_pause),
    PHOTO_LIBRARY(Res.drawable.ic_photo_library),
    PLAY_ARROW(Res.drawable.ic_play_arrow),
    PLAYLIST_ADD(Res.drawable.ic_playlist_add),
    PLAYLIST_PLAY(Res.drawable.ic_playlist_play),
    PRESENT_TO_ALL(Res.drawable.ic_present_to_all),
    REFRESH(Res.drawable.ic_refresh),
    REMOVE(Res.drawable.ic_remove),
    SELECT_ALL(Res.drawable.ic_select_all),
    SEND(Res.drawable.ic_send),
    SETTINGS(Res.drawable.ic_settings),
    STOP_CIRCLE(Res.drawable.ic_stop_circle),
    SWAP_HORIZ(Res.drawable.ic_swap_horiz),
    SYNC(Res.drawable.ic_sync),
    SYNC_PROBLEM(Res.drawable.ic_sync_problem),
    TIMER(Res.drawable.ic_timer),
    TV(Res.drawable.ic_tv),
    UNDO(Res.drawable.ic_undo),
    UPLOAD(Res.drawable.ic_upload),
    VISIBILITY(Res.drawable.ic_visibility),
    VISIBILITY_OFF(Res.drawable.ic_visibility_off),
    WARNING(Res.drawable.ic_warning),
    WIFI(Res.drawable.ic_wifi),
    WIFI_OFF(Res.drawable.ic_wifi_off),
}

/**
 * Single entry point for icons. Decorative icons pass `contentDescription = null`;
 * icons that are the only content of a button must describe the action.
 */
@Composable
fun AppIcon(
    icon: AppIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.icon,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(icon.resource),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}
