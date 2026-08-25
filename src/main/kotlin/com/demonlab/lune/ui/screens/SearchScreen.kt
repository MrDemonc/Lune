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
import com.demonlab.lune.data.Album
import com.demonlab.lune.data.L10n
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.data.Song
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.ui.components.CoverArtwork
import com.demonlab.lune.ui.components.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    songs: List<Song>,
    albums: List<Album>,
    playlists: List<Playlist>,
    playbackManager: PlaybackManager,
    onAlbumClick: (Album) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Songs, 2: Albums, 3: Playlists

    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else songs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredAlbums = remember(albums, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else albums.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredPlaylists = remember(playlists, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else playlists.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(L10n.searchSongs) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val filters = listOf(L10n.filterAll, L10n.tabSongs, L10n.tabAlbums, L10n.tabPlaylists)
            filters.forEachIndexed { index, label ->
                com.demonlab.lune.ui.components.SettingSegmentedChip(
                    selected = selectedFilter == index,
                    label = label,
                    onClick = { selectedFilter = index }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (searchQuery.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = L10n.searchTypePrompt,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Albums section
                if ((selectedFilter == 0 || selectedFilter == 2) && filteredAlbums.isNotEmpty()) {
                    item {
                        Text(
                            text = "${L10n.tabAlbums} (${filteredAlbums.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(filteredAlbums) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAlbumClick(album) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoverArtwork(coverPath = album.coverPath, modifier = Modifier.size(48.dp), shape = RoundedCornerShape(8.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(album.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("${album.artist} • ${String.format(L10n.songsCount, album.songCount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Playlists section
                if ((selectedFilter == 0 || selectedFilter == 3) && filteredPlaylists.isNotEmpty()) {
                    item {
                        Text(
                            text = "${L10n.tabPlaylists} (${filteredPlaylists.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(filteredPlaylists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPlaylistClick(playlist) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(playlist.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(String.format(L10n.songsCount, playlist.songIds.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Songs section
                if ((selectedFilter == 0 || selectedFilter == 1) && filteredSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "${L10n.tabSongs} (${filteredSongs.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(filteredSongs) { song ->
                        SongItem(
                            song = song.copy(isFavorite = playbackManager.isFavorite(song.id)),
                            isPlaying = playbackManager.isPlaying && playbackManager.currentSong?.id == song.id,
                            isCurrent = playbackManager.currentSong?.id == song.id,
                            onPlay = { playbackManager.playSong(song, filteredSongs) },
                            onFavoriteToggle = { playbackManager.toggleFavorite(song) }
                        )
                    }
                }

                if (filteredSongs.isEmpty() && filteredAlbums.isEmpty() && filteredPlaylists.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text(String.format(L10n.noSearchResults, searchQuery), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
