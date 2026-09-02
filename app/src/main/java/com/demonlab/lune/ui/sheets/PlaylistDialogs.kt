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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.demonlab.lune.R
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.components.rememberBlurSheetColors
import com.demonlab.lune.ui.utils.bounceClick
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
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong ?: allSongs.firstOrNull()
    val blurColors = rememberBlurSheetColors(currentSong)
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

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = blurColors.textColor,
        unfocusedTextColor = blurColors.textColor,
        focusedBorderColor = blurColors.primaryTint,
        unfocusedBorderColor = if (blurColors.hasBlur) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline,
        focusedLabelColor = blurColors.primaryTint,
        unfocusedLabelColor = blurColors.textSecondaryColor
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = currentSong,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.add_songs),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = blurColors.textColor
                        )
                        if (newlySelectedCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "+$newlySelectedCount",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = blurColors.primaryTint,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_songs), color = blurColors.textSecondaryColor) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = textFieldColors,
                        leadingIcon = {
                            Surface(
                                shape = CircleShape,
                                color = if (blurColors.hasBlur) blurColors.itemContainerColor else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = if (blurColors.hasBlur) blurColors.textColor else blurColors.primaryTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.bounceClick()) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = null,
                                        tint = if (blurColors.hasBlur) blurColors.textColor else blurColors.textSecondaryColor,
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
                                val isSelected = selectedFolder == null
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFolder = null },
                                    label = { Text(stringResource(R.string.filter_all_songs)) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (blurColors.hasBlur) (if (blurColors.isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.12f)) else MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedLeadingIconColor = if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = blurColors.itemContainerColor,
                                        labelColor = blurColors.textSecondaryColor,
                                        iconColor = if (blurColors.hasBlur) blurColors.textColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.LibraryMusic,
                                            contentDescription = null,
                                            tint = if (blurColors.hasBlur) blurColors.textColor else Color.Unspecified,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.bounceClick()
                                )
                            }
                            items(availableFolders) { folder ->
                                val isSelected = selectedFolder == folder
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFolder = folder },
                                    label = { Text(folder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (blurColors.hasBlur) (if (blurColors.isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.12f)) else MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedLeadingIconColor = if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = blurColors.itemContainerColor,
                                        labelColor = blurColors.textSecondaryColor,
                                        iconColor = if (blurColors.hasBlur) blurColors.textColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (blurColors.hasBlur) blurColors.textColor else Color.Unspecified,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.bounceClick()
                                )
                            }
                        }
                    }

                    // 3. Sort chips + 4. Select all
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(sortOptions) { (key, labelRes) ->
                                val isSelected = sortOption == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            isSortAscending = !isSortAscending
                                        } else {
                                            sortOption = key
                                            isSortAscending = true
                                        }
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stringResource(labelRes))
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (isSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (blurColors.hasBlur) (if (blurColors.isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.12f)) else MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedLeadingIconColor = if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = blurColors.itemContainerColor,
                                        labelColor = blurColors.textSecondaryColor,
                                        iconColor = if (blurColors.hasBlur) blurColors.textColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.bounceClick()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Select all button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAllSelected) (if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer) else blurColors.itemContainerColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .bounceClick()
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
                                    tint = if (blurColors.hasBlur) blurColors.textColor else (if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAllSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isAllSelected) (if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onPrimaryContainer) else blurColors.textColor
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
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = if (blurColors.hasBlur) (if (blurColors.isDark) Color.White else blurColors.primaryTint) else MaterialTheme.colorScheme.primary,
                                        uncheckedColor = if (blurColors.hasBlur) blurColors.textColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        checkmarkColor = if (blurColors.hasBlur) (if (blurColors.isDark) Color.Black else Color.White) else MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = blurColors.itemContainerColor,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    val cover = song.coverUrl ?: song.uri
                                    AsyncImage(
                                        model = cover,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = blurColors.textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = blurColors.textSecondaryColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss, modifier = Modifier.bounceClick()) {
                            Text(stringResource(R.string.cancel), color = blurColors.textSecondaryColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val toAdd = selectedIds.value.filter { it !in initialSelectedIds }
                                val toRemove = initialSelectedIds.filter { it !in selectedIds.value }
                                onSave(toAdd, toRemove)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = blurColors.primaryTint,
                                contentColor = if (blurColors.hasBlur) Color.Black else Color.White
                            ),
                            modifier = Modifier.bounceClick()
                        ) {
                            Text(
                                stringResource(R.string.save_selection),
                                fontWeight = FontWeight.Bold,
                                color = if (blurColors.hasBlur) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    song: Song,
    viewModel: MusicViewModel,
    playbackManager: PlaybackManager,
    onDismiss: () -> Unit
) {
    val targetSong = song
    val blurColors = rememberBlurSheetColors(targetSong)
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
            onDismiss = { showCreateDialog = false },
            targetSong = targetSong
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = targetSong,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        stringResource(R.string.add_to_playlist),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = blurColors.textColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .bounceClick()
                            .clickable { showCreateDialog = true },
                        color = if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add, 
                                null, 
                                tint = if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                stringResource(R.string.create_playlist), 
                                fontWeight = FontWeight.Bold,
                                color = if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Text(
                        "Playlists", 
                        style = MaterialTheme.typography.labelLarge,
                        color = blurColors.textSecondaryColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(playlists) { index, playlist ->
                            val isInPlaylist = containingPlaylistIds.contains(playlist.id)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .bounceClick()
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
                                color = if (isInPlaylist) (if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)) else blurColors.itemContainerColor,
                                shape = RoundedCornerShape(12.dp),
                                border = if (isInPlaylist) BorderStroke(1.dp, blurColors.primaryTint.copy(alpha = 0.4f)) else blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (isInPlaylist) blurColors.primaryTint 
                                                 else Color.Transparent, 
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        playlist.name, 
                                        fontWeight = if (isInPlaylist) FontWeight.Bold else FontWeight.SemiBold,
                                        color = blurColors.textColor,
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
                                            modifier = Modifier.size(32.dp).bounceClick()
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(
                            onClick = onDismiss,
                            shape = CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = blurColors.itemContainerColor,
                                contentColor = blurColors.textColor
                            ),
                            modifier = Modifier.bounceClick()
                        ) {
                            Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
    targetSong: Song? = null
) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val song = targetSong ?: playbackManager.currentSong
    val blurColors = rememberBlurSheetColors(song)
    var name by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = blurColors.textColor,
        unfocusedTextColor = blurColors.textColor,
        focusedBorderColor = blurColors.primaryTint,
        unfocusedBorderColor = if (blurColors.hasBlur) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline,
        focusedLabelColor = blurColors.primaryTint,
        unfocusedLabelColor = blurColors.textSecondaryColor
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = song,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 380.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        stringResource(R.string.create_playlist),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = blurColors.textColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.playlist_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss, modifier = Modifier.bounceClick()) {
                            Text(stringResource(R.string.cancel), color = blurColors.textSecondaryColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { if (name.isNotBlank()) onCreate(name) },
                            enabled = name.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = blurColors.primaryTint,
                                contentColor = if (blurColors.hasBlur) Color.Black else Color.White
                            ),
                            modifier = Modifier.bounceClick()
                        ) {
                            Text(
                                stringResource(R.string.create),
                                fontWeight = FontWeight.Bold,
                                color = if (blurColors.hasBlur) (if (name.isNotBlank()) Color.Black else Color.Gray) else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
