package com.demonlab.lune.ui.playlist

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.demonlab.lune.ui.utils.bounceClick
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.demonlab.lune.R
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.Song
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.sheets.CreatePlaylistDialog
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.components.HeaderSurface

import com.demonlab.lune.ui.utils.formatLongDuration
import com.demonlab.lune.ui.viewmodels.MusicViewModel
import com.demonlab.lune.ui.components.rememberBlurSheetColors
import com.demonlab.lune.ui.components.AppBlurBackdrop
import kotlinx.coroutines.delay

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
    isSortActive: Boolean = false,
    onSortClick: (() -> Unit)? = null,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0,
    showSectionHeader: Boolean = true
) {
    val playlists = viewModel.playlists
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedOptionsPlaylist by remember { mutableStateOf<Playlist?>(null) }
    
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val playlistSortOption = settingsManager.getSortOption("folder_PLAYLISTS")
    val playlistSortAscending = settingsManager.getIsSortAscending("folder_PLAYLISTS")
    val playlistCaseSensitive = settingsManager.getIsCaseSensitiveSort("folder_PLAYLISTS")

    val sortedPlaylists = remember(playlists, viewModel.playlistMappings, playlistSortOption, playlistSortAscending, playlistCaseSensitive) {
        val comparator: Comparator<Playlist> = when (playlistSortOption) {
            "DATE_ADDED" -> compareBy { it.createdAt }
            "TRACK_COUNT" -> compareBy { pl -> viewModel.playlistMappings.count { it.playlistId == pl.id } }
            else -> if (playlistCaseSensitive) compareBy { it.name } else compareBy { it.name.lowercase(java.util.Locale.getDefault()) }
        }
        if (playlistSortAscending) playlists.sortedWith(comparator) else playlists.sortedWith(comparator.reversed())
    }
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

    val plIconBg = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f)
    } else MaterialTheme.colorScheme.secondaryContainer

    val plIconTint = if (hasBlurBackground) {
        if (isDarkTheme) Color.White else Color(0xFF141316)
    } else MaterialTheme.colorScheme.primary

    val plTitleColor = if (hasBlurBackground) {
        if (isDarkTheme) Color.White else Color(0xFF141316)
    } else MaterialTheme.colorScheme.onSurface

    val plCountColor = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.80f) else Color(0xFF141316).copy(alpha = 0.75f)
    } else MaterialTheme.colorScheme.onSurfaceVariant

    val activePrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)
    val actionBtnBg = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f)
    } else MaterialTheme.colorScheme.secondaryContainer

    val actionBtnTint = if (hasBlurBackground) {
        if (isDarkTheme) Color.White else Color(0xFF141316)
    } else MaterialTheme.colorScheme.onSecondaryContainer

    val actionBtnActiveBg = if (useCustomControlsColor) {
        activePrimary
    } else if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.18f)
    } else MaterialTheme.colorScheme.primary

    val actionBtnActiveTint = if (hasBlurBackground && !useCustomControlsColor) {
        if (isDarkTheme) Color.White else Color(0xFF141316)
    } else if (actionBtnActiveBg.luminance() > 0.45f) {
        MaterialTheme.colorScheme.onPrimary.takeIf { it.luminance() < 0.4f } ?: Color(0xFF141316)
    } else {
        MaterialTheme.colorScheme.onPrimary.takeIf { it.luminance() > 0.6f } ?: Color.White
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showSectionHeader,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            HeaderSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                hasBlurBackground = hasBlurBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = plIconBg,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = plIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${playlists.size}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = plCountColor
                            )
                        }
                    }

                    Surface(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        color = actionBtnBg,
                        modifier = Modifier.size(44.dp).bounceClick()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.create_playlist),
                                tint = actionBtnTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    if (onSortClick != null) {
                        Surface(
                            onClick = onSortClick,
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSortActive) actionBtnActiveBg else actionBtnBg,
                            modifier = Modifier.size(44.dp).bounceClick()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSortActive) Icons.Default.Schedule else Icons.Default.SortByAlpha,
                                    contentDescription = stringResource(R.string.sort_options_title),
                                    tint = if (isSortActive) actionBtnActiveTint else actionBtnTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding + 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sortedPlaylists, key = { it.id }) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    viewModel = viewModel,
                    playbackManager = playbackManager,
                    hasBlurBackground = hasBlurBackground,
                    isDarkTheme = isDarkTheme,
                    useCustomControlsColor = useCustomControlsColor,
                    controlsColorPalette = controlsColorPalette,
                    onPlaylistClick = onPlaylistClick,
                    onPlayPlaylist = onPlayPlaylist,
                    onLongClick = { selectedOptionsPlaylist = playlist }
                )
            }
        }
}
}

private val PLAYLIST_GRADIENTS = listOf(
    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899)),
    listOf(Color(0xFF0D9488), Color(0xFF059669), Color(0xFF10B981)),
    listOf(Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFEC4899)),
    listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF6366F1)),
    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF43F5E)),
    listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF3B82F6)),
    listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFB45309)),
    listOf(Color(0xFFE11D48), Color(0xFFBE123C), Color(0xFF881337))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistCard(
    playlist: Playlist,
    viewModel: MusicViewModel,
    playbackManager: PlaybackManager,
    hasBlurBackground: Boolean,
    isDarkTheme: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0,
    onPlaylistClick: (Playlist) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onLongClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlistSongs = remember(playlist.id, viewModel.playlistMappings, viewModel.allSongs) {
        viewModel.getSongsForPlaylistSync(playlist.id)
    }
    val songCount = playlistSongs.size
    val totalDuration = remember(playlistSongs) {
        playlistSongs.sumOf { it.duration }
    }

    val songsWithCover = remember(playlistSongs) {
        playlistSongs.filter { !it.coverUrl.isNullOrEmpty() || it.albumArtUri != null }
    }

    var currentCoverIndex by remember(playlist.id, songsWithCover.size) {
        mutableIntStateOf(if (songsWithCover.isNotEmpty()) kotlin.random.Random.nextInt(songsWithCover.size) else 0)
    }

    LaunchedEffect(playlist.id, songsWithCover) {
        if (songsWithCover.size > 1) {
            while (true) {
                delay(6000)
                currentCoverIndex = (currentCoverIndex + 1) % songsWithCover.size
            }
        }
    }

    val activeCoverSong = remember(songsWithCover, currentCoverIndex) {
        songsWithCover.getOrNull(currentCoverIndex) ?: playlistSongs.firstOrNull()
    }

    val gradientColors = remember(playlist.id) {
        val index = kotlin.math.abs(playlist.id.hashCode()) % PLAYLIST_GRADIENTS.size
        PLAYLIST_GRADIENTS[index]
    }

    val isCurrentPlaylist = playbackManager.activePlaylistId == playlist.id
    val isPlaying = isCurrentPlaylist && playbackManager.isPlaying

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (hasBlurBackground) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = { onPlaylistClick(playlist) },
                onLongClick = { onLongClick(playlist) }
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Top Banner with Full Cover & Random Change
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .background(Brush.horizontalGradient(gradientColors))
            ) {
                val coverObj = activeCoverSong?.coverUrl ?: activeCoverSong?.albumArtUri
                if (coverObj != null) {
                    Crossfade(
                        targetState = coverObj,
                        animationSpec = tween(600),
                        label = "playlistCoverCrossfade",
                        modifier = Modifier.fillMaxSize()
                    ) { targetCover ->
                        SongCoverImage(
                            coverUrl = targetCover,
                            contentDescription = playlist.name,
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(0.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Subtle gradient overlay on top of cover
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.15f),
                                        Color.Black.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                if (isCurrentPlaylist) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasBlurBackground) Color.Black.copy(alpha = 0.45f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = if (isPlaying) {
                                            if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                        } else {
                                            if (hasBlurBackground) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (isPlaying) stringResource(R.string.queue_now_playing) else stringResource(R.string.play_state_pause),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Playlist Details
            Column(modifier = Modifier.padding(12.dp)) {
                // Top row: Playlist name + Play button where the song count previously was
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee()
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    val activePrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)
                    val playBtnBg = if (useCustomControlsColor) {
                        activePrimary
                    } else if (hasBlurBackground) {
                        if (isDarkTheme) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                    val playBtnTint = if (useCustomControlsColor) {
                        if (activePrimary.luminance() > 0.45f) Color(0xFF141316) else Color.White
                    } else if (hasBlurBackground) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }

                    Surface(
                        onClick = {
                            if (isCurrentPlaylist) {
                                if (isPlaying) playbackManager.pause() else playbackManager.resume()
                            } else {
                                onPlayPlaylist(playlist)
                            }
                        },
                        shape = CircleShape,
                        color = playBtnBg,
                        border = if (hasBlurBackground) BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)) else null,
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) stringResource(R.string.play_state_pause) else stringResource(R.string.play_state_play),
                                tint = playBtnTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom row: Time + Song count beside each other!
                val secondaryColor = if (hasBlurBackground) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = secondaryColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = formatLongDuration(totalDuration),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                        maxLines = 1
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor.copy(alpha = 0.6f)
                    )
                    Text(
                        text = stringResource(R.string.ai_mix_songs_count, songCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
    onDeleteConfirm: (Playlist) -> Unit,
    onAddSongsClick: (() -> Unit)? = null
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
            onDeleteClick = { showDeleteConfirm = true },
            onAddSongsClick = onAddSongsClick
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
    onDeleteClick: () -> Unit,
    onAddSongsClick: (() -> Unit)? = null
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
        dragHandle = null,
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
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetDefaults.DragHandle(
                        color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Header (Cover left, Name right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
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
                if (onAddSongsClick != null) {
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
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.clickable {
                                onDismissRequest()
                                onAddSongsClick()
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) {
                            Text(stringResource(R.string.add_songs), color = blurColors.textColor)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 1.dp)
                        .bounceClick(),
                    shape = if (onAddSongsClick != null) {
                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    } else {
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    },
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
