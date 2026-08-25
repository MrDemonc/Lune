package com.demonlab.lune.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.data.L10n
import com.demonlab.lune.data.Song
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.components.SongItem

enum class SongSortOrder(val getTitle: () -> String) {
    TITLE({ L10n.sortTitle }),
    ARTIST({ L10n.sortArtist }),
    ALBUM({ L10n.sortAlbum }),
    DATE_ADDED({ L10n.sortDate }),
    DURATION({ L10n.sortDuration });

    val title: String get() = getTitle()
}

@Composable
fun SongsScreen(
    songs: List<Song>,
    playbackManager: PlaybackManager,
    settingsManager: SettingsManager,
    onAddToPlaylistClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortOrder by remember { mutableStateOf(SongSortOrder.TITLE) }
    var sortAscending by remember { mutableStateOf(true) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }

    val sortedSongs = remember(songs, sortOrder, sortAscending) {
        val list = when (sortOrder) {
            SongSortOrder.TITLE -> songs.sortedBy { it.title.lowercase() }
            SongSortOrder.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SongSortOrder.ALBUM -> songs.sortedBy { it.album.lowercase() }
            SongSortOrder.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
            SongSortOrder.DURATION -> songs.sortedByDescending { it.duration }
        }
        if (sortAscending) list else list.reversed()
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        // Header & Sort options
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format(L10n.songsCount, songs.size),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                var showSortMenu by remember { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showSortMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${L10n.sort}: ${sortOrder.title}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SongSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.title) },
                            onClick = {
                                sortOrder = order
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (sortOrder == order) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }

                IconButton(onClick = { sortAscending = !sortAscending }) {
                    Icon(
                        imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Toggle sort direction",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (sortedSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = L10n.noSongsFound,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sortedSongs) { song ->
                    SongItem(
                        song = song.copy(isFavorite = playbackManager.isFavorite(song.id)),
                        isPlaying = playbackManager.isPlaying && playbackManager.currentSong?.id == song.id,
                        isCurrent = playbackManager.currentSong?.id == song.id,
                        onPlay = { playbackManager.playSong(song, sortedSongs) },
                        onFavoriteToggle = { playbackManager.toggleFavorite(song) },
                        onOptionsClick = { selectedSongForOptions = song }
                    )
                }
            }
        }
    }

    // Song Options Dialog
    selectedSongForOptions?.let { song ->
        AlertDialog(
            onDismissRequest = { selectedSongForOptions = null },
            title = { Text(song.title, maxLines = 1, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(L10n.queue) },
                        leadingContent = { Icon(Icons.Default.QueuePlayNext, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                            playbackManager.addToQueue(song, playNext = true)
                            selectedSongForOptions = null
                        }
                    )
                    ListItem(
                        headlineContent = { Text(L10n.tabPlaylists) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                            onAddToPlaylistClick(song)
                            selectedSongForOptions = null
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSongForOptions = null }) {
                    Text(L10n.close)
                }
            }
        )
    }
}
