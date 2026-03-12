package com.demonlab.lune.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.demonlab.lune.R
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.Song

@Composable
fun ResumeScreen(
    viewModel: com.demonlab.lune.ui.viewmodels.MusicViewModel,
    allSongs: List<Song>,
    allPlaylists: List<Playlist>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaylistClick: (Playlist) -> Unit
) {
    val scrollState = rememberScrollState()

    // 1. Recommendations (Shuffle top 5 artists, limit 10)
    val recommendations = remember(allSongs) {
        val topArtists = allSongs.groupingBy { it.artist }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }

        allSongs.filter { topArtists.contains(it.artist) }
            .distinctBy { it.id }
            .shuffled()
            .take(10)
    }

    // 2. Playlists
    val topPlaylists = remember(allPlaylists) {
        // Since we don't have play counts for playlists, we just show the first 10
        allPlaylists.take(10)
    }

    // 3. Recently Added (Sort by dateAdded descending)
    val recentlyAdded = remember(allSongs) {
        allSongs.sortedByDescending { it.dateAdded }.take(10)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        
        Spacer(modifier = Modifier.height(8.dp))

        // Recommendations Section
        if (recommendations.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.resume_recommendations))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(recommendations, key = { it.id }) { song ->
                    HorizontalSongCard(
                        song = song,
                        onClick = { onSongClick(song, recommendations) }
                    )
                }
            }
        }

        // Top Playlists Section
        if (topPlaylists.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.resume_top_playlists))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(topPlaylists, key = { it.id }) { playlist ->
                    HorizontalPlaylistCard(
                        viewModel = viewModel,
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist) }
                    )
                }
            }
        }

        // Recently Added Section
        if (recentlyAdded.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.resume_recently_added))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(recentlyAdded, key = { it.id }) { song ->
                    HorizontalSongCard(
                        song = song,
                        onClick = { onSongClick(song, recentlyAdded) }
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun HorizontalSongCard(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                AsyncImage(
                    model = song.coverUrl ?: song.albumArtUri,
                    contentDescription = "Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_launcher_background)
                )
                // Small play overlay indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist.ifBlank { stringResource(R.string.unknown_artist) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HorizontalPlaylistCard(
    viewModel: com.demonlab.lune.ui.viewmodels.MusicViewModel,
    playlist: Playlist, 
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                com.demonlab.lune.ui.activities.PlaylistPreviewCovers(
                    playlistId = playlist.id,
                    viewModel = viewModel,
                    size = 64.dp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = playlist.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
