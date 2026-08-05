package com.demonlab.lune.ui.screens.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.ui.playlist.PlaylistPreviewCovers
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.viewmodels.MusicViewModel

@Composable
fun PlaylistGridSection(
    viewModel: MusicViewModel,
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
) {
    if (playlists.isEmpty()) return

    val playlistInfo = remember(playlists, viewModel.playlistMappings) {
        playlists.map { playlist ->
            val count = viewModel.getSongsForPlaylistSync(playlist.id).size
            playlist to count
        }
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        SectionHeader(title = stringResource(R.string.resume_top_playlists))
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(playlistInfo, key = { it.first.id }) { (playlist, songCount) ->
                PlaylistCard(
                    playlist = playlist,
                    songCount = songCount,
                    viewModel = viewModel,
                    onClick = { onPlaylistClick(playlist) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    songCount: Int,
    viewModel: MusicViewModel,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .width(150.dp)
            .height(190.dp)
            .bounceClick()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PlaylistPreviewCovers(
                playlistId = playlist.id,
                viewModel = viewModel,
                size = 190.dp,
                shape = RoundedCornerShape(0.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.92f)
                            ),
                            startY = 0f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$songCount ${stringResource(R.string.tab_songs).lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
        }
    }
}
