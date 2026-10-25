package app.kiosksync.feature.playlist.presentation

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
import app.kiosksync.core.MAX_DURATION_SEC
import app.kiosksync.core.MIN_DURATION_SEC
import app.kiosksync.core.Strings
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
import app.kiosksync.feature.playlist.presentation.PlaylistEditorAction
import app.kiosksync.feature.playlist.presentation.PlaylistEditorStateHolder
import app.kiosksync.feature.playlist.presentation.PlaylistEditorUiState
import app.kiosksync.feature.playlist.domain.PlaylistItem
import app.kiosksync.feature.media.domain.MediaRepository

@Composable
fun PlaylistPanel(
    stateHolder: PlaylistEditorStateHolder,
    state: PlaylistEditorUiState,
    mediaRepository: MediaRepository,
    fillHeight: Boolean,
    onRequestRemove: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra

    Column(modifier = modifier) {
        AppPanelHeader(
            title = Strings.PLAYLIST_TITLE,
            subtitle = Strings.PLAYLIST_SUBTITLE,
            actions = {
                AppButton(
                    text = if (state.isImporting) Strings.ADDING_IMAGES else Strings.ADD_IMAGES,
                    onClick = { stateHolder.dispatch(PlaylistEditorAction.AddImages) },
                    variant = AppButtonVariant.TONAL,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.ADD_PHOTO_ALTERNATE,
                    loading = state.isImporting,
                    enabled = !state.isImporting,
                )
            },
        )

        Spacer(Modifier.height(Space.sm))

        PlaylistSelectorRow(state = state, stateHolder = stateHolder)

        Spacer(Modifier.height(Space.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.DEFAULT_DURATION,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = Strings.DURATION_HINT,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Space.sm))
            AppStepper(
                value = state.defaultDurationDraft.toIntOrNull()
                    ?: state.playlist.defaultDurationSec,
                onValueChange = { stateHolder.dispatch(PlaylistEditorAction.UpdateDefaultDuration(it.toString())) },
                range = MIN_DURATION_SEC..MAX_DURATION_SEC,
                unit = Strings.SECONDS_SUFFIX,
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
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    AppIconButton(
                        icon = AppIcon.CLOSE,
                        contentDescription = Strings.CLOSE_BUTTON,
                        onClick = { stateHolder.dispatch(PlaylistEditorAction.DismissMessage) },
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
            val listModifier = if (fillHeight) {
                Modifier.fillMaxWidth().weight(1f)
            } else {
                Modifier.fillMaxWidth().heightIn(min = Sizes.emptyStateMinHeight)
            }
            Box(
                modifier = listModifier,
                contentAlignment = Alignment.Center,
            ) {
                AppEmptyState(
                    icon = AppIcon.PHOTO_LIBRARY,
                    title = Strings.EMPTY_PLAYLIST_TITLE,
                    description = Strings.PLAYLIST_EMPTY,
                    action = {
                        AppButton(
                            text = Strings.ADD_IMAGES,
                            onClick = { stateHolder.dispatch(PlaylistEditorAction.AddImages) },
                            variant = AppButtonVariant.TONAL,
                            size = AppButtonSize.SMALL,
                            icon = AppIcon.ADD_PHOTO_ALTERNATE,
                            enabled = !state.isImporting,
                        )
                    },
                )
            }
        } else {
            val listModifier = if (fillHeight) {
                Modifier.fillMaxWidth().weight(1f)
            } else {
                Modifier.fillMaxWidth().heightIn(max = Sizes.stackedListMaxHeight)
            }
            val lastIndex = state.playlist.items.lastIndex
            LazyColumn(
                modifier = listModifier,
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
                        mediaRepository = mediaRepository,
                        onDurationChange = {
                            stateHolder.dispatch(PlaylistEditorAction.UpdateItemDuration(item.mediaId, it))
                        },
                        onMoveUp = { stateHolder.dispatch(PlaylistEditorAction.MoveItemUp(index)) },
                        onMoveDown = { stateHolder.dispatch(PlaylistEditorAction.MoveItemDown(index)) },
                        onRemove = { onRequestRemove(item) },
                    )
                }
            }
        }
    }
}

