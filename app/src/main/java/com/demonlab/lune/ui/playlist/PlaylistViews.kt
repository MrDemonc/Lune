package com.demonlab.lune.ui.playlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.demonlab.lune.R
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.sheets.CreatePlaylistDialog
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.components.HeaderSurface
import com.demonlab.lune.ui.components.headerWaveBorder
import com.demonlab.lune.ui.utils.formatLongDuration
import com.demonlab.lune.ui.viewmodels.MusicViewModel

@Composable
fun PlaylistPreviewCovers(
    playlistId: Long,
    viewModel: MusicViewModel,
    size: Dp = 56.dp,
    shape: Shape = CircleShape
) {
    var covers by remember { mutableStateOf<List<String?>>(emptyList()) }

    LaunchedEffect(playlistId, viewModel.playlistMappings) {
        viewModel.getPlaylistPreviewCovers(playlistId) {
            covers = it
        }
    }

    Surface(
        modifier = Modifier.size(size),
        shape = shape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        val validCovers = remember(covers) { covers.filterNotNull() }
        var failedIndices by remember(covers) { mutableStateOf(setOf<Int>()) }

        if (validCovers.isEmpty() || failedIndices.size >= validCovers.take(4).size) {
            SongCoverImage(
                coverUrl = null,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                shape = shape,
                iconScale = 0.60f
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (validCovers.size == 1) {
                    SongCoverImage(
                        coverUrl = validCovers[0],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        shape = shape,
                        iconScale = 0.60f,
                        onError = { failedIndices = failedIndices + 0 }
                    )
                } else {
                    val gridCovers = validCovers.take(4)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (gridCovers.size > 0) SongCoverImage(gridCovers[0], null, Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp), onError = { failedIndices = failedIndices + 0 })
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (gridCovers.size > 1) SongCoverImage(gridCovers[1], null, Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp), onError = { failedIndices = failedIndices + 1 })
                            }
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (gridCovers.size > 2) SongCoverImage(gridCovers[2], null, Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp), onError = { failedIndices = failedIndices + 2 })
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (gridCovers.size > 3) SongCoverImage(gridCovers[3], null, Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp), onError = { failedIndices = failedIndices + 3 })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(
    viewModel: MusicViewModel,
    onPlaylistClick: (Playlist) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    bottomPadding: Dp
) {
    val playlists = viewModel.playlists
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedOptionsPlaylist by remember { mutableStateOf<Playlist?>(null) }
    
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    PlaylistOptionsAndRename(
        playlist = selectedOptionsPlaylist,
        playlists = playlists,
        viewModel = viewModel,
        onDismissRequest = { selectedOptionsPlaylist = null },
        onDeleteConfirm = { playlist ->
            val isActive = playbackManager.activePlaylistId == playlist.id
            onDeletePlaylist(playlist)
            if (isActive) playbackManager.stop()
            selectedOptionsPlaylist = null
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
    ) {
        item {
            HeaderSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.playlists),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${playlists.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        onClick = { showCreateDialog = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        
        itemsIndexed(playlists, key = { _, it -> it.id }) { index, playlist ->
            val isFirst = index == 0
            val isLast = index == playlists.lastIndex
            var songCount by remember { mutableIntStateOf(0) }
            var totalDuration by remember { mutableLongStateOf(0L) }
            
            LaunchedEffect(playlist.id, viewModel.filteredSongs, viewModel.playlistMappings) {
                viewModel.getPlaylistInfo(playlist.id) { count, duration ->
                    songCount = count
                    totalDuration = duration
                }
            }

            ListItem(
                supportingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MusicNote, 
                            null, 
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "$songCount • ${formatLongDuration(totalDuration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                leadingContent = { 
                    Box(contentAlignment = Alignment.Center) {
                        PlaylistPreviewCovers(playlist.id, viewModel, 56.dp)
                        if (playbackManager.activePlaylistId == playlist.id) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(2.dp)
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                    }
                },
                modifier = Modifier.combinedClickable(
                    onClick = { onPlaylistClick(playlist) },
                    onLongClick = { selectedOptionsPlaylist = playlist }
                )
            ) {
                Text(playlist.name, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun DeletePlaylistDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_playlist)) },
        text = { Text(stringResource(R.string.delete_playlist_confirm)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
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
fun PlaylistOptionsAndRename(
    playlist: Playlist?,
    playlists: List<Playlist>,
    viewModel: MusicViewModel,
    onDismissRequest: () -> Unit,
    onDeleteConfirm: (Playlist) -> Unit
) {
    if (playlist == null) return
    val currentPlaylist = playlists.find { it.id == playlist.id } ?: playlist
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(currentPlaylist.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.edit_playlist_name)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renamePlaylist(currentPlaylist.id, newName.trim())
                        }
                        showRenameDialog = false
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteConfirm) {
        DeletePlaylistDialog(
            onConfirm = {
                onDeleteConfirm(currentPlaylist)
                showDeleteConfirm = false
                onDismissRequest()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (!showRenameDialog && !showDeleteConfirm) {
        PlaylistOptionsSheet(
            playlist = currentPlaylist,
            viewModel = viewModel,
            onDismissRequest = onDismissRequest,
            onRenameClick = { showRenameDialog = true },
            onDeleteClick = { showDeleteConfirm = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistOptionsSheet(
    playlist: Playlist,
    viewModel: MusicViewModel,
    onDismissRequest: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header (Cover left, Name right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaylistPreviewCovers(playlist.id, viewModel, 64.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Options
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 1.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ) {
                ListItem(
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable { onRenameClick() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) {
                    Text(stringResource(R.string.edit_name))
                }
            }
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 1.dp),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ) {
                ListItem(
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable { onDeleteClick() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) {
                    Text(stringResource(R.string.delete_playlist), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
