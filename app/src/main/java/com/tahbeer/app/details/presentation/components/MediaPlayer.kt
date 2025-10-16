package com.tahbeer.app.details.presentation.components

import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.tahbeer.app.R
import com.tahbeer.app.core.domain.model.MediaType
import com.tahbeer.app.core.presentation.components.IconWithTooltip
import com.tahbeer.app.details.presentation.DetailScreenAction
import com.tahbeer.app.details.presentation.DetailScreenState
import com.tahbeer.app.details.utils.longToTimestamp

@OptIn(UnstableApi::class)
@Composable
fun MediaPlayer(
    player: Player,
    onAction: (
        DetailScreenAction
    ) -> Unit,
    state: DetailScreenState,
    mediaType: MediaType
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (mediaType == MediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .aspectRatio(16f / 9f)
            ) {
                Crossfade(
                    targetState = player.isCurrentMediaItemSeekable,
                ) { isSeekable ->
                    if (isSeekable) {
                        PlayerSurface(
                            modifier = Modifier.fillMaxSize(),
                            player = player,
                            surfaceType = SURFACE_TYPE_TEXTURE_VIEW
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {}
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        val progress =
            if (player.duration > 0) state.mediaPosition.toFloat() / player.duration.toFloat() else 0f

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = longToTimestamp(state.mediaPosition),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = longToTimestamp(player.duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Slider(
                value = progress,
                onValueChange = { onAction(DetailScreenAction.OnSeek((it * player.duration.toFloat()).toLong())) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(48.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        onAction(
                            DetailScreenAction.OnSeek(state.mediaPosition - 10000L)
                        )
                    }) {
                        IconWithTooltip(
                            icon = ImageVector.vectorResource(R.drawable.rounded_replay_10),
                            text = stringResource(R.string.rewind_button),
                            iconModifier = Modifier.size(40.dp),
                        )
                    }

                    Spacer(Modifier.width(24.dp))

                    PlayPauseButton(
                        player = player,
                    )

                    Spacer(Modifier.width(24.dp))

                    IconButton(onClick = {
                        onAction(
                            DetailScreenAction.OnSeek(state.mediaPosition + 10000L)
                        )
                    }) {
                        IconWithTooltip(
                            icon = ImageVector.vectorResource(R.drawable.rounded_forward_10),
                            text = stringResource(R.string.forward_button),
                            iconModifier = Modifier.size(40.dp),
                        )
                    }
                }
            }

            PlaybackSpeedPopUpButton(player = player)
        }
    }
}
