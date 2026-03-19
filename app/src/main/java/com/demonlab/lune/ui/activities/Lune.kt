package com.demonlab.lune.ui.activities
import android.Manifest
import android.content.ContentUris
import android.content.Intent
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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.demonlab.lune.ui.viewmodels.MusicViewModel
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import com.demonlab.lune.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.demonlab.lune.tools.CoverProvider
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
import com.demonlab.lune.ui.screens.OnboardingScreen
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.demonlab.lune.tools.ImageAnalyzer
import android.graphics.drawable.BitmapDrawable
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
        enableEdgeToEdge()
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
            val TAB_PLAYLISTS = "PLAYLISTS"

            // LIFTED STRINGS
            val sTabResume = stringResource(R.string.tab_resume)
            val sTabAll = stringResource(R.string.tab_all)
            val sTabFavorites = stringResource(R.string.tab_favorites)
            val sTabAlbums = stringResource(R.string.tab_albums)
            val sTabPlaylists = stringResource(R.string.playlists)

            // LIFTED STATES & LOGIC
            var showOnboarding by remember { mutableStateOf(settingsManager.isFirstRun) }

            if (showOnboarding) {
                LuneTheme(darkTheme = isSystemInDarkTheme()) {
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
            
            val currentSong = playbackManager.currentSong
            val isPlaying = playbackManager.isPlaying
            var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
            var playbackProgress by remember { mutableStateOf(playbackManager.getProgress()) }

            // Permissions logic lifted
            val permissionsToRequest = remember {
                val list = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    list.add(Manifest.permission.READ_MEDIA_AUDIO)
                    list.add(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                list.add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    list.add(Manifest.permission.BLUETOOTH_CONNECT)
                }
                list
            }

            var hasPermission by remember {
                mutableStateOf(permissionsToRequest.all { 
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
                })
            }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val granted = permissions.values.all { it }
                hasPermission = granted
                if (granted) musicViewModel.loadSongs()
                else Toast.makeText(context, "Permissions required for full functionality", Toast.LENGTH_SHORT).show()
            }

            LaunchedEffect(hasPermission) {
                if (hasPermission) {
                    musicViewModel.loadSongs()
                } else {
                    launcher.launch(permissionsToRequest.toTypedArray())
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME && hasPermission) {
                        musicViewModel.loadSongs()
                        musicViewModel.loadPlaylists()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Sync Progress
            LaunchedEffect(isPlaying) {
                if (isPlaying) {
                    while (isPlaying) {
                        playbackProgress = playbackManager.getProgress()
                        kotlinx.coroutines.delay(500)
                    }
                } else {
                    // When playback stops (natural end), reset progress
                    if (playbackManager.getProgress() >= 0.90f || playbackManager.getProgress() < 0.01f) {
                        playbackProgress = 0f
                    }
                }
            }
            // Sync Visualizer when permission or playback state changes
            LaunchedEffect(hasPermission, isPlaying) {
                if (hasPermission && isPlaying) {
                    playbackManager.startVisualizer()
                } else if (!isPlaying) {
                     // Keep it running if it was already running? 
                     // Actually PlaybackManager.play() starts it. 
                     // But if we pause, we might want to stop it to save battery?
                     // The user said "move to rhythm", so if paused, it should stop.
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
            val folders = remember(visibleFolders, rawAllSongs, sTabPlaylists) {
                val hasFavorites = rawAllSongs.any { it.isFavorite }
                val base = mutableListOf("RESUME", "ALL", "PLAYLISTS")
                if (hasFavorites) base.add("FAVORITES")
                base.add("ALBUMS")
                base.addAll(visibleFolders)
                base
            }
            val visibleSongs = remember(rawAllSongs, hiddenFolders.value) {
                rawAllSongs.filter { !hiddenFolders.value.contains(it.folderName) }
            }
            val filteredSongs = remember(visibleSongs, selectedFolder) {
                when (selectedFolder) {
                    TAB_RESUME, TAB_ALL, TAB_ALBUMS -> visibleSongs
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

            LuneTheme(darkTheme = targetDarkTheme) {
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
                    onRefreshSongs = { musicViewModel.loadSongs() },
                    musicViewModel = musicViewModel,
                    settingsManager = settingsManager
                )

            }
        }
    }
}

@Composable
fun ResponsiveText(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    targetTextSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1
) {
    var textSize by remember(text) { mutableStateOf(targetTextSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        color = color,
        fontWeight = fontWeight,
        fontSize = textSize,
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow && textSize.value > 10f) {
                textSize = (textSize.value * 0.9f).sp
            } else {
                readyToDraw = true
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeMode: Int,
    onThemeModeChange: () -> Unit,
    rawAllSongs: List<Song>,
    filteredSongs: List<Song>,
    folders: List<String>,
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
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    val sTabResume = stringResource(R.string.tab_resume)
    val sTabAll = stringResource(R.string.tab_all)
    val sTabFavorites = stringResource(R.string.tab_favorites)
    val sTabAlbums = stringResource(R.string.tab_albums)
    val sTabPlaylists = stringResource(R.string.playlists)
    val contextId = remember(selectedFolder) {
        when (selectedFolder) {
            "RESUME", "ALL", "ALBUMS" -> -100L
            "FAVORITES" -> -200L
            else -> selectedFolder.hashCode().toLong()
        }
    }
    var editingSong by remember { mutableStateOf<Song?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var optionsSong by remember { mutableStateOf<Song?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }
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
    
    val mainListState = rememberLazyListState()
    
    var showMenu by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }

    
    val albums = remember(rawAllSongs, hiddenFolders.value) {
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

    // Reset selected album when folder changes (except when specifically navigating to detail views)
    LaunchedEffect(selectedFolder) {
        if (selectedFolder.isNotEmpty()) {
            settingsManager.lastCategory = selectedFolder
        }
        // Force scroll to top when category changes so header is visible
        mainListState.scrollToItem(0)
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

    val playNext = {
        playbackManager.playNextFromService()
        onCurrentSongChange(playbackManager.currentSong)
        onIsPlayingChange(playbackManager.isPlaying)
    }

    @Composable
    fun AnimatedLogo(
        modifier: Modifier = Modifier,
        onAnimationFinished: () -> Unit = {}
    ) {
        var startAnimation by remember { mutableStateOf(false) }
        val rotation by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (startAnimation) 360f else 0f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 1000,
                easing = androidx.compose.animation.core.LinearOutSlowInEasing
            ),
            label = "LogoRotation",
            finishedListener = { if (it == 360f) onAnimationFinished() }
        )

        LaunchedEffect(Unit) {
            startAnimation = true
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
                    .graphicsLayer(rotationZ = rotation)
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
        playbackManager.playPreviousFromService()
        onCurrentSongChange(playbackManager.currentSong)
        onIsPlayingChange(playbackManager.isPlaying)
    }

    var showSearchScreen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                                    progress = undoProgress,
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
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = { 
                        val customTitle by settingsManager.customTitleFlow.collectAsState()
                        val titleText = if (customTitle.isEmpty()) "Lune" else customTitle
                        var isTitleVisible by remember { mutableStateOf(false) }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnimatedLogo(
                                modifier = Modifier.padding(end = 0.5.dp),
                                onAnimationFinished = { isTitleVisible = true }
                            )
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isTitleVisible,
                                enter = androidx.compose.animation.fadeIn(
                                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 500)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                ResponsiveText(
                                    text = titleText,
                                    modifier = Modifier.fillMaxWidth(),
                                    targetTextSize = 32.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {

                if (rawAllSongs.isNotEmpty()) {
                    // Search Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showSearchScreen = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.search),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.search),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.92f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "EditChipScale"
                            )
                            FilterChip(
                                selected = false,
                                onClick = { onShowFolderSheetChange(true) },
                                label = { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit_folders), modifier = Modifier.size(16.dp)) },
                                shape = MaterialTheme.shapes.medium,
                                interactionSource = interactionSource,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        items(folders) { folder ->
                            val isCurrentContext = playbackManager.activeCategory == folder && playbackManager.currentSong != null
                            val isSelected = selectedFolder == folder
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.92f else if (isSelected) 1.05f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "CategoryChipScale"
                            )

                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectedFolderChange(folder) },
                                interactionSource = interactionSource,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                                label = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val label = when(folder) {
                                            "RESUME" -> sTabResume
                                            "ALL" -> sTabAll
                                            "FAVORITES" -> sTabFavorites
                                            "ALBUMS" -> sTabAlbums
                                            "PLAYLISTS" -> sTabPlaylists
                                            else -> folder
                                        }
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (isCurrentContext) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            )
                                        }
                                    }
                                },
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    }
                }

                val bottomPadding = if (currentSong != null) 80.dp else 0.dp

                val tabAlbumsText = stringResource(R.string.tab_albums)
                val playlistsText = stringResource(R.string.playlists)
                val currentScreen = remember(selectedFolder, filteredSongs.isEmpty()) {
                    when {
                        selectedFolder == "RESUME" -> "RESUME"
                        selectedFolder == "ALBUMS" -> "ALBUM_GRID"
                        selectedFolder == "PLAYLISTS" -> "PLAYLIST_GRID"
                        filteredSongs.isEmpty() -> "EMPTY"
                        else -> "LIST"
                    }
                }

                androidx.compose.animation.Crossfade(
                    targetState = currentScreen,
                    animationSpec = androidx.compose.animation.core.tween(150),
                    label = "main_screen_crossfade"
                ) { screen ->
                    when (screen) {

                        "RESUME" -> {
                            com.demonlab.lune.ui.screens.ResumeScreen(
                                viewModel = musicViewModel,
                                allSongs = filteredSongs,
                                allPlaylists = musicViewModel.playlists,
                                bottomPadding = bottomPadding,
                                onSongClick = { song, listContext ->
                                    // Play from the context of Resume items but keeping them loaded in All Songs
                                    onCurrentSongChange(song)
                                    playbackManager.play(song, listContext, -100L, category = "ALL")
                                    onIsPlayingChange(true)
                                    onIsPlayerExpandedChange(true)
                                },
                                onPlaylistClick = { playlist ->
                                    selectedPlaylist = playlist
                                    // Make sure it visually navigates (it requires changing selectedFolder or letting PlaylistListScreen handle it)
                                    // In our architecture Playlist mode is overlaid via selectedPlaylist state.
                                }
                            )
                        }
                        "ALBUM_GRID" -> {
                            AlbumGrid(
                                albums = albums,
                                onAlbumClick = { selectedAlbum = it },
                                bottomPadding = bottomPadding,
                                activePlaylistId = if (playbackManager.activeCategory == sTabAlbums) playbackManager.activePlaylistId else null
                            )
                        }
                        "PLAYLIST_GRID" -> {
                            PlaylistListScreen(
                                viewModel = musicViewModel,
                                onPlaylistClick = { selectedPlaylist = it },
                                onPlayPlaylist = { playlist ->
                                    musicViewModel.getSongsForPlaylist(playlist.id) { songs ->
                                        if (songs.isNotEmpty()) {
                                            playbackManager.play(songs[0], songs, playlist.id, category = "PLAYLISTS")
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
                                            // Revert to all songs if the playing playlist was deleted
                                            if (musicViewModel.allSongs.isNotEmpty()) {
                                                playbackManager.play(currentSong ?: musicViewModel.allSongs[0], musicViewModel.allSongs, -100L, category = "ALL")
                                            }
                                        }
                                    }
                                },
                                bottomPadding = bottomPadding
                            )
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
                                val isCurrentListPlaying = playbackManager.activePlaylistId == contextId && playbackManager.activeCategory == selectedFolder
                                var localShuffleState by remember(contextId) { mutableStateOf(settingsManager.getPlaylistShuffle(contextId)) }
                                val isShuffleActive = if (isCurrentListPlaying) playbackManager.isShuffle else localShuffleState
                                val showSimplifiedHeader = selectedFolder == "ALL" || selectedFolder == "FAVORITES" || (!listOf("RESUME", "ALBUMS", "PLAYLISTS").contains(selectedFolder))

                                LazyColumn(
                                    state = mainListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = bottomPadding)
                                ) {
                                    if (showSimplifiedHeader) {
                                        item {
                                            SongsListHeader(
                                                songs = filteredSongs,
                                                isShuffleActive = isShuffleActive,
                                                isCurrentListPlaying = isCurrentListPlaying,
                                                isPlaying = isPlaying,
                                                onPlayClick = {
                                                    if (isCurrentListPlaying) {
                                                        if (isPlaying) playbackManager.pause() else playbackManager.resume()
                                                        onIsPlayingChange(!isPlaying)
                                                    } else if (filteredSongs.isNotEmpty()) {
                                                        val songToPlay = if (isShuffleActive) filteredSongs.random() else filteredSongs[0]
                                                        onCurrentSongChange(songToPlay)
                                                        playbackManager.play(songToPlay, filteredSongs, contextId, category = selectedFolder)
                                                        onIsPlayingChange(true)
                                                    }
                                                },
                                                onShuffleClick = {
                                                    if (isCurrentListPlaying) {
                                                        playbackManager.toggleShuffle()
                                                        localShuffleState = playbackManager.isShuffle
                                                    } else {
                                                        localShuffleState = !localShuffleState
                                                        settingsManager.setPlaylistShuffle(contextId, localShuffleState)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    items(filteredSongs, key = { it.id }) { song ->
                                            SongItem(
                                                song = song,
                                                currentlyPlaying = currentSong?.id == song.id && playbackManager.activePlaylistId == contextId,
                                                isPlaying = isPlaying,
                                            onClick = {
                                                if (currentSong?.id != song.id || playbackManager.activePlaylistId != contextId) {
                                                    onCurrentSongChange(song)
                                                    playbackManager.play(song, filteredSongs, contextId, category = selectedFolder)
                                                    onIsPlayingChange(true)
                                                }
                                                onIsPlayerExpandedChange(true)
                                            },
                                            onOptionsClick = {
                                                optionsSong = song
                                                showOptionsSheet = true
                                            }
                                        )
                                        
                                    }
                                }

                                // Scroll to Current Button
                                val targetIndex = remember(filteredSongs, currentSong, contextId, playbackManager.activePlaylistId) {
                                    if (currentSong != null && playbackManager.activePlaylistId == contextId) {
                                        filteredSongs.indexOfFirst { it.id == currentSong.id }
                                    } else -1
                                }
                                
                                ScrollToCurrentButton(
                                    listState = mainListState,
                                    targetIndex = targetIndex,
                                    label = stringResource(R.string.queue_now_playing),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = bottomPadding + 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showFolderSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { onShowFolderSheetChange(false) },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        stringResource(R.string.filter_folders),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    val settingsManager = SettingsManager.getInstance(context)
                    allFolders.forEach { folder ->
                        val isHidden = hiddenFolders.value.contains(folder)
                        ListItem(
                            headlineContent = { Text(folder) },
                            trailingContent = {
                                Switch(
                                    checked = !isHidden,
                                    onCheckedChange = { isVisible ->
                                        val newHidden = hiddenFolders.value.toMutableSet()
                                        if (isVisible) newHidden.remove(folder) else newHidden.add(folder)
                                        hiddenFolders.value = newHidden
                                        settingsManager.hiddenFolders = newHidden
                                        
                                        // If we hidden current selected folder, reset to "Todo"
                                        if (!isVisible && selectedFolder == folder) {
                                            onSelectedFolderChange("ALL")
                                        }
                                    }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            } // End of Column inside Scaffold
        } // End of Scaffold

        if (showFolderSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { onShowFolderSheetChange(false) },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        stringResource(R.string.filter_folders),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    val settingsManager = SettingsManager.getInstance(context)
                    allFolders.forEach { folder ->
                        val isHidden = hiddenFolders.value.contains(folder)
                        ListItem(
                            headlineContent = { Text(folder) },
                            trailingContent = {
                                Switch(
                                    checked = !isHidden,
                                    onCheckedChange = { isVisible ->
                                        val newHidden = hiddenFolders.value.toMutableSet()
                                        if (isVisible) newHidden.remove(folder) else newHidden.add(folder)
                                        hiddenFolders.value = newHidden
                                        settingsManager.hiddenFolders = newHidden
                                        
                                        // If we hidden current selected folder, reset to "Todo"
                                        if (!isVisible && selectedFolder == folder) {
                                            onSelectedFolderChange("ALL")
                                        }
                                    }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
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
                    onBack = { selectedPlaylist = null },
                    onSongClick = { song ->
                        playbackManager.play(song, playlistSongs, playListRender.id, category = "PLAYLISTS")
                        onCurrentSongChange(song)
                        onIsPlayingChange(true)
                    },
                    onOptionsClick = { song ->
                        optionsSong = song
                        showOptionsSheet = true
                    },
                    currentlyPlayingId = if (playbackManager.activePlaylistId == playListRender.id) currentSong?.id else null,
                    bottomPadding = if (currentSong != null) 80.dp else 0.dp,
                    viewModel = musicViewModel
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
                AlbumDetailView(
                    album = albumRender,
                    onBack = { selectedAlbum = null },
                    onSongClick = { song ->
                        playbackManager.play(song, albumRender.songs, albumRender.id, category = "ALBUMS")
                        onCurrentSongChange(song)
                        onIsPlayingChange(true)
                    },
                    onOptionsClick = { song ->
                        optionsSong = song
                        showOptionsSheet = true
                    },
                    currentlyPlayingId = if (playbackManager.activePlaylistId == albumRender.id) currentSong?.id else null,
                    bottomPadding = if (currentSong != null) 80.dp else 0.dp
                )
            }
        }

        // Mini Player
        AnimatedVisibility(
            visible = currentSong != null && !isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            val song = currentSong
            if (song != null) {
                MiniPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    showWaveform = playbackManager.isMiniPlayerVisualizerEnabled,
                    visualizerData = visualizerData,
                    currentOutputIcon = playbackManager.currentOutputIcon,
                    onTogglePlay = { 
                        if (isPlaying) playbackManager.pause() else playbackManager.resume()
                        onIsPlayingChange(!isPlaying)
                    },
                    onExpand = { onIsPlayerExpandedChange(true) },
                    onPrevious = playPrevious,
                    onNext = playNext
                )
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
                        if (isPlaying) playbackManager.pause() else playbackManager.resume()
                    onIsPlayingChange(!isPlaying)
                    },
                    onMinimize = { onIsPlayerExpandedChange(false) },
                    onNext = playNext,
                    onPrevious = playPrevious,
                    onRefreshSongs = onRefreshSongs,
                    showWaveform = playbackManager.isFullPlayerVisualizerEnabled,
                    onToggleWaveform = {}, // Not used anymore as we have settings sheet
                    visualizerData = visualizerData,
                    onShowLyrics = {
                        val intent = Intent(context, LyricsActivity::class.java)
                        context.startActivity(intent)
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
        val albumsTabStr = stringResource(R.string.tab_albums)
        val playlistsTabStr = stringResource(R.string.playlists)

        SearchScreen(
            viewModel = musicViewModel,
            allFolders = folders,
            onDismiss = { showSearchScreen = false },
            onSongClick = { song, queue, category, parentId ->
                playbackManager.play(song, queue, parentId, category)
                onCurrentSongChange(song)
                onIsPlayingChange(true)
                onSelectedFolderChange(category)
                showSearchScreen = false
            },
            onNavigateToAlbum = { album ->
                selectedAlbum = album
                showSearchScreen = false
                onSelectedFolderChange(albumsTabStr)
            },
            onNavigateToPlaylist = { playlist ->
                selectedPlaylist = playlist
                showSearchScreen = false
                onSelectedFolderChange(playlistsTabStr)
            },
            onNavigateToFolder = { folder ->
                onSelectedFolderChange(folder)
                showSearchScreen = false
            },
            onOptionsClick = { song ->
                optionsSong = song
                showOptionsSheet = true
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
                onSave = { updatedTitle, updatedArtist, updatedCoverUri ->
                    musicViewModel.updateMetadata(
                        song = song,
                        title = updatedTitle,
                        artist = updatedArtist,                        album = song.album,
                        genre = song.genre,
                        coverUri = updatedCoverUri,
                        onSuccess = {
                            // Update current song if it's the one being edited
                            if (currentSong?.id == song.id) {
                                val updatedSong = song.copy(
                                    title = updatedTitle,
                                    artist = updatedArtist,
                                    coverUrl = updatedCoverUri?.toString() ?: song.coverUrl
                                )
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
                        
                        // Fix: If current song is deleted, skip to next
                        if (currentSong?.id == song.id) {
                            playbackManager.playNextFromService()
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
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SongItem(
    song: Song, 
    currentlyPlaying: Boolean, 
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier, 
    onClick: (() -> Unit)? = null, 
    onOptionsClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val coverProvider = remember { CoverProvider() }
    var onlineCoverUrl by remember { mutableStateOf<String?>(null) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "PlayingIndicatorRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    LaunchedEffect(song.id) {
        if (settingsManager.downloadCovers && song.coverUrl == null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                var localCoverExists = false
                if (song.albumArtUri != null) {
                    try {
                        val pfd = context.contentResolver.openFileDescriptor(song.albumArtUri!!, "r")
                        pfd?.close()
                        localCoverExists = pfd != null
                    } catch (e: Exception) {
                        localCoverExists = false
                    }
                }
                
                if (!localCoverExists) {
                    val fetchedUrl = coverProvider.fetchCoverUrl(song.artist, song.title)
                    if (fetchedUrl != null) {
                        val localUri = coverProvider.downloadAndSaveCover(context, song.id, fetchedUrl)
                        if (localUri != null) {
                            val metadataManager = MetadataManager(context)
                            metadataManager.updateCoverUrl(song.id, localUri)
                            onlineCoverUrl = localUri
                        }
                    }
                }
            }
        }
    }

    ListItem(
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.isHiFi) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "HI-FI",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    song.title,
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false),
                    color = if (currentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        supportingContent = { 
            Text(
                "${formatDuration(song.duration)} • ${song.artist}",
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                color = if (currentlyPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        leadingContent = {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    val model = song.coverUrl ?: onlineCoverUrl ?: song.albumArtUri
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.secondaryContainer),
                        placeholder = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.secondaryContainer)
                    )
                }
                if (currentlyPlaying) {
                    Box(
                        modifier = Modifier.size(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logo_diamonds),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(60.dp)
                                .then(if (isPlaying) Modifier.rotate(rotation) else Modifier)
                        )
                        Icon(
                            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onOptionsClick ?: {}) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.player_options),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick ?: {})
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onEditMetadataClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.coverUrl ?: song.albumArtUri ?: R.drawable.ic_launcher_foreground,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${song.artist} • ${song.album}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.add_to_playlist)) },
                leadingContent = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    onAddToPlaylistClick()
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.edit_information)) },
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
                modifier = Modifier.clickable {
                    onDismiss()
                    onEditMetadataClick()
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.delete_song), color = MaterialTheme.colorScheme.error) },
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
                modifier = Modifier.clickable {
                    onDismiss()
                    onDeleteClick()
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun formatDuration(duration: Long): String {
    val minutes = (duration / 1000) / 60
    val seconds = (duration / 1000) % 60
    return "%d:%02d".format(java.util.Locale.getDefault(), minutes, seconds)
}

@Composable
fun SongsListHeader(
    songs: List<Song>,
    isShuffleActive: Boolean,
    isCurrentListPlaying: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Play and Shuffle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onPlayClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isCurrentListPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                onClick = onShuffleClick,
                shape = CircleShape,
                color = if (isShuffleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = if (isShuffleActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Right side: Info
        Column(horizontalAlignment = Alignment.End) {
            val totalDuration = songs.sumOf { it.duration }
            Text(
                text = formatLongDuration(totalDuration),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                val songsLabel = if (songs.size == 1) stringResource(R.string.song_singular) else stringResource(R.string.song_plural)
                Text(
                    text = "${songs.size} $songsLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatLongDuration(durationInMillis: Long): String {
    val totalSeconds = durationInMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(java.util.Locale.getDefault(), hours, minutes, seconds)
    } else {
        "%02d:%02d".format(java.util.Locale.getDefault(), minutes, seconds)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqBottomSheet(
    playbackManager: PlaybackManager,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.eq_title), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { playbackManager.toggleEq() }) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.PowerSettingsNew,
                        contentDescription = stringResource(R.string.cd_activate_eq),
                        tint = if (playbackManager.isEqEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val isEnabled = playbackManager.isEqEnabled
            val numBands = playbackManager.getEqNumberOfBands()
            val bandRange = playbackManager.getEqBandLevelRange()
            val friendlyNames = listOf(
                stringResource(R.string.band_sub_bass),
                stringResource(R.string.band_bass),
                stringResource(R.string.band_mid),
                stringResource(R.string.band_presence),
                stringResource(R.string.band_brilliance)
            )
            
            if (numBands > 0 && bandRange != null) {
                val minLevel = bandRange[0]
                val maxLevel = bandRange[1]
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0 until numBands) {
                        val freq = playbackManager.getEqCenterFreq(i.toShort())
                        val freqLabel = if (freq >= 1000000) "${freq / 1000000}k" else "${freq / 1000}"
                        val displayLabel = friendlyNames.getOrElse(i) { freqLabel }
                        val level = playbackManager.eqBandLevels.getOrElse(i) { 0.toShort() }
                        val value = ((level - minLevel).toFloat() / (maxLevel - minLevel).toFloat()).coerceIn(0f, 1f)
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(displayLabel, style = MaterialTheme.typography.labelSmall, color = if(isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(modifier = Modifier.height(170.dp).width(60.dp), contentAlignment = Alignment.Center) {
                                Slider(
                                    value = value,
                                    onValueChange = { newVal ->
                                        val newLevel = (minLevel + newVal * (maxLevel - minLevel)).toInt().toShort()
                                        playbackManager.setEqBandLevel(i.toShort(), newLevel)
                                    },
                                    enabled = isEnabled,
                                    modifier = Modifier
                                        .requiredWidth(170.dp)
                                        .rotate(-90f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            val dbLabel = if (level > 0) "+${level/100}" else "${level/100}"
                            Text(dbLabel, style = MaterialTheme.typography.labelSmall, color = if(isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val presets = playbackManager.getEqPresets()
                if (presets.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presets.size) { index ->
                            val isActive = presets[index] == playbackManager.activeEqPresetName
                            FilterChip(
                                selected = isActive,
                                onClick = { playbackManager.applyEqPreset(index.toShort()) },
                                label = { Text(presets[index]) },
                                enabled = isEnabled
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.height(170.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.eq_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.bass_label), style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = playbackManager.isBassBoostEnabled,
                    onCheckedChange = { playbackManager.toggleBassBoost() }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.spatial_audio_label), style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = playbackManager.isSpatialAudioEnabled,
                    onCheckedChange = { playbackManager.toggleSpatialAudio() }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { playbackManager.resetEq() },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.restore_defaults))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun WaveformVisualizer(
    modifier: Modifier = Modifier,
    magnitudes: FloatArray,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        magnitudes.forEach { magnitude ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(magnitude)
                    .background(color, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FullPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onMinimize: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onRefreshSongs: (() -> Unit)? = null,
    showWaveform: Boolean,
    onToggleWaveform: () -> Unit,
    visualizerData: FloatArray,
    onShowLyrics: () -> Unit
) {
    val context = LocalContext.current
    val coverProvider = remember { CoverProvider() }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var onlineCoverUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(song.id) {
        if (settingsManager.downloadCovers && song.coverUrl == null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                var localCoverExists = false
                if (song.albumArtUri != null) {
                    try {
                        val pfd = context.contentResolver.openFileDescriptor(song.albumArtUri, "r")
                        pfd?.close()
                        localCoverExists = pfd != null
                    } catch (e: Exception) {
                        localCoverExists = false
                    }
                }
                
                if (!localCoverExists) {
                    val fetchedUrl = coverProvider.fetchCoverUrl(song.artist, song.title)
                    if (fetchedUrl != null) {
                        val localUri = coverProvider.downloadAndSaveCover(context, song.id, fetchedUrl)
                        if (localUri != null) {
                            val metadataManager = MetadataManager(context)
                            metadataManager.updateCoverUrl(song.id, localUri)
                            onlineCoverUrl = localUri
                        }
                    }
                }
            }
        }
    }

    val activity = context as? Activity
    
    val isCinematic = settingsManager.isCinematicPlayerEnabled
    
    DisposableEffect(isCinematic) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isCinematic) {
                controller.hide(WindowInsetsCompat.Type.statusBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            if (isCinematic) {
                val window = activity?.window
                if (window != null) {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.show(WindowInsetsCompat.Type.statusBars())
                }
            }
        }
    }

    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val sheetPeekHeight = 0.dp
    val sheetFullHeight = 0.dp
    
    var showQueueSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistInPlayer by remember { mutableStateOf(false) }
    var showVolumeBar by remember { mutableStateOf(false) }
    var showVisualizerSettings by remember { mutableStateOf(false) }
    
    // Auto-hide volume bar after 5 seconds of inactivity
    LaunchedEffect(showVolumeBar) {
        if (showVolumeBar) {
            kotlinx.coroutines.delay(3000)
            showVolumeBar = false
        }
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val peekHeightPx = with(density) { sheetPeekHeight.toPx() }
    val fullHeightPx = with(density) { sheetFullHeight.toPx() }
    


    val infiniteTransition = rememberInfiniteTransition(label = "CoverAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -150f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(23000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OffsetX"
    )
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -120f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(29000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OffsetY"
    )

    var focalPoint by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    
    LaunchedEffect(song.id) {
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(song.coverUrl ?: song.albumArtUri)
            .size(100, 100)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        (result.drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
            focalPoint = ImageAnalyzer.findFocalPoint(bitmap)
        }
    }

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface) // Opaque base
    ) {
        if (isCinematic) {
            // Cinematic Background (Ken Burns + Gradient)
            AsyncImage(
                model = song.coverUrl ?: onlineCoverUrl ?: song.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.TopCenter)
                    .clipToBounds()
                    .graphicsLayer {
                        val baseScale = scale - 1f
                        val maxTransX = (size.width * baseScale) / 2f
                        val maxTransY = (size.height * baseScale) / 2f
                        val targetTransX = (0.5f - focalPoint.x) * size.width * baseScale + (offsetX * baseScale)
                        val targetTransY = (0.5f - focalPoint.y) * size.height * baseScale + (offsetY * baseScale)
                        translationX = targetTransX.coerceIn(-maxTransX, maxTransX)
                        translationY = targetTransY.coerceIn(-maxTransY, maxTransY)
                        scaleX = scale
                        scaleY = scale
                    },
                contentScale = ContentScale.Crop
            )

            val surf = MaterialTheme.colorScheme.surface
            val mAlpha = if (isDarkTheme) 0.6f else 0.3f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.10f to Color.Transparent,
                            0.25f to surf.copy(alpha = mAlpha * 0.2f),
                            0.35f to surf.copy(alpha = mAlpha * 0.5f),
                            0.42f to surf.copy(alpha = mAlpha * 0.85f),
                            0.48f to surf.copy(alpha = mAlpha + (1f - mAlpha) * 0.4f),
                            0.52f to surf.copy(alpha = mAlpha + (1f - mAlpha) * 0.75f),
                            0.56f to surf.copy(alpha = mAlpha + (1f - mAlpha) * 0.9f),
                            0.60f to surf,
                            1.00f to surf
                        )
                    )
            )
        } else if (isDarkTheme) {
            // Classic Dark Background (Blurred Image)
            AsyncImage(
                model = song.coverUrl ?: song.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .alpha(0.2f),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 50) onMinimize()
                    }
                }
                .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (isCinematic) {
                // Header Spacer
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cinematic layout filler
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                )
            } else {
                // Classic Header Spacer
                Spacer(modifier = Modifier.height(16.dp))

                // Classic Cover Art
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(28.dp)),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    AsyncImage(
                        model = song.coverUrl ?: onlineCoverUrl ?: song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.basicMarquee()
                )
            }

            Column {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        amplitude = { 1f }
                    )
                    
                    val infiniteTransition = rememberInfiniteTransition(label = "thumbRotation")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "thumbRotation"
                    )

                    Slider(
                        value = progress,
                        onValueChange = onProgressChange,
                        modifier = Modifier.fillMaxWidth(),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .graphicsLayer {
                                        rotationZ = if (isPlaying) rotation else 0f
                                    }
                                    .background(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(5.dp))
                            )
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatDuration((song.duration * progress).toLong()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "-${formatDuration((song.duration * (1 - progress)).toLong())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.SkipPrevious, 
                                contentDescription = stringResource(R.string.cd_previous), 
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                @OptIn(ExperimentalAnimationGraphicsApi::class)
                Surface(
                    onClick = onTogglePlay,
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val avd = AnimatedImageVector.animatedVectorResource(R.drawable.avd_play_pause_morph)
                        Icon(
                            painter = rememberAnimatedVectorPainter(avd, atEnd = isPlaying),
                            contentDescription = stringResource(R.string.cd_play_pause),
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.SkipNext, 
                                contentDescription = stringResource(R.string.cd_next), 
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            androidx.compose.animation.AnimatedContent(
                targetState = showVolumeBar,
                transitionSpec = {
                    androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut()
                },
                label = "VolumeBarTransition"
            ) { isVolumeVisible ->
                if (isVolumeVisible) {
                    var sliderValue by remember { mutableStateOf(playbackManager.currentVolumePercent) }
                    
                    // Sync local state when manager state changes (e.g. hardware buttons)
                    LaunchedEffect(playbackManager.currentVolumePercent) {
                        sliderValue = playbackManager.currentVolumePercent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.width(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (sliderValue == 0f) Icons.AutoMirrored.Filled.VolumeOff else if (sliderValue < 0.5f) Icons.AutoMirrored.Filled.VolumeDown else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        androidx.compose.material3.Slider(
                            value = sliderValue,
                            onValueChange = { 
                                sliderValue = it
                                playbackManager.setVolume(it)
                            },
                            thumb = {}, // Remove thumb
                            modifier = Modifier.weight(0.5f),
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                        
                        Box(
                            modifier = Modifier.width(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${(sliderValue * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Audio Output Button
                        PlayerActionButton(
                            icon = playbackManager.currentOutputIcon,
                            label = playbackManager.currentOutputName,
                            onClick = { showVolumeBar = true }
                        )

                        // Queue Button
                        PlayerActionButton(
                            icon = Icons.Default.QueueMusic,
                            label = stringResource(R.string.player_queue),
                            onClick = { showQueueSheet = true }
                        )

                        // Options Button
                        PlayerActionButton(
                            icon = Icons.Default.MoreHoriz,
                            label = stringResource(R.string.player_options),
                            onClick = { showOptionsSheet = true }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            if (showWaveform) {
                WaveformVisualizer(
                    modifier = Modifier
                        .height(45.dp)
                        .fillMaxWidth()
                        .alpha(0.6f),
                    magnitudes = visualizerData,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (showQueueSheet) {
            QueueBottomSheet(
                playbackManager = playbackManager,
                onDismiss = { showQueueSheet = false }
            )
        }

        if (showOptionsSheet) {
            PlayerOptionsBottomSheet(
                playbackManager = playbackManager,
                showWaveform = showWaveform,
                onToggleWaveform = onToggleWaveform,
                onRefreshSongs = onRefreshSongs,
                onDismiss = { showOptionsSheet = false },
                onAddToPlaylistClick = { 
                    showOptionsSheet = false
                    showAddToPlaylistInPlayer = true 
                },
                onShowVisualizerSettings = {
                    showOptionsSheet = false
                    showVisualizerSettings = true
                },
                onShowLyrics = {
                    showOptionsSheet = false
                    onShowLyrics()
                }
            )
        }

        if (showAddToPlaylistInPlayer) {
            val currentSong = playbackManager.currentSong
            if (currentSong != null) {
                val musicViewModel: com.demonlab.lune.ui.viewmodels.MusicViewModel = viewModel()
                LaunchedEffect(Unit) {
                    musicViewModel.loadPlaylists()
                }
                AddToPlaylistDialog(
                    song = currentSong,
                    viewModel = musicViewModel,
                    playbackManager = playbackManager,
                    onDismiss = { 
                        showAddToPlaylistInPlayer = false
                        playbackManager.checkPlaylistStatus()
                        onRefreshSongs?.invoke()
                    }
                )
            }
        }

        if (showVisualizerSettings) {
            VisualizerSettingsBottomSheet(
                playbackManager = playbackManager,
                onClose = { showVisualizerSettings = false }
            )
        }
    }
}

@Composable
fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 80.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    playbackManager: PlaybackManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val musicViewModel: com.demonlab.lune.ui.viewmodels.MusicViewModel = viewModel()
    var optionsSong by remember { mutableStateOf<Song?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    if (showOptionsSheet && optionsSong != null) {
        SongOptionsBottomSheet(
            song = optionsSong!!,
            onDismiss = { showOptionsSheet = false },
            onAddToPlaylistClick = { 
                showOptionsSheet = false
                showAddToPlaylist = true 
            },
            onEditMetadataClick = {
                showOptionsSheet = false
                showEditSheet = true
            },
            onDeleteClick = {
                showOptionsSheet = false
                Toast.makeText(context, "Funcionalidad de borrado disponible en la lista principal", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddToPlaylist && optionsSong != null) {
        AddToPlaylistDialog(
            song = optionsSong!!,
            viewModel = musicViewModel,
            playbackManager = playbackManager,
            onDismiss = { showAddToPlaylist = false }
        )
    }

    if (showEditSheet && optionsSong != null) {
        EditSongBottomSheet(
            song = optionsSong!!,
            onDismiss = { showEditSheet = false },
            onRestore = {
                musicViewModel.restoreOriginalMetadata(
                    song = optionsSong!!,
                    onSuccess = {
                        val updatedSong = musicViewModel.allSongs.find { it.id == optionsSong!!.id }
                        if (updatedSong != null) {
                            playbackManager.updateSongMetadata(updatedSong)
                        }
                        showEditSheet = false
                        Toast.makeText(context, context.getString(R.string.info_restored), Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onSave = { updatedTitle, updatedArtist, updatedCoverUri ->
                musicViewModel.updateMetadata(
                    song = optionsSong!!,
                    title = updatedTitle,
                    artist = updatedArtist,
                    album = optionsSong!!.album,
                    genre = optionsSong!!.genre,
                    coverUri = updatedCoverUri,
                    onSuccess = {
                        val updatedSong = optionsSong!!.copy(
                            title = updatedTitle,
                            artist = updatedArtist,
                            coverUrl = updatedCoverUri?.toString() ?: optionsSong!!.coverUrl
                        )
                        playbackManager.updateSongMetadata(updatedSong)
                        Toast.makeText(context, context.getString(R.string.info_updated), Toast.LENGTH_SHORT).show()
                        showEditSheet = false
                    }
                )
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val currentSong = playbackManager.currentSong
        val activePlaylist = playbackManager.activePlaylist
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                stringResource(R.string.player_queue),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentSong != null) {
                    item {
                        Text(
                            stringResource(R.string.queue_now_playing),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SongItem(
                            song = currentSong,
                            currentlyPlaying = true,
                            isPlaying = playbackManager.isPlaying,
                            onClick = { onDismiss() },
                            onOptionsClick = {
                                optionsSong = currentSong
                                showOptionsSheet = true
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                item {
                    Text(
                        stringResource(R.string.queue_up_next),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                val fullQueue = playbackManager.getCurrentQueue()
                val currentIdx = fullQueue.indexOfFirst { it.id == currentSong?.id }
                val nextSongs = if (currentIdx != -1 && currentIdx < fullQueue.size - 1) {
                    fullQueue.subList(currentIdx + 1, fullQueue.size)
                } else {
                    emptyList()
                }
                
                items(nextSongs) { song ->
                    SongItem(
                        song = song,
                        currentlyPlaying = false,
                        isPlaying = false,
                        onClick = { 
                            playbackManager.play(song, activePlaylist, playbackManager.activePlaylistId, playbackManager.activeCategory)
                        },
                        onOptionsClick = {
                            optionsSong = song
                            showOptionsSheet = true
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOptionsBottomSheet(
    playbackManager: PlaybackManager,
    showWaveform: Boolean,
    onToggleWaveform: () -> Unit,
    onRefreshSongs: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onShowVisualizerSettings: () -> Unit,
    onShowLyrics: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val isFavorite = playbackManager.currentSong?.isFavorite == true
        var showEqSheet by remember { mutableStateOf(false) }

        if (showEqSheet) {
            EqBottomSheet(
                playbackManager = playbackManager,
                onDismiss = { showEqSheet = false }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Favorite
                OptionButton(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = stringResource(R.string.option_favorite),
                    active = isFavorite,
                    onClick = { 
                        playbackManager.toggleFavorite {
                            onRefreshSongs?.invoke() 
                        }
                    }
                )

                // Repeat
                val repeatIcon = when (playbackManager.repeatMode) {
                    1 -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                val repeatLabel = when (playbackManager.repeatMode) {
                    1 -> stringResource(R.string.option_repeat_one)
                    2 -> stringResource(R.string.option_repeat_all)
                    else -> stringResource(R.string.option_repeat_off)
                }
                OptionButton(
                    icon = repeatIcon,
                    label = repeatLabel,
                    active = playbackManager.repeatMode > 0,
                    onClick = { playbackManager.toggleRepeatMode() }
                )

                // Shuffle
                OptionButton(
                    icon = Icons.Default.Shuffle,
                    label = stringResource(R.string.option_shuffle),
                    active = playbackManager.isShuffle,
                    onClick = { playbackManager.toggleShuffle() }
                )

                // Crossfade
                OptionButton(
                    icon = Icons.Default.Tune,
                    label = stringResource(R.string.option_crossfade),
                    active = playbackManager.isCrossfade,
                    onClick = { playbackManager.toggleCrossfade() }
                )

                // Automix
                OptionButton(
                    icon = Icons.Default.AutoAwesome,
                    label = stringResource(R.string.option_automix),
                    active = playbackManager.isAutomix,
                    onClick = { playbackManager.toggleAutomix() }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Timer
                OptionButton(
                    icon = Icons.Default.Timer,
                    label = if (playbackManager.sleepTimerMinutes > 0) "${playbackManager.sleepTimerMinutes}m" else stringResource(R.string.option_timer),
                    active = playbackManager.sleepTimerMinutes > 0,
                    onClick = { playbackManager.toggleSleepTimer() }
                )

                // EQ
                OptionButton(
                    icon = Icons.Default.GraphicEq,
                    label = stringResource(R.string.eq_title),
                    active = playbackManager.isEqEnabled,
                    onClick = { showEqSheet = true }
                )

                // Playlist
                OptionButton(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    label = stringResource(R.string.add_to_playlist),
                    active = false,
                    onClick = {
                        onDismiss()
                        onAddToPlaylistClick()
                    }
                )

                // Waveform Visualizer
                OptionButton(
                    icon = Icons.Default.Audiotrack,
                    label = stringResource(R.string.option_visualizer),
                    active = playbackManager.isFullPlayerVisualizerEnabled || playbackManager.isMiniPlayerVisualizerEnabled,
                    onClick = onShowVisualizerSettings
                )

                // Lyrics
                val hasLyrics = playbackManager.currentLyrics != null
                OptionButton(
                    icon = Icons.Default.Lyrics,
                    label = stringResource(R.string.option_lyrics),
                    active = hasLyrics,
                    enabled = hasLyrics,
                    onClick = onShowLyrics
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerSettingsBottomSheet(
    playbackManager: PlaybackManager,
    onClose: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                stringResource(R.string.option_visualizer),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.visualizer_full_player)) },
                trailingContent = {
                    Switch(
                        checked = playbackManager.isFullPlayerVisualizerEnabled,
                        onCheckedChange = { playbackManager.toggleFullPlayerVisualizer() }
                    )
                },
                modifier = Modifier.clickable { playbackManager.toggleFullPlayerVisualizer() }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.visualizer_mini_player)) },
                trailingContent = {
                    Switch(
                        checked = playbackManager.isMiniPlayerVisualizerEnabled,
                        onCheckedChange = { playbackManager.toggleMiniPlayerVisualizer() }
                    )
                },
                modifier = Modifier.clickable { playbackManager.toggleMiniPlayerVisualizer() }
            )
        }
    }
}

@Composable
fun OptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    sublabel: String? = null,
    enabled: Boolean = true
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(56.dp).alpha(if (enabled) 1f else 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (sublabel != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    sublabel,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    showWaveform: Boolean,
    visualizerData: FloatArray,
    currentOutputIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onTogglePlay: () -> Unit,
    onExpand: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onExpand() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        if (showWaveform) {
            WaveformVisualizer(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.3f)
                    .blur(16.dp),
                magnitudes = visualizerData,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                AsyncImage(
                    model = song.coverUrl ?: song.albumArtUri ?: com.demonlab.lune.R.drawable.ic_launcher_foreground,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.basicMarquee()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = currentOutputIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        song.artist,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.basicMarquee().weight(1f, fill = false)
                    )
                }
            }

            IconButton(onClick = onPrevious) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.SkipPrevious,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onTogglePlay) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onNext) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.SkipNext,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSongBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onSave: (title: String, artist: String, coverUri: android.net.Uri?) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var selectedCoverUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedCoverUri = uri }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.imePadding() // Fix keyboard compression
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()), // Fix keyboard compression
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Editar Información",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = {
                    onRestore()
                }) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = stringResource(R.string.cd_restore),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Cover with Circular Edit Button
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedCoverUri ?: song.coverUrl ?: song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                Surface(
                    onClick = { 
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_change_cover),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.edit_title)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text(stringResource(R.string.edit_artist)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onSave(title, artist, selectedCoverUri) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.save_changes))
            }
        }
    }
}

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val albumArtUri: Uri?,
    val coverUrl: String?,
    val songs: List<Song>
)

@Composable
fun AlbumGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    bottomPadding: Dp,
    activePlaylistId: Long? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding + 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(albums) { album ->
            val isPlaying = album.id == activePlaylistId
            AlbumCard(album = album, onClick = { onAlbumClick(album) }, isPlaying = isPlaying)
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit, isPlaying: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                AsyncImage(
                    model = album.coverUrl ?: album.albumArtUri ?: com.demonlab.lune.R.drawable.ic_launcher_foreground,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun PlaylistPreviewCovers(
    playlistId: Long,
    viewModel: com.demonlab.lune.ui.viewmodels.MusicViewModel,
    size: Dp = 56.dp
) {
    var covers by remember { mutableStateOf<List<String?>>(emptyList()) }
    LaunchedEffect(playlistId) {
        viewModel.getPlaylistPreviewCovers(playlistId) { 
            covers = it
        }
    }

    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (covers.isEmpty()) {
            Icon(Icons.Default.PlaylistPlay, null, modifier = Modifier.padding(16.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (covers.size == 1) {
                    AsyncImage(
                        model = covers[0],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val gridCovers = covers.take(4)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (gridCovers.size > 0) AsyncImage(gridCovers[0], null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (gridCovers.size > 1) AsyncImage(gridCovers[1], null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (gridCovers.size > 2) AsyncImage(gridCovers[2], null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (gridCovers.size > 3) AsyncImage(gridCovers[3], null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(
    viewModel: com.demonlab.lune.ui.viewmodels.MusicViewModel,
    onPlaylistClick: (com.demonlab.lune.data.Playlist) -> Unit,
    onPlayPlaylist: (com.demonlab.lune.data.Playlist) -> Unit,
    onDeletePlaylist: (com.demonlab.lune.data.Playlist) -> Unit,
    bottomPadding: Dp
) {
    val playlists = viewModel.playlists
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedOptionsPlaylist by remember { mutableStateOf<com.demonlab.lune.data.Playlist?>(null) }
    
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }

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
            ListItem(
                headlineContent = { Text(stringResource(R.string.create_playlist), fontWeight = FontWeight.SemiBold) },
                leadingContent = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.clickable { showCreateDialog = true }
            )
        }
        
        items(playlists, key = { it.id }) { playlist ->
            var songCount by remember { mutableIntStateOf(0) }
            var totalDuration by remember { mutableLongStateOf(0L) }
            
            LaunchedEffect(playlist.id, viewModel.filteredSongs) {
                viewModel.getPlaylistInfo(playlist.id) { count, duration ->
                    songCount = count
                    totalDuration = duration
                }
            }

            ListItem(
                headlineContent = { Text(playlist.name, fontWeight = FontWeight.SemiBold) },
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
            )
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

@Composable
fun AddSongsToPlaylistDialog(
    playlistId: Long,
    allSongs: List<Song>,
    initialSelectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (List<Long>, List<Long>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateOf(initialSelectedIds.toMutableSet()) }
    
    val filteredSongs = remember(searchQuery, allSongs) {
        if (searchQuery.isBlank()) allSongs
        else allSongs.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.artist.contains(searchQuery, ignoreCase = true) 
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_songs)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_songs)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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
                    }
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(filteredSongs, key = { it.id }) { song ->
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
                            // Added Cover Art
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                AsyncImage(
                                    model = song.coverUrl ?: song.albumArtUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
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
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun AddToPlaylistDialog(
    song: Song,
    viewModel: com.demonlab.lune.ui.viewmodels.MusicViewModel,
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
                    items(playlists) { playlist ->
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun PlaylistDetailView(
    playlist: com.demonlab.lune.data.Playlist,
    songs: List<Song>,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onOptionsClick: (Song) -> Unit,
    currentlyPlayingId: Long?,
    bottomPadding: Dp,
    viewModel: com.demonlab.lune.ui.viewmodels.MusicViewModel
) {
    val playbackManager = PlaybackManager.getInstance(LocalContext.current)
    val listState = rememberLazyListState()
    var showPlaylistOptions by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    val isPlaying = playbackManager.isPlaying
    
    // Calculate header visibility based on scroll
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
        songs.firstOrNull()?.let { it.coverUrl ?: it.albumArtUri }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // No top toolbar, gesture back navigation only

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
            ) {
                // Collapsing Header as an item
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
                                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(55.dp))
                        PlaylistPreviewCovers(
                            playlistId = playlist.id,
                            viewModel = viewModel,
                            size = 180.dp
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Centered Title
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
                        
                        // Information and Controls Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val settingsManager = SettingsManager.getInstance(LocalContext.current)
                            val isCurrentPlaylistPlaying = playbackManager.activePlaylistId == playlist.id && playbackManager.activeCategory == "PLAYLISTS"
                            var localShuffleState by remember(playlist.id) { mutableStateOf(settingsManager.getPlaylistShuffle(playlist.id)) }
                            val isShuffleActive = if (isCurrentPlaylistPlaying) playbackManager.isShuffle else localShuffleState

                            // Left side: Info (Total hours and songs)
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

                            // Right side: Controls (MoreVert, Shuffle, Play)
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
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
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
                                    shape = CircleShape,
                                    color = if (isShuffleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Shuffle,
                                            contentDescription = null,
                                            tint = if (isShuffleActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Surface(
                                    onClick = { 
                                        if (isCurrentPlaylistPlaying) {
                                            if (isPlaying) playbackManager.pause() else playbackManager.resume()
                                        } else if (songs.isNotEmpty()) {
                                            val songToPlay = if (isShuffleActive) songs.random() else songs[0]
                                            onSongClick(songToPlay)
                                        }
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isCurrentPlaylistPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                }
                if (songs.isEmpty()) {
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
                    items(songs, key = { it.id }) { song ->
                        SongItem(
                            song = song, 
                            currentlyPlaying = song.id == currentlyPlayingId, 
                            isPlaying = isPlaying,
                            onClick = { onSongClick(song) },
                            onOptionsClick = { onOptionsClick(song) }
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
                        viewModel.addSongsToPlaylist(playlist.id, toAdd)
                    }
                    if (toRemove.isNotEmpty()) {
                        viewModel.removeSongsFromPlaylist(playlist.id, toRemove)
                    }
                    showAddSongsDialog = false
                }
            )
        }

        // Scroll to Current Button overlay
        val targetIndex = remember(songs, currentlyPlayingId) {
            val idx = songs.indexOfFirst { it.id == currentlyPlayingId }
            if (idx != -1) idx + 1 else -1 // +1 for the header
        }
        
        ScrollToCurrentButton(
            listState = listState,
            targetIndex = targetIndex,
            label = stringResource(R.string.queue_now_playing),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 16.dp)
        )
    }
}

@Composable
fun AlbumDetailView(
    album: Album,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onOptionsClick: (Song) -> Unit,
    currentlyPlayingId: Long?,
    bottomPadding: Dp
) {
    val context = LocalContext.current
    val playbackManager = PlaybackManager.getInstance(context)
    val listState = rememberLazyListState()
    val isPlaying = playbackManager.isPlaying
    
    // Calculate header visibility based on scroll
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
        album.songs.firstOrNull()?.let { it.coverUrl ?: it.albumArtUri }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
            ) {
                // Collapsing Header
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
                                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(55.dp))
                        
                        val albumCoverBytes = remember(album) {
                            album.songs.firstOrNull()?.let { 
                                it.coverUrl ?: it.albumArtUri 
                            }
                        }
                        
                        AsyncImage(
                            model = albumCoverBytes ?: com.demonlab.lune.R.drawable.ic_launcher_foreground,
                            contentDescription = null,
                            modifier = Modifier
                                .size(180.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Centered Title
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

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Information and Controls Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val settingsManager = SettingsManager.getInstance(context)
                            val albumId = album.id
                            val isCurrentAlbumPlaying = playbackManager.activePlaylistId == album.id && playbackManager.activeCategory == "ALBUMS"
                            var localShuffleState by remember(album.id) { mutableStateOf(settingsManager.getPlaylistShuffle(album.id)) }
                            val isShuffleActive = if (isCurrentAlbumPlaying) playbackManager.isShuffle else localShuffleState

                            // Left side: Info (Total duration and songs)
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

                            // Right side: Controls (Shuffle, Play) - No MoreVert for artists
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    shape = CircleShape,
                                    color = if (isShuffleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Shuffle,
                                            contentDescription = null,
                                            tint = if (isShuffleActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                                                 Surface(
                                    onClick = { 
                                        if (isCurrentAlbumPlaying) {
                                            if (isPlaying) playbackManager.pause() else playbackManager.resume()
                                        } else if (album.songs.isNotEmpty()) {
                                            val songToPlay = if (isShuffleActive) album.songs.random() else album.songs[0]
                                            onSongClick(songToPlay)
                                        }
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isCurrentAlbumPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                }
                if (album.songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay canciones de este artista", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    items(album.songs, key = { it.id }) { song ->
                        SongItem(
                            song = song, 
                            currentlyPlaying = song.id == currentlyPlayingId, 
                            isPlaying = isPlaying,
                            onClick = { onSongClick(song) },
                            onOptionsClick = { onOptionsClick(song) }
                        )
                    }
                }
            }
        }

        // Scroll to Current Button overlay
        val targetIndex = remember(album.songs, currentlyPlayingId) {
            val idx = album.songs.indexOfFirst { it.id == currentlyPlayingId }
            if (idx != -1) idx + 1 else -1 // +1 for the header
        }
        
        ScrollToCurrentButton(
            listState = listState,
            targetIndex = targetIndex,
            label = stringResource(R.string.queue_now_playing),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 16.dp)
        )
    }
}

@Composable
fun PlaylistOptionsAndRename(
    playlist: com.demonlab.lune.data.Playlist?,
    playlists: List<com.demonlab.lune.data.Playlist>,
    viewModel: com.demonlab.lune.ui.viewmodels.MusicViewModel,
    onDismissRequest: () -> Unit,
    onDeleteConfirm: (com.demonlab.lune.data.Playlist) -> Unit
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
                androidx.compose.material3.OutlinedTextField(
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
fun SearchScreen(
    viewModel: com.demonlab.lune.ui.viewmodels.MusicViewModel,
    allFolders: List<String>,
    onDismiss: () -> Unit,
    onSongClick: (Song, List<Song>, String, Long) -> Unit, // song, queue, category/folder, parentId
    onNavigateToAlbum: (Album) -> Unit,
    onNavigateToPlaylist: (com.demonlab.lune.data.Playlist) -> Unit,
    onNavigateToFolder: (String) -> Unit,
    onOptionsClick: (Song) -> Unit,
    currentlyPlayingId: Long?,
    activeCategory: String?,
    activePlaylistId: Long?
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val isPlaying = PlaybackManager.getInstance(LocalContext.current).isPlaying
    
    // Auto-focus keyboard on launch
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val allSongs = remember(viewModel.filteredSongs, allFolders) {
        viewModel.filteredSongs.filter { song -> 
            song.folderName == null || allFolders.contains(song.folderName) 
        }
    }
    val allAlbums = remember(allSongs) {
        allSongs.groupBy { it.artist ?: "Desconocido" }
            .map { (artistName, songs) -> 
                Album(
                    id = artistName.hashCode().toLong(), 
                    name = artistName, 
                    artist = "", 
                    albumArtUri = songs.first().albumArtUri, 
                    coverUrl = songs.first().coverUrl, 
                    songs = songs.sortedWith(compareBy({ it.album }, { it.title }))
                )
            }.sortedBy { it.name }
    }
    val allPlaylists = viewModel.playlists
    val playlistMappings = viewModel.playlistMappings

    val sTabAll = stringResource(R.string.tab_all)
    val sTabFavorites = stringResource(R.string.tab_favorites)
    val sTabAlbums = stringResource(R.string.tab_albums)
    val sPlaylists = stringResource(R.string.playlists)

    // Search Logic
    val searchResults = remember(query, allSongs, allAlbums, allPlaylists, allFolders, sTabFavorites, playlistMappings) {
        if (query.isBlank()) return@remember SearchResults(emptyList(), emptyList(), emptyMap(), emptyMap(), emptyMap())
        
        val searchTerms = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        
        val matchedSongs = allSongs.filter { song ->
            val searchTarget = "${song.title} ${song.artist}".lowercase()
            searchTerms.all { term -> searchTarget.contains(term) }
        }

        val albumResults = mutableMapOf<Album, List<Song>>()
        allAlbums.forEach { album ->
            val nameMatches = searchTerms.all { term -> album.name.lowercase().contains(term) }
            val matchingSongs = album.songs.filter { song ->
                val searchTarget = "${song.title} ${song.artist}".lowercase()
                searchTerms.all { term -> searchTarget.contains(term) }
            }
            if (nameMatches || matchingSongs.isNotEmpty()) {
                albumResults[album] = matchingSongs
            }
        }

        val playlistResults = mutableMapOf<com.demonlab.lune.data.Playlist, List<Song>>()
        allPlaylists.forEach { playlist ->
            val nameMatches = searchTerms.all { term -> playlist.name.lowercase().contains(term) }
            
            val playlistSongs = playlistMappings.filter { it.playlistId == playlist.id }
                .mapNotNull { mapping -> allSongs.find { it.id == mapping.songId } }
                
            val matchingSongs = playlistSongs.filter { song ->
                val searchTarget = "${song.title} ${song.artist}".lowercase()
                searchTerms.all { term -> searchTarget.contains(term) }
            }
            
            if (nameMatches || matchingSongs.isNotEmpty()) {
                playlistResults[playlist] = matchingSongs
            }
        }

        val favoriteSongs = matchedSongs.filter { it.isFavorite }

        // Group matched songs by their folder/tag, but only for visible folders
        val tagResults = mutableMapOf<String, List<Song>>()
        
        // Include "Favoritos" in tagResults ONLY if there are favorite songs matching
        if (favoriteSongs.isNotEmpty()) {
            tagResults[sTabFavorites] = favoriteSongs
        }

        // Match folders by name
        allFolders.forEach { folder ->
            // Skip Favorites here since it is handled exclusively above
            if (folder.lowercase() == sTabFavorites.lowercase() || folder.lowercase() == "favorites" || folder.lowercase() == "favoritos") {
                return@forEach
            }
            val nameMatches = searchTerms.all { term -> folder.lowercase().contains(term) }
            if (nameMatches) {
                // Determine if this folder should be highlighted as a section
                val folderSongs = allSongs.filter { it.folderName == folder && matchedSongs.contains(it) }
                tagResults[folder] = folderSongs
            }
        }

        // Also add folders that contain matched songs but weren't matched by name
        matchedSongs.forEach { song ->
            val folder = song.folderName ?: "Desconocido"
            if (allFolders.contains(folder) && !tagResults.containsKey(folder)) {
                val folderSongs = allSongs.filter { it.folderName == folder && matchedSongs.contains(it) }
                tagResults[folder] = folderSongs
            }
        }

        SearchResults(matchedSongs, favoriteSongs, albumResults, playlistResults, tagResults)
    }

    BackHandler(onBack = onDismiss)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .focusRequester(focusRequester),
                        placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack, 
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // "All" / Songs Section
            if (searchResults.songs.isNotEmpty()) {
                item {
                    Text(
                        text = sTabAll,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onNavigateToFolder("ALL") }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                    )
                }
                items(searchResults.songs) { song ->
                    val isCurrent = song.id == currentlyPlayingId && activeCategory == "ALL"
                    SongItem(
                        song = song,
                        currentlyPlaying = isCurrent,
                        isPlaying = isPlaying && isCurrent,
                        onClick = { onSongClick(song, allSongs, "ALL", -100L) },
                        onOptionsClick = { onOptionsClick(song) }
                    )
                }
            }

            // Favorites Section (Extract from tagResults)
            searchResults.tagResults[sTabFavorites]?.let { songs ->
                if (songs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = sTabFavorites,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onNavigateToFolder("FAVORITES") }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                    items(songs) { song ->
                        val isCurrent = song.id == currentlyPlayingId && activeCategory == "FAVORITES"
                        SongItem(
                            song = song,
                            currentlyPlaying = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            onClick = { onSongClick(song, searchResults.favoriteSongs, "FAVORITES", -200L) },
                            onOptionsClick = { onOptionsClick(song) }
                        )
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        ListItem(
                            headlineContent = { Text(sTabFavorites) },
                            leadingContent = { 
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                            },
                            modifier = Modifier.clickable { onNavigateToFolder("FAVORITES") }
                        )
                    }
                }
            }

            // Playlists Section
            if (searchResults.playlistResults.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.playlists),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                val playlistList = searchResults.playlistResults.toList()
                if (playlistList.isNotEmpty()) {
                    for ((playlist, matchingSongs) in playlistList) {
                        item {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            ListItem(
                                headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = {
                                    PlaylistPreviewCovers(playlistId = playlist.id, viewModel = viewModel, size = 50.dp)
                                },
                                modifier = Modifier.clickable { onNavigateToPlaylist(playlist) }
                            )
                        }
                        items(matchingSongs) { song ->
                            val isCurrent = song.id == currentlyPlayingId && activeCategory == "PLAYLISTS" && activePlaylistId == playlist.id
                            SongItem(
                                song = song,
                                currentlyPlaying = isCurrent,
                                isPlaying = isPlaying && isCurrent,
                                modifier = Modifier.padding(start = 32.dp),
                                onClick = { 
                                    val fullPlaylistSongs = playlistMappings.filter { it.playlistId == playlist.id }
                                        .mapNotNull { mapping -> allSongs.find { s -> s.id == mapping.songId } }
                                    onSongClick(song, fullPlaylistSongs, "PLAYLISTS", playlist.id) 
                                    onNavigateToPlaylist(playlist)
                                },
                                onOptionsClick = { onOptionsClick(song) }
                            )
                        }
                    }
                }
            }

            // Albums / Artists Section
            if (searchResults.albumResults.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.tab_albums),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                val albumList = searchResults.albumResults.toList()
                if (albumList.isNotEmpty()) {
                    for ((album, matchingSongs) in albumList) {
                        item {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            ListItem(
                                headlineContent = { Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text("${album.songs.size} canciones") },
                                leadingContent = {
                                    AsyncImage(
                                        model = album.songs.firstOrNull()?.let { getAlbumArtUri(it.albumId) } ?: R.drawable.ic_launcher_foreground,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                },
                                modifier = Modifier.clickable { onNavigateToAlbum(album) }
                            )
                        }
                        items(matchingSongs) { song ->
                            val isCurrent = song.id == currentlyPlayingId && activeCategory == "ALBUMS" && activePlaylistId == album.id
                            SongItem(
                                song = song,
                                currentlyPlaying = isCurrent,
                                isPlaying = isPlaying && isCurrent,
                                modifier = Modifier.padding(start = 32.dp),
                                onClick = { 
                                    onSongClick(song, album.songs, "ALBUMS", album.id)
                                    onNavigateToAlbum(album)
                                },
                                onOptionsClick = { onOptionsClick(song) }
                            )
                        }
                    }
                }
            }

            // Dynamic Folder/Tag Sections (excluding Favorites)
            searchResults.tagResults.filterKeys { it != sTabFavorites }.forEach { (tagName, songs) ->
                if (songs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tagName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onNavigateToFolder(tagName) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                    items(songs) { song ->
                        val isCurrent = song.id == currentlyPlayingId && activeCategory == tagName
                        SongItem(
                            song = song,
                            currentlyPlaying = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            onClick = {
                                onSongClick(song, songs, tagName, tagName.hashCode().toLong())
                                onNavigateToFolder(tagName)
                            },
                            onOptionsClick = { onOptionsClick(song) }
                        )
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        ListItem(
                            headlineContent = { Text(tagName) },
                            leadingContent = { 
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                            },
                            modifier = Modifier.clickable { onNavigateToFolder(tagName) }
                        )
                    }
                }
            }
            
            if (query.isNotBlank() && searchResults.songs.isEmpty() && searchResults.albumResults.isEmpty() && searchResults.playlistResults.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

data class SearchResults(
    val songs: List<Song>,
    val favoriteSongs: List<Song>,
    val albumResults: Map<Album, List<Song>>,
    val playlistResults: Map<com.demonlab.lune.data.Playlist, List<Song>>,
    val tagResults: Map<String, List<Song>>
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaylistOptionsSheet(
    playlist: com.demonlab.lune.data.Playlist,
    viewModel: com.demonlab.lune.ui.viewmodels.MusicViewModel,
    onDismissRequest: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
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
            ListItem(
                headlineContent = { Text(stringResource(R.string.edit_name)) },
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
            )
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.delete_playlist), color = MaterialTheme.colorScheme.error) },
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
            )
        }
    }
}


fun getAlbumArtUri(albumId: Long): Uri {
    return ContentUris.withAppendedId(
        Uri.parse("content://media/external/audio/albumart"),
        albumId
    )
}

@Composable
fun ScrollToCurrentButton(
    listState: androidx.compose.foundation.lazy.LazyListState,
    targetIndex: Int,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    
    val isVisible by remember(targetIndex) {
        derivedStateOf {
            if (targetIndex == -1) false
            else {
                val layoutInfo = listState.layoutInfo
                val visibleIndices = layoutInfo.visibleItemsInfo.map { it.index }
                !visibleIndices.contains(targetIndex)
            }
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            isExpanded = true
            delay(4000)
            isExpanded = false
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Surface(
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(targetIndex)
                }
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            tonalElevation = 8.dp,
            modifier = Modifier
                .height(56.dp)
                .shadow(8.dp, CircleShape)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = if (isExpanded && !label.isNullOrEmpty()) 20.dp else 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                if (isExpanded && !label.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
