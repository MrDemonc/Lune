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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.demonlab.lune.ui.utils.bounceClick
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.demonlab.lune.ui.components.rememberBlurSheetColors
import com.demonlab.lune.ui.components.AppBlurBackdrop

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
    bottomPadding: Dp,
    hasBlurBackground: Boolean = false,
) {
    val playlists = viewModel.playlists
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedOptionsPlaylist by remember { mutableStateOf<Playlist?>(null) }
    
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val themeMode = settingsManager.themeMode
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
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
            val plIconBg = if (hasBlurBackground) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
            val plIconTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
            val plTitleColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
            val plCountColor = if (hasBlurBackground) Color.White.copy(alpha = 0.80f) else MaterialTheme.colorScheme.onSurfaceVariant
            val plAddBg = if (hasBlurBackground) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary

            HeaderSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                hasBlurBackground = hasBlurBackground
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
                            color = plIconBg,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = plIconTint,
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
                                color = plTitleColor
                            )
                            Text(
                                text = "${playlists.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = plCountColor
                            )
                        }
                    }
                    Surface(
                        onClick = { showCreateDialog = true },
                        shape = CircleShape,
                        color = plAddBg,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        
        itemsIndexed(playlists, key = { _, it -> it.id }) { index, playlist ->
            var songCount by remember { mutableIntStateOf(0) }
            var totalDuration by remember { mutableLongStateOf(0L) }
            
            LaunchedEffect(playlist.id, viewModel.filteredSongs, viewModel.playlistMappings) {
                viewModel.getPlaylistInfo(playlist.id) { count, duration ->
                    songCount = count
                    totalDuration = duration
                }
            }

            val itemTitleColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
            val itemSubtitleColor = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                supportingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MusicNote, 
                            null, 
                            modifier = Modifier.size(14.dp),
                            tint = itemSubtitleColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "$songCount • ${formatLongDuration(totalDuration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = itemSubtitleColor
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
                                    .border(2.dp, if (hasBlurBackground) Color.Transparent else MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                    }
                },
                modifier = Modifier.combinedClickable(
                    onClick = { onPlaylistClick(playlist) },
                    onLongClick = { selectedOptionsPlaylist = playlist }
                )
            ) {
                Text(
                    playlist.name,
                    fontWeight = FontWeight.SemiBold,
                    color = itemTitleColor
                )
            }
        }
    }
}

@Composable
fun DeletePlaylistDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong
    val blurColors = rememberBlurSheetColors(currentSong)

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
                        stringResource(R.string.delete_playlist),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = blurColors.textColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.delete_playlist_confirm),
                        color = blurColors.textSecondaryColor
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
                        TextButton(
                            onClick = onConfirm,
                            colors = ButtonDefaults.textButtonColors(contentColor = if (blurColors.hasBlur) Color.White else MaterialTheme.colorScheme.error),
                            modifier = Modifier.bounceClick()
                        ) {
                            Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
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
    val blurColors = rememberBlurSheetColors()

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(currentPlaylist.name) }
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = blurColors.textColor,
            unfocusedTextColor = blurColors.textColor,
            focusedBorderColor = blurColors.primaryTint,
            unfocusedBorderColor = if (blurColors.hasBlur) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline,
            focusedLabelColor = blurColors.primaryTint,
            unfocusedLabelColor = blurColors.textSecondaryColor
        )
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = blurColors.containerColor,
            title = { 
                Text(
                    stringResource(R.string.edit_playlist_name),
                    fontWeight = FontWeight.Bold,
                    color = blurColors.textColor
                ) 
            },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = textFieldColors
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renamePlaylist(currentPlaylist.id, newName.trim())
                        }
                        showRenameDialog = false
                        onDismissRequest()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = blurColors.primaryTint,
                        contentColor = if (blurColors.hasBlur && blurColors.isDark) Color.Black else Color.White
                    ),
                    modifier = Modifier.bounceClick()
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }, modifier = Modifier.bounceClick()) {
                    Text(stringResource(R.string.cancel), color = blurColors.textSecondaryColor)
                }
            },
            shape = RoundedCornerShape(28.dp)
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
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val blurColors = rememberBlurSheetColors(currentSong)
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = blurColors.containerColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant) },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = currentSong,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
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
                        color = blurColors.textColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Options
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 1.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    color = blurColors.itemContainerColor,
                    border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) }
                ) {
                    ListItem(
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { onRenameClick() },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        Text(stringResource(R.string.edit_name), color = blurColors.textColor)
                    }
                }
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 1.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                    color = blurColors.itemContainerColor,
                    border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) }
                ) {
                    ListItem(
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = if (blurColors.hasBlur) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { onDeleteClick() },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        Text(stringResource(R.string.delete_playlist), color = if (blurColors.hasBlur) Color.White else MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
