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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.playlist.presentation.PlaylistEditorAction
import app.kiosksync.feature.playlist.presentation.PlaylistEditorState
import org.jetbrains.compose.ui.tooling.preview.Preview
import app.kiosksync.resources.Res
import app.kiosksync.resources.add_images
import app.kiosksync.resources.adding_images
import app.kiosksync.resources.close_button
import app.kiosksync.resources.default_duration
import app.kiosksync.resources.duration_hint
import app.kiosksync.resources.empty_playlist_title
import app.kiosksync.resources.playlist_empty
import app.kiosksync.resources.playlist_subtitle
import app.kiosksync.resources.playlist_title
import app.kiosksync.resources.seconds_suffix
import org.jetbrains.compose.resources.stringResource
import app.kiosksync.core.ui.resolve

@Composable
fun PlaylistPanel(
    state: PlaylistEditorState,
    onAction: (PlaylistEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extra = KioskColors.extra

    Column(modifier = modifier) {
        AppPanelHeader(
            title = stringResource(Res.string.playlist_title),
            subtitle = stringResource(Res.string.playlist_subtitle),
            actions = {
                AppButton(
                    text = if (state.isImporting) stringResource(Res.string.adding_images) else stringResource(Res.string.add_images),
                    onClick = { onAction(PlaylistEditorAction.AddImages) },
                    variant = AppButtonVariant.TONAL,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.ADD_PHOTO_ALTERNATE,
                    loading = state.isImporting,
                    enabled = !state.isImporting,
                )
            },
        )

        Spacer(Modifier.height(Space.sm))

        PlaylistSelectorRow(state = state, onAction = onAction)

        Spacer(Modifier.height(Space.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.default_duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = stringResource(Res.string.duration_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Space.sm))
            AppStepper(
                value = state.defaultDurationDraft.toIntOrNull()
                    ?: state.playlist.defaultDurationSec,
                onValueChange = { onAction(PlaylistEditorAction.UpdateDefaultDuration(it.toString())) },
                range = MIN_DURATION_SEC..MAX_DURATION_SEC,
                unit = stringResource(Res.string.seconds_suffix),
                modifier = Modifier.width(Sizes.stepperWidth),
            )
        }

        state.message?.let { message ->
            Spacer(Modifier.height(Space.xs))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = KioskShape.small,
                color = if (state.messageIsError) colors.errorContainer else extra.successContainer,
                contentColor = if (state.messageIsError) {
                    colors.onErrorContainer
                } else {
                    extra.onSuccessContainer
                },
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = Space.sm,
                        vertical = Space.xs,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    AppIcon(
                        icon = if (state.messageIsError) AppIcon.ERROR else AppIcon.CHECK_CIRCLE,
                        contentDescription = null,
                        size = Sizes.iconSmall,
                        tint = if (state.messageIsError) {
                            colors.onErrorContainer
                        } else {
                            extra.onSuccessContainer
                        },
                    )
                    Text(
                        text = message.resolve(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    AppIconButton(
                        icon = AppIcon.CLOSE,
                        contentDescription = stringResource(Res.string.close_button),
                        onClick = { onAction(PlaylistEditorAction.DismissMessage) },
                        size = AppIconButtonSize.SMALL,
                        tint = if (state.messageIsError) {
                            colors.onErrorContainer
                        } else {
                            extra.onSuccessContainer
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.xs))

        if (state.playlist.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                AppEmptyState(
                    icon = AppIcon.PHOTO_LIBRARY,
                    title = stringResource(Res.string.empty_playlist_title),
                    description = stringResource(Res.string.playlist_empty),
                    action = {
                        AppButton(
                            text = stringResource(Res.string.add_images),
                            onClick = { onAction(PlaylistEditorAction.AddImages) },
                            variant = AppButtonVariant.TONAL,
                            size = AppButtonSize.SMALL,
                            icon = AppIcon.ADD_PHOTO_ALTERNATE,
                            enabled = !state.isImporting,
                        )
                    },
                )
            }
        } else {
            val lastIndex = state.playlist.items.lastIndex
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                itemsIndexed(
                    items = state.playlist.items,
                    key = { _, item -> item.mediaId },
                ) { index, item ->
                    PlaylistItemRow(
                        item = item,
                        index = index,
                        lastIndex = lastIndex,
                        defaultDurationSec = state.playlist.defaultDurationSec,
                        durationDraft = state.durationDrafts[item.mediaId],
                        thumbnail = state.thumbnailByMediaId[item.mediaId],
                        thumbnailLoaded = item.mediaId in state.thumbnailByMediaId,
                        onLoadThumbnail = {
                            onAction(PlaylistEditorAction.LoadThumbnail(item.mediaId, item.fileName))
                        },
                        onDurationChange = { onAction(PlaylistEditorAction.UpdateItemDuration(item.mediaId, it)) },
                        onMoveUp = { onAction(PlaylistEditorAction.MoveItemUp(index)) },
                        onMoveDown = { onAction(PlaylistEditorAction.MoveItemDown(index)) },
                        onRemove = { onAction(PlaylistEditorAction.RequestRemoveItem(item.mediaId)) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PlaylistPanelPreview() {
    KioskTheme {
        PlaylistPanel(
            state = PlaylistEditorState(),
            onAction = {},
        )
    }
}

