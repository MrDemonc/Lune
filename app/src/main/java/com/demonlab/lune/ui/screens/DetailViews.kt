package com.demonlab.lune.ui.screens

import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.demonlab.lune.R
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.FastScrollbar
import com.demonlab.lune.ui.components.SongItem
import com.demonlab.lune.ui.data.Album
import com.demonlab.lune.ui.playlist.PlaylistOptionsAndRename
import com.demonlab.lune.ui.playlist.PlaylistPreviewCovers
import com.demonlab.lune.ui.sheets.AddSongsToPlaylistDialog
import com.demonlab.lune.ui.utils.formatLongDuration
import com.demonlab.lune.ui.utils.triggerLightVibration
import com.demonlab.lune.ui.viewmodels.MusicViewModel

@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    songs: List<Song>,
    sortOption: String,
    isSortAscending: Boolean,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onOptionsClick: (Song) -> Unit,
    onSortClick: () -> Unit,
    currentlyPlayingId: Long?,
    bottomPadding: Dp,
    viewModel: MusicViewModel,
    onFavoriteClick: ((Song) -> Unit)? = null,
    scrollToCurrentTrigger: MutableState<Int>
) {
    val playbackManager = PlaybackManager.getInstance(LocalContext.current)
    val settingsManager = SettingsManager.getInstance(LocalContext.current)
    val vibrator = LocalContext.current.getSystemService(Vibrator::class.java)!!
    val listState = rememberLazyListState()
    var showPlaylistOptions by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    val isPlaying = playbackManager.isPlaying
    
    val sortedSongs = remember(songs, sortOption, isSortAscending) {
        playbackManager.getSortedList(songs, sortOption, isSortAscending)
    }
    
    val headerAlpha by remember {
        derivedStateOf {
            val firstItemIndex = listState.firstVisibleItemIndex
            val firstItemOffset = listState.firstVisibleItemScrollOffset
            if (firstItemIndex > 0) 0f
            else (1f - (firstItemOffset / 600f)).coerceIn(0f, 1f)
        }
    }
    
    val headerScale by remember {
        derivedStateOf {
            val firstItemIndex = listState.firstVisibleItemIndex
            val firstItemOffset = listState.firstVisibleItemScrollOffset
            if (firstItemIndex > 0) 0.8f
            else (1f - (firstItemOffset / 1200f)).coerceIn(0.8f, 1f)
        }
    }

    val backgroundCover = remember(songs) {
        songs.firstOrNull()?.let { it.coverUrl ?: it.uri }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (backgroundCover != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp)
                                    .offset(y = (-50).dp)
                                    .graphicsLayer {
                                        alpha = headerAlpha
                                    }
                            ) {
                                AsyncImage(
                                    model = backgroundCover,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().blur(60.dp).alpha(0.4f),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                    MaterialTheme.colorScheme.surface
                                                ),
                                                startY = 0f
                                            )
                                        )
                                )
                            }
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = headerAlpha
                                    scaleX = headerScale
                                    scaleY = headerScale
                                }
                                .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(55.dp))
                        PlaylistPreviewCovers(
                            playlistId = playlist.id,
                            viewModel = viewModel,
                            size = 180.dp
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val currentPlaylistName = viewModel.playlists.find { it.id == playlist.id }?.name ?: playlist.name
                        Text(
                            text = currentPlaylistName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 4.dp,
                            shadowElevation = 0.dp
                        ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val settingsManager = SettingsManager.getInstance(LocalContext.current)
                            val isCurrentPlaylistPlaying = playbackManager.activePlaylistId == playlist.id && playbackManager.activeCategory == "PLAYLISTS"
                            var localShuffleState by remember(playlist.id) { mutableStateOf(settingsManager.getPlaylistShuffle(playlist.id)) }
                            val isShuffleActive = if (isCurrentPlaylistPlaying) playbackManager.isShuffle else localShuffleState

                            Column(horizontalAlignment = Alignment.Start) {
                                val totalPlaylistDuration = songs.sumOf { it.duration }
                                Text(
                                    text = formatLongDuration(totalPlaylistDuration),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${songs.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showPlaylistOptions = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                IconButton(
                                    onClick = { showAddSongsDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(R.string.add_songs),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                val isSortActive = playbackManager.sortOption != "ALPHABETICAL" || !playbackManager.isSortAscending
                                Surface(
                                    onClick = onSortClick,
                                    shape = CircleShape,
                                    color = if (isSortActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isSortActive) Icons.Default.Schedule else Icons.Default.SortByAlpha,
                                            contentDescription = null,
                                            tint = if (isSortActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = { 
                                            if (isCurrentPlaylistPlaying) {
                                                playbackManager.toggleShuffle()
                                                localShuffleState = playbackManager.isShuffle
                                            } else {
                                                localShuffleState = !localShuffleState
                                                settingsManager.setPlaylistShuffle(playlist.id, localShuffleState)
                                            }
                                        },
                                        shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                                        color = if (isShuffleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Shuffle,
                                                contentDescription = null,
                                                tint = if (isShuffleActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = { 
                                            if (settingsManager.isHapticVibrationEnabled) {
                                                vibrator.triggerLightVibration()
                                            }
                                            if (isCurrentPlaylistPlaying) {
                                                if (isPlaying) playbackManager.pause() else playbackManager.resume()
                                            } else if (sortedSongs.isNotEmpty()) {
                                                val songToPlay = if (isShuffleActive) sortedSongs.random() else sortedSongs[0]
                                                onSongClick(songToPlay, sortedSongs)
                                            }
                                        },
                                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isCurrentPlaylistPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                            }
                        }
                        }
                    }
                }

                }
                if (sortedSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_songs_in_playlist), 
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    itemsIndexed(sortedSongs, key = { _, it -> it.id }) { index, song ->
                        val isFirst = index == 0
                        val isLast = index == sortedSongs.lastIndex
                        SongItem(
                            isFirst = isFirst,
                            isLast = isLast,
                            song = song, 
                            currentlyPlaying = song.id == currentlyPlayingId, 
                            isPlaying = isPlaying,
                            onClick = { onSongClick(song, sortedSongs) },
                            onOptionsClick = { onOptionsClick(song) },
                            onFavoriteClick = onFavoriteClick
                        )
                    }
                }
            }
        }

        val playlistsCategory = stringResource(R.string.playlists)
        PlaylistOptionsAndRename(
            playlist = if (showPlaylistOptions) playlist else null,
            playlists = viewModel.playlists,
            viewModel = viewModel,
            onDismissRequest = { showPlaylistOptions = false },
            onDeleteConfirm = { deletedPlaylist ->
                val isActive = playbackManager.activePlaylistId == deletedPlaylist.id && playbackManager.activeCategory == playlistsCategory
                viewModel.deletePlaylist(deletedPlaylist)
                if (isActive) playbackManager.stop()
                showPlaylistOptions = false
                onBack()
            }
        )

        if (showAddSongsDialog) {
            val hiddenFolders = SettingsManager.getInstance(LocalContext.current).hiddenFolders
            val allSongs = viewModel.filteredSongs.filter { !hiddenFolders.contains(it.folderName) }
            val alreadyAddedIds = songs.map { it.id }.toSet()
            
            AddSongsToPlaylistDialog(
                playlistId = playlist.id,
                allSongs = allSongs,
                initialSelectedIds = alreadyAddedIds,
                onDismiss = { showAddSongsDialog = false },
                onSave = { toAdd, toRemove ->
                    if (toAdd.isNotEmpty()) {
                        viewModel.addSongsToPlaylist(playlist.id, toAdd) {
                            if (playbackManager.activePlaylistId == playlist.id) {
                                playbackManager.refreshActivePlaylist(viewModel.getSongsForPlaylistSync(playlist.id))
                            }
                        }
                    }
                    if (toRemove.isNotEmpty()) {
                        viewModel.removeSongsFromPlaylist(playlist.id, toRemove) {
                            if (playbackManager.activePlaylistId == playlist.id) {
                                playbackManager.refreshActivePlaylist(viewModel.getSongsForPlaylistSync(playlist.id))
                            }
                        }
                    }
                    showAddSongsDialog = false
                }
            )
        }

        val targetIndex = remember(sortedSongs, currentlyPlayingId) {
            val idx = sortedSongs.indexOfFirst { it.id == currentlyPlayingId }
            if (idx != -1) idx + 1 else -1
        }

        LaunchedEffect(scrollToCurrentTrigger.value) {
            if (targetIndex != -1 && scrollToCurrentTrigger.value > 0) {
                listState.animateScrollToItem(targetIndex)
                scrollToCurrentTrigger.value = 0
            }
        }

        FastScrollbar(
            listState = listState,
            items = sortedSongs,
            headerItemCount = 1,
            itemKeyOrLetter = { if (sortOption == "ALPHABETICAL") it.title else "" },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = bottomPadding)
        )
    }
}

@Composable
fun AlbumDetailView(
    album: Album,
    songs: List<Song>,
    sortOption: String,
    isSortAscending: Boolean,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onOptionsClick: (Song) -> Unit,
    onSortClick: () -> Unit,
    currentlyPlayingId: Long?,
    bottomPadding: Dp,
    onFavoriteClick: ((Song) -> Unit)? = null,
    scrollToCurrentTrigger: MutableState<Int>
) {
    val context = LocalContext.current
    val playbackManager = PlaybackManager.getInstance(context)
    val settingsManager = SettingsManager.getInstance(context)
    val vibrator = context.getSystemService(Vibrator::class.java)!!
    val listState = rememberLazyListState()
    val isPlaying = playbackManager.isPlaying

    val sortedSongs = remember(songs, sortOption, isSortAscending) {
        playbackManager.getSortedList(songs, sortOption, isSortAscending)
    }
    
    val headerAlpha by remember {
        derivedStateOf {
            val firstItemIndex = listState.firstVisibleItemIndex
            val firstItemOffset = listState.firstVisibleItemScrollOffset
            if (firstItemIndex > 0) 0f
            else (1f - (firstItemOffset / 600f)).coerceIn(0f, 1f)
        }
    }
    
    val headerScale by remember {
        derivedStateOf {
            val firstItemIndex = listState.firstVisibleItemIndex
            val firstItemOffset = listState.firstVisibleItemScrollOffset
            if (firstItemIndex > 0) 0.8f
            else (1f - (firstItemOffset / 1200f)).coerceIn(0.8f, 1f)
        }
    }

    BackHandler(onBack = onBack)

    val backgroundCover = remember(album) {
        album.songs.firstOrNull()?.let { it.coverUrl ?: it.uri }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (backgroundCover != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp)
                                    .offset(y = (-50).dp)
                                    .graphicsLayer {
                                        alpha = headerAlpha
                                    }
                            ) {
                                AsyncImage(
                                    model = backgroundCover,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().blur(60.dp).alpha(0.4f),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                    MaterialTheme.colorScheme.surface
                                                ),
                                                startY = 0f
                                            )
                                        )
                                )
                            }
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = headerAlpha
                                    scaleX = headerScale
                                    scaleY = headerScale
                                }
                                .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(55.dp))
                        
                        val albumCoverBytes = remember(album) {
                            album.songs.firstOrNull()?.let { 
                                it.coverUrl ?: it.uri 
                            }
                        }
                        
                        AsyncImage(
                            model = albumCoverBytes ?: R.drawable.ic_launcher_foreground,
                            contentDescription = null,
                            modifier = Modifier
                                .size(180.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )

                        if (album.artist.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = album.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 4.dp,
                            shadowElevation = 0.dp
                        ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val albumId = album.id
                            val isCurrentAlbumPlaying = playbackManager.activePlaylistId == album.id && playbackManager.activeCategory == "ALBUMS"
                            var localShuffleState by remember(album.id) { mutableStateOf(settingsManager.getPlaylistShuffle(album.id)) }
                            val isShuffleActive = if (isCurrentAlbumPlaying) playbackManager.isShuffle else localShuffleState

                            Column(horizontalAlignment = Alignment.Start) {
                                val totalDuration = album.songs.sumOf { it.duration }
                                Text(
                                    text = formatLongDuration(totalDuration),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${album.songs.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isSortActive = playbackManager.sortOption != "ALPHABETICAL" || !playbackManager.isSortAscending
                                Surface(
                                    onClick = onSortClick,
                                    shape = CircleShape,
                                    color = if (isSortActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isSortActive) Icons.Default.Schedule else Icons.Default.SortByAlpha,
                                            contentDescription = null,
                                            tint = if (isSortActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = { 
                                            if (isCurrentAlbumPlaying) {
                                                playbackManager.toggleShuffle()
                                                localShuffleState = playbackManager.isShuffle
                                            } else {
                                                localShuffleState = !localShuffleState
                                                settingsManager.setPlaylistShuffle(albumId, localShuffleState)
                                            }
                                        },
                                        shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                                        color = if (isShuffleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Shuffle,
                                                contentDescription = null,
                                                tint = if (isShuffleActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = { 
                                            if (settingsManager.isHapticVibrationEnabled) {
                                                vibrator.triggerLightVibration()
                                            }
                                            if (isCurrentAlbumPlaying) {
                                                if (isPlaying) playbackManager.pause() else playbackManager.resume()
                                            } else if (sortedSongs.isNotEmpty()) {
                                                val songToPlay = if (isShuffleActive) sortedSongs.random() else sortedSongs[0]
                                                onSongClick(songToPlay, sortedSongs)
                                            }
                                        },
                                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isCurrentAlbumPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }
 
                }
                if (sortedSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (album.artist.isNotEmpty()) "No hay canciones de este álbum"
                                else "No hay canciones de este artista",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    itemsIndexed(sortedSongs, key = { _, it -> it.id }) { index, song ->
                        val isFirst = index == 0
                        val isLast = index == sortedSongs.lastIndex
                        SongItem(
                            isFirst = isFirst,
                            isLast = isLast,
                            song = song, 
                            currentlyPlaying = song.id == currentlyPlayingId, 
                            isPlaying = isPlaying,
                            onClick = { onSongClick(song, sortedSongs) },
                            onOptionsClick = { onOptionsClick(song) },
                            onFavoriteClick = onFavoriteClick
                        )
                    }
                }
            }
        }

        val targetIndex = remember(sortedSongs, currentlyPlayingId) {
            val idx = sortedSongs.indexOfFirst { it.id == currentlyPlayingId }
            if (idx != -1) idx + 1 else -1
        }

        LaunchedEffect(scrollToCurrentTrigger.value) {
            if (targetIndex != -1 && scrollToCurrentTrigger.value > 0) {
                listState.animateScrollToItem(targetIndex)
                scrollToCurrentTrigger.value = 0
            }
        }

        FastScrollbar(
            listState = listState,
            items = sortedSongs,
            headerItemCount = 1,
            itemKeyOrLetter = { if (sortOption == "ALPHABETICAL") it.title else "" },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = bottomPadding)
        )
    }
}

@Composable
fun FolderDetailView(
    folderName: String,
    songs: List<Song>,
    sortOption: String,
    isSortAscending: Boolean,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onOptionsClick: (Song) -> Unit,
    onSortClick: () -> Unit,
    currentlyPlayingId: Long?,
    bottomPadding: Dp,
    onFavoriteClick: ((Song) -> Unit)? = null,
    scrollToCurrentTrigger: MutableState<Int>
) {
    val playbackManager = PlaybackManager.getInstance(LocalContext.current)
    val settingsManager = SettingsManager.getInstance(LocalContext.current)
    val vibrator = LocalContext.current.getSystemService(Vibrator::class.java)!!
    val listState = rememberLazyListState()
    val isPlaying = playbackManager.isPlaying

    val sortedSongs = remember(songs, sortOption, isSortAscending) {
        playbackManager.getSortedList(songs, sortOption, isSortAscending)
    }

    val headerAlpha by remember {
        derivedStateOf {
            val firstItemIndex = listState.firstVisibleItemIndex
            val firstItemOffset = listState.firstVisibleItemScrollOffset
            if (firstItemIndex > 0) 0f
            else (1f - (firstItemOffset / 600f)).coerceIn(0f, 1f)
        }
    }

    val headerScale by remember {
        derivedStateOf {
            val firstItemIndex = listState.firstVisibleItemIndex
            val firstItemOffset = listState.firstVisibleItemScrollOffset
            if (firstItemIndex > 0) 0.8f
            else (1f - (firstItemOffset / 1200f)).coerceIn(0.8f, 1f)
        }
    }

    val backgroundCover = remember(songs) {
        songs.firstOrNull()?.let { it.coverUrl ?: it.uri }
    }

    val covers = remember(songs) {
        songs.map { it.coverUrl ?: it.uri }.distinct().take(4)
    }

    val folderId = folderName.hashCode().toLong()
    val isCurrentFolderPlaying = playbackManager.activePlaylistId == folderId && playbackManager.activeCategory == "FOLDERS"
    var localShuffleState by remember(folderId) { mutableStateOf(settingsManager.getPlaylistShuffle(folderId)) }
    val isShuffleActive = if (isCurrentFolderPlaying) playbackManager.isShuffle else localShuffleState

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
            ) {

                item {
                    Box(modifier = Modifier.fillMaxWidth()) {

                        if (backgroundCover != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp)
                                    .offset(y = (-50).dp)
                                    .graphicsLayer { alpha = headerAlpha }
                            ) {
                                AsyncImage(
                                    model = backgroundCover,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().blur(60.dp).alpha(0.4f),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                    MaterialTheme.colorScheme.surface
                                                ),
                                                startY = 0f
                                            )
                                        )
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = headerAlpha
                                    scaleX = headerScale
                                    scaleY = headerScale
                                }
                                .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(55.dp))

                            Surface(
                                modifier = Modifier.size(180.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                if (covers.isEmpty()) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                } else if (covers.size == 1) {
                                    AsyncImage(
                                        model = covers[0],
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(modifier = Modifier.weight(1f)) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                if (covers.size > 0) {
                                                    AsyncImage(
                                                        model = covers[0],
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                if (covers.size > 1) {
                                                    AsyncImage(
                                                        model = covers[1],
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                        }
                                        Row(modifier = Modifier.weight(1f)) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                if (covers.size > 2) {
                                                    AsyncImage(
                                                        model = covers[2],
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                if (covers.size > 3) {
                                                    AsyncImage(
                                                        model = covers[3],
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 4.dp,
                                shadowElevation = 0.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        val totalDuration = songs.sumOf { it.duration }
                                        Text(
                                            text = formatLongDuration(totalDuration),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.MusicNote,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${songs.size}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            val songsLabel = if (songs.size == 1) stringResource(R.string.song_singular) else stringResource(R.string.song_plural)
                                            Text(
                                                text = songsLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${songs.size}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val isSortActive = playbackManager.sortOption != "ALPHABETICAL" || !playbackManager.isSortAscending
                                        Surface(
                                            onClick = onSortClick,
                                            shape = CircleShape,
                                            color = if (isSortActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isSortActive) Icons.Default.Schedule else Icons.Default.SortByAlpha,
                                                    contentDescription = null,
                                                    tint = if (isSortActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                onClick = {
                                                    if (isCurrentFolderPlaying) {
                                                        playbackManager.toggleShuffle()
                                                        localShuffleState = playbackManager.isShuffle
                                                    } else {
                                                        localShuffleState = !localShuffleState
                                                        settingsManager.setPlaylistShuffle(folderId, localShuffleState)
                                                    }
                                                },
                                                shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                                                color = if (isShuffleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Shuffle,
                                                        contentDescription = null,
                                                        tint = if (isShuffleActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                onClick = {
                                                    if (settingsManager.isHapticVibrationEnabled) vibrator.triggerLightVibration()
                                                    if (isCurrentFolderPlaying) {
                                                        if (isPlaying) playbackManager.pause() else playbackManager.resume()
                                                    } else if (sortedSongs.isNotEmpty()) {
                                                        val songToPlay = if (isShuffleActive) sortedSongs.random() else sortedSongs[0]
                                                        onSongClick(songToPlay, sortedSongs)
                                                    }
                                                },
                                                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (isCurrentFolderPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (sortedSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_songs_in_playlist),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    itemsIndexed(sortedSongs, key = { _, it -> it.id }) { index, song ->
                        val isFirst = index == 0
                        val isLast = index == sortedSongs.lastIndex
                        SongItem(
                            isFirst = isFirst,
                            isLast = isLast,
                            song = song, 
                            currentlyPlaying = song.id == currentlyPlayingId, 
                            isPlaying = isPlaying,
                            onClick = { onSongClick(song, sortedSongs) },
                            onOptionsClick = { onOptionsClick(song) },
                            onFavoriteClick = onFavoriteClick
                        )
                    }
                }
            }
        }
        val targetIndex = remember(sortedSongs, currentlyPlayingId) {
            val idx = sortedSongs.indexOfFirst { it.id == currentlyPlayingId }
            if (idx != -1) idx + 1 else -1
        }

        LaunchedEffect(scrollToCurrentTrigger.value) {
            if (targetIndex != -1 && scrollToCurrentTrigger.value > 0) {
                listState.animateScrollToItem(targetIndex)
                scrollToCurrentTrigger.value = 0
            }
        }

        FastScrollbar(
            listState = listState,
            items = sortedSongs,
            headerItemCount = 1,
            itemKeyOrLetter = { if (sortOption == "ALPHABETICAL") it.title else "" },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = bottomPadding)
        )
    }
}
