package com.demonlab.lune.ui.activities
import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.widget.Toast
import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.demonlab.lune.ui.viewmodels.MusicViewModel
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.*
import com.demonlab.lune.ui.components.FastScrollbar
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import com.demonlab.lune.R
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.animation.Crossfade
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage

import com.demonlab.lune.tools.MusicProvider
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.demonlab.lune.tools.MetadataManager
import com.demonlab.lune.ui.theme.LuneTheme
import com.demonlab.lune.ui.utils.*
import com.demonlab.lune.ui.components.*
import com.demonlab.lune.ui.player.*
import com.demonlab.lune.ui.sheets.*
import com.demonlab.lune.ui.screens.OnboardingScreen
import com.demonlab.lune.ui.data.*
import com.demonlab.lune.ui.playlist.*
import com.demonlab.lune.ui.search.*
import com.demonlab.lune.ui.screens.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import coil.imageLoader
import coil.request.ImageRequest

class Lune : AppCompatActivity() {
    companion object {
        const val ACTION_VIEW_PLAYLISTS = "com.demonlab.lune.ACTION_VIEW_PLAYLISTS"
    }

    private var shortcutFolder = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_VIEW_PLAYLISTS) {
            shortcutFolder.value = "PLAYLISTS"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        val settingsManager = SettingsManager.getInstance(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            val context = LocalContext.current
            val settingsManager = SettingsManager.getInstance(context)
            val musicViewModel: MusicViewModel = viewModel()
            val playbackManager = remember { PlaybackManager.getInstance(context) }

            // Stable Tab IDs
            val TAB_RESUME = "RESUME"
            val TAB_ALL = "ALL"
            val TAB_FAVORITES = "FAVORITES"
            val TAB_ALBUMS = "ALBUMS"
            val TAB_ARTISTS = "ARTISTS"
            val TAB_PLAYLISTS = "PLAYLISTS"
            val TAB_FOLDERS = "FOLDERS"

            // LIFTED STRINGS
            val sTabResume = stringResource(R.string.tab_resume)
            val sTabAll = stringResource(R.string.tab_all)
            val sTabFavorites = stringResource(R.string.tab_favorites)
            val sTabFolders = stringResource(R.string.tab_folders)
            val sTabAlbums = stringResource(R.string.tab_albums_real)
            val sTabArtists = stringResource(R.string.tab_artists)
            val sTabPlaylists = stringResource(R.string.playlists)

            // LIFTED STATES & LOGIC
            var showOnboarding by remember { mutableStateOf(settingsManager.isFirstRun) }
            var useCustomColors by remember { mutableStateOf(settingsManager.useCustomColors) }
            var customColorPalette by remember { mutableIntStateOf(settingsManager.customColorPalette) }
            var useAmoledPitchBlack by remember { mutableStateOf(settingsManager.useAmoledPitchBlack) }
            var isSectionCustomizationEnabled by remember { mutableStateOf(settingsManager.isSectionCustomizationEnabled) }
            var hiddenSectionTabs by remember { mutableStateOf(settingsManager.hiddenSectionTabs) }
            var keepScreenOn by remember { mutableStateOf(settingsManager.keepScreenOn) }

            LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            if (showOnboarding) {
                LuneTheme(
                    darkTheme = isSystemInDarkTheme(),
                    useCustomColors = useCustomColors,
                    customColorPalette = customColorPalette,
                    useAmoledPitchBlack = useAmoledPitchBlack
                ) {
                    OnboardingScreen(onStartClick = {
                        settingsManager.isFirstRun = false
                        showOnboarding = false
                    })
                }
                return@setContent
            }

            val rawAllSongs = musicViewModel.filteredSongs
            var selectedFolder by rememberSaveable { mutableStateOf(TAB_RESUME) }
            
            // Handle Shortcut Navigation
            LaunchedEffect(shortcutFolder.value) {
                shortcutFolder.value?.let {
                    selectedFolder = it
                    shortcutFolder.value = null
                }
            }
            
            var showFolderSheet by remember { mutableStateOf(false) }
            val hiddenFolders = remember { mutableStateOf(settingsManager.hiddenFolders) }
            
            // Sync hidden folders when songs update (e.g. initial scan)
            LaunchedEffect(rawAllSongs) {
                hiddenFolders.value = settingsManager.hiddenFolders
            }

            // Restore playback state once songs are loaded
            LaunchedEffect(musicViewModel.allSongs) {
                if (musicViewModel.allSongs.isNotEmpty() && !playbackManager.stateRestored) {
                    playbackManager.restorePlaybackState(musicViewModel.allSongs)
                }
            }

            // Sync favorite status from external sources (notification, system media)
            LaunchedEffect(Unit) {
                playbackManager.favoriteChanged.collect { (songId, isFavorite) ->
                    musicViewModel.syncFavoriteStatusInMemory(songId, isFavorite)
                }
            }
            
            val currentSong = playbackManager.currentSong
            val isPlaying = playbackManager.isPlaying
            var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
            var playbackProgress by remember { mutableStateOf(playbackManager.getProgress()) }

            var coverShape by remember { mutableIntStateOf(settingsManager.coverShape) }
            var coverScale by remember { mutableFloatStateOf(settingsManager.coverScale) }
            var coverSpin by remember { mutableStateOf(settingsManager.coverSpin) }
            var coverVinylEffect by remember { mutableStateOf(settingsManager.coverVinylEffect) }

            var controlsIconStyle by remember { mutableIntStateOf(settingsManager.controlsIconStyle) }
            var isControlsFilled by remember { mutableStateOf(settingsManager.isControlsFilled) }
            var useCustomControlsColor by remember { mutableStateOf(settingsManager.useCustomControlsColor) }
            var controlsColorPalette by remember { mutableIntStateOf(settingsManager.controlsColorPalette) }

            LaunchedEffect(currentSong, isPlayerExpanded) {
                if (currentSong == null && isPlayerExpanded) {
                    isPlayerExpanded = false
                }
            }

            // Permissions logic lifted
            val essentialPermissions = remember {
                val list = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    list.add(Manifest.permission.READ_MEDIA_AUDIO)
                } else {
                    list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                list
            }

            var hasPermission by remember {
                mutableStateOf(essentialPermissions.all { 
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
                })
            }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val granted = essentialPermissions.all { permissions[it] == true }
                hasPermission = granted
                if (granted) musicViewModel.loadSongs()
                else Toast.makeText(context, context.getString(R.string.permission_required), Toast.LENGTH_SHORT).show()
            }

            val recordAudioLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    playbackManager.startVisualizer()
                }
            }

            LaunchedEffect(hasPermission) {
                if (hasPermission) {
                    musicViewModel.loadSongs()
                } else {
                    launcher.launch(essentialPermissions.toTypedArray())
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE) {
                        playbackManager.savePlaybackState()
                    }
                    if (event == Lifecycle.Event.ON_RESUME) {
                        useCustomColors = settingsManager.useCustomColors
                        customColorPalette = settingsManager.customColorPalette
                        useAmoledPitchBlack = settingsManager.useAmoledPitchBlack
                        coverShape = settingsManager.coverShape
                        coverScale = settingsManager.coverScale
                        coverSpin = settingsManager.coverSpin
                        coverVinylEffect = settingsManager.coverVinylEffect
                        controlsIconStyle = settingsManager.controlsIconStyle
                        isControlsFilled = settingsManager.isControlsFilled
                        useCustomControlsColor = settingsManager.useCustomControlsColor
                        controlsColorPalette = settingsManager.controlsColorPalette
                        isSectionCustomizationEnabled = settingsManager.isSectionCustomizationEnabled
                        hiddenSectionTabs = settingsManager.hiddenSectionTabs
                        keepScreenOn = settingsManager.keepScreenOn
                        if (hasPermission) {
                            musicViewModel.loadSongs()
                            musicViewModel.loadPlaylists()
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Sync Progress
            LaunchedEffect(isPlaying) {
                if (isPlaying) {
                    var saveCounter = 0
                    while (isPlaying) {
                        playbackProgress = playbackManager.getProgress()
                        saveCounter++
                        if (saveCounter >= 10) { // Save position every ~5 seconds
                            saveCounter = 0
                            playbackManager.savePlaybackState(wasPlaying = true)
                        }
                        kotlinx.coroutines.delay(500)
                    }
                } else {
                    // Only reset progress when the queue actually ended, not on user pause
                    if (playbackManager.isQueueFinished) {
                        playbackProgress = 0f
                    }
                }
            }
            // Sync Visualizer when permission or playback state changes
            LaunchedEffect(isPlaying) {
                val hasAudioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (hasAudioPermission && isPlaying) {
                    playbackManager.startVisualizer()
                } else if (!isPlaying) {
                     playbackManager.stopVisualizer()
                }
            }

            // Derivations (Reactive)
            val allFolders = remember(rawAllSongs) {
                rawAllSongs.map { it.folderName }.distinct().sorted()
            }
            val allAlbumsList = remember(rawAllSongs) {
                rawAllSongs.map { it.album }.distinct().sorted()
            }
            val visibleFolders = remember(allFolders, hiddenFolders.value) {
                allFolders.filter { !hiddenFolders.value.contains(it) }
            }
            val folders = remember(visibleFolders, rawAllSongs, sTabPlaylists, isSectionCustomizationEnabled, hiddenSectionTabs) {
                val hasFavorites = rawAllSongs.any { it.isFavorite }
                val base = mutableListOf("RESUME", "ALL", "PLAYLISTS")
                if (hasFavorites) base.add("FAVORITES")
                base.add("ALBUMS")
                base.add("ARTISTS")
                base.add("GENRES")
                if (visibleFolders.isNotEmpty()) base.add("FOLDERS")
                if (isSectionCustomizationEnabled) {
                    base.removeAll(hiddenSectionTabs)
                    if ("RESUME" !in base) base.add(0, "RESUME")
                }
                base
            }
            LaunchedEffect(folders) {
                if (selectedFolder !in folders && selectedFolder.isNotEmpty()) {
                    selectedFolder = TAB_RESUME
                }
            }
            val visibleSongs = remember(rawAllSongs, hiddenFolders.value) {
                rawAllSongs.filter { !hiddenFolders.value.contains(it.folderName) }
            }
            val filteredSongs = remember(visibleSongs, selectedFolder) {
                when (selectedFolder) {
                    TAB_RESUME, TAB_ALL, TAB_ALBUMS, TAB_ARTISTS, "GENRES" -> visibleSongs
                    TAB_FAVORITES -> visibleSongs.filter { it.isFavorite }
                    else -> visibleSongs.filter { it.folderName == selectedFolder }
                }
            }

            // Theme State (No animation)
            var themeMode by remember { mutableIntStateOf(settingsManager.themeMode) }
            val systemInDarkTheme = isSystemInDarkTheme()
            val targetDarkTheme = when (themeMode) {
                1 -> false // Light
                2 -> true  // Dark
                else -> systemInDarkTheme // Auto
            }

            LuneTheme(
                darkTheme = targetDarkTheme,
                useCustomColors = useCustomColors,
                customColorPalette = customColorPalette,
                useAmoledPitchBlack = useAmoledPitchBlack
            ) {
                MainScreen(
                    themeMode = themeMode,
                    onThemeModeChange = { 
                        val newMode = (themeMode + 1) % 3
                        themeMode = newMode
                        settingsManager.themeMode = newMode
                    },
                    rawAllSongs = rawAllSongs,
                    filteredSongs = filteredSongs,
                    folders = folders,
                    isSectionCustomizationEnabled = isSectionCustomizationEnabled,
                    allFolders = allFolders,
                    allAlbums = allAlbumsList,
                    selectedFolder = selectedFolder,
                    onSelectedFolderChange = { selectedFolder = it },
                    showFolderSheet = showFolderSheet,
                    onShowFolderSheetChange = { showFolderSheet = it },
                    hiddenFolders = hiddenFolders,
                    currentSong = currentSong,
                    onCurrentSongChange = { /* reactive */ },
                    isPlaying = isPlaying,
                    onIsPlayingChange = { /* reactive */ },
                    isPlayerExpanded = isPlayerExpanded,
                    onIsPlayerExpandedChange = { isPlayerExpanded = it },
                    playbackProgress = playbackProgress,
                    onPlaybackProgressChange = { playbackProgress = it },
                    hasPermission = hasPermission,
                    playbackManager = playbackManager,
                    onRefreshSongs = { musicViewModel.refreshLibrary() },
                    musicViewModel = musicViewModel,
                    settingsManager = settingsManager,
                    coverShape = coverShape,
                    coverScale = coverScale,
                    coverSpin = coverSpin,
                    coverVinylEffect = coverVinylEffect,
                    controlsIconStyle = controlsIconStyle,
                    isControlsFilled = isControlsFilled,
                    useCustomControlsColor = useCustomControlsColor,
                    controlsColorPalette = controlsColorPalette,
                    onRequestAudioPermission = { recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeMode: Int,
    onThemeModeChange: () -> Unit,
    rawAllSongs: List<Song>,
    filteredSongs: List<Song>,
    folders: List<String>,
    isSectionCustomizationEnabled: Boolean,
    allFolders: List<String>,
    allAlbums: List<String>,
    selectedFolder: String,
    onSelectedFolderChange: (String) -> Unit,
    showFolderSheet: Boolean,
    onShowFolderSheetChange: (Boolean) -> Unit,
    hiddenFolders: MutableState<Set<String>>,
    currentSong: Song?,
    onCurrentSongChange: (Song?) -> Unit,
    isPlaying: Boolean,
    onIsPlayingChange: (Boolean) -> Unit,
    isPlayerExpanded: Boolean,
    onIsPlayerExpandedChange: (Boolean) -> Unit,
    playbackProgress: Float,
    onPlaybackProgressChange: (Float) -> Unit,
    hasPermission: Boolean,
    playbackManager: PlaybackManager,
    onRefreshSongs: () -> Unit,
    musicViewModel: com.demonlab.lune.ui.viewmodels.MusicViewModel,
    settingsManager: SettingsManager,
    coverShape: Int,
    coverScale: Float,
    coverSpin: Boolean,
    coverVinylEffect: Boolean,
    controlsIconStyle: Int,
    isControlsFilled: Boolean,
    useCustomControlsColor: Boolean,
    controlsColorPalette: Int,
    onRequestAudioPermission: () -> Unit
) {
    val context = LocalContext.current
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isButtonNavigation = bottomInset > 24.dp
    val bottomPadding = if (!isPlayerExpanded) {
        if (currentSong != null && !settingsManager.isMiniPlayerMinimized) {
            if (isButtonNavigation) bottomInset + 140.dp else 130.dp
        } else {
            if (isButtonNavigation) bottomInset + 80.dp else 76.dp
        }
    } else {
        0.dp
    }
    val sTabResume = stringResource(R.string.tab_resume)
    val sTabAll = stringResource(R.string.tab_all)
    val sTabFavorites = stringResource(R.string.tab_favorites)
    val sTabFolders = stringResource(R.string.tab_folders)
    val sTabAlbums = stringResource(R.string.tab_albums_real)
    val sTabArtists = stringResource(R.string.tab_artists)
    val sTabGenres = stringResource(R.string.tab_genres)
    val sTabPlaylists = stringResource(R.string.playlists)

    val visibleFolders = remember(allFolders, hiddenFolders.value) {
        allFolders.filter { !hiddenFolders.value.contains(it) }
    }

    var editingSong by remember { mutableStateOf<Song?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var optionsSong by remember { mutableStateOf<Song?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showSectionMenuSheet by remember { mutableStateOf(false) }
    var showMainAddToPlaylistDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var undoSecondsRemaining by remember { mutableIntStateOf(0) }
    var undoProgress by remember { mutableFloatStateOf(1f) }
    var selectedPlaylist by remember { mutableStateOf<com.demonlab.lune.data.Playlist?>(null) }
    
    val visualizerData by playbackManager.visualizerData.collectAsState()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(initialHeightOffset = -Float.MAX_VALUE)
    )
    
    var showMenu by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var selectedFolderItem by remember { mutableStateOf<String?>(null) }
    var isAlbumView by remember { mutableStateOf(settingsManager.albumBrowseMode) }
    var folderHierarchyMode by remember { mutableStateOf(settingsManager.folderHierarchyMode) }

    val visibleSongs = remember(rawAllSongs, hiddenFolders.value) {
        rawAllSongs.filter { !hiddenFolders.value.contains(it.folderName) }
    }

    data class FolderEntry(val name: String, val depth: Int, val isVirtual: Boolean)

    val hierarchyEntries = remember(visibleFolders, rawAllSongs, folderHierarchyMode) {
        if (!folderHierarchyMode) {
            visibleFolders.sorted().map { FolderEntry(it, 0, false) }
        } else {
            val dirMap = visibleFolders.mapNotNull { folder ->
                rawAllSongs.firstOrNull { it.folderName == folder }
                    ?.let { folder to it.path.substringBeforeLast("/") }
            }.toMap()

            val songDirDepths = dirMap.values.map { it.count { c -> c == '/' } }
            val minDepth = if (songDirDepths.isEmpty()) 0 else songDirDepths.min()

            val virtualParents = mutableMapOf<String, String>()
            for ((folder, dir) in dirMap) {
                val depth = dir.count { c -> c == '/' }
                val parentPath = dir.substringBeforeLast("/")
                val parentName = parentPath.substringAfterLast("/")
                if (parentName !in visibleFolders && depth > minDepth) {
                    virtualParents[parentName] = parentPath
                }
            }

            val allNames = dirMap.keys + virtualParents.keys

            val childrenMap = mutableMapOf<String, MutableList<String>>()
            val roots = mutableListOf<String>()

            for ((folder, dir) in dirMap) {
                val parentDir = dir.substringBeforeLast("/")
                val parentName = parentDir.substringAfterLast("/")
                if (parentName in allNames) {
                    childrenMap.getOrPut(parentName) { mutableListOf() }.add(folder)
                } else {
                    roots.add(folder)
                }
            }

            for ((parentName, parentPath) in virtualParents) {
                val grandParentDir = parentPath.substringBeforeLast("/")
                val grandParentName = grandParentDir.substringAfterLast("/")
                if (grandParentName in allNames) {
                    childrenMap.getOrPut(grandParentName) { mutableListOf() }.add(parentName)
                } else {
                    roots.add(parentName)
                }
            }

            val entries = mutableListOf<FolderEntry>()
            fun addEntry(name: String, depth: Int) {
                val isVirtual = name !in visibleFolders
                entries.add(FolderEntry(name, depth, isVirtual))
                childrenMap[name]?.sorted()?.forEach { addEntry(it, depth + 1) }
            }
            roots.sorted().forEach { addEntry(it, 0) }
            visibleFolders.filter { it !in dirMap }.sorted().forEach {
                entries.add(FolderEntry(it, 0, false))
            }
            entries
        }
    }

    val contextId = remember(selectedFolder) {
        when (selectedFolder) {
            "RESUME", "ALL", "ALBUMS", "ARTISTS", "GENRES" -> -100L
            "FAVORITES" -> -200L
            else -> selectedFolder.hashCode().toLong()
        }
    }
    val currentSortKey = remember(selectedFolder, selectedPlaylist, selectedAlbum) {
        when {
            selectedPlaylist != null -> "playlist_${selectedPlaylist?.id}"
            selectedAlbum != null -> "album_${selectedAlbum?.name}"
            else -> "folder_$selectedFolder"
        }
    }
    val activeContextId = remember(selectedFolder, selectedPlaylist, selectedAlbum) {
        when {
            selectedPlaylist != null -> selectedPlaylist?.id ?: -1L
            selectedAlbum != null -> selectedAlbum?.id ?: -1L
            else -> contextId
        }
    }
    var activeSortOption by remember(currentSortKey) {
        mutableStateOf(settingsManager.getSortOption(currentSortKey))
    }
    var activeIsSortAscending by remember(currentSortKey) {
        mutableStateOf(settingsManager.getIsSortAscending(currentSortKey))
    }
    var activeIsCaseSensitive by remember(currentSortKey) {
        mutableStateOf(settingsManager.getIsCaseSensitiveSort(currentSortKey))
    }
    val sortedSongs = remember(filteredSongs, activeSortOption, activeIsSortAscending, activeIsCaseSensitive) {
        playbackManager.getSortedList(filteredSongs, activeSortOption, activeIsSortAscending, activeIsCaseSensitive)
    }

    
    val albumsList = remember(rawAllSongs, hiddenFolders.value) {
        rawAllSongs.filter { !hiddenFolders.value.contains(it.folderName) }
            .groupBy { it.album }
            .map { (albumName, songs) ->
                Album(
                    id = albumName.hashCode().toLong(),
                    name = albumName,
                    artist = songs.first().artist,
                    albumArtUri = songs.first().albumArtUri,
                    coverUrl = songs.first().coverUrl,
                    songs = songs.sortedBy { it.title }
                )
            }
            .sortedBy { it.name }
    }

    val artistsList = remember(rawAllSongs, hiddenFolders.value) {
        rawAllSongs.filter { !hiddenFolders.value.contains(it.folderName) }
            .groupBy { it.artist }
            .map { (artistName, songs) -> 
                Album(
                    id = artistName.hashCode().toLong(),
                    name = artistName, 
                    artist = "", 
                    albumArtUri = songs.first().albumArtUri, 
                    coverUrl = songs.first().coverUrl, 
                    songs = songs.sortedWith(compareBy({ it.album }, { it.title }))
                ) 
            }
            .sortedBy { it.name }
    }

    val genresList = remember(rawAllSongs, hiddenFolders.value) {
        rawAllSongs.filter { !hiddenFolders.value.contains(it.folderName) }
            .groupBy {
                val g = it.genre?.trim()
                if (g.isNullOrEmpty() || g.equals("<unknown>", ignoreCase = true) || g.equals("unknown", ignoreCase = true)) {
                    "Desconocido"
                } else {
                    g
                }
            }
            .map { (genreName, songs) ->
                Album(
                    id = genreName.hashCode().toLong(),
                    name = genreName,
                    artist = "",
                    albumArtUri = songs.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                    coverUrl = songs.firstOrNull { it.coverUrl != null }?.coverUrl,
                    songs = songs.sortedWith(compareBy({ it.album }, { it.title }))
                )
            }
            .sortedBy { if (it.name == "Desconocido") "zzzz" else it.name.lowercase() }
    }

    LaunchedEffect(selectedFolder) {
        if (selectedFolder.isNotEmpty()) {
            settingsManager.lastCategory = selectedFolder
        }
    }

    LaunchedEffect(folders) {
        if (selectedFolder !in folders && selectedFolder.isNotEmpty()) {
            onSelectedFolderChange("RESUME")
        }
        if (isSectionCustomizationEnabled && playbackManager.activeCategory != null && playbackManager.activeCategory !in folders) {
            playbackManager.stopAndClearQueue()
        }
    }



    if (selectedAlbum != null) {
        BackHandler {
            selectedAlbum = null
        }
    }
    
    if (selectedPlaylist != null) {
        BackHandler {
            selectedPlaylist = null
        }
    }

    if (selectedFolderItem != null) {
        BackHandler {
            selectedFolderItem = null
        }
    }

    val vibrator = LocalContext.current.getSystemService(android.os.Vibrator::class.java)!!

    val playNext = {
        if (settingsManager.isHapticVibrationEnabled) {
            vibrator.triggerLightVibration()
        }
        playbackManager.playNextFromService()
        onCurrentSongChange(playbackManager.currentSong)
        onIsPlayingChange(playbackManager.isPlaying)
    }

    @Composable
    fun AnimatedLogo(
        isPlaying: Boolean,
        modifier: Modifier = Modifier
    ) {
        val rotation = remember { Animatable(0f) }

        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                while (true) {
                    rotation.animateTo(
                        targetValue = 360f,
                        animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
                    )
                    rotation.snapTo(0f)
                }
            } else {
                rotation.snapTo(0f)
            }
        }

        Box(
            modifier = modifier.size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_logo_diamonds),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(rotationZ = rotation.value)
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_logo_note),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    val playPrevious = {
        if (settingsManager.isHapticVibrationEnabled) {
            vibrator.triggerLightVibration()
        }
        playbackManager.playPreviousFromService()
        onCurrentSongChange(playbackManager.currentSong)
        onIsPlayingChange(playbackManager.isPlaying)
    }

    var showSearchScreen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        val scrollToCurrentTrigger = remember { mutableStateOf(0) }

        Scaffold(
            snackbarHost = { 
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter) // Restored to standard position
                        .padding(bottom = if (currentSong != null && !isPlayerExpanded) 80.dp else 0.dp)
                ) { data ->
                    Surface(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Countdown
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(32.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { undoProgress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                )
                                Text(
                                    text = undoSecondsRemaining.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Message
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Action
                            data.visuals.actionLabel?.let { label ->
                                TextButton(
                                    onClick = { data.performAction() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp) // Match countdown height for balance
                                ) {
                                    Text(
                                        text = label, 
                                        fontWeight = FontWeight.Bold, 
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                LargeTopAppBar(
                    title = { 
                        val customTitle by settingsManager.customTitleFlow.collectAsState()
                        val titleText = if (customTitle.isEmpty()) "Lune" else customTitle

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnimatedLogo(
                                isPlaying = isPlaying,
                                modifier = Modifier.padding(end = 0.5.dp)
                            )
                            ResponsiveText(
                                text = titleText,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                targetTextSize = 32.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(
                            onClick = onThemeModeChange
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (themeMode) {
                                            1 -> Icons.Outlined.LightMode
                                            2 -> Icons.Outlined.DarkMode
                                            else -> Icons.Outlined.BrightnessAuto
                                        },
                                        contentDescription = when (themeMode) {
                                            1 -> stringResource(R.string.theme_light)
                                            2 -> stringResource(R.string.theme_dark)
                                            else -> stringResource(R.string.theme_auto)
                                        },
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { 
                                context.startActivity(Intent(context, SettingsActivity::class.java))
                            },
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = stringResource(R.string.settings),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(top = innerPadding.calculateTopPadding())) {




                val pagerState = rememberPagerState(
                    pageCount = { folders.size },
                    initialPage = (folders.indexOf(selectedFolder).coerceAtLeast(0))
                )

                var isPagerProgrammaticScroll by remember { mutableStateOf(false) }

                LaunchedEffect(selectedFolder) {
                    val target = folders.indexOf(selectedFolder)
                    if (target != -1 && target != pagerState.currentPage) {
                        isPagerProgrammaticScroll = true
                        pagerState.animateScrollToPage(target)
                        isPagerProgrammaticScroll = false
                    }
                }

                LaunchedEffect(pagerState.currentPage) {
                    if (!isPagerProgrammaticScroll) {
                        val f = folders.getOrNull(pagerState.currentPage)
                        if (f != null && f != selectedFolder) {
                            onSelectedFolderChange(f)
                        }
                    }
                }

                LaunchedEffect(folders) {
                    val target = folders.indexOf(selectedFolder)
                    if (target != -1 && target != pagerState.currentPage) {
                        isPagerProgrammaticScroll = true
                        pagerState.animateScrollToPage(target)
                        isPagerProgrammaticScroll = false
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val folder = folders.getOrNull(page) ?: return@HorizontalPager

                    val pageFilteredSongs = remember(visibleSongs, folder) {
                        when (folder) {
                            "RESUME", "ALL", "ALBUMS", "FOLDERS" -> visibleSongs
                            "FAVORITES" -> visibleSongs.filter { it.isFavorite }
                            else -> visibleSongs.filter { it.folderName == folder }
                        }
                    }

                    val pageSortedSongs = remember(pageFilteredSongs, activeSortOption, activeIsSortAscending) {
                        playbackManager.getSortedList(pageFilteredSongs, activeSortOption, activeIsSortAscending)
                    }

                    val pageContextId = remember(folder) {
                        when (folder) {
                            "RESUME", "ALL", "ALBUMS", "FOLDERS" -> -100L
                            "FAVORITES" -> -200L
                            else -> folder.hashCode().toLong()
                        }
                    }

                    val pageCurrentScreen = remember(folder, pageFilteredSongs.isEmpty()) {
                        when {
                            folder == "RESUME" -> "RESUME"
                            folder == "ALBUMS" -> "ALBUM_GRID"
                            folder == "ARTISTS" -> "ARTIST_GRID"
                            folder == "GENRES" -> "GENRE_GRID"
                            folder == "PLAYLISTS" -> "PLAYLIST_GRID"
                            folder == "FOLDERS" -> "FOLDER_GRID"
                            pageFilteredSongs.isEmpty() -> "EMPTY"
                            else -> "LIST"
                        }
                    }

                    val pageMainListState = remember(folder) { LazyListState() }

                    when (pageCurrentScreen) {

                        "RESUME" -> {
                            com.demonlab.lune.ui.screens.ResumeScreen(
                                viewModel = musicViewModel,
                                allSongs = pageFilteredSongs,
                                allPlaylists = musicViewModel.playlists,
                                bottomPadding = bottomPadding,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                    onSongClick = { song, listContext ->
                                    onCurrentSongChange(song)
                                    playbackManager.play(song, listContext, -100L, category = "ALL", shuffleMode = playbackManager.isShuffle)
                                    onIsPlayingChange(true)
                                },
                                onPlaylistClick = { playlist ->
                                    selectedPlaylist = playlist
                                },
                                onArtistClick = { artistName ->
                                    val artistSongs = visibleSongs.filter { it.artist == artistName }
                                    val artistAlbum = Album(
                                        id = artistName.hashCode().toLong(),
                                        name = artistName,
                                        artist = "",
                                        albumArtUri = artistSongs.firstOrNull()?.albumArtUri,
                                        coverUrl = artistSongs.firstOrNull()?.coverUrl,
                                        songs = artistSongs.sortedBy { it.title }
                                    )
                                    selectedAlbum = artistAlbum
                                    isAlbumView = false
                                    onSelectedFolderChange("ARTISTS")
                                },
                                onGenreClick = { genreName ->
                                    val genreSongs = if (genreName == "Desconocido") {
                                        visibleSongs.filter {
                                            val g = it.genre?.trim()
                                            g.isNullOrEmpty() || g.equals("<unknown>", ignoreCase = true) || g.equals("unknown", ignoreCase = true)
                                        }
                                    } else {
                                        visibleSongs.filter { it.genre?.trim() == genreName }
                                    }
                                    val genreAlbum = Album(
                                        id = genreName.hashCode().toLong(),
                                        name = genreName,
                                        artist = "",
                                        albumArtUri = genreSongs.firstOrNull()?.albumArtUri,
                                        coverUrl = genreSongs.firstOrNull()?.coverUrl,
                                        songs = genreSongs.sortedBy { it.title }
                                    )
                                    selectedAlbum = genreAlbum
                                    isAlbumView = false
                                    onSelectedFolderChange("GENRES")
                                },
                                onExpandPlayer = { onIsPlayerExpandedChange(true) },
                                onPlayToggle = {
                                    if (isPlaying) playbackManager.pause() else playbackManager.resume()
                                    onIsPlayingChange(!isPlaying)
                                }
                            )
                        }
                        "ALBUM_GRID" -> {
                            var viewStyle by remember { mutableIntStateOf(settingsManager.albumViewStyle) }
                            
                            Column(modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 4.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    AlbumsListHeader(
                                        albumCount = albumsList.size,
                                        viewStyle = viewStyle,
                                        onToggleViewStyle = {
                                            val newStyle = if (viewStyle == 0) 1 else 0
                                            viewStyle = newStyle
                                            settingsManager.albumViewStyle = newStyle
                                        },
                                        isAlbumView = true,
                                        onToggleAlbumView = null
                                    )
                                }
                                
                                Box(modifier = Modifier.weight(1f)) {
                                    if (viewStyle == 0) {
                                        AlbumGrid(
                                            albums = albumsList,
                                            onAlbumClick = { selectedAlbum = it },
                                            bottomPadding = bottomPadding,
                                            activePlaylistId = currentSong?.album?.hashCode()?.toLong()
                                        )
                                    } else {
                                        AlbumStackedCarousel(
                                            albums = albumsList,
                                            onAlbumClick = { selectedAlbum = it },
                                            bottomPadding = bottomPadding,
                                            activePlaylistId = currentSong?.album?.hashCode()?.toLong()
                                        )
                                    }
                                }
                            }
                        }
                        "ARTIST_GRID" -> {
                            var viewStyle by remember { mutableIntStateOf(settingsManager.albumViewStyle) }
                            
                            Column(modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 4.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    AlbumsListHeader(
                                        albumCount = artistsList.size,
                                        viewStyle = viewStyle,
                                        onToggleViewStyle = {
                                            val newStyle = if (viewStyle == 0) 1 else 0
                                            viewStyle = newStyle
                                            settingsManager.albumViewStyle = newStyle
                                        },
                                        isAlbumView = false,
                                        onToggleAlbumView = null
                                    )
                                }
                                
                                Box(modifier = Modifier.weight(1f)) {
                                    if (viewStyle == 0) {
                                        AlbumGrid(
                                            albums = artistsList,
                                            onAlbumClick = { selectedAlbum = it },
                                            bottomPadding = bottomPadding,
                                            activePlaylistId = currentSong?.artist?.hashCode()?.toLong()
                                        )
                                    } else {
                                        AlbumStackedCarousel(
                                            albums = artistsList,
                                            onAlbumClick = { selectedAlbum = it },
                                            bottomPadding = bottomPadding,
                                            activePlaylistId = currentSong?.artist?.hashCode()?.toLong()
                                        )
                                    }
                                }
                            }
                        }
                        "GENRE_GRID" -> {
                            var viewStyle by remember { mutableIntStateOf(settingsManager.albumViewStyle) }
                            
                            Column(modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 4.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    AlbumsListHeader(
                                        albumCount = genresList.size,
                                        viewStyle = viewStyle,
                                        onToggleViewStyle = {
                                            val newStyle = if (viewStyle == 0) 1 else 0
                                            viewStyle = newStyle
                                            settingsManager.albumViewStyle = newStyle
                                        },
                                        isAlbumView = false,
                                        onToggleAlbumView = null,
                                        title = sTabGenres,
                                        icon = Icons.Default.Category
                                    )
                                }
                                
                                Box(modifier = Modifier.weight(1f)) {
                                    if (viewStyle == 0) {
                                        AlbumGrid(
                                            albums = genresList,
                                            onAlbumClick = { selectedAlbum = it },
                                            bottomPadding = bottomPadding,
                                            activePlaylistId = null
                                        )
                                    } else {
                                        AlbumStackedCarousel(
                                            albums = genresList,
                                            onAlbumClick = { selectedAlbum = it },
                                            bottomPadding = bottomPadding,
                                            activePlaylistId = null
                                        )
                                    }
                                }
                            }
                        }
                        "PLAYLIST_GRID" -> {
                            PlaylistListScreen(
                                viewModel = musicViewModel,
                                onPlaylistClick = { selectedPlaylist = it },
                                onPlayPlaylist = { playlist ->
                                    musicViewModel.getSongsForPlaylist(playlist.id) { songs ->
                                        if (songs.isNotEmpty()) {
                                            playbackManager.play(songs[0], songs, playlist.id, playlist.name, category = "PLAYLISTS")
                                            onCurrentSongChange(songs[0])
                                            onIsPlayingChange(true)
                                        }
                                    }
                                },
                                onDeletePlaylist = { playlist ->
                                    val isActive = playbackManager.activePlaylistId == playlist.id
                                    musicViewModel.deletePlaylist(playlist) {
                                        playbackManager.checkPlaylistStatus()
                                        if (isActive) {
                                            if (musicViewModel.allSongs.isNotEmpty()) {
                                                playbackManager.play(currentSong ?: musicViewModel.allSongs[0], musicViewModel.allSongs, -100L, category = "ALL", shuffleMode = playbackManager.isShuffle)
                                            }
                                        }
                                    }
                                },
                                bottomPadding = bottomPadding
                            )
                        }
                        "FOLDER_GRID" -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 4.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Folder,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.tab_folders),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = visibleFolders.size.toString(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(
                                                onClick = {
                                                    folderHierarchyMode = !folderHierarchyMode
                                                    settingsManager.folderHierarchyMode = folderHierarchyMode
                                                },
                                                shape = CircleShape,
                                                color = if (folderHierarchyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        if (folderHierarchyMode) Icons.AutoMirrored.Filled.List else Icons.Default.Folder,
                                                        contentDescription = null,
                                                        tint = if (folderHierarchyMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                onClick = { onShowFolderSheetChange(true) },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.FilterList,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                val parentFolders = remember(hierarchyEntries) {
                                    hierarchyEntries.filterIndexed { index, entry ->
                                        index + 1 < hierarchyEntries.size && hierarchyEntries[index + 1].depth > entry.depth
                                    }.map { it.name }.toSet()
                                }

                                var expandedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }

                                val filteredHierarchy = remember(hierarchyEntries, expandedFolders) {
                                    val result = mutableListOf<FolderEntry>()
                                    var i = 0
                                    while (i < hierarchyEntries.size) {
                                        val entry = hierarchyEntries[i]
                                        result.add(entry)
                                        val nextIdx = i + 1
                                        if (nextIdx < hierarchyEntries.size && hierarchyEntries[nextIdx].depth > entry.depth) {
                                            if (entry.name !in expandedFolders) {
                                                while (i + 1 < hierarchyEntries.size && hierarchyEntries[i + 1].depth > entry.depth) {
                                                    i++
                                                }
                                            }
                                        }
                                        i++
                                    }
                                    result
                                }

                                val folderDirMap = remember(visibleFolders, rawAllSongs) {
                                    visibleFolders.mapNotNull { folder ->
                                        rawAllSongs.firstOrNull { it.folderName == folder }
                                            ?.let { folder to it.path.substringBeforeLast("/") }
                                    }.toMap()
                                }

                                val isFolderCategory = playbackManager.activeCategory == "FOLDERS"
                                val playingFolderName = remember(playbackManager.activePlaylistId, visibleFolders) {
                                    if (playbackManager.activeCategory == "FOLDERS" && playbackManager.activePlaylistId != null) {
                                        visibleFolders.firstOrNull { it.hashCode().toLong() == playbackManager.activePlaylistId }
                                    } else null
                                }

                                val ancestorNames = remember(playingFolderName, folderDirMap) {
                                    val playingDir = playingFolderName?.let { folderDirMap[it] } ?: return@remember emptySet()
                                    folderDirMap.filter { (name, dir) ->
                                        name != playingFolderName && playingDir.startsWith(dir + "/")
                                    }.keys
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
                                ) {
                                    if (folderHierarchyMode) {
                                        itemsIndexed(filteredHierarchy) { index, entry ->
                                            val songCount = visibleSongs.count { it.folderName == entry.name }
                                            val hasChildren = entry.name in parentFolders
                                            val isPlaying = isFolderCategory && playbackManager.activePlaylistId == entry.name.hashCode().toLong()
                                            val isAncestor = entry.name in ancestorNames
                                            ListItem(
                                                supportingContent = {
                                                    if (!entry.isVirtual) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("$songCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                },
                                                leadingContent = {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = if (entry.isVirtual) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.secondaryContainer,
                                                            modifier = Modifier.size(56.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    Icons.Default.Folder,
                                                                    contentDescription = null,
                                                                    tint = if (entry.isVirtual) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer,
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                            }
                                                        }
                                                        if (isPlaying) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.BottomEnd)
                                                                    .size(14.dp)
                                                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                                            )
                                                        }
                                                    }
                                                },
                                                trailingContent = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (isAncestor) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(8.dp)
                                                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                        }
                                                        if (hasChildren) {
                                                            IconButton(onClick = {
                                                                expandedFolders = if (entry.name in expandedFolders) {
                                                                    expandedFolders - entry.name
                                                                } else {
                                                                    expandedFolders + entry.name
                                                                }
                                                            }) {
                                                                Icon(
                                                                    if (entry.name in expandedFolders) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowRight,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .padding(start = (entry.depth * 24).dp)
                                                    .then(
                                                        if (entry.isVirtual) Modifier else Modifier.clickable { selectedFolderItem = entry.name }
                                                    )
                                            ) {
                                                Text(entry.name, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    } else {
                                        itemsIndexed(hierarchyEntries) { index, entry ->
                                            val songCount = visibleSongs.count { it.folderName == entry.name }
                                            val isPlaying = isFolderCategory && playbackManager.activePlaylistId == entry.name.hashCode().toLong()
                                            ListItem(
                                                supportingContent = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("$songCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                leadingContent = {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(56.dp)) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                                                            }
                                                        }
                                                        if (isPlaying) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.BottomEnd)
                                                                    .size(14.dp)
                                                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                                            )
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .padding(start = (entry.depth * 24).dp)
                                                    .clickable { selectedFolderItem = entry.name }
                                            ) {
                                                Text(entry.name, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "EMPTY" -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (hasPermission) stringResource(R.string.no_music_available) else stringResource(R.string.permission_required),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                        "LIST" -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val isCurrentListPlaying = playbackManager.activePlaylistId == pageContextId && playbackManager.activeCategory == folder
                                var localShuffleState by remember(pageContextId) { mutableStateOf(settingsManager.getPlaylistShuffle(pageContextId)) }
                                val isShuffleActive = if (folder == "ALL" || isCurrentListPlaying) playbackManager.isShuffle else localShuffleState
                                val showSimplifiedHeader = folder == "ALL" || folder == "FAVORITES" || (!listOf("RESUME", "ALBUMS", "PLAYLISTS").contains(folder))

                                LazyColumn(
                                    state = pageMainListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = bottomPadding)
                                ) {
                                    if (showSimplifiedHeader) {
                                        item {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                shape = RoundedCornerShape(20.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                tonalElevation = 4.dp,
                                                shadowElevation = 0.dp
                                            ) {
                                                SongsListHeader(
                                                    songs = pageSortedSongs,
                                                    folderName = folder,
                                                    isShuffleActive = isShuffleActive,
                                                    isCurrentListPlaying = isCurrentListPlaying,
                                                    isSortActive = activeSortOption != "ALPHABETICAL" || !activeIsSortAscending,
                                                    onSortClick = { showSortSheet = true },
                                                    onPlayClick = {
                                                        if (settingsManager.isHapticVibrationEnabled) {
                                                            vibrator.triggerLightVibration()
                                                        }
                                                        if (isCurrentListPlaying) {
                                                            if (isPlaying) playbackManager.pause() else playbackManager.resume()
                                                            onIsPlayingChange(!isPlaying)
                                                        } else if (pageSortedSongs.isNotEmpty()) {
                                                            val songToPlay = if (isShuffleActive) pageSortedSongs.random() else pageSortedSongs[0]
                                                            onCurrentSongChange(songToPlay)
                                                            playbackManager.play(songToPlay, pageSortedSongs, pageContextId, category = folder, shuffleMode = isShuffleActive)
                                                            onIsPlayingChange(true)
                                                        }
                                                    },
                                                    onShuffleClick = {
                                                        if (isCurrentListPlaying || folder == "ALL") {
                                                            playbackManager.toggleShuffle()
                                                            localShuffleState = playbackManager.isShuffle
                                                        } else {
                                                            localShuffleState = !localShuffleState
                                                            settingsManager.setPlaylistShuffle(pageContextId, localShuffleState)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    itemsIndexed(pageSortedSongs, key = { _, it -> it.id }) { index, song ->
                                        val isFirst = index == 0
                                        val isLast = index == pageSortedSongs.lastIndex
                                            SongItem(
                                                isFirst = isFirst,
                                                isLast = isLast,
                                                song = song,
                                                currentlyPlaying = playbackManager.currentSong?.id == song.id && playbackManager.activePlaylistId == pageContextId,
                                                isPlaying = isPlaying,
                                            onClick = {
                                                if (playbackManager.currentSong?.id != song.id || playbackManager.activePlaylistId != pageContextId) {
                                                    onCurrentSongChange(song)
                                                    playbackManager.play(song, pageSortedSongs, pageContextId, category = folder, shuffleMode = isShuffleActive)
                                                    onIsPlayingChange(true)
                                                }
                                            },
                                            onOptionsClick = {
                                                optionsSong = song
                                                showOptionsSheet = true
                                            },
                                            onFavoriteClick = { s ->
                                                playbackManager.toggleFavorite(s)?.let { updated ->
                                                    musicViewModel.syncFavoriteStatusInMemory(updated.id, updated.isFavorite)
                                                }
                                            }
                                        )
                                        
                                    }
                                }

                                val targetIndex = remember(pageSortedSongs, playbackManager.currentSong, pageContextId, playbackManager.activePlaylistId, showSimplifiedHeader) {
                                    val cs = playbackManager.currentSong
                                    if (cs != null && playbackManager.activePlaylistId == pageContextId) {
                                        val idx = pageSortedSongs.indexOfFirst { it.id == cs.id }
                                        if (idx != -1) idx + (if (showSimplifiedHeader) 1 else 0) else -1
                                    } else -1
                                }

                                LaunchedEffect(scrollToCurrentTrigger.value) {
                                    if (targetIndex != -1 && scrollToCurrentTrigger.value > 0) {
                                        pageMainListState.animateScrollToItem(targetIndex)
                                        scrollToCurrentTrigger.value = 0
                                    }
                                }

                                FastScrollbar(
                                    listState = pageMainListState,
                                    items = pageSortedSongs,
                                    headerItemCount = if (showSimplifiedHeader) 1 else 0,
                                    itemKeyOrLetter = { if (activeSortOption == "ALPHABETICAL") it.title else "" },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(bottom = bottomPadding)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showFolderSheet) {
            val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
            ModalBottomSheet(
                onDismissRequest = { onShowFolderSheetChange(false) },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                FolderFilterContent(
                    allFolders = allFolders,
                    hiddenFolders = hiddenFolders,
                    selectedFolder = selectedFolder,
                    onSelectedFolderChange = onSelectedFolderChange
                )
            }
        }

        // com.demonlab.lune.data.Playlist Detail Overlay
        AnimatedVisibility(
            visible = selectedPlaylist != null && !isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            var lastPlaylist by remember { mutableStateOf(selectedPlaylist) }
            if (selectedPlaylist != null) {
                lastPlaylist = selectedPlaylist
            }
            
            val playlistSongs = remember(lastPlaylist, musicViewModel.allSongs, musicViewModel.playlistMappings) {
                lastPlaylist?.let {
                    musicViewModel.getSongsForPlaylistSync(it.id)
                } ?: emptyList()
            }
            
            lastPlaylist?.let { playListRender ->
                PlaylistDetailView(
                    playlist = playListRender,
                    songs = playlistSongs,
                    sortOption = activeSortOption,
                    isSortAscending = activeIsSortAscending,
                    onBack = { selectedPlaylist = null },
                    onSongClick = { song, sortedList ->
                        playbackManager.play(song, sortedList, playListRender.id, playListRender.name, category = "PLAYLISTS")
                        onCurrentSongChange(song)
                        onIsPlayingChange(true)
                    },
                    onOptionsClick = { song ->
                        optionsSong = song
                        showOptionsSheet = true
                    },
                    onSortClick = { showSortSheet = true },
                    currentlyPlayingId = if (playbackManager.activePlaylistId == playListRender.id) currentSong?.id else null,
                    bottomPadding = bottomPadding,
                    viewModel = musicViewModel,
                    onFavoriteClick = { song ->
                        playbackManager.toggleFavorite(song)?.let { updated ->
                            musicViewModel.syncFavoriteStatusInMemory(updated.id, updated.isFavorite)
                        }
                    },
                    scrollToCurrentTrigger = scrollToCurrentTrigger
                )
            }
        }

        // Album Detail Overlay
        AnimatedVisibility(
            visible = selectedAlbum != null && !isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            var lastAlbum by remember { mutableStateOf(selectedAlbum) }
            if (selectedAlbum != null) {
                lastAlbum = selectedAlbum
            }
            
            lastAlbum?.let { albumRender ->
                val albumSongs = remember(albumRender.name, visibleSongs, selectedFolder, isAlbumView) {
                    when (selectedFolder) {
                        "GENRES" -> {
                            if (albumRender.name == "Desconocido") {
                                visibleSongs.filter {
                                    val g = it.genre?.trim()
                                    g.isNullOrEmpty() || g.equals("<unknown>", ignoreCase = true) || g.equals("unknown", ignoreCase = true)
                                }
                            } else {
                                visibleSongs.filter { it.genre?.trim() == albumRender.name }
                            }
                        }
                        "ARTISTS" -> visibleSongs.filter { it.artist == albumRender.name }
                        "ALBUMS" -> visibleSongs.filter { it.album == albumRender.name }
                        else -> {
                            if (isAlbumView) visibleSongs.filter { it.album == albumRender.name }
                            else visibleSongs.filter { it.artist == albumRender.name }
                        }
                    }
                }
                AlbumDetailView(
                    album = albumRender,
                    songs = albumSongs,
                    sortOption = activeSortOption,
                    isSortAscending = activeIsSortAscending,
                    onBack = { selectedAlbum = null },
                    onSongClick = { song, sortedList ->
                        val cat = if (selectedFolder == "GENRES") "GENRES" else if (selectedFolder == "ARTISTS") "ARTISTS" else "ALBUMS"
                        playbackManager.play(song, sortedList, albumRender.id, category = cat)
                        onCurrentSongChange(song)
                        onIsPlayingChange(true)
                    },
                    onOptionsClick = { song ->
                        optionsSong = song
                        showOptionsSheet = true
                    },
                    onSortClick = { showSortSheet = true },
                    currentlyPlayingId = if (playbackManager.activePlaylistId == albumRender.id) currentSong?.id else null,
                    bottomPadding = bottomPadding,
                    onFavoriteClick = { song ->
                        playbackManager.toggleFavorite(song)?.let { updated ->
                            musicViewModel.syncFavoriteStatusInMemory(updated.id, updated.isFavorite)
                        }
                    },
                    scrollToCurrentTrigger = scrollToCurrentTrigger
                )
            }
        }

        // com.demonlab.lune.data.Folder Detail Overlay
        AnimatedVisibility(
            visible = selectedFolderItem != null && !isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            var lastFolder by remember { mutableStateOf(selectedFolderItem) }
            if (selectedFolderItem != null) {
                lastFolder = selectedFolderItem
            }

            lastFolder?.let { folderName ->
                val folderSongs = remember(folderName, visibleSongs) {
                    visibleSongs.filter { it.folderName == folderName }
                }
                FolderDetailView(
                    folderName = folderName,
                    songs = folderSongs,
                    sortOption = activeSortOption,
                    isSortAscending = activeIsSortAscending,
                    onBack = { selectedFolderItem = null },
                    onSongClick = { song, sortedList ->
                        playbackManager.play(song, sortedList, folderName.hashCode().toLong(), category = "FOLDERS")
                        onCurrentSongChange(song)
                        onIsPlayingChange(true)
                    },
                    onOptionsClick = { song ->
                        optionsSong = song
                        showOptionsSheet = true
                    },
                    onSortClick = { showSortSheet = true },
                    currentlyPlayingId = if (playbackManager.activePlaylistId == folderName.hashCode().toLong()) currentSong?.id else null,
                    bottomPadding = bottomPadding,
                    onFavoriteClick = { song ->
                        playbackManager.toggleFavorite(song)?.let { updated ->
                            musicViewModel.syncFavoriteStatusInMemory(updated.id, updated.isFavorite)
                        }
                    },
                    scrollToCurrentTrigger = scrollToCurrentTrigger
                )
            }
        }

        val miniPlayerShape = RoundedCornerShape(20.dp)

        // Mini Player

        // Bottom Controls (Unified Pill + Mini Player)
        if (!isPlayerExpanded) {
            val isDarkThemeMini = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            val miniPrefs = LocalContext.current.getSharedPreferences("lune_settings", android.content.Context.MODE_PRIVATE)
            var blurEnabled by remember { mutableStateOf(settingsManager.isBlurEnabled) }
            var blurDarkMode by remember { mutableStateOf(settingsManager.isBlurDarkMode) }
            var blurLightMode by remember { mutableStateOf(settingsManager.isBlurLightMode) }
            DisposableEffect(miniPrefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "is_blur_enabled" -> blurEnabled = miniPrefs.getBoolean("is_blur_enabled", true)
                        "is_blur_dark_mode" -> blurDarkMode = miniPrefs.getBoolean("is_blur_dark_mode", true)
                        "is_blur_light_mode" -> blurLightMode = miniPrefs.getBoolean("is_blur_light_mode", false)
                    }
                }
                miniPrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { miniPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            val hasBlurBackgroundMini = blurEnabled &&
                (if (isDarkThemeMini) blurDarkMode else blurLightMode)

            if (currentSong != null) {
                val song = currentSong
                AnimatedContent(
                    targetState = settingsManager.isMiniPlayerMinimized,
                    transitionSpec = {
                        fadeIn(tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(150)) + scaleOut(targetScale = 0.8f, animationSpec = tween(250, easing = FastOutSlowInEasing)) using
                        SizeTransform(clip = false) { _, _ ->
                            tween(300, easing = FastOutSlowInEasing)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    label = "miniPlayerTransition"
                ) { minimized ->
                    if (minimized) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = bottomInset + 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                UnifiedHeaderPill(
                                    selectedFolder = selectedFolder,
                                    folders = folders,
                                    onSelectedFolderChange = onSelectedFolderChange,
                                    showSectionMenuSheet = { showSectionMenuSheet = true },
                                    showSearchScreen = { showSearchScreen = true },
                                    playbackManager = playbackManager,
                                    sTabResume = sTabResume,
                                    sTabAll = sTabAll,
                                    sTabFavorites = sTabFavorites,
                                    sTabAlbums = sTabAlbums,
                                    sTabArtists = sTabArtists,
                                    sTabGenres = sTabGenres,
                                    sTabPlaylists = sTabPlaylists,
                                    sTabFolders = sTabFolders
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            MiniPlayerMinimized(
                                song = song,
                                coverShape = coverShape,
                                coverScale = coverScale,
                                coverSpin = coverSpin,
                                coverVinylEffect = coverVinylEffect,
                                hasBlurBackground = hasBlurBackgroundMini,
                                isDarkTheme = isDarkThemeMini,
                                isPlaying = isPlaying,
                                onRestore = { settingsManager.isMiniPlayerMinimized = false },
                                onExpandPlayer = { onIsPlayerExpandedChange(true) }
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = bottomInset + 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                            ) {
                                UnifiedHeaderPill(
                                    selectedFolder = selectedFolder,
                                    folders = folders,
                                    onSelectedFolderChange = onSelectedFolderChange,
                                    showSectionMenuSheet = { showSectionMenuSheet = true },
                                    showSearchScreen = { showSearchScreen = true },
                                    playbackManager = playbackManager,
                                    sTabResume = sTabResume,
                                    sTabAll = sTabAll,
                                    sTabFavorites = sTabFavorites,
                                    sTabAlbums = sTabAlbums,
                                    sTabArtists = sTabArtists,
                                    sTabGenres = sTabGenres,
                                    sTabPlaylists = sTabPlaylists,
                                    sTabFolders = sTabFolders
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 25.dp)
                            ) {
                                MiniPlayer(
                                    song = song,
                                    isPlaying = isPlaying,
                                    showWaveform = playbackManager.isMiniPlayerVisualizerEnabled,
                                    visualizerData = visualizerData,
                                    currentOutputIcon = playbackManager.currentOutputIcon,
                                    coverShape = coverShape,
                                    coverScale = coverScale,
                                    coverSpin = coverSpin,
                                    coverVinylEffect = coverVinylEffect,
                                    controlsIconStyle = controlsIconStyle,
                                    isControlsFilled = isControlsFilled,
                                    useCustomControlsColor = useCustomControlsColor,
                                    controlsColorPalette = controlsColorPalette,
                                    shape = miniPlayerShape,
                                    hasBlurBackground = hasBlurBackgroundMini,
                                    isDarkTheme = isDarkThemeMini,
                                    onTogglePlay = { 
                                        if (settingsManager.isHapticVibrationEnabled) {
                                            vibrator.triggerLightVibration()
                                        }
                                        if (isPlaying) playbackManager.pause() else playbackManager.resume()
                                        onIsPlayingChange(!isPlaying)
                                    },
                                    onExpand = { onIsPlayerExpandedChange(true) },
                                    onPrevious = playPrevious,
                                    onNext = playNext,
                                    onSearchClick = { showSearchScreen = true },
                                    onScrollToCurrent = { scrollToCurrentTrigger.value++ },
                                    onMinimize = { settingsManager.isMiniPlayerMinimized = true }
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = bottomInset + 8.dp)
                ) {
                    UnifiedHeaderPill(
                        selectedFolder = selectedFolder,
                        folders = folders,
                        onSelectedFolderChange = onSelectedFolderChange,
                        showSectionMenuSheet = { showSectionMenuSheet = true },
                        showSearchScreen = { showSearchScreen = true },
                        playbackManager = playbackManager,
                        sTabResume = sTabResume,
                        sTabAll = sTabAll,
                        sTabFavorites = sTabFavorites,
                        sTabAlbums = sTabAlbums,
                        sTabArtists = sTabArtists,
                        sTabGenres = sTabGenres,
                        sTabPlaylists = sTabPlaylists,
                        sTabFolders = sTabFolders
                    )
                }
            }
        }

        // Full Player
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            val song = currentSong
            if (song != null) {
                FullPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    progress = playbackProgress,
                    onProgressChange = { newProgress -> 
                        onPlaybackProgressChange(newProgress)
                        playbackManager.seekTo(newProgress)
                    },
                    onTogglePlay = { 
                        if (settingsManager.isHapticVibrationEnabled) {
                            vibrator.triggerLightVibration()
                        }
                        if (isPlaying) playbackManager.pause() else playbackManager.resume()
                        onIsPlayingChange(!isPlaying)
                    },
                    onMinimize = { onIsPlayerExpandedChange(false) },
                    onNext = playNext,
                    onPrevious = playPrevious,
                    onRefreshSongs = onRefreshSongs,
                    onSyncFavorite = { songId, isFav -> musicViewModel.syncFavoriteStatusInMemory(songId, isFav) },
                    showWaveform = playbackManager.isFullPlayerVisualizerEnabled,
                    onToggleWaveform = {}, // Not used anymore as we have settings sheet
                    visualizerData = visualizerData,
                    coverShape = coverShape,
                    coverScale = coverScale,
                    coverSpin = coverSpin,
                    coverVinylEffect = coverVinylEffect,
                    controlsIconStyle = controlsIconStyle,
                    isControlsFilled = isControlsFilled,
                    useCustomControlsColor = useCustomControlsColor,
                    controlsColorPalette = controlsColorPalette,
                    onShowLyrics = {
                        val intent = Intent(context, LyricsActivity::class.java)
                        context.startActivity(intent)
                    },
                    onRequestAudioPermission = onRequestAudioPermission,
                    onArtistClick = { artistName ->
                        val artistAlbum = Album(
                            id = artistName.hashCode().toLong(),
                            name = artistName,
                            artist = "",
                            albumArtUri = visibleSongs.firstOrNull { it.artist == artistName }?.albumArtUri,
                            coverUrl = visibleSongs.firstOrNull { it.artist == artistName }?.coverUrl,
                            songs = visibleSongs.filter { it.artist == artistName }.sortedBy { it.title }
                        )
                        selectedAlbum = artistAlbum
                        isAlbumView = false
                        onIsPlayerExpandedChange(false)
                        onSelectedFolderChange("ALBUMS")
                    }
                )
            }
        }
    }

    if (isPlayerExpanded) {
        BackHandler {
            onIsPlayerExpandedChange(false)
        }
    }

    // Search Screen Overlay
    AnimatedVisibility(
        visible = showSearchScreen,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        modifier = Modifier.fillMaxSize()
    ) {
        SearchScreen(
            viewModel = musicViewModel,
            allFolders = visibleFolders,
            onDismiss = { showSearchScreen = false },
            onSongClick = { song, queue, category, parentId ->
                val useAllContext = parentId == -300L
                playbackManager.play(song, queue, playlistId = if (useAllContext) -100L else parentId, category = if (useAllContext) "ALL" else category)
                onCurrentSongChange(song)
                onIsPlayingChange(true)
                onSelectedFolderChange(if (useAllContext) "ALL" else category)
                showSearchScreen = false
            },
            onPlayAll = { matchedSongs, shuffleOn ->
                playbackManager.play(matchedSongs.first(), matchedSongs, playlistId = -300L, category = "ALL", shuffleMode = shuffleOn)
                onCurrentSongChange(matchedSongs.first())
                onIsPlayingChange(true)
                onSelectedFolderChange("RESUME")
                showSearchScreen = false
            },
            onNavigateToAlbum = { album ->
                selectedAlbum = album
                isAlbumView = album.artist.isNotEmpty()
                showSearchScreen = false
                onSelectedFolderChange("ALBUMS")
            },
            onNavigateToPlaylist = { playlist ->
                selectedPlaylist = playlist
                showSearchScreen = false
                onSelectedFolderChange("PLAYLISTS")
            },
            onNavigateToFolder = { folder ->
                onSelectedFolderChange("FOLDERS")
                selectedFolderItem = folder
                showSearchScreen = false
            },
            onOptionsClick = { song ->
                optionsSong = song
                showOptionsSheet = true
            },
            onFavoriteClick = { song ->
                playbackManager.toggleFavorite(song)?.let { updated ->
                    musicViewModel.syncFavoriteStatusInMemory(updated.id, updated.isFavorite)
                }
            },
            currentlyPlayingId = currentSong?.id,
            activeCategory = playbackManager.activeCategory,
            activePlaylistId = playbackManager.activePlaylistId
        )
    }
    if (showEditSheet) {
        editingSong?.let { song ->
            EditSongBottomSheet(
                song = song,
                onDismiss = { showEditSheet = false },
                onRestore = {
                    musicViewModel.restoreOriginalMetadata(
                        song = song,
                        onSuccess = {
                            val updatedSong = musicViewModel.allSongs.find { it.id == song.id }
                            if (updatedSong != null && currentSong?.id == song.id) {
                                playbackManager.updateSongMetadata(updatedSong)
                                onCurrentSongChange(updatedSong)
                            }
                            Toast.makeText(context, context.getString(R.string.info_restored), Toast.LENGTH_SHORT).show()
                            showEditSheet = false
                        }
                    )
                },
                onSave = { updatedTitle, updatedArtist, updatedAlbum, updatedGenre, updatedCoverUri ->
                    musicViewModel.updateMetadata(
                        song = song,
                        title = updatedTitle,
                        artist = updatedArtist,
                        album = updatedAlbum,
                        genre = updatedGenre,
                        coverUri = updatedCoverUri,
                        onSuccess = {
                            val updatedSong = musicViewModel.allSongs.find { it.id == song.id }
                            if (updatedSong != null && currentSong?.id == song.id) {
                                playbackManager.updateSongMetadata(updatedSong)
                                onCurrentSongChange(updatedSong)
                            }
                            Toast.makeText(context, context.getString(R.string.info_updated), Toast.LENGTH_SHORT).show()
                            showEditSheet = false
                        }
                    )
                }
            )
        }
    }

    if (showOptionsSheet && optionsSong != null) {
        SongOptionsBottomSheet(
            song = optionsSong!!,
            onDismiss = { showOptionsSheet = false },
            onAddToPlaylistClick = { showMainAddToPlaylistDialog = true },
            onEditMetadataClick = {
                editingSong = optionsSong
                showEditSheet = true
            },
            onDeleteClick = {
                songToDelete = optionsSong
                showDeleteDialog = true
            }
        )
    }

    if (showSortSheet) {
        SortBottomSheet(
            sortOption = activeSortOption,
            isSortAscending = activeIsSortAscending,
            isCaseSensitive = activeIsCaseSensitive,
            onSortSettingsChange = { option, ascending, caseSensitive ->
                activeSortOption = option
                activeIsSortAscending = ascending
                activeIsCaseSensitive = caseSensitive
                settingsManager.setSortOption(currentSortKey, option)
                settingsManager.setIsSortAscending(currentSortKey, ascending)
                settingsManager.setIsCaseSensitiveSort(currentSortKey, caseSensitive)
                if (playbackManager.activePlaylistId == activeContextId) {
                    playbackManager.setSortSettings(option, ascending)
                }
            },
            onDismiss = { showSortSheet = false }
        )
    }

    if (showDeleteDialog && songToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_song)) },
            text = { Text(stringResource(R.string.delete_song_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val song = songToDelete!!
                        showDeleteDialog = false
                        
                        // Fix: If current song is deleted, skip to next (respect pause state)
                        if (currentSong?.id == song.id) {
                            playbackManager.playNextFromService(startPlayback = isPlaying)
                        }
                        
                        musicViewModel.prepareDeleteSong(song)
                        coroutineScope.launch {
                            val totalTime = 8000L
                            val startTime = System.currentTimeMillis()
                            
                            val timerJob = launch {
                                while (System.currentTimeMillis() - startTime < totalTime) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    undoProgress = 1f - (elapsed.toFloat() / totalTime)
                                    undoSecondsRemaining = ((totalTime - elapsed) / 1000).toInt() + 1
                                    kotlinx.coroutines.delay(50)
                                }
                                undoProgress = 0f
                                undoSecondsRemaining = 0
                            }
                            
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.song_deleted),
                                actionLabel = context.getString(R.string.restore_music),
                                duration = SnackbarDuration.Indefinite
                            )
                            
                            timerJob.cancel()
                            
                            if (result == SnackbarResult.ActionPerformed) {
                                musicViewModel.undoDeleteSong(song)
                            } else {
                                musicViewModel.deleteSongPermanently(song.id, song.path, song.uri)
                            }
                        }
                        
                        // Auto-dismiss indefinite snackbar after 8s
                        coroutineScope.launch {
                             kotlinx.coroutines.delay(8000)
                             snackbarHostState.currentSnackbarData?.dismiss()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showMainAddToPlaylistDialog && optionsSong != null) {
        AddToPlaylistDialog(
            song = optionsSong!!,
            viewModel = musicViewModel,
            playbackManager = playbackManager,
            onDismiss = { showMainAddToPlaylistDialog = false }
        )
    }

    if (showSectionMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSectionMenuSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.sections_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
                )

                folders.forEach { folder ->
                    val isSelected = selectedFolder == folder
                    val label = when (folder) {
                        "RESUME" -> sTabResume
                        "ALL" -> sTabAll
                        "FAVORITES" -> sTabFavorites
                        "ALBUMS" -> sTabAlbums
                        "ARTISTS" -> sTabArtists
                        "GENRES" -> sTabGenres
                        "PLAYLISTS" -> sTabPlaylists
                        "FOLDERS" -> sTabFolders
                        else -> folder
                    }

                    Surface(
                        onClick = {
                            onSelectedFolderChange(folder)
                            showSectionMenuSheet = false
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    when (folder) {
                                        "RESUME" -> Icon(Icons.Default.History, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        "ALL" -> Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        "ALBUMS" -> Icon(Icons.Default.Album, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        "ARTISTS" -> Icon(Icons.Default.Person, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        "GENRES" -> Icon(Icons.Default.Category, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        "PLAYLISTS" -> Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        "FOLDERS" -> Icon(Icons.Default.Folder, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        "FAVORITES" -> Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        else -> Icon(Icons.Default.Folder, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun UnifiedHeaderPill(
    selectedFolder: String,
    folders: List<String>,
    onSelectedFolderChange: (String) -> Unit,
    showSectionMenuSheet: () -> Unit,
    showSearchScreen: () -> Unit,
    playbackManager: PlaybackManager,
    sTabResume: String,
    sTabAll: String,
    sTabFavorites: String,
    sTabAlbums: String,
    sTabArtists: String,
    sTabGenres: String,
    sTabPlaylists: String,
    sTabFolders: String,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val luma = surfaceColor.red * 0.299f + surfaceColor.green * 0.587f + surfaceColor.blue * 0.114f
    val isDark = luma < 0.5f
    val selectedBg = if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
    val onSelected = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary

    val outerPillColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val outerBorder = if (isDark) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }

    val entranceNudge = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entranceNudge.animateTo(
            targetValue = 28f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
        entranceNudge.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
        )
    }

    val sectionPillScale = remember { Animatable(1f) }
    LaunchedEffect(selectedFolder) {
        sectionPillScale.snapTo(0.88f)
        sectionPillScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = 350f)
        )
    }

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = outerPillColor,
        border = outerBorder,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: Active Section Pill
            val activeLabel = when(selectedFolder) {
                "RESUME" -> sTabResume
                "ALL" -> sTabAll
                "FAVORITES" -> sTabFavorites
                "ALBUMS" -> sTabAlbums
                "ARTISTS" -> sTabArtists
                "GENRES" -> sTabGenres
                "PLAYLISTS" -> sTabPlaylists
                "FOLDERS" -> sTabFolders
                else -> selectedFolder
            }

            val isCurrentContext = playbackManager.activeCategory == selectedFolder && playbackManager.currentSong != null && playbackManager.activePlaylistId != -300L

            Surface(
                onClick = { showSectionMenuSheet() },
                shape = RoundedCornerShape(24.dp),
                color = selectedBg,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = entranceNudge.value
                        scaleX = sectionPillScale.value
                        scaleY = sectionPillScale.value
                    }
                    .bounceClick()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (selectedFolder) {
                            "RESUME" -> Icon(Icons.Default.History, contentDescription = activeLabel, tint = onSelected, modifier = Modifier.size(20.dp))
                            "ALL" -> Icon(Icons.Default.LibraryMusic, contentDescription = activeLabel, tint = onSelected, modifier = Modifier.size(20.dp))
                            "ALBUMS" -> Icon(Icons.Default.Album, contentDescription = activeLabel, tint = onSelected, modifier = Modifier.size(20.dp))
                            "ARTISTS" -> Icon(Icons.Default.Person, contentDescription = activeLabel, tint = onSelected, modifier = Modifier.size(20.dp))
                            "GENRES" -> Icon(Icons.Default.Category, contentDescription = activeLabel, tint = onSelected, modifier = Modifier.size(20.dp))
                            "PLAYLISTS" -> Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = activeLabel, tint = onSelected, modifier = Modifier.size(20.dp))
                            "FOLDERS" -> Icon(Icons.Default.Folder, contentDescription = activeLabel, tint = onSelected, modifier = Modifier.size(20.dp))
                            "FAVORITES" -> Icon(Icons.Default.FavoriteBorder, contentDescription = activeLabel, tint = onSelected, modifier = Modifier.size(20.dp))
                            else -> Icon(Icons.Default.Folder, contentDescription = activeLabel, tint = onSelected, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    AnimatedContent(
                        targetState = activeLabel,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) + slideInHorizontally { it / 2 } togetherWith
                                fadeOut(animationSpec = tween(150)) + slideOutHorizontally { -it / 2 }
                        },
                        label = "section_label"
                    ) { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onSelected
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = onSelected.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                    if (isCurrentContext) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(onSelected, CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // RIGHT SIDE: Search Bar (Text + Icon)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { showSearchScreen() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}



