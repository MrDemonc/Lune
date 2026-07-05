package com.demonlab.lune.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.viewmodels.MusicViewModel
import coil.compose.AsyncImage

@Composable
fun AddSongsToPlaylistDialog(
    playlistId: Long,
    allSongs: List<Song>,
    initialSelectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (List<Long>, List<Long>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateOf(initialSelectedIds.toMutableSet()) }
    
    val filteredSongs = remember(searchQuery, allSongs) {
        if (searchQuery.isBlank()) allSongs
        else allSongs.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.artist.contains(searchQuery, ignoreCase = true) 
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_songs)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_songs)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    leadingIcon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    itemsIndexed(filteredSongs, key = { _, it -> it.id }) { index, song ->
                        val isFirst = index == 0
                        val isLast = index == filteredSongs.lastIndex
                        val isSelected = selectedIds.value.contains(song.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = selectedIds.value.toMutableSet()
                                    if (isSelected) newSet.remove(song.id) else newSet.add(song.id)
                                    selectedIds.value = newSet
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    val newSet = selectedIds.value.toMutableSet()
                                    if (checked) newSet.add(song.id) else newSet.remove(song.id)
                                    selectedIds.value = newSet
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Added Cover Art
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                AsyncImage(
                                    model = song.coverUrl ?: song.uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    song.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val toAdd = selectedIds.value.filter { it !in initialSelectedIds }
                    val toRemove = initialSelectedIds.filter { it !in selectedIds.value }
                    onSave(toAdd, toRemove) 
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.save_selection))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun AddToPlaylistDialog(
    song: Song,
    viewModel: MusicViewModel,
    playbackManager: PlaybackManager,
    onDismiss: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val playlists = viewModel.playlists
    var containingPlaylistIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    
    LaunchedEffect(song.id, playlists) {
        viewModel.getPlaylistsContainingSong(song.id) {
            containingPlaylistIds = it
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showCreateDialog = true },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            stringResource(R.string.create_playlist), 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Text(
                    "Playlists", 
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(playlists) { index, playlist ->
                        val isFirst = index == 0
                        val isLast = index == playlists.lastIndex
                        val isInPlaylist = containingPlaylistIds.contains(playlist.id)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (!isInPlaylist) {
                                        viewModel.addSongToPlaylist(playlist.id, song.id) {
                                            viewModel.getPlaylistsContainingSong(song.id) {
                                                containingPlaylistIds = it
                                                playbackManager.checkPlaylistStatus()
                                                if (playbackManager.activePlaylistId == playlist.id) {
                                                    val updated = viewModel.getSongsForPlaylistSync(playlist.id)
                                                    playbackManager.refreshActivePlaylist(updated)
                                                }
                                                onDismiss()
                                            }
                                        }
                                    }
                                },
                            color = if (isInPlaylist) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isInPlaylist) BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isInPlaylist) MaterialTheme.colorScheme.primary 
                                            else Color.Transparent, 
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    playlist.name, 
                                    fontWeight = if (isInPlaylist) FontWeight.Bold else FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isInPlaylist) {
                                    IconButton(
                                        onClick = {
                                            viewModel.removeSongFromPlaylist(playlist.id, song.id) {
                                                viewModel.getPlaylistsContainingSong(song.id) {
                                                    containingPlaylistIds = it
                                                    playbackManager.checkPlaylistStatus()
                                                    if (playbackManager.activePlaylistId == playlist.id) {
                                                        val updated = viewModel.getSongsForPlaylistSync(playlist.id)
                                                        playbackManager.refreshActivePlaylist(updated)
                                                    }
                                                    onDismiss()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onDismiss,
                shape = CircleShape
            ) {
                Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun CreatePlaylistDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
