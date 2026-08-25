package com.demonlab.lune.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demonlab.lune.data.*
import com.demonlab.lune.tools.MusicProvider
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.player.EqualizerDialog
import com.demonlab.lune.ui.player.FloatingRightPlayer
import com.demonlab.lune.ui.player.QueueDialog
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image as SkiaImage
import java.io.File

enum class MainTab(val getTitle: () -> String, val icon: ImageVector) {
    RESUME({ L10n.tabResume }, Icons.Default.Home),
    SONGS({ L10n.tabSongs }, Icons.Default.MusicNote),
    ALBUMS({ L10n.tabAlbums }, Icons.Default.Album),
    PLAYLISTS({ L10n.tabPlaylists }, Icons.AutoMirrored.Filled.PlaylistPlay),
    FOLDERS({ L10n.tabFolders }, Icons.Default.Folder),
    SEARCH({ L10n.get("search_songs") }, Icons.Default.Search),
    SETTINGS({ L10n.tabSettings }, Icons.Default.Settings),
    ABOUT({ L10n.tabAbout }, Icons.Default.Info);

    val title: String get() = getTitle()
}

@Composable
fun AppLogoBadge(modifier: Modifier = Modifier) {
    val bitmap = remember {
        try {
            val fileLogo = File("src/main/resources/icons/icon.png")
            if (fileLogo.exists()) {
                SkiaImage.makeFromEncoded(fileLogo.readBytes()).toComposeImageBitmap()
            } else {
                val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("icons/icon.png")
                stream?.readBytes()?.let { SkiaImage.makeFromEncoded(it).toComposeImageBitmap() }
            }
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Lune Logo",
            contentScale = ContentScale.Fit,
            modifier = modifier.clip(RoundedCornerShape(8.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Logo",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Capsule Navigation Tab with Icon-in-Circle and Animated Expandable Text
@Composable
fun NavigationTabItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .animateContentSize(
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (isSelected) 3.dp else 0.dp,
                end = if (isSelected) 12.dp else 0.dp,
                top = if (isSelected) 3.dp else 0.dp,
                bottom = if (isSelected) 3.dp else 0.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon inside circle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Animated Tab Title (Only expands when active)
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(150)) + expandHorizontally(expandFrom = Alignment.Start),
                exit = fadeOut(animationSpec = tween(100)) + shrinkHorizontally(shrinkTowards = Alignment.Start)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    playbackManager: PlaybackManager,
    settingsManager: SettingsManager,
    musicProvider: MusicProvider,
    dataManager: DataManager,
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onClose: () -> Unit = {},
    isMaximized: Boolean = false
) {
    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(MainTab.RESUME) }

    // Library state
    var songs by remember { mutableStateOf(musicProvider.getCachedSongs()) }
    val albums = remember(songs) { musicProvider.groupAlbums(songs) }
    val folders = remember(songs) { musicProvider.groupFolders(songs) }
    var playlists by remember { mutableStateOf(dataManager.getPlaylists()) }

    // Detail View State
    var activeAlbumDetail by remember { mutableStateOf<Album?>(null) }
    var activePlaylistDetail by remember { mutableStateOf<Playlist?>(null) }
    var activeFolderDetail by remember { mutableStateOf<Folder?>(null) }

    // Overlays
    var showEqualizer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var songForPlaylistAdd by remember { mutableStateOf<Song?>(null) }

    // Initial Scan if library is empty
    LaunchedEffect(Unit) {
        if (songs.isEmpty()) {
            songs = musicProvider.scanLibrary(settingsManager.musicDirectories)
        }
    }

    val refreshLibrary = {
        songs = musicProvider.getCachedSongs()
        playlists = dataManager.getPlaylists()
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // TOP NAVIGATION BAR
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Logo & Title (Left-aligned, Non-clickable static brand)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    AppLogoBadge(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = settingsManager.customTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Center Navigation Tabs (Strictly Centered horizontally)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabsBeforeSettings = listOf(
                        MainTab.RESUME,
                        MainTab.SONGS,
                        MainTab.ALBUMS,
                        MainTab.PLAYLISTS,
                        MainTab.FOLDERS,
                        MainTab.SEARCH
                    )

                    tabsBeforeSettings.forEach { tab ->
                        val isSelected = currentTab == tab && activeAlbumDetail == null && activePlaylistDetail == null && activeFolderDetail == null
                        NavigationTabItem(
                            tab = tab,
                            isSelected = isSelected,
                            onClick = {
                                activeAlbumDetail = null
                                activePlaylistDetail = null
                                activeFolderDetail = null
                                currentTab = tab
                            }
                        )
                    }

                    // Rescan Button (Circular Pill with Icon, before Settings)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                scope.launch {
                                    songs = musicProvider.scanLibrary(settingsManager.musicDirectories)
                                    refreshLibrary()
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Re-scan Library",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Settings Tab
                    val isSettingsSelected = currentTab == MainTab.SETTINGS && activeAlbumDetail == null && activePlaylistDetail == null && activeFolderDetail == null
                    NavigationTabItem(
                        tab = MainTab.SETTINGS,
                        isSelected = isSettingsSelected,
                        onClick = {
                            activeAlbumDetail = null
                            activePlaylistDetail = null
                            activeFolderDetail = null
                            currentTab = MainTab.SETTINGS
                        }
                    )

                    // About Tab
                    val isAboutSelected = currentTab == MainTab.ABOUT && activeAlbumDetail == null && activePlaylistDetail == null && activeFolderDetail == null
                    NavigationTabItem(
                        tab = MainTab.ABOUT,
                        isSelected = isAboutSelected,
                        onClick = {
                            activeAlbumDetail = null
                            activePlaylistDetail = null
                            activeFolderDetail = null
                            currentTab = MainTab.ABOUT
                        }
                    )
                }

                // Window Controls (Right-aligned inside circles)
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimize Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onMinimize() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Minimize",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Maximize / Restore Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onMaximize() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isMaximized) Icons.Default.FullscreenExit else Icons.Default.CropSquare,
                                contentDescription = if (isMaximized) "Restore" else "Maximize",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Close Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onClose() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // MAIN CONTENT + RIGHT FLOATING PLAYER ROW
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Main Views Content Area (Left/Center)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when {
                    activeAlbumDetail != null -> {
                        AlbumDetailView(
                            album = activeAlbumDetail!!,
                            playbackManager = playbackManager,
                            onBack = { activeAlbumDetail = null }
                        )
                    }
                    activePlaylistDetail != null -> {
                        PlaylistDetailView(
                            playlist = activePlaylistDetail!!,
                            allSongs = songs,
                            playbackManager = playbackManager,
                            onBack = { activePlaylistDetail = null },
                            onDeletePlaylist = {
                                val mutable = playlists.toMutableList()
                                mutable.removeAll { it.id == activePlaylistDetail!!.id }
                                playlists = mutable
                                dataManager.savePlaylists(mutable)
                                activePlaylistDetail = null
                            }
                        )
                    }
                    activeFolderDetail != null -> {
                        FolderDetailView(
                            folder = activeFolderDetail!!,
                            playbackManager = playbackManager,
                            onBack = { activeFolderDetail = null }
                        )
                    }
                    currentTab == MainTab.RESUME -> {
                        ResumeScreen(
                            songs = songs,
                            albums = albums,
                            playlists = playlists,
                            playbackManager = playbackManager,
                            settingsManager = settingsManager,
                            onAlbumClick = { activeAlbumDetail = it },
                            onPlaylistClick = { activePlaylistDetail = it }
                        )
                    }
                    currentTab == MainTab.SONGS -> {
                        SongsScreen(
                            songs = songs,
                            playbackManager = playbackManager,
                            settingsManager = settingsManager,
                            onAddToPlaylistClick = { songForPlaylistAdd = it }
                        )
                    }
                    currentTab == MainTab.ALBUMS -> {
                        AlbumsScreen(
                            albums = albums,
                            onAlbumClick = { activeAlbumDetail = it }
                        )
                    }
                    currentTab == MainTab.PLAYLISTS -> {
                        PlaylistsScreen(
                            playlists = playlists,
                            songs = songs,
                            onPlaylistClick = { activePlaylistDetail = it },
                            onCreatePlaylist = { name ->
                                val newP = Playlist(name = name)
                                val mutable = playlists.toMutableList()
                                mutable.add(newP)
                                playlists = mutable
                                dataManager.savePlaylists(mutable)
                            },
                            onDeletePlaylist = { p ->
                                val mutable = playlists.toMutableList()
                                mutable.removeAll { it.id == p.id }
                                playlists = mutable
                                dataManager.savePlaylists(mutable)
                            }
                        )
                    }
                    currentTab == MainTab.FOLDERS -> {
                        FoldersScreen(
                            folders = folders,
                            onFolderClick = { activeFolderDetail = it }
                        )
                    }
                    currentTab == MainTab.SEARCH -> {
                        SearchScreen(
                            songs = songs,
                            albums = albums,
                            playlists = playlists,
                            playbackManager = playbackManager,
                            onAlbumClick = { activeAlbumDetail = it },
                            onPlaylistClick = { activePlaylistDetail = it }
                        )
                    }
                    currentTab == MainTab.SETTINGS -> {
                        SettingsScreen(
                            settingsManager = settingsManager,
                            onRescanLibrary = { refreshLibrary() }
                        )
                    }
                    currentTab == MainTab.ABOUT -> {
                        AboutScreen()
                    }
                }
            }

            // Floating Player on the Right Side (Non-stretching widget)
            AnimatedVisibility(
                visible = playbackManager.currentSong != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                FloatingRightPlayer(
                    playbackManager = playbackManager,
                    settingsManager = settingsManager,
                    onOpenEqualizer = { showEqualizer = true },
                    onOpenQueue = { showQueue = true }
                )
            }
        }
    }

    // Equalizer Dialog
    if (showEqualizer) {
        EqualizerDialog(
            settingsManager = settingsManager,
            onDismiss = { showEqualizer = false }
        )
    }

    // Queue Dialog
    if (showQueue) {
        QueueDialog(
            playbackManager = playbackManager,
            onDismiss = { showQueue = false }
        )
    }

    // Add to Playlist Dialog
    songForPlaylistAdd?.let { song ->
        AlertDialog(
            onDismissRequest = { songForPlaylistAdd = null },
            title = { Text("${L10n.tabPlaylists}: ${song.title}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                if (playlists.isEmpty()) {
                    Text(L10n.noPlaylistsYet, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column {
                        playlists.forEach { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name, color = MaterialTheme.colorScheme.onSurface) },
                                leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                                    if (!playlist.songIds.contains(song.id)) {
                                        playlist.songIds.add(song.id)
                                        dataManager.savePlaylists(playlists)
                                    }
                                    songForPlaylistAdd = null
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { songForPlaylistAdd = null }, shape = RoundedCornerShape(8.dp)) {
                    Text(L10n.cancel)
                }
            }
        )
    }
}
