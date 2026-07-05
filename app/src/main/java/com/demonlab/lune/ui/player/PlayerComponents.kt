package com.demonlab.lune.ui.player

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.demonlab.lune.R
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.data.Album
import com.demonlab.lune.ui.components.VinylRecordAsyncCover
import com.demonlab.lune.ui.components.WaveformVisualizer
import com.demonlab.lune.ui.sheets.AddToPlaylistDialog
import com.demonlab.lune.ui.sheets.PlayerOptionsBottomSheet
import com.demonlab.lune.ui.sheets.QueueBottomSheet
import com.demonlab.lune.ui.sheets.VisualizerSettingsBottomSheet
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.utils.formatDuration
import com.demonlab.lune.ui.utils.songSwipeGestures
import com.demonlab.lune.ui.viewmodels.MusicViewModel
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumStackedCarousel(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    bottomPadding: Dp,
    activePlaylistId: Long? = null
) {
    val initialPage = remember(activePlaylistId, albums) {
        if (activePlaylistId != null) {
            val idx = albums.indexOfFirst { it.id == activePlaylistId }
            if (idx >= 0) idx else 0
        } else 0
    }
    val pagerState = rememberPagerState(pageCount = { albums.size })

    LaunchedEffect(activePlaylistId) {
        if (initialPage > 0) {
            pagerState.scrollToPage(initialPage)
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 32.dp, bottom = bottomPadding + 32.dp),
        pageSpacing = (-240).dp
    ) { page ->
        val album = albums[page]
        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val absPageOffset = abs(pageOffset)

        val scale = 1f - (absPageOffset * 0.1f).coerceIn(0f, 0.4f)
        val alpha = if (pageOffset > 3f || pageOffset < -1f) 0f else (1f - (absPageOffset * 0.3f)).coerceIn(0f, 1f)
        val translationY = if (pageOffset > 0) {
            (pageOffset * 60.dp.value)
        } else {
            -(pageOffset * 300.dp.value)
        }

        val zIndex = 100f - absPageOffset
        val isPlaying = album.id == activePlaylistId

        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    this.translationY = translationY
                    this.shadowElevation = if (pageOffset == 0f) 16f else 4f
                }
                .zIndex(zIndex)
                .fillMaxWidth(0.75f)
                .aspectRatio(0.85f)
                .clickable { onAlbumClick(album) },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxSize()
            ) {
                Box {
                    AsyncImage(
                        model = album.coverUrl ?: album.albumArtUri ?: R.drawable.ic_launcher_foreground,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = album.artist,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
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
    onSyncFavorite: ((Long, Boolean) -> Unit)? = null,
    showWaveform: Boolean,
    onToggleWaveform: () -> Unit,
    visualizerData: FloatArray,
    coverShape: Int,
    coverScale: Float,
    coverSpin: Boolean,
    coverVinylEffect: Boolean,
    controlsIconStyle: Int,
    isControlsFilled: Boolean,
    useCustomControlsColor: Boolean,
    controlsColorPalette: Int,
    onShowLyrics: () -> Unit,
    onRequestAudioPermission: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }


    val activity = context as? Activity

    var isGesturesEnabled by remember { mutableStateOf(settingsManager.isGesturesEnabled) }
    var swipeUpAction by remember { mutableIntStateOf(settingsManager.swipeUpAction) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGesturesEnabled = settingsManager.isGesturesEnabled
                swipeUpAction = settingsManager.swipeUpAction
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isCinematic = settingsManager.isCinematicPlayerEnabled

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    DisposableEffect(isCinematic, isLandscape) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isCinematic || isLandscape) {
                controller.hide(WindowInsetsCompat.Type.statusBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            if (isCinematic || isLandscape) {
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
    var showSpeedBar by remember { mutableStateOf(false) }
    var showVisualizerSettings by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val pillAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        pillAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    fun retriggerPillAnim() {
        scope.launch {
            pillAnim.snapTo(0f)
            pillAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    LaunchedEffect(showVolumeBar) {
        if (showVolumeBar) {
            delay(3000)
            showVolumeBar = false
            retriggerPillAnim()
        }
    }

    LaunchedEffect(showSpeedBar) {
        if (showSpeedBar) {
            delay(3000)
            showSpeedBar = false
            retriggerPillAnim()
        }
    }
    val density = LocalDensity.current
    val peekHeightPx = with(density) { sheetPeekHeight.toPx() }
    val fullHeightPx = with(density) { sheetFullHeight.toPx() }


    val infiniteTransition = rememberInfiniteTransition(label = "CoverAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )
    val orbitX by infiniteTransition.animateFloat(
        initialValue = -0.05f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(23000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbitX"
    )
    val orbitY by infiniteTransition.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(29000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbitY"
    )

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }

    val hasBlurBackground = settingsManager.isBlurEnabled &&
        (if (isCinematic) settingsManager.isBlurCinematicMode
        else if (isDarkTheme) settingsManager.isBlurDarkMode else settingsManager.isBlurLightMode)
    val useBlurControls = hasBlurBackground && settingsManager.isBlurControlsEnabled

    val blurContainerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.4f)
    val blurPlayContainerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.5f)

    val infiniteSpinTransition = rememberInfiniteTransition(label = "PlayerCoverSpin")
    val spinRotation by infiniteSpinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAnimation"
    )


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (!isCinematic && hasBlurBackground) {
            val blurRequest = remember(song.id) {
                ImageRequest.Builder(context)
                            .data(song.coverUrl ?: song.uri)
                    .crossfade(true)
                    .fallback(R.drawable.ic_artwork_fallback)
                    .error(R.drawable.ic_artwork_fallback)
                    .build()
            }
            AsyncImage(
                model = blurRequest,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .alpha(if (isDarkTheme) 0.2f else 0.35f),
                contentScale = ContentScale.Crop
            )
            if (!isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                )
            }
        }

        if (isCinematic) {
            val cinematicTransform: @Composable (Modifier) -> Modifier = { mod ->
                mod.clipToBounds().graphicsLayer {
                    val dim = size.width.coerceAtMost(size.height)
                    translationX = orbitX * dim
                    translationY = orbitY * dim
                    scaleX = scale
                    scaleY = scale
                }
            }

            Crossfade(targetState = song.id, animationSpec = tween(400)) { _ ->
                val request = remember(song.id) {
                    ImageRequest.Builder(context)
                        .data(song.coverUrl ?: song.uri)
                        .crossfade(true)
                        .fallback(R.drawable.ic_artwork_fallback)
                        .error(R.drawable.ic_artwork_fallback)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = cinematicTransform(Modifier.fillMaxSize()),
                    contentScale = ContentScale.Crop
                )
            }

            if (hasBlurBackground) {
                val blurGradientBrush = if (isLandscape) {
                    Brush.horizontalGradient(
                        0.00f to Color.Transparent,
                        0.35f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.3f),
                        0.55f to Color.Black.copy(alpha = 0.6f),
                        0.70f to Color.Black.copy(alpha = 0.85f),
                        1.00f to Color.Black.copy(alpha = 0.95f)
                    )
                } else {
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.35f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.3f),
                        0.55f to Color.Black.copy(alpha = 0.6f),
                        0.70f to Color.Black.copy(alpha = 0.85f),
                        1.00f to Color.Black.copy(alpha = 0.95f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = blurGradientBrush, blendMode = BlendMode.DstIn)
                        }
                ) {
                    Crossfade(targetState = song.id, animationSpec = tween(400)) { _ ->
                        val request = remember(song.id) {
                            ImageRequest.Builder(context)
                                .data(song.coverUrl ?: song.uri)
                                .crossfade(true)
                                .fallback(R.drawable.ic_artwork_fallback)
                                .error(R.drawable.ic_artwork_fallback)
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = null,
                            modifier = cinematicTransform(
                                Modifier
                                    .fillMaxSize()
                                    .blur(80.dp)
                            ),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else {
                val surf = MaterialTheme.colorScheme.surface
                val mAlpha = if (isDarkTheme) 0.6f else 0.3f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isLandscape) {
                                Brush.horizontalGradient(
                                    0.00f to Color.Transparent,
                                    0.30f to Color.Transparent,
                                    0.45f to surf.copy(alpha = mAlpha * 0.3f),
                                    0.50f to surf.copy(alpha = mAlpha * 0.8f),
                                    0.55f to surf,
                                    1.00f to surf
                                )
                            } else {
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
                            }
                        )
                )
            }
        }

        val coverSection: @Composable () -> Unit = {
            if (isCinematic) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .scale(coverScale)
                        .songSwipeGestures(
                            enabled = isGesturesEnabled,
                            onNext = onNext,
                            onPrevious = onPrevious
                        ),

                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .scale(coverScale)
                        .songSwipeGestures(
                            enabled = isGesturesEnabled,
                            onNext = onNext,
                            onPrevious = onPrevious
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverShape == 2 && coverVinylEffect) {
                        VinylRecordAsyncCover(
                            model = song.coverUrl ?: song.uri,
                            rotation = if (coverSpin && isPlaying) spinRotation else 0f,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val activeShape = when (coverShape) {
                            1 -> RoundedCornerShape(0.dp)
                            2 -> CircleShape
                            else -> RoundedCornerShape(28.dp)
                        }
                        Surface(
                            shape = activeShape,
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(if (coverShape == 2 && coverSpin && isPlaying) spinRotation else 0f),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 8.dp
                        ) {
                            AsyncImage(
                                model = song.coverUrl ?: song.uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        val controlsSection: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        textAlign = TextAlign.Start,
                        color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (useBlurControls) blurContainerColor else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(percent = 50),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).basicMarquee()
                            )
                        }

                        if (settingsManager.isBitrateOnPlayer && song.format.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (useBlurControls) blurContainerColor else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(percent = 50),
                            ) {
                                Text(
                                    text = if (song.bitrate != null) "${song.format} | ${song.bitrate / 1000}kbps" else song.format,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                val pillBg = if (useBlurControls) blurContainerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                val isShuffling = playbackManager.isShuffle
                val shuffleIconColor = if (isShuffling) {
                    if (useBlurControls) Color.White else MaterialTheme.colorScheme.primary
                } else {
                    if (useBlurControls) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { playbackManager.toggleShuffle() },
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                        color = pillBg,
                        modifier = Modifier.size(48.dp).bounceClick()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = stringResource(R.string.option_shuffle),
                                tint = shuffleIconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            playbackManager.toggleFavorite { updatedSong ->
                                onSyncFavorite?.invoke(updatedSong.id, updatedSong.isFavorite)
                            }
                        },
                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                        color = pillBg,
                        modifier = Modifier.size(48.dp).bounceClick()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(R.string.option_favorite),
                                tint = if (song.isFavorite) {
                                    if (useBlurControls) Color.White else MaterialTheme.colorScheme.primary
                                } else {
                                    if (useBlurControls) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
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
                        color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.primary,
                        trackColor = if (useBlurControls) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
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
                                    .background(color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(5.dp))
                            )
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .padding(top = 2.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (useBlurControls) blurContainerColor else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(
                            text = formatDuration((song.duration * progress).toLong()),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = if (useBlurControls) blurContainerColor else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(
                            text = formatDuration(song.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            val activePrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)
            val activeContainerColor = if (useBlurControls) {
                blurContainerColor
            } else if (useCustomControlsColor) {
                activePrimary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            }
            val activeIconTint = if (useBlurControls) {
                Color.White
            } else if (useCustomControlsColor) {
                activePrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onPrevious,
                    shape = CircleShape,
                    color = activeContainerColor,
                    modifier = Modifier.size(64.dp).bounceClick()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ReusableSkipIcon(
                            isNext = false,
                            controlsIconStyle = controlsIconStyle,
                            isControlsFilled = isControlsFilled,
                            tint = activeIconTint,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                @OptIn(ExperimentalAnimationGraphicsApi::class)
                Surface(
                    onClick = onTogglePlay,
                    shape = CircleShape,
                    color = if (useBlurControls) blurPlayContainerColor else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp).bounceClick()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val avd = AnimatedImageVector.animatedVectorResource(R.drawable.avd_play_pause_morph)
                        Icon(
                            painter = rememberAnimatedVectorPainter(avd, atEnd = isPlaying),
                            contentDescription = stringResource(R.string.cd_play_pause),
                            modifier = Modifier.size(40.dp),
                            tint = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    onClick = onNext,
                    shape = CircleShape,
                    color = activeContainerColor,
                    modifier = Modifier.size(64.dp).bounceClick()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ReusableSkipIcon(
                            isNext = true,
                            controlsIconStyle = controlsIconStyle,
                            isControlsFilled = isControlsFilled,
                            tint = activeIconTint,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = if (isLandscape) Modifier.height(12.dp) else Modifier.width(16.dp))

            AnimatedContent(
                targetState = Pair(showVolumeBar, showSpeedBar),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "BarsTransition"
            ) { (isVolumeVisible, isSpeedVisible) ->
                if (isVolumeVisible) {
                    var sliderValue by remember { mutableStateOf(playbackManager.currentVolumePercent) }

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
                                tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Slider(
                            value = sliderValue,
                            onValueChange = {
                                sliderValue = it
                                playbackManager.setVolume(it)
                            },
                            thumb = {},
                            modifier = Modifier.weight(0.5f),
                            colors = SliderDefaults.colors(
                                activeTrackColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = if (hasBlurBackground) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )

                        Box(
                            modifier = Modifier.width(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${(sliderValue * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (isSpeedVisible) {
                    var speedValue by remember { mutableStateOf(playbackManager.playbackSpeed) }

                    LaunchedEffect(playbackManager.playbackSpeed) {
                        speedValue = playbackManager.playbackSpeed
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val speedSteps = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

                        Surface(
                            shape = CircleShape,
                            color = if (hasBlurBackground) blurContainerColor else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                speedSteps.forEach { speedOption ->
                                    val isSelected = Math.abs(speedOption - speedValue) < 0.05f
                                    Surface(
                                        onClick = {
                                            speedValue = speedOption
                                            playbackManager.updatePlaybackSpeed(speedOption)
                                        },
                                        shape = CircleShape,
                                        color = if (isSelected) if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (isSelected) if (hasBlurBackground) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimary else if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (speedOption == 1.0f) "1x" else "${speedOption}x",
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedVisibility(visible = settingsManager.isOptionsBarVisible) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                                    .graphicsLayer {
                                        scaleX = pillAnim.value
                                        scaleY = pillAnim.value
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val buttonBg = if (useBlurControls) blurContainerColor else MaterialTheme.colorScheme.surfaceContainerHigh

                                    PlayerActionButton(
                                        icon = playbackManager.currentOutputIcon,
                                        label = playbackManager.currentOutputName,
                                        onClick = { showVolumeBar = true },
                                        useBlurControls = useBlurControls,
                                        containerColor = buttonBg
                                    )

                                    PlayerActionButton(
                                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                                        label = stringResource(R.string.player_queue),
                                        onClick = { showQueueSheet = true },
                                        useBlurControls = useBlurControls,
                                        containerColor = buttonBg
                                    )

                                    PlayerActionButton(
                                        icon = Icons.Default.Speed,
                                        label = stringResource(R.string.option_speed),
                                        onClick = { showSpeedBar = true },
                                        useBlurControls = useBlurControls,
                                        containerColor = buttonBg
                                    )

                                    PlayerActionButton(
                                        icon = Icons.Default.MoreHoriz,
                                        label = stringResource(R.string.player_options),
                                        onClick = { showOptionsSheet = true },
                                        useBlurControls = useBlurControls,
                                        containerColor = buttonBg
                                    )

                                    val hasLyrics = playbackManager.currentLyrics != null
                                    val lyricsTint by animateColorAsState(
                                        targetValue = if (hasLyrics) {
                                            if (useBlurControls) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        },
                                        label = "lyricsTint"
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = buttonBg,
                                        modifier = Modifier.size(36.dp).bounceClick()
                                    ) {
                                        IconButton(
                                            onClick = onShowLyrics,
                                            enabled = hasLyrics
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lyrics,
                                                contentDescription = stringResource(R.string.option_lyrics),
                                                modifier = Modifier.size(20.dp),
                                                tint = lyricsTint
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        IconButton(
                            onClick = { settingsManager.isOptionsBarVisible = !settingsManager.isOptionsBarVisible },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (settingsManager.isOptionsBarVisible) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (settingsManager.isOptionsBarVisible) stringResource(R.string.hide_options) else stringResource(R.string.show_options),
                                tint = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var totalDragY = 0f
                    var gestureConsumed = false
                    detectDragGestures(
                        onDragStart = {
                            totalDragY = 0f
                            gestureConsumed = false
                        },
                        onDrag = { _, dragAmount ->
                            if (!gestureConsumed) {
                                totalDragY += dragAmount.y
                                val absY = abs(totalDragY)
                                val absX = abs(dragAmount.x)
                                if (absY > 60 && absY > absX * 1.5f) {
                                    if (totalDragY > 0) {
                                        onMinimize()
                                    } else {
                                        when (swipeUpAction) {
                                            1 -> showQueueSheet = true
                                            2 -> {
                                                val eqIntent = android.content.Intent(context, com.demonlab.lune.ui.activities.EqualizerActivity::class.java)
                                                context.startActivity(eqIntent)
                                            }
                                            3 -> showAddToPlaylistInPlayer = true
                                            4 -> {
                                                try {
                                                    val file = File(song.path)
                                                    if (file.exists()) {
                                                        val contentUri = FileProvider.getUriForFile(
                                                            context,
                                                            "com.demonlab.lune.fileprovider",
                                                            file
                                                        )
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "audio/*"
                                                            putExtra(Intent.EXTRA_STREAM, contentUri)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.option_share)))
                                                    }
                                                } catch (e: Exception) {}
                                            }
                                        }
                                    }
                                    gestureConsumed = true
                                }
                            }
                        }
                    )
                }
        ) {
            if (showWaveform) {
                WaveformVisualizer(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .height(80.dp)
                        .fillMaxWidth()
                        .alpha(0.6f),
                    magnitudes = visualizerData,
                    color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.primary
                )
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp, bottom = 24.dp, start = 48.dp, end = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).padding(end = 32.dp), contentAlignment = Alignment.Center) {
                        coverSection()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        controlsSection()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    coverSection()
                    controlsSection()
                }
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
                onSyncFavorite = onSyncFavorite,
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
            val currentSongState = playbackManager.currentSong
            if (currentSongState != null) {
                val musicViewModel: MusicViewModel = viewModel()
                LaunchedEffect(Unit) {
                    musicViewModel.loadPlaylists()
                }
                AddToPlaylistDialog(
                    song = currentSongState,
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
                onClose = { showVisualizerSettings = false },
                onRequestPermission = onRequestAudioPermission
            )
        }
    }
}

@Composable
fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    useBlurControls: Boolean = false,
    containerColor: Color = Color.Transparent,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        modifier = modifier.size(36.dp).bounceClick()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    showWaveform: Boolean,
    visualizerData: FloatArray,
    currentOutputIcon: ImageVector,
    coverShape: Int,
    coverScale: Float,
    coverSpin: Boolean,
    coverVinylEffect: Boolean,
    controlsIconStyle: Int,
    isControlsFilled: Boolean,
    useCustomControlsColor: Boolean,
    controlsColorPalette: Int,
    shape: Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    onTogglePlay: () -> Unit,
    onExpand: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSearchClick: (() -> Unit)? = null,
    onScrollToCurrent: (() -> Unit)? = null,
    onMinimize: (() -> Unit)? = null
) {
    val infiniteSpinTransition = rememberInfiniteTransition(label = "MiniPlayerSpin")
    val spinRotation by infiniteSpinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAnimation"
    )

    val miniContext = LocalContext.current
    val blurContainerColorMini = if (isDarkTheme) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.4f)
    val blurPlayContainerColorMini = if (isDarkTheme) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.5f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onExpand() },
        shape = shape,
        color = if (hasBlurBackground) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = if (hasBlurBackground) 0.dp else 8.dp
    ) {
        Box {
            if (hasBlurBackground) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(80.dp)
                        .alpha(if (isDarkTheme) 0.2f else 0.35f)
                ) {
                    val miniBlurRequest = remember(song.id, song.coverUrl) {
                        ImageRequest.Builder(miniContext)
                            .data(song.coverUrl ?: song.uri ?: R.drawable.ic_launcher_foreground)
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = miniBlurRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                if (!isDarkTheme) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.28f))
                    )
                }
            }

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
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxSize()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .scale(coverScale),
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverShape == 2 && coverVinylEffect) {
                                VinylRecordAsyncCover(
                                    model = song.coverUrl ?: song.uri,
                                    rotation = if (coverSpin && isPlaying) spinRotation else 0f,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val activeShape = when (coverShape) {
                                    1 -> RoundedCornerShape(0.dp)
                                    2 -> CircleShape
                                    else -> RoundedCornerShape(8.dp)
                                }
                                Surface(
                                    shape = activeShape,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .rotate(if (coverShape == 2 && coverSpin && isPlaying) spinRotation else 0f),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    AsyncImage(
                                        model = song.coverUrl ?: song.uri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                modifier = Modifier.basicMarquee(),
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = currentOutputIcon,
                                    contentDescription = null,
                                    tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = song.artist,
                                    modifier = Modifier.basicMarquee().weight(1f, fill = false),
                                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (onMinimize != null) {
                        IconButton(
                            onClick = onMinimize,
                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimize",
                                tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                val activePrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)
                val pillMiniColor = if (useCustomControlsColor) {
                    activePrimary.copy(alpha = 0.25f)
                } else if (hasBlurBackground) {
                    blurContainerColorMini
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
                val pillMiniIconTint = if (useCustomControlsColor) {
                    activePrimary
                } else if (hasBlurBackground) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onSearchClick != null) {
                        Surface(
                            onClick = onSearchClick,
                            shape = CircleShape,
                            color = pillMiniColor,
                            modifier = Modifier.size(36.dp).bounceClick()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = pillMiniIconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onPrevious,
                            shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                            color = pillMiniColor,
                            modifier = Modifier.size(40.dp).bounceClick()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                ReusableSkipIcon(
                                    isNext = false,
                                    controlsIconStyle = controlsIconStyle,
                                    isControlsFilled = isControlsFilled,
                                    tint = pillMiniIconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = onTogglePlay,
                            shape = RoundedCornerShape(4.dp),
                            color = pillMiniColor,
                            modifier = Modifier.size(40.dp).bounceClick()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    tint = pillMiniIconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = onNext,
                            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp),
                            color = pillMiniColor,
                            modifier = Modifier.size(40.dp).bounceClick()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                ReusableSkipIcon(
                                    isNext = true,
                                    controlsIconStyle = controlsIconStyle,
                                    isControlsFilled = isControlsFilled,
                                    tint = pillMiniIconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (onScrollToCurrent != null) {
                        val infiniteTransition = rememberInfiniteTransition(label = "ScrollPulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "PulseAnim"
                        )
                        Surface(
                            onClick = onScrollToCurrent,
                            shape = CircleShape,
                            color = pillMiniColor,
                            modifier = Modifier.size(36.dp).bounceClick().scale(pulseScale)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Scroll to current",
                                    tint = pillMiniIconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerMinimized(
    song: Song,
    coverShape: Int,
    coverScale: Float,
    coverSpin: Boolean,
    coverVinylEffect: Boolean,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    isPlaying: Boolean = false,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteSpinTransition = rememberInfiniteTransition(label = "MiniSpin")
    val spinRotation by infiniteSpinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAnim"
    )

    Surface(
        onClick = onRestore,
        shape = CircleShape,
        color = if (hasBlurBackground) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = if (hasBlurBackground) 0.dp else 8.dp,
        modifier = modifier
            .size(52.dp)
            .scale(coverScale)
            .shadow(6.dp, CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (hasBlurBackground) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(40.dp)
                        .alpha(if (isDarkTheme) 0.2f else 0.35f)
                ) {
                    val miniCtx = LocalContext.current
                    val blurRequest = remember(song.id, miniCtx) {
                        ImageRequest.Builder(miniCtx)
                            .data(song.coverUrl ?: song.uri ?: R.drawable.ic_launcher_foreground)
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = blurRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                if (!isDarkTheme) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.28f))
                    )
                }
            }
            if (coverShape == 2 && coverVinylEffect) {
                VinylRecordAsyncCover(
                    model = song.coverUrl ?: song.uri,
                    rotation = if (coverSpin && isPlaying) spinRotation else 0f,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val activeShape = when (coverShape) {
                    1 -> RoundedCornerShape(0.dp)
                    2 -> CircleShape
                    else -> RoundedCornerShape(8.dp)
                }
                Surface(
                    shape = activeShape,
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(if (coverShape == 2 && coverSpin && isPlaying) spinRotation else 0f),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    AsyncImage(
                        model = song.coverUrl ?: song.uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun ReusableSkipIcon(
    isNext: Boolean,
    controlsIconStyle: Int,
    isControlsFilled: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val visualOffset = if (controlsIconStyle > 0) {
        if (isNext) 2.dp else (-2).dp
    } else 0.dp
    val flipModifier = if (!isNext && controlsIconStyle > 0) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier
    val combinedModifier = modifier.offset(x = visualOffset).then(flipModifier)

    when (controlsIconStyle) {
        1 -> {
            val res = if (isControlsFilled) R.drawable.play_2_filled else R.drawable.play_2
            Icon(
                painter = painterResource(res),
                contentDescription = null,
                tint = tint,
                modifier = combinedModifier
            )
        }
        2 -> {
            val res = if (isControlsFilled) R.drawable.play_3_filled else R.drawable.play_3
            Icon(
                painter = painterResource(res),
                contentDescription = null,
                tint = tint,
                modifier = combinedModifier
            )
        }
        else -> {
            val vector = if (isControlsFilled) {
                if (isNext) Icons.Default.SkipNext else Icons.Default.SkipPrevious
            } else {
                if (isNext) Icons.Outlined.SkipNext else Icons.Outlined.SkipPrevious
            }
            Icon(
                imageVector = vector,
                contentDescription = null,
                tint = tint,
                modifier = modifier
            )
        }
    }
}
