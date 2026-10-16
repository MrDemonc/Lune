package com.demonlab.lune.ui.screens

import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random
import com.demonlab.lune.ui.playlist.PlaylistOptionsAndRename
import com.demonlab.lune.ui.sheets.AddSongsToPlaylistDialog
import com.demonlab.lune.ui.utils.formatLongDuration
import com.demonlab.lune.ui.utils.triggerLightVibration
import com.demonlab.lune.ui.utils.rememberReorderableState
import com.demonlab.lune.ui.utils.reorderable
import com.demonlab.lune.ui.utils.reorderableItem
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.utils.fastSmoothScrollToItem
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

    val allCovers = remember(songs) {
        songs.groupBy { song ->
            song.coverUrl ?: song.albumArtUri?.toString() ?: song.albumId.takeIf { it > 0 } ?: song.uri
        }.values.mapNotNull { group ->
            group.firstOrNull()?.let { it.coverUrl ?: it.albumArtUri ?: it.uri }
        }
    }

    var currentCoverIndex by remember(allCovers) { mutableIntStateOf(0) }

    LaunchedEffect(allCovers) {
        if (allCovers.size > 1) {
            while (isActive) {
                delay(5000L)
                val nextIndex = if (allCovers.size == 2) {
                    1 - currentCoverIndex
                } else {
                    var candidate = Random.nextInt(allCovers.size)
                    while (candidate == currentCoverIndex) {
                        candidate = Random.nextInt(allCovers.size)
                    }
                    candidate
                }
                currentCoverIndex = nextIndex
            }
        }
    }

    val activeCover = remember(allCovers, currentCoverIndex) {
        allCovers.getOrNull(currentCoverIndex)
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
                        if (activeCover != null) {
                            val fadeBrush = remember {
                                Brush.verticalGradient(
                                    0.0f to Color.Black,
                                    0.40f to Color.Black,
                                    0.70f to Color.Black.copy(alpha = 0.50f),
                                    0.88f to Color.Black.copy(alpha = 0.15f),
                                    1.0f to Color.Transparent
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(380.dp)
                                    .offset(y = (-50).dp)
                                    .graphicsLayer {
                                        alpha = headerAlpha
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = fadeBrush,
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                            ) {
                                Crossfade(
                                    targetState = activeCover,
                                    animationSpec = tween(durationMillis = 1000),
                                    label = "playlistCoverCrossfade"
                                ) { cover ->
                                    AsyncImage(
                                        model = cover,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                0.0f to Color.Transparent,
                                                0.50f to Color.Transparent,
                                                1.0f to (if (hasBlurBackground) Color.Black.copy(alpha = 0.40f) else Color.Transparent)
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
                            Spacer(modifier = Modifier.height(260.dp))
                        
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
                        val isSystemDark = isSystemInDarkTheme()
                        val effectiveDarkTheme = isDarkTheme || when (settingsManager.themeMode) {
                            1 -> false
                            2 -> true
                            else -> isSystemDark
                        }

                        val pillBg = if (hasBlurBackground && !useCustomControlsColor) {
                            if (effectiveDarkTheme) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.10f)
                        } else if (hasBlurBackground) {
                            dynamicPrimary.copy(alpha = 0.85f)
                        } else {
                            dynamicPrimary
                        }

                        val pillContentColor = if (hasBlurBackground && !useCustomControlsColor) {
                            if (effectiveDarkTheme) Color.White else Color(0xFF141316)
                        } else if (pillBg.luminance() > 0.45f) {
                            MaterialTheme.colorScheme.onPrimary.takeIf { it.luminance() < 0.4f } ?: Color(0xFF141316)
                        } else {
                            MaterialTheme.colorScheme.onPrimary.takeIf { it.luminance() > 0.6f } ?: Color.White
                        }

                        val actionBtnInactiveBg = if (hasBlurBackground) {
                            if (effectiveDarkTheme) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f)
                        } else MaterialTheme.colorScheme.secondaryContainer

                        val actionBtnInactiveTint = if (hasBlurBackground) {
                            if (effectiveDarkTheme) Color.White else Color(0xFF141316)
                        } else MaterialTheme.colorScheme.onSecondaryContainer

                        val actionBtnActiveBg = if (useCustomControlsColor) {
                            activeControlsPrimary
                        } else if (hasBlurBackground) {
                            if (effectiveDarkTheme) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.18f)
                        } else MaterialTheme.colorScheme.primary

                        val actionBtnActiveTint = if (hasBlurBackground && !useCustomControlsColor) {
                            if (effectiveDarkTheme) Color.White else Color(0xFF141316)
                        } else if (actionBtnActiveBg.luminance() > 0.45f) {
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
                    itemsIndexed(
                        currentPlaylistSongs,
                        key = { _, it -> it.id },
                        contentType = { _, _ -> "song_item" }
                    ) { index, song ->
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
                listState.fastSmoothScrollToItem(targetIndex)
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
                    listState.fastSmoothScrollToItem(0)
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
    controlsColorPalette: Int = 0,
    isArtistView: Boolean = album.artist.isEmpty()
) {
    val context = LocalContext.current
    val playbackManager = PlaybackManager.getInstance(context)
    val settingsManager = SettingsManager.getInstance(context)
    val vibrator = context.getSystemService(Vibrator::class.java)!!
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isPlaying = playbackManager.isPlaying
    val activeControlsPrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)
    val stretchProgress = remember { Animatable(0f) }

    var selectedAlbumName by remember(album.id) { mutableStateOf<String?>(null) }

    val artistAlbums = remember(songs, isArtistView) {
        if (!isArtistView) {
            emptyList()
        } else {
            songs
                .groupBy { it.album.trim() }
                .filter { (albumName, _) ->
                    albumName.isNotBlank() &&
                    !albumName.equals("<unknown>", ignoreCase = true) &&
                    !albumName.equals("unknown", ignoreCase = true)
                }
                .map { (albumName, albumSongs) ->
                    Album(
                        id = albumName.hashCode().toLong(),
                        name = albumName,
                        artist = albumSongs.firstOrNull()?.artist ?: "",
                        albumArtUri = albumSongs.firstOrNull()?.albumArtUri,
                        coverUrl = albumSongs.firstOrNull()?.coverUrl,
                        songs = albumSongs
                    )
                }
                .sortedBy { it.name.lowercase(java.util.Locale.getDefault()) }
        }
    }

    val singlesCount = remember(songs, isArtistView) {
        if (!isArtistView) 0
        else songs.count {
            it.album.isBlank() ||
            it.album.equals("<unknown>", ignoreCase = true) ||
            it.album.equals("unknown", ignoreCase = true)
        }
    }

    val displayedSongs = remember(songs, selectedAlbumName) {
        if (selectedAlbumName == null) {
            songs
        } else if (selectedAlbumName == "__SINGLES__") {
            songs.filter {
                it.album.isBlank() ||
                it.album.equals("<unknown>", ignoreCase = true) ||
                it.album.equals("unknown", ignoreCase = true)
            }
        } else {
            songs.filter { it.album.trim().equals(selectedAlbumName!!.trim(), ignoreCase = true) }
        }
    }

    val sortedSongs = remember(displayedSongs, sortOption, isSortAscending) {
        playbackManager.getSortedList(displayedSongs, sortOption, isSortAscending)
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

    BackHandler(onBack = {
        if (selectedAlbumName != null) {
            selectedAlbumName = null
        } else {
            onBack()
        }
    })

    val allCovers = remember(songs, album) {
        val list = if (songs.isNotEmpty()) songs else album.songs
        list.groupBy { song ->
            song.coverUrl ?: song.albumArtUri?.toString() ?: song.albumId.takeIf { it > 0 } ?: song.uri
        }.values.mapNotNull { group ->
            group.firstOrNull()?.let { it.coverUrl ?: it.albumArtUri ?: it.uri }
        }
    }

    var currentCoverIndex by remember(allCovers) { mutableIntStateOf(0) }

    LaunchedEffect(allCovers) {
        if (allCovers.size > 1) {
            while (isActive) {
                delay(5000L)
                val nextIndex = if (allCovers.size == 2) {
                    1 - currentCoverIndex
                } else {
                    var candidate = Random.nextInt(allCovers.size)
                    while (candidate == currentCoverIndex) {
                        candidate = Random.nextInt(allCovers.size)
                    }
                    candidate
                }
                currentCoverIndex = nextIndex
            }
        }
    }

    val activeCover = remember(allCovers, currentCoverIndex) {
        allCovers.getOrNull(currentCoverIndex)
    }

    val currentDisplayCover = remember(selectedAlbumName, activeCover, displayedSongs) {
        if (selectedAlbumName != null) {
            displayedSongs.firstNotNullOfOrNull { it.coverUrl ?: it.albumArtUri ?: it.uri } ?: activeCover
        } else {
            activeCover
        }
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
                        if (currentDisplayCover != null) {
                            val fadeBrush = remember {
                                Brush.verticalGradient(
                                    0.0f to Color.Black,
                                    0.40f to Color.Black,
                                    0.70f to Color.Black.copy(alpha = 0.50f),
                                    0.88f to Color.Black.copy(alpha = 0.15f),
                                    1.0f to Color.Transparent
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(380.dp)
                                    .offset(y = (-50).dp)
                                    .graphicsLayer {
                                        alpha = headerAlpha
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = fadeBrush,
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                            ) {
                                Crossfade(
                                    targetState = currentDisplayCover,
                                    animationSpec = tween(durationMillis = 1000),
                                    label = "albumCoverCrossfade"
                                ) { cover ->
                                    AsyncImage(
                                        model = cover,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                0.0f to Color.Transparent,
                                                0.50f to Color.Transparent,
                                                1.0f to (if (hasBlurBackground) Color.Black.copy(alpha = 0.40f) else Color.Transparent)
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
                                .padding(bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(260.dp))
                        
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
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
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val isCurrentAlbumPlaying = playbackManager.activePlaylistId == album.id
                        val isCurrentPlayingInSelection = isCurrentAlbumPlaying && (selectedAlbumName == null || displayedSongs.any { it.id == currentlyPlayingId })
                        val isPillPlaying = isCurrentPlayingInSelection && isPlaying

                        val pillCornerRadius by animateDpAsState(
                            targetValue = if (isPillPlaying) 14.dp else 28.dp,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                            label = "albumPillCornerRadius"
                        )
                        val iconCornerRadius by animateDpAsState(
                            targetValue = if (isPillPlaying) 10.dp else 20.dp,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                            label = "albumIconCornerRadius"
                        )

                        val dynamicPrimary = if (useCustomControlsColor) activeControlsPrimary else MaterialTheme.colorScheme.primary
                        val isSystemDark = isSystemInDarkTheme()
                        val effectiveDarkTheme = isDarkTheme || when (settingsManager.themeMode) {
                            1 -> false
                            2 -> true
                            else -> isSystemDark
                        }

                        val pillBg = if (hasBlurBackground && !useCustomControlsColor) {
                            if (effectiveDarkTheme) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.10f)
                        } else if (hasBlurBackground) {
                            dynamicPrimary.copy(alpha = 0.85f)
                        } else {
                            dynamicPrimary
                        }

                        val pillContentColor = if (hasBlurBackground && !useCustomControlsColor) {
                            if (effectiveDarkTheme) Color.White else Color(0xFF141316)
                        } else if (pillBg.luminance() > 0.45f) {
                            MaterialTheme.colorScheme.onPrimary.takeIf { it.luminance() < 0.4f } ?: Color(0xFF141316)
                        } else {
                            MaterialTheme.colorScheme.onPrimary.takeIf { it.luminance() > 0.6f } ?: Color.White
                        }

                        val actionBtnInactiveBg = if (hasBlurBackground) {
                            if (effectiveDarkTheme) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f)
                        } else MaterialTheme.colorScheme.secondaryContainer

                        val actionBtnInactiveTint = if (hasBlurBackground) {
                            if (effectiveDarkTheme) Color.White else Color(0xFF141316)
                        } else MaterialTheme.colorScheme.onSecondaryContainer

                        val actionBtnActiveBg = if (useCustomControlsColor) {
                            activeControlsPrimary
                        } else if (hasBlurBackground) {
                            if (effectiveDarkTheme) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.18f)
                        } else MaterialTheme.colorScheme.primary

                        val actionBtnActiveTint = if (hasBlurBackground && !useCustomControlsColor) {
                            if (effectiveDarkTheme) Color.White else Color(0xFF141316)
                        } else if (actionBtnActiveBg.luminance() > 0.45f) {
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
                            if (isCurrentPlayingInSelection) {
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
                            // Píldora de Play en el centro
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
                                        label = "albumPillText"
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

                            // Sort a la derecha
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
                        val totalAlbumDuration = remember(displayedSongs) { displayedSongs.sumOf { it.duration } }
                        val songsCountText = if (displayedSongs.size == 1) "1 ${stringResource(R.string.song_singular)}" else stringResource(R.string.ai_mix_songs_count, displayedSongs.size)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatLongDuration(totalAlbumDuration)} • $songsCountText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isArtistView && (artistAlbums.isNotEmpty() || singlesCount > 0)) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Chip General ("Todas las canciones")
                                item(key = "artist_all_songs_chip") {
                                    val isSelected = selectedAlbumName == null
                                    ArtistFilterChip(
                                        label = stringResource(R.string.filter_all_songs),
                                        count = songs.size,
                                        isSelected = isSelected,
                                        leadingIcon = Icons.Default.MusicNote,
                                        hasBlurBackground = hasBlurBackground,
                                        isDarkTheme = effectiveDarkTheme,
                                        useCustomControlsColor = useCustomControlsColor,
                                        activeControlsPrimary = activeControlsPrimary,
                                        onClick = {
                                            if (settingsManager.isHapticVibrationEnabled) {
                                                vibrator.triggerLightVibration()
                                            }
                                            selectedAlbumName = null
                                        }
                                    )
                                }

                                // Chips de Álbumes
                                items(
                                    items = artistAlbums,
                                    key = { "album_${it.id}" }
                                ) { albumItem ->
                                    val isSelected = selectedAlbumName?.equals(albumItem.name, ignoreCase = true) == true
                                    ArtistFilterChip(
                                        label = albumItem.name,
                                        count = albumItem.songs.size,
                                        isSelected = isSelected,
                                        coverUrl = albumItem.coverUrl ?: albumItem.albumArtUri ?: albumItem.songs.firstNotNullOfOrNull { it.coverUrl ?: it.albumArtUri },
                                        hasBlurBackground = hasBlurBackground,
                                        isDarkTheme = effectiveDarkTheme,
                                        useCustomControlsColor = useCustomControlsColor,
                                        activeControlsPrimary = activeControlsPrimary,
                                        onClick = {
                                            if (settingsManager.isHapticVibrationEnabled) {
                                                vibrator.triggerLightVibration()
                                            }
                                            selectedAlbumName = if (isSelected) null else albumItem.name
                                        }
                                    )
                                }

                                // Chip de Sencillos si existen
                                if (singlesCount > 0) {
                                    item(key = "artist_singles_chip") {
                                        val isSelected = selectedAlbumName == "__SINGLES__"
                                        ArtistFilterChip(
                                            label = stringResource(R.string.artist_filter_singles),
                                            count = singlesCount,
                                            isSelected = isSelected,
                                            leadingIcon = Icons.Default.Album,
                                            hasBlurBackground = hasBlurBackground,
                                            isDarkTheme = effectiveDarkTheme,
                                            useCustomControlsColor = useCustomControlsColor,
                                            activeControlsPrimary = activeControlsPrimary,
                                            onClick = {
                                                if (settingsManager.isHapticVibrationEnabled) {
                                                    vibrator.triggerLightVibration()
                                                }
                                                selectedAlbumName = if (isSelected) null else "__SINGLES__"
                                            }
                                        )
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
                    itemsIndexed(
                        sortedSongs,
                        key = { _, it -> it.id },
                        contentType = { _, _ -> "song_item" }
                    ) { index, song ->
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
                listState.fastSmoothScrollToItem(targetIndex)
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
                    listState.fastSmoothScrollToItem(0)
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
private fun ArtistFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    coverUrl: Any? = null,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    useCustomControlsColor: Boolean = false,
    activeControlsPrimary: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        if (useCustomControlsColor) {
            activeControlsPrimary
        } else if (hasBlurBackground) {
            if (isDarkTheme) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.20f)
        } else {
            MaterialTheme.colorScheme.primary
        }
    } else {
        if (hasBlurBackground) {
            if (isDarkTheme) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        }
    }

    val contentColor = if (isSelected) {
        if (hasBlurBackground && !useCustomControlsColor) {
            if (isDarkTheme) Color.White else Color(0xFF141316)
        } else if (containerColor.luminance() > 0.45f) {
            Color(0xFF141316)
        } else {
            Color.White
        }
    } else {
        if (hasBlurBackground) {
            if (isDarkTheme) Color.White.copy(alpha = 0.90f) else Color(0xFF141316).copy(alpha = 0.90f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    val countBadgeBg = if (isSelected) {
        contentColor.copy(alpha = 0.18f)
    } else {
        if (hasBlurBackground) {
            if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = modifier
            .height(36.dp)
            .bounceClick()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (coverUrl != null) {
                SongCoverImage(
                    coverUrl = coverUrl,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                shape = CircleShape,
                color = countBadgeBg,
                modifier = Modifier
                    .height(18.dp)
                    .defaultMinSize(minWidth = 18.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
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
    val stretchProgress = remember { Animatable(0f) }

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

    val allCovers = remember(songs) {
        songs.groupBy { song ->
            song.coverUrl ?: song.albumArtUri?.toString() ?: song.albumId.takeIf { it > 0 } ?: song.uri
        }.values.mapNotNull { group ->
            group.firstOrNull()?.let { it.coverUrl ?: it.albumArtUri ?: it.uri }
        }
    }

    var currentCoverIndex by remember(allCovers) { mutableIntStateOf(0) }

    LaunchedEffect(allCovers) {
        if (allCovers.size > 1) {
            while (isActive) {
                delay(5000L)
                val nextIndex = if (allCovers.size == 2) {
                    1 - currentCoverIndex
                } else {
                    var candidate = Random.nextInt(allCovers.size)
                    while (candidate == currentCoverIndex) {
                        candidate = Random.nextInt(allCovers.size)
                    }
                    candidate
                }
                currentCoverIndex = nextIndex
            }
        }
    }

    val activeCover = remember(allCovers, currentCoverIndex) {
        allCovers.getOrNull(currentCoverIndex)
    }

    val folderId = folderName.hashCode().toLong()
    val isCurrentFolderPlaying = playbackManager.activePlaylistId == folderId && playbackManager.activeCategory == "FOLDERS"

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

                        if (activeCover != null) {
                            val fadeBrush = remember {
                                Brush.verticalGradient(
                                    0.0f to Color.Black,
                                    0.40f to Color.Black,
                                    0.70f to Color.Black.copy(alpha = 0.50f),
                                    0.88f to Color.Black.copy(alpha = 0.15f),
                                    1.0f to Color.Transparent
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(380.dp)
                                    .offset(y = (-50).dp)
                                    .graphicsLayer {
                                        alpha = headerAlpha
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = fadeBrush,
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                            ) {
                                Crossfade(
                                    targetState = activeCover,
                                    animationSpec = tween(durationMillis = 1000),
                                    label = "folderCoverCrossfade"
                                ) { cover ->
                                    AsyncImage(
                                        model = cover,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                0.0f to Color.Transparent,
                                                0.50f to Color.Transparent,
                                                1.0f to (if (hasBlurBackground) Color.Black.copy(alpha = 0.40f) else Color.Transparent)
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
                            Spacer(modifier = Modifier.height(260.dp))

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

                        val isPillPlaying = isCurrentFolderPlaying && isPlaying

                        val pillCornerRadius by animateDpAsState(
                            targetValue = if (isPillPlaying) 14.dp else 28.dp,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                            label = "folderPillCornerRadius"
                        )
                        val iconCornerRadius by animateDpAsState(
                            targetValue = if (isPillPlaying) 10.dp else 20.dp,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                            label = "folderIconCornerRadius"
                        )

                        val dynamicPrimary = if (useCustomControlsColor) activeControlsPrimary else MaterialTheme.colorScheme.primary
                        val isSystemDark = isSystemInDarkTheme()
                        val effectiveDarkTheme = isDarkTheme || when (settingsManager.themeMode) {
                            1 -> false
                            2 -> true
                            else -> isSystemDark
                        }

                        val pillBg = if (hasBlurBackground && !useCustomControlsColor) {
                            if (effectiveDarkTheme) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.10f)
                        } else if (hasBlurBackground) {
                            dynamicPrimary.copy(alpha = 0.85f)
                        } else {
                            dynamicPrimary
                        }

                        val pillContentColor = if (hasBlurBackground && !useCustomControlsColor) {
                            if (effectiveDarkTheme) Color.White else Color(0xFF141316)
                        } else if (pillBg.luminance() > 0.45f) {
                            MaterialTheme.colorScheme.onPrimary.takeIf { it.luminance() < 0.4f } ?: Color(0xFF141316)
                        } else {
                            MaterialTheme.colorScheme.onPrimary.takeIf { it.luminance() > 0.6f } ?: Color.White
                        }

                        val actionBtnInactiveBg = if (hasBlurBackground) {
                            if (effectiveDarkTheme) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f)
                        } else MaterialTheme.colorScheme.secondaryContainer

                        val actionBtnInactiveTint = if (hasBlurBackground) {
                            if (effectiveDarkTheme) Color.White else Color(0xFF141316)
                        } else MaterialTheme.colorScheme.onSecondaryContainer

                        val actionBtnActiveBg = if (useCustomControlsColor) {
                            activeControlsPrimary
                        } else if (hasBlurBackground) {
                            if (effectiveDarkTheme) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.18f)
                        } else MaterialTheme.colorScheme.primary

                        val actionBtnActiveTint = if (hasBlurBackground && !useCustomControlsColor) {
                            if (effectiveDarkTheme) Color.White else Color(0xFF141316)
                        } else if (actionBtnActiveBg.luminance() > 0.45f) {
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
                            if (isCurrentFolderPlaying) {
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
                            // Píldora de Play en el centro
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
                                        label = "folderPillText"
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

                            // Sort a la derecha
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
                        val totalFolderDuration = remember(songs) { songs.sumOf { it.duration } }
                        val songsCountText = if (songs.size == 1) "1 ${stringResource(R.string.song_singular)}" else stringResource(R.string.ai_mix_songs_count, songs.size)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatLongDuration(totalFolderDuration)} • $songsCountText",
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
                    itemsIndexed(
                        sortedSongs,
                        key = { _, it -> it.id },
                        contentType = { _, _ -> "song_item" }
                    ) { index, song ->
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
                listState.fastSmoothScrollToItem(targetIndex)
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
                    listState.fastSmoothScrollToItem(0)
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
