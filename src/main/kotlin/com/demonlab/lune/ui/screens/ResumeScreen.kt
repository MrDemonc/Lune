package com.demonlab.lune.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demonlab.lune.data.L10n
import com.demonlab.lune.data.Album
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.data.Song
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.components.CoverArtwork
import com.demonlab.lune.ui.components.SongItem
import com.demonlab.lune.ui.utils.formatLongDuration
import java.util.Calendar

@Composable
fun ResumeScreen(
    songs: List<Song>,
    albums: List<Album>,
    playlists: List<Playlist>,
    playbackManager: PlaybackManager,
    settingsManager: SettingsManager,
    onAlbumClick: (Album) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = rememberGreeting()
    val favoriteSongs = remember(songs, playbackManager.favoritesSet) {
        songs.filter { playbackManager.isFavorite(it.id) }
    }
    val favoritesCount = favoriteSongs.size
    val recentlyAdded = remember(songs) {
        songs.sortedByDescending { it.dateAdded }.take(10)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero Greeting Card
        if (settingsManager.showHeroGreeting) {
            item {
                HeroGreetingCard(
                    greeting = greeting,
                    totalSongs = songs.size,
                    favoritesCount = favoritesCount,
                    onQuickMix = {
                        if (songs.isNotEmpty()) {
                            val shuffled = songs.shuffled()
                            playbackManager.playSong(shuffled.first(), shuffled)
                        }
                    }
                )
            }
        }

        // Quick Mix / Recommendations
        if (songs.isNotEmpty()) {
            item {
                Text(
                    text = L10n.recommendations,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (favoriteSongs.isNotEmpty()) {
                                    playbackManager.playSong(favoriteSongs.first(), favoriteSongs)
                                } else if (songs.isNotEmpty()) {
                                    playbackManager.playSong(songs.first(), songs)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Favorites Mix",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(L10n.favoriteTracks, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(String.format(L10n.songsCount, favoritesCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val randomMix = songs.shuffled()
                                if (randomMix.isNotEmpty()) {
                                    playbackManager.playSong(randomMix.first(), randomMix)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = L10n.randomShuffle,
                                    tint = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(L10n.discoveryMix, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(L10n.randomShuffle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Favorites Quick Section (If any)
        if (favoriteSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${L10n.favoriteTracks} ($favoritesCount)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(favoriteSongs.take(10)) { song ->
                        Column(
                            modifier = Modifier
                                .width(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { playbackManager.playSong(song, favoriteSongs) }
                        ) {
                            CoverArtwork(
                                coverPath = song.coverPath,
                                modifier = Modifier.size(130.dp).shadow(8.dp, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Albums Carousel
        if (albums.isNotEmpty()) {
            item {
                Text(
                    text = L10n.tabAlbums,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(albums.take(12)) { album ->
                        Column(
                            modifier = Modifier
                                .width(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAlbumClick(album) }
                        ) {
                            CoverArtwork(
                                coverPath = album.coverPath,
                                modifier = Modifier.size(130.dp).shadow(8.dp, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = album.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Recently Added Songs
        if (recentlyAdded.isNotEmpty()) {
            item {
                Text(
                    text = L10n.recentlyAdded,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(recentlyAdded) { song ->
                SongItem(
                    song = song.copy(isFavorite = playbackManager.isFavorite(song.id)),
                    isPlaying = playbackManager.isPlaying && playbackManager.currentSong?.id == song.id,
                    isCurrent = playbackManager.currentSong?.id == song.id,
                    onPlay = { playbackManager.playSong(song, songs) },
                    onFavoriteToggle = { playbackManager.toggleFavorite(song) }
                )
            }
        }
    }
}

@Composable
fun HeroGreetingCard(
    greeting: String,
    totalSongs: Int,
    favoritesCount: Int,
    onQuickMix: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = String.format(L10n.tracksAvailable, totalSongs, favoritesCount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onQuickMix,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = L10n.quickMix,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(L10n.quickMix, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun rememberGreeting(): String {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val lang = SettingsManager.instance.language
    return remember(hour, lang) {
        when (hour) {
            in 5..11 -> "${L10n.goodMorning} ☀️"
            in 12..17 -> "${L10n.goodAfternoon} 🌤️"
            in 18..21 -> "${L10n.goodEvening} 🌆"
            else -> "${L10n.goodEvening} 🌙"
        }
    }
}
