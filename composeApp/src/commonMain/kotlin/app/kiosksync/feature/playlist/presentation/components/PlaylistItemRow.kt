package app.kiosksync.feature.playlist.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import app.kiosksync.core.common.playlist.MAX_DURATION_SEC
import app.kiosksync.core.common.playlist.MIN_DURATION_SEC
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDropdownMenu
import app.kiosksync.core.ui.components.AppEmptyState
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppIconButton
import app.kiosksync.core.ui.components.AppIconButtonSize
import app.kiosksync.core.ui.components.AppMenuItem
import app.kiosksync.core.ui.components.AppPanelHeader
import app.kiosksync.core.ui.components.AppStepper
import app.kiosksync.core.ui.components.AppTextField
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.feature.playlist.domain.PlaylistItem
import app.kiosksync.resources.Res
import app.kiosksync.resources.duration_field_hint
import app.kiosksync.resources.item_default_duration
import app.kiosksync.resources.move_down
import app.kiosksync.resources.move_up
import app.kiosksync.resources.remove_item
import app.kiosksync.resources.seconds_suffix
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PlaylistItemRow(
    item: PlaylistItem,
    index: Int,
    lastIndex: Int,
    defaultDurationSec: Int,
    durationDraft: String?,
    thumbnail: ImageBitmap?,
    thumbnailLoaded: Boolean,
    onLoadThumbnail: () -> Unit,
    onDurationChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    LaunchedEffect(item.mediaId, item.fileName, thumbnailLoaded) {
        if (!thumbnailLoaded) onLoadThumbnail()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.playlistRowMinHeight)
            .clip(KioskShape.small)
            .background(colors.surfaceContainerLow)
            .padding(Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = Sizes.listThumbnailWidth,
                    height = Sizes.listThumbnailHeight,
                )
                .clip(KioskShape.thumbnail)
                .background(KioskScreenColors.background),
            contentAlignment = Alignment.Center,
        ) {
            val loadedBitmap = thumbnail
            if (loadedBitmap != null) {
                Image(
                    bitmap = loadedBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                AppIcon(
                    icon = AppIcon.IMAGE,
                    contentDescription = null,
                    size = Sizes.iconCompact,
                    tint = colors.onSurfaceVariant,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.fileName,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (item.durationSec == null) {
                    "${stringResource(Res.string.item_default_duration)} · $defaultDurationSec${stringResource(Res.string.seconds_suffix)}"
                } else {
                    "${item.durationSec}${stringResource(Res.string.seconds_suffix)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        AppTextField(
            value = durationDraft ?: item.durationSec?.toString().orEmpty(),
            onValueChange = onDurationChange,
            modifier = Modifier.width(Sizes.durationFieldWidth),
            placeholder = stringResource(Res.string.duration_field_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        AppIconButton(
            icon = AppIcon.ARROW_UPWARD,
            contentDescription = stringResource(Res.string.move_up),
            onClick = onMoveUp,
            enabled = index > 0,
            size = AppIconButtonSize.SMALL,
            tooltip = stringResource(Res.string.move_up),
        )
        AppIconButton(
            icon = AppIcon.ARROW_DOWNWARD,
            contentDescription = stringResource(Res.string.move_down),
            onClick = onMoveDown,
            enabled = index < lastIndex,
            size = AppIconButtonSize.SMALL,
            tooltip = stringResource(Res.string.move_down),
        )
        AppIconButton(
            icon = AppIcon.DELETE,
            contentDescription = stringResource(Res.string.remove_item),
            onClick = onRemove,
            size = AppIconButtonSize.SMALL,
            tooltip = stringResource(Res.string.remove_item),
            tint = colors.error,
        )
    }
}
