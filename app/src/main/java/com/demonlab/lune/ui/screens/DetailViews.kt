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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.components.FastScrollbar
import com.demonlab.lune.ui.components.ScrollToTopPill
import com.demonlab.lune.ui.components.rememberScrollToTopVisibility
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.components.SongItem
import com.demonlab.lune.ui.data.Album
import com.demonlab.lune.ui.playlist.PlaylistOptionsAndRename
import com.demonlab.lune.ui.playlist.PlaylistPreviewCovers
import com.demonlab.lune.ui.sheets.AddSongsToPlaylistDialog
import com.demonlab.lune.ui.utils.formatLongDuration
import com.demonlab.lune.ui.utils.triggerLightVibration
import com.demonlab.lune.ui.utils.rememberReorderableState
import com.demonlab.lune.ui.utils.reorderable
import com.demonlab.lune.ui.utils.reorderableItem
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
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
    scrollToCurrentTrigger: MutableState<Int>,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0
) {
    val playbackManager = PlaybackManager.getInstance(LocalContext.current)
    val settingsManager = SettingsManager.getInstance(LocalContext.current)
    val vibrator = LocalContext.current.getSystemService(Vibrator::class.java)!!
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showPlaylistOptions by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    val isPlaying = playbackManager.isPlaying
    val activeControlsPrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)
    val stretchProgress = remember { Animatable(0f) }
    
    val sortedSongs = remember(songs, sortOption, isSortAscending) {
        playbackManager.getSortedList(songs, sortOption, isSortAscending)
    }

    val isCustomOrder = sortOption == "CUSTOM"
    var currentPlaylistSongs by remember(sortedSongs) { mutableStateOf(sortedSongs) }
    LaunchedEffect(sortedSongs) {
        currentPlaylistSongs = sortedSongs
    }

    val reorderState = rememberReorderableState(
        listState = listState,
        canDragOver = { it >= 1 },
        onDragEnd = {
            if (isCustomOrder) {
                viewModel.savePlaylistOrder(playlist.id, currentPlaylistSongs.map { it.id })
            }
        }
    ) { from, to ->
        if (isCustomOrder) {
            val fromIdx = from - 1
            val toIdx = to - 1
            if (fromIdx in currentPlaylistSongs.indices && toIdx in currentPlaylistSongs.indices) {
                val mutable = currentPlaylistSongs.toMutableList()
                val moved = mutable.removeAt(fromIdx)
                mutable.add(toIdx, moved)
                currentPlaylistSongs = mutable
            }
        }
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

    AppBlurBackdrop(
        hasBlurBackground = hasBlurBackground,
        isDarkTheme = isDarkTheme,
        currentSong = playbackManager.currentSong
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isCustomOrder) Modifier.reorderable(reorderState) else Modifier),
                contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (backgroundCover != null && !hasBlurBackground) {
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
                            color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val isCurrentPlaylistPlaying = playbackManager.activePlaylistId == playlist.id && playbackManager.activeCategory == "PLAYLISTS"
                        val isPillPlaying = isCurrentPlaylistPlaying && isPlaying

                        val pillCornerRadius by animateDpAsState(
                            targetValue = if (isPillPlaying) 14.dp else 28.dp,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                            label = "playlistPillCornerRadius"
                        )
                        val iconCornerRadius by animateDpAsState(
                            targetValue = if (isPillPlaying) 10.dp else 20.dp,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                            label = "playlistIconCornerRadius"
                        )

                        val dynamicPrimary = if (useCustomControlsColor) activeControlsPrimary else MaterialTheme.colorScheme.primary
                        val pillBg = if (hasBlurBackground) dynamicPrimary.copy(alpha = 0.85f) else dynamicPrimary
                        val pillContentColor = if (pillBg.luminance() > 0.45f) {
                            MaterialTheme.colorScheme.onPrimary.takeIf { it.luminance() < 0.4f } ?: Color(0xFF141316)
                        } else {
                            MaterialTheme.colorScheme.onPrimary.takeIf { it.luminance() > 0.6f } ?: Color.White
                        }

                        val actionBtnInactiveBg = if (hasBlurBackground) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
                        val actionBtnInactiveTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                        val actionBtnActiveBg = if (useCustomControlsColor) activeControlsPrimary else if (hasBlurBackground) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary
                        val actionBtnActiveTint = if (actionBtnActiveBg.luminance() > 0.45f) {
                            MaterialTheme.colorScheme.onPrimary.takeIf { it.luminance() < 0.4f } ?: Color(0xFF141316)
                        } else {
                            MaterialTheme.colorScheme.onPrimary.takeIf { it.luminance() > 0.6f } ?: Color.White
                        }

                        val onPlayWithStretch = {
                            scope.launch {
                                stretchProgress.snapTo(0f)
                                stretchProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
                                )
                                stretchProgress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            if (settingsManager.isHapticVibrationEnabled) {
                                vibrator.triggerLightVibration()
                            }
                            if (isCurrentPlaylistPlaying) {
                                if (isPlaying) playbackManager.pause() else playbackManager.resume()
                            } else if (sortedSongs.isNotEmpty()) {
                                val songToPlay = if (playbackManager.isShuffle) sortedSongs.random() else sortedSongs[0]
                                onSongClick(songToPlay, sortedSongs)
                            }
                        }

                        val extraLateralPadding = (stretchProgress.value * 14f).coerceAtLeast(0f).dp

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Botón de 3 puntos a la izquierda
                            Surface(
                                onClick = { showPlaylistOptions = true },
                                shape = RoundedCornerShape(14.dp),
                                color = actionBtnInactiveBg,
                                modifier = Modifier
                                    .size(44.dp)
                                    .graphicsLayer {
                                        translationX = -stretchProgress.value * 8.dp.toPx()
                                    }
                                    .bounceClick()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.player_options),
                                        tint = actionBtnInactiveTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // 2. Píldora de Play en el centro
                            Surface(
                                onClick = onPlayWithStretch,
                                shape = RoundedCornerShape(pillCornerRadius),
                                color = pillBg,
                                modifier = Modifier
                                    .height(48.dp)
                                    .graphicsLayer {
                                        scaleY = 1f - (stretchProgress.value * 0.08f)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 6.dp + extraLateralPadding,
                                        end = 16.dp + extraLateralPadding
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(iconCornerRadius),
                                        color = pillContentColor.copy(alpha = 0.22f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isPillPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isPillPlaying) stringResource(R.string.play_state_pause) else stringResource(R.string.play_state_play),
                                                tint = pillContentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    AnimatedContent(
                                        targetState = isPillPlaying,
                                        transitionSpec = {
                                            (slideInVertically { h -> h / 2 } + fadeIn(tween(150)))
                                                .togetherWith(slideOutVertically { h -> -h / 2 } + fadeOut(tween(150)))
                                        },
                                        label = "playlistPillText"
                                    ) { playing ->
                                        Text(
                                            text = if (playing) stringResource(R.string.play_state_pause) else stringResource(R.string.play_state_play),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = pillContentColor
                                        )
                                    }
                                }
                            }

                            // 3. Sort a la derecha
                            val isSortActive = playbackManager.sortOption != "ALPHABETICAL" || !playbackManager.isSortAscending
                            Surface(
                                onClick = onSortClick,
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSortActive) actionBtnActiveBg else actionBtnInactiveBg,
                                modifier = Modifier
                                    .size(44.dp)
                                    .graphicsLayer {
                                        translationX = stretchProgress.value * 8.dp.toPx()
                                    }
                                    .bounceClick()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isSortActive) Icons.Default.Schedule else Icons.Default.SortByAlpha,
                                        contentDescription = stringResource(R.string.sort_options_title),
                                        tint = if (isSortActive) actionBtnActiveTint else actionBtnInactiveTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Info: time + separador + número de canciones
                        val totalPlaylistDuration = remember(songs) { songs.sumOf { it.duration } }
                        val songsCountText = if (songs.size == 1) "1 ${stringResource(R.string.song_singular)}" else stringResource(R.string.ai_mix_songs_count, songs.size)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatLongDuration(totalPlaylistDuration)} • $songsCountText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    itemsIndexed(currentPlaylistSongs, key = { _, it -> it.id }) { index, song ->
                        val isFirst = index == 0
                        val isLast = index == currentPlaylistSongs.lastIndex
                        SongItem(
                            isFirst = isFirst,
                            isLast = isLast,
                            song = song, 
                            currentlyPlaying = song.id == currentlyPlayingId, 
                            isPlaying = isPlaying,
                            hasBlurBackground = hasBlurBackground,
                            useCustomControlsColor = useCustomControlsColor,
                            controlsColorPalette = controlsColorPalette,
                            onClick = { onSongClick(song, currentPlaylistSongs) },
                            onOptionsClick = { onOptionsClick(song) },
                            onFavoriteClick = onFavoriteClick,
                            modifier = if (isCustomOrder) Modifier.reorderableItem(reorderState, index + 1) else Modifier
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
            },
            onAddSongsClick = {
                showAddSongsDialog = true
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
            hasBlurBackground = hasBlurBackground,
            useCustomControlsColor = useCustomControlsColor,
            controlsColorPalette = controlsColorPalette,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = bottomPadding)
        )

        val isScrollToTopVisible = rememberScrollToTopVisibility(listState, settingsManager.isScrollToTopEnabled)
        ScrollToTopPill(
            visible = isScrollToTopVisible.value,
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 14.dp),
            hasBlurBackground = hasBlurBackground,
            isDarkTheme = isDarkTheme,
            customActiveColor = if (useCustomControlsColor) activeControlsPrimary else null
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
    scrollToCurrentTrigger: MutableState<Int>,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0
) {
    val context = LocalContext.current
    val playbackManager = PlaybackManager.getInstance(context)
    val settingsManager = SettingsManager.getInstance(context)
    val vibrator = context.getSystemService(Vibrator::class.java)!!
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isPlaying = playbackManager.isPlaying
    val activeControlsPrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)

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

    AppBlurBackdrop(
        hasBlurBackground = hasBlurBackground,
        isDarkTheme = isDarkTheme,
        currentSong = playbackManager.currentSong
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (backgroundCover != null && !hasBlurBackground) {
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
                        
                        SongCoverImage(
                            coverUrl = albumCoverBytes,
                            contentDescription = null,
                            modifier = Modifier.size(180.dp),
                            shape = CircleShape
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
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
                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.25f)) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = if (hasBlurBackground) 0.dp else 4.dp,
                            shadowElevation = 0.dp
                        ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val albumId = album.id
                            val isCurrentAlbumPlaying = playbackManager.activePlaylistId == album.id
                            var localShuffleState by remember(album.id) { mutableStateOf(settingsManager.getPlaylistShuffle(album.id)) }
                            val isShuffleActive = if (isCurrentAlbumPlaying) playbackManager.isShuffle else localShuffleState

                            Column(horizontalAlignment = Alignment.Start) {
                                val totalDuration = album.songs.sumOf { it.duration }
                                Text(
                                    text = formatLongDuration(totalDuration),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${album.songs.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isSortActive = playbackManager.sortOption != "ALPHABETICAL" || !playbackManager.isSortAscending
                                val sortActiveBg = if (useCustomControlsColor) activeControlsPrimary else if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                val sortInactiveBg = if (hasBlurBackground) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
                                val sortActiveTint = if (useCustomControlsColor) Color.White else if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary
                                val sortInactiveTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

                                Surface(
                                    onClick = onSortClick,
                                    shape = CircleShape,
                                    color = if (isSortActive) sortActiveBg else sortInactiveBg,
                                    modifier = Modifier.size(36.dp).bounceClick()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isSortActive) Icons.Default.Schedule else Icons.Default.SortByAlpha,
                                            contentDescription = null,
                                            tint = if (isSortActive) sortActiveTint else sortInactiveTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))

                                val shuffleActiveBg = if (useCustomControlsColor) activeControlsPrimary else if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                val shuffleInactiveBg = if (hasBlurBackground) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
                                val shuffleActiveTint = if (useCustomControlsColor) Color.White else if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary
                                val shuffleInactiveTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = { 
                                            playbackManager.toggleShuffle()
                                            localShuffleState = playbackManager.isShuffle
                                            settingsManager.setPlaylistShuffle(albumId, playbackManager.isShuffle)
                                        },
                                        shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                                        color = if (isShuffleActive) shuffleActiveBg else shuffleInactiveBg,
                                        modifier = Modifier.size(44.dp).bounceClick()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Shuffle,
                                                contentDescription = null,
                                                tint = if (isShuffleActive) shuffleActiveTint else shuffleInactiveTint,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    val playBg = if (useCustomControlsColor) activeControlsPrimary else if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                    val playTint = if (useCustomControlsColor) Color.White else if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary

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
                                        color = playBg,
                                        modifier = Modifier.size(44.dp).bounceClick()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isCurrentAlbumPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                                contentDescription = null,
                                                tint = playTint,
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
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
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
                            hasBlurBackground = hasBlurBackground,
                            useCustomControlsColor = useCustomControlsColor,
                            controlsColorPalette = controlsColorPalette,
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
            hasBlurBackground = hasBlurBackground,
            useCustomControlsColor = useCustomControlsColor,
            controlsColorPalette = controlsColorPalette,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = bottomPadding)
        )

        val isScrollToTopVisible = rememberScrollToTopVisibility(listState, settingsManager.isScrollToTopEnabled)
        ScrollToTopPill(
            visible = isScrollToTopVisible.value,
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 14.dp),
            hasBlurBackground = hasBlurBackground,
            isDarkTheme = isDarkTheme,
            customActiveColor = if (useCustomControlsColor) activeControlsPrimary else null
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
    scrollToCurrentTrigger: MutableState<Int>,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0
) {
    val playbackManager = PlaybackManager.getInstance(LocalContext.current)
    val settingsManager = SettingsManager.getInstance(LocalContext.current)
    val vibrator = LocalContext.current.getSystemService(Vibrator::class.java)!!
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isPlaying = playbackManager.isPlaying
    val activeControlsPrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)

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

    AppBlurBackdrop(
        hasBlurBackground = hasBlurBackground,
        isDarkTheme = isDarkTheme,
        currentSong = playbackManager.currentSong
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
            ) {

                item {
                    Box(modifier = Modifier.fillMaxWidth()) {

                        if (backgroundCover != null && !hasBlurBackground) {
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
                                val validCovers = remember(covers) { covers.filterNotNull() }
                                var failedIndices by remember(covers) { mutableStateOf(setOf<Int>()) }

                                if (validCovers.isEmpty() || failedIndices.size >= validCovers.take(4).size) {
                                    SongCoverImage(
                                        coverUrl = null,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        shape = CircleShape
                                    )
                                } else if (validCovers.size == 1) {
                                    SongCoverImage(
                                        coverUrl = validCovers[0],
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        shape = CircleShape,
                                        onError = { failedIndices = failedIndices + 0 }
                                    )
                                } else {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(modifier = Modifier.weight(1f)) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                if (validCovers.size > 0) {
                                                    SongCoverImage(
                                                        coverUrl = validCovers[0],
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        shape = RoundedCornerShape(0.dp),
                                                        onError = { failedIndices = failedIndices + 0 }
                                                    )
                                                }
                                            }
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                if (validCovers.size > 1) {
                                                    SongCoverImage(
                                                        coverUrl = validCovers[1],
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        shape = RoundedCornerShape(0.dp),
                                                        onError = { failedIndices = failedIndices + 1 }
                                                    )
                                                }
                                            }
                                        }
                                        Row(modifier = Modifier.weight(1f)) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                if (validCovers.size > 2) {
                                                    SongCoverImage(
                                                        coverUrl = validCovers[2],
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        shape = RoundedCornerShape(0.dp),
                                                        onError = { failedIndices = failedIndices + 2 }
                                                    )
                                                }
                                            }
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                if (validCovers.size > 3) {
                                                    SongCoverImage(
                                                        coverUrl = validCovers[3],
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        shape = RoundedCornerShape(0.dp),
                                                        onError = { failedIndices = failedIndices + 3 }
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
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.25f)) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = if (hasBlurBackground) 0.dp else 4.dp,
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
                                            color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.MusicNote,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${songs.size}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            val songsLabel = if (songs.size == 1) stringResource(R.string.song_singular) else stringResource(R.string.song_plural)
                                            Text(
                                                text = songsLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${songs.size}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val isSortActive = playbackManager.sortOption != "ALPHABETICAL" || !playbackManager.isSortAscending
                                        val sortActiveBg = if (useCustomControlsColor) activeControlsPrimary else if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                        val sortInactiveBg = if (hasBlurBackground) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
                                        val sortActiveTint = if (useCustomControlsColor) Color.White else if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary
                                        val sortInactiveTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

                                        Surface(
                                            onClick = onSortClick,
                                            shape = CircleShape,
                                            color = if (isSortActive) sortActiveBg else sortInactiveBg,
                                            modifier = Modifier.size(36.dp).bounceClick()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isSortActive) Icons.Default.Schedule else Icons.Default.SortByAlpha,
                                                    contentDescription = null,
                                                    tint = if (isSortActive) sortActiveTint else sortInactiveTint,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        val shuffleActiveBg = if (useCustomControlsColor) activeControlsPrimary else if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                        val shuffleInactiveBg = if (hasBlurBackground) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
                                        val shuffleActiveTint = if (useCustomControlsColor) Color.White else if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary
                                        val shuffleInactiveTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

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
                                                color = if (isShuffleActive) shuffleActiveBg else shuffleInactiveBg,
                                                modifier = Modifier.size(44.dp).bounceClick()
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Shuffle,
                                                        contentDescription = null,
                                                        tint = if (isShuffleActive) shuffleActiveTint else shuffleInactiveTint,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }

                                            val playBg = if (useCustomControlsColor) activeControlsPrimary else if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                            val playTint = if (useCustomControlsColor) Color.White else if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary

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
                                                color = playBg,
                                                modifier = Modifier.size(44.dp).bounceClick()
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (isCurrentFolderPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = playTint,
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
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
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
                            hasBlurBackground = hasBlurBackground,
                            useCustomControlsColor = useCustomControlsColor,
                            controlsColorPalette = controlsColorPalette,
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
            hasBlurBackground = hasBlurBackground,
            useCustomControlsColor = useCustomControlsColor,
            controlsColorPalette = controlsColorPalette,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = bottomPadding)
        )

        val isScrollToTopVisible = rememberScrollToTopVisibility(listState, settingsManager.isScrollToTopEnabled)
        ScrollToTopPill(
            visible = isScrollToTopVisible.value,
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 14.dp),
            hasBlurBackground = hasBlurBackground,
            isDarkTheme = isDarkTheme,
            customActiveColor = if (useCustomControlsColor) activeControlsPrimary else null
        )
    }
}
