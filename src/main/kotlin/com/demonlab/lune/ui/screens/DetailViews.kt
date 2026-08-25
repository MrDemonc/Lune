package com.demonlab.lune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.data.Album
import com.demonlab.lune.data.Folder
import com.demonlab.lune.data.L10n
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.data.Song
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.ui.components.CoverArtwork
import com.demonlab.lune.ui.components.SongItem

@Composable
fun AlbumDetailView(
    album: Album,
    playbackManager: PlaybackManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        // Back Button
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L10n.back)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Album Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverArtwork(
                coverPath = album.coverPath,
                modifier = Modifier.size(140.dp).shadow(12.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format(L10n.songsCount, album.songs.size)} ${if (album.year != null) "• ${album.year}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row {
                    Button(
                        onClick = {
                            if (album.songs.isNotEmpty()) {
                                playbackManager.playSong(album.songs.first(), album.songs)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.play)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = {
                            if (album.songs.isNotEmpty()) {
                                val shuff = album.songs.shuffled()
                                playbackManager.playSong(shuff.first(), shuff)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.shuffle)
                    }
                }
            }
        }

        // Song List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(album.songs) { song ->
                SongItem(
                    song = song.copy(isFavorite = playbackManager.isFavorite(song.id)),
                    isPlaying = playbackManager.isPlaying && playbackManager.currentSong?.id == song.id,
                    isCurrent = playbackManager.currentSong?.id == song.id,
                    onPlay = { playbackManager.playSong(song, album.songs) },
                    onFavoriteToggle = { playbackManager.toggleFavorite(song) }
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    allSongs: List<Song>,
    playbackManager: PlaybackManager,
    onBack: () -> Unit,
    onDeletePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songMap = remember(allSongs) { allSongs.associateBy { it.id } }
    val playlistSongs = remember(playlist.songIds, allSongs) {
        playlist.songIds.mapNotNull { songMap[it] }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L10n.back)
            }

            IconButton(onClick = onDeletePlaylist) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Playlist Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val firstCover = playlistSongs.firstOrNull { it.coverPath != null }?.coverPath
            CoverArtwork(
                coverPath = playlist.customCoverPath ?: firstCover,
                modifier = Modifier.size(140.dp).shadow(12.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(L10n.songsCount, playlistSongs.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row {
                    Button(
                        onClick = {
                            if (playlistSongs.isNotEmpty()) {
                                playbackManager.playSong(playlistSongs.first(), playlistSongs)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.play)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = {
                            if (playlistSongs.isNotEmpty()) {
                                val shuff = playlistSongs.shuffled()
                                playbackManager.playSong(shuff.first(), shuff)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.shuffle)
                    }
                }
            }
        }

        // Song List
        if (playlistSongs.isEmpty()) {
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
                items(playlistSongs) { song ->
                    SongItem(
                        song = song.copy(isFavorite = playbackManager.isFavorite(song.id)),
                        isPlaying = playbackManager.isPlaying && playbackManager.currentSong?.id == song.id,
                        isCurrent = playbackManager.currentSong?.id == song.id,
                        onPlay = { playbackManager.playSong(song, playlistSongs) },
                        onFavoriteToggle = { playbackManager.toggleFavorite(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun FolderDetailView(
    folder: Folder,
    playbackManager: PlaybackManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        // Back Button
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L10n.back)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Folder Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = folder.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(L10n.songsCount, folder.songs.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row {
                    Button(
                        onClick = {
                            if (folder.songs.isNotEmpty()) {
                                playbackManager.playSong(folder.songs.first(), folder.songs)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.play)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = {
                            if (folder.songs.isNotEmpty()) {
                                val shuff = folder.songs.shuffled()
                                playbackManager.playSong(shuff.first(), shuff)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.shuffle)
                    }
                }
            }
        }

        // Song List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(folder.songs) { song ->
                SongItem(
                    song = song.copy(isFavorite = playbackManager.isFavorite(song.id)),
                    isPlaying = playbackManager.isPlaying && playbackManager.currentSong?.id == song.id,
                    isCurrent = playbackManager.currentSong?.id == song.id,
                    onPlay = { playbackManager.playSong(song, folder.songs) },
                    onFavoriteToggle = { playbackManager.toggleFavorite(song) }
                )
            }
        }
    }
}
