package com.demonlab.lune.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.demonlab.lune.R
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
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
    val context = LocalContext.current
    val hiddenFolders = remember { SettingsManager.getInstance(context).hiddenFolders }
    val visibleSongs = remember(allSongs, hiddenFolders) {
        allSongs.filter { !hiddenFolders.contains(it.folderName) }
    }
    val availableFolders = remember(visibleSongs) {
        visibleSongs.map { it.folderName }.filter { it.isNotBlank() }.distinct().sorted()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf("ALPHABETICAL") }
    var isSortAscending by remember { mutableStateOf(true) }
    val selectedIds = remember { mutableStateOf(initialSelectedIds.toMutableSet()) }

    val songsInScope = remember(selectedFolder, visibleSongs) {
        if (selectedFolder == null) visibleSongs
        else visibleSongs.filter { it.folderName == selectedFolder }
    }

    val sortedSongs = remember(songsInScope, sortOption, isSortAscending) {
        val comparator = when (sortOption) {
            "ALPHABETICAL" -> compareBy<Song> { it.title.lowercase(java.util.Locale.getDefault()) }
            "ARTIST" -> compareBy<Song> { it.artist.lowercase(java.util.Locale.getDefault()) }
            "DATE_ADDED" -> compareBy<Song> { it.dateAdded }
            "DURATION" -> compareBy<Song> { it.duration }
            "TRACK_NUMBER" -> compareBy<Song> { it.trackNumber }
            else -> compareBy<Song> { it.title.lowercase(java.util.Locale.getDefault()) }
        }
        if (isSortAscending) {
            songsInScope.sortedWith(comparator)
        } else {
            songsInScope.sortedWith(comparator.reversed())
        }
    }

    val filteredSongs = remember(searchQuery, sortedSongs) {
        if (searchQuery.isBlank()) sortedSongs
        else sortedSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val isAllSelected = filteredSongs.isNotEmpty() && filteredSongs.all { selectedIds.value.contains(it.id) }
    val newlySelectedCount = remember(selectedIds.value, initialSelectedIds) {
        selectedIds.value.count { it !in initialSelectedIds }
    }

    val sortOptions = remember {
        listOf(
            "ALPHABETICAL" to R.string.sort_alphabetical,
            "ARTIST" to R.string.sort_artist,
            "DATE_ADDED" to R.string.sort_date_added,
            "DURATION" to R.string.sort_duration,
            "TRACK_NUMBER" to R.string.sort_track_number
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .heightIn(max = 620.dp),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.add_songs),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (newlySelectedCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "+$newlySelectedCount",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_songs)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
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
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null
                )

                // 2. Folder chips (above sort chips and select all)
                if (availableFolders.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFolder == null,
                                onClick = { selectedFolder = null },
                                label = { Text(stringResource(R.string.filter_all_songs)) },
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LibraryMusic,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        items(availableFolders) { folder ->
                            FilterChip(
                                selected = selectedFolder == folder,
                                onClick = { selectedFolder = folder },
                                label = { Text(folder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // 3. Sort chips (under folder chips)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(sortOptions) { (option, labelRes) ->
                        val isSelected = sortOption == option
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    isSortAscending = !isSortAscending
                                } else {
                                    sortOption = option
                                    isSortAscending = true
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(labelRes),
                                    maxLines = 1
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = if (isSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }

                // 4. Select All toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val newSet = selectedIds.value.toMutableSet()
                                if (isAllSelected) {
                                    filteredSongs.forEach { newSet.remove(it.id) }
                                } else {
                                    filteredSongs.forEach { newSet.add(it.id) }
                                }
                                selectedIds.value = newSet
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAllSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAllSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAllSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 5. Songs list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(filteredSongs, key = { _, it -> it.id }) { index, song ->
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
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                AsyncImage(
                                    model = song.coverUrl ?: song.albumArtUri ?: song.uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
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
        }
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
