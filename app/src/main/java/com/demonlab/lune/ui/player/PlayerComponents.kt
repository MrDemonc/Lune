package com.demonlab.lune.ui.player

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
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
import androidx.compose.ui.input.pointer.PointerEventPass
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
import androidx.compose.ui.unit.sp
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
import com.demonlab.lune.tools.toArtwork
import com.demonlab.lune.ui.data.Album
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.components.VinylRecordAsyncCover
import com.demonlab.lune.ui.components.WaveformVisualizer
import com.demonlab.lune.ui.activities.EqualizerActivity
import com.demonlab.lune.ui.sheets.AddToPlaylistDialog
import com.demonlab.lune.ui.sheets.AudioDetailsBottomSheet
import com.demonlab.lune.ui.sheets.CustomRepeatDialog
import com.demonlab.lune.ui.sheets.PlayerOptionsBottomSheet
import com.demonlab.lune.ui.sheets.QueueBottomSheet
import com.demonlab.lune.ui.sheets.VisualizerSettingsBottomSheet
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import com.demonlab.lune.ui.utils.MaterialExpressiveScallopShape
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.utils.formatDuration
import com.demonlab.lune.ui.utils.songSwipeGestures
import com.demonlab.lune.ui.viewmodels.MusicViewModel
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumStackedCarousel(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    bottomPadding: Dp,
    hasBlurBackground: Boolean = false,
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
                        model = album.coverUrl ?: album.albumArtUri ?: R.drawable.ic_lune_placeholder,
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
                            color = if (hasBlurBackground) Color.Black.copy(alpha = 0.60f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            contentColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
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

@Composable
fun AudioQualityBadges(
    song: Song,
    runtimeSampleRate: Int?,
    runtimeBitDepth: Int?,
    runtimeBitrate: Int?,
    useBlurControls: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    enableScroll: Boolean = false
) {
    val format = song.format.uppercase().ifEmpty {
        song.path.substringAfterLast('.', "").uppercase().ifEmpty { "AUDIO" }
    }
    val effectiveSampleRate = runtimeSampleRate ?: song.sampleRate
    val effectiveBitDepth = runtimeBitDepth ?: song.bitDepth
    val effectiveBitrate = runtimeBitrate ?: song.bitrate

    val isHiRes = song.isHiRes ||
        ((effectiveSampleRate ?: 0) >= 48000 && (effectiveBitDepth ?: 0) >= 24) ||
        (effectiveSampleRate ?: 0) >= 88200 ||
        format in listOf("DSF", "DFF") ||
        ((effectiveBitrate ?: 0) >= 2304000)

    val isHiFi = !isHiRes && (song.isHiFi ||
        format in listOf("FLAC", "WAV", "ALAC", "APE", "AIFF") ||
        ((effectiveBitrate ?: 0) > 320000))

    val isHq = !isHiRes && !isHiFi && ((effectiveBitrate ?: 0) >= 256000)

    val tierName = when {
        isHiRes -> "HI-RES"
        isHiFi -> "HI-FI"
        isHq -> "HQ"
        else -> null
    }

    val tierBg = when {
        isHiRes -> if (useBlurControls) Color(0xFFFFB300).copy(alpha = 0.28f) else Color(0xFFFFB300).copy(alpha = 0.20f)
        isHiFi -> if (useBlurControls) Color(0xFF00E5FF).copy(alpha = 0.24f) else Color(0xFF00E5FF).copy(alpha = 0.16f)
        else -> if (useBlurControls) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }

    val tierColor = when {
        isHiRes -> Color(0xFFFFC107)
        isHiFi -> Color(0xFF00E5FF)
        else -> if (useBlurControls) Color.White else MaterialTheme.colorScheme.primary
    }

    val badgeBg = if (useBlurControls) {
        if (isDarkTheme) Color.Black.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }

    val badgeTextColor = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onSurface

    val sampleRateStr = effectiveSampleRate?.let {
        if (it % 1000 == 0) "${it / 1000} kHz" else "${String.format(java.util.Locale.US, "%.1f", it / 1000f)} kHz"
    }
    val bitDepthStr = effectiveBitDepth?.let { if (it > 0) "${it}-bit" else null }
    val bitrateStr = effectiveBitrate?.let { if (it > 0) "${it / 1000} kbps" else null }

    val specsText = when {
        bitDepthStr != null && sampleRateStr != null -> "$bitDepthStr / $sampleRateStr"
        sampleRateStr != null -> sampleRateStr
        bitrateStr != null -> bitrateStr
        else -> null
    }

    val badgeContent: @Composable () -> Unit = {
        if (tierName != null) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = tierBg
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    if (isHiRes) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = tierColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = tierName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = tierColor
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = badgeBg
        ) {
            Text(
                text = format,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeTextColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        if (specsText != null) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = badgeBg
            ) {
                Text(
                    text = specsText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (useBlurControls) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }

    if (enableScroll) {
        Box(
            modifier = modifier,
            contentAlignment = if (horizontalAlignment == Alignment.CenterHorizontally) Alignment.Center else Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .bounceClick()
                    .clickable { onClick() }
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp, horizontalAlignment),
                verticalAlignment = Alignment.CenterVertically
            ) {
                badgeContent()
            }
        }
    } else {
        Row(
            modifier = modifier
                .bounceClick()
                .clickable { onClick() },
            horizontalArrangement = Arrangement.spacedBy(5.dp, horizontalAlignment),
            verticalAlignment = Alignment.CenterVertically
        ) {
            badgeContent()
        }
    }
}

@Composable
fun UnifiedMarqueeHeader(
    song: Song,
    useBlurControls: Boolean,
    blurContainerColor: Color,
    isAmoled: Boolean,
    isDarkTheme: Boolean,
    settingsManager: SettingsManager,
    runtimeSampleRate: Int?,
    runtimeBitDepth: Int?,
    runtimeBitrate: Int?,
    onArtistClick: ((String) -> Unit)?,
    onAudioBadgesClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    key(song.id) {
        var marqueeOffset by remember(song.id) { mutableFloatStateOf(0f) }
        var isUserInteracting by remember(song.id) { mutableStateOf(false) }
        var containerWidth by remember { mutableIntStateOf(0) }
        var titleWidth by remember(song.id) { mutableIntStateOf(0) }
        var artistRowWidth by remember(song.id) { mutableIntStateOf(0) }

        val density = LocalDensity.current
        val speedPxPerSec = with(density) { 28.dp.toPx() }
        val spacingPx = with(density) { 40.dp.toPx() }

        val isTitleLong = containerWidth > 0 && titleWidth > containerWidth
        val isArtistLong = containerWidth > 0 && artistRowWidth > containerWidth
        val shouldScroll = isTitleLong || isArtistLong

        val cycle = when {
            isTitleLong && isArtistLong -> maxOf(titleWidth, artistRowWidth) + spacingPx
            isTitleLong -> titleWidth + spacingPx
            isArtistLong -> artistRowWidth + spacingPx
            else -> 0f
        }

        LaunchedEffect(song.id) {
            marqueeOffset = 0f
            isUserInteracting = false
        }

        LaunchedEffect(song.id, shouldScroll, isUserInteracting) {
            if (isUserInteracting || !shouldScroll) return@LaunchedEffect
            delay(1500L)
            var lastFrameNanos = withFrameNanos { it }
            while (isActive && !isUserInteracting && shouldScroll) {
                withFrameNanos { frameNanos ->
                    val dtSec = (frameNanos - lastFrameNanos) / 1_000_000_000f
                    lastFrameNanos = frameNanos
                    marqueeOffset += speedPxPerSec * dtSec
                }
            }
        }

        val titleContent: @Composable (Boolean) -> Unit = { isMeasuring ->
            Text(
                text = song.title,
                style = if (isCompact) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Start,
                color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = if (isMeasuring) {
                    Modifier.onSizeChanged { titleWidth = it.width }
                } else {
                    Modifier
                }
            )
        }

        val artistRowContent: @Composable (Boolean) -> Unit = { isMeasuring ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp),
                modifier = if (isMeasuring) {
                    Modifier.onSizeChanged { artistRowWidth = it.width }
                } else {
                    Modifier
                }
            ) {
                Surface(
                    color = if (useBlurControls) blurContainerColor else if (isAmoled) Color(0xFF222222) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .bounceClick(0.95f)
                        .clickable { onArtistClick?.invoke(song.artist) }
                ) {
                    Text(
                        text = song.artist,
                        style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                        color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(
                            horizontal = if (isCompact) 8.dp else 12.dp,
                            vertical = if (isCompact) 2.dp else 4.dp
                        )
                    )
                }

                if (settingsManager.isBitrateOnPlayer) {
                    AudioQualityBadges(
                        song = song,
                        runtimeSampleRate = runtimeSampleRate,
                        runtimeBitDepth = runtimeBitDepth,
                        runtimeBitrate = runtimeBitrate,
                        useBlurControls = useBlurControls,
                        isDarkTheme = isDarkTheme,
                        onClick = onAudioBadgesClick,
                        horizontalAlignment = Alignment.Start,
                        enableScroll = false
                    )
                }
            }
        }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { containerWidth = it.width }
                .pointerInput(song.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                            val downChange = downEvent.changes.firstOrNull { it.pressed } ?: continue
                            val downId = downChange.id
                            isUserInteracting = true

                            var isDragging = false
                            var totalDx = 0f
                            val touchSlop = viewConfiguration.touchSlop

                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == downId }
                                    if (change == null || !change.pressed) {
                                        break
                                    }

                                    val dx = change.position.x - change.previousPosition.x
                                    val dy = change.position.y - change.previousPosition.y

                                    if (!isDragging) {
                                        totalDx += dx
                                        if (abs(totalDx) > touchSlop) {
                                            if (abs(totalDx) > abs(dy)) {
                                                isDragging = true
                                                change.consume()
                                                marqueeOffset = (marqueeOffset - dx).coerceAtLeast(0f)
                                            } else {
                                                break
                                            }
                                        }
                                    } else {
                                        change.consume()
                                        marqueeOffset = (marqueeOffset - dx).coerceAtLeast(0f)
                                    }
                                }
                            } finally {
                                isUserInteracting = false
                            }
                        }
                    }
                },
            horizontalAlignment = Alignment.Start
        ) {
            // Line 1: Song Title
            if (!isTitleLong) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                            val layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
                            layout(layoutWidth, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                ) {
                    titleContent(true)
                }
            } else {
                val offsetInCycle = if (cycle > 0f) ((marqueeOffset % cycle) + cycle) % cycle else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    // Copia 1
                    Box(
                        modifier = Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                            val layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
                            layout(layoutWidth, placeable.height) {
                                placeable.placeRelative((-offsetInCycle).roundToInt(), 0)
                            }
                        }
                    ) {
                        titleContent(true)
                    }

                    // Copia 2
                    Box(
                        modifier = Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                            val layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
                            layout(layoutWidth, placeable.height) {
                                placeable.placeRelative((-offsetInCycle + cycle).roundToInt(), 0)
                            }
                        }
                    ) {
                        titleContent(false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 6.dp))

            // Line 2: Artist & Audio Badges Row
            if (!isArtistLong) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                            val layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
                            layout(layoutWidth, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                ) {
                    artistRowContent(true)
                }
            } else {
                val offsetInCycle = if (cycle > 0f) ((marqueeOffset % cycle) + cycle) % cycle else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    // Copia 1
                    Box(
                        modifier = Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                            val layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
                            layout(layoutWidth, placeable.height) {
                                placeable.placeRelative((-offsetInCycle).roundToInt(), 0)
                            }
                        }
                    ) {
                        artistRowContent(true)
                    }

                    // Copia 2
                    Box(
                        modifier = Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                            val layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
                            layout(layoutWidth, placeable.height) {
                                placeable.placeRelative((-offsetInCycle + cycle).roundToInt(), 0)
                            }
                        }
                    ) {
                        artistRowContent(false)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    visualizerData: FloatArray = remember { FloatArray(0) },
    coverShape: Int,
    coverScale: Float,
    coverSpin: Boolean,
    coverVinylEffect: Boolean,
    controlsIconStyle: Int,
    isControlsFilled: Boolean,
    useCustomControlsColor: Boolean,
    controlsColorPalette: Int,
    onShowLyrics: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onArtistClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }


    val activity = context as? Activity

    var isGesturesEnabled by remember { mutableStateOf(settingsManager.isGesturesEnabled) }
    var swipeUpAction by remember { mutableIntStateOf(settingsManager.swipeUpAction) }
    var useAmoledPitchBlack by remember { mutableStateOf(settingsManager.useAmoledPitchBlack) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGesturesEnabled = settingsManager.isGesturesEnabled
                swipeUpAction = settingsManager.swipeUpAction
                useAmoledPitchBlack = settingsManager.useAmoledPitchBlack
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isCinematic = settingsManager.isCinematicPlayerEnabled

    val isLandscapeDevice = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isInMultiWindow = activity?.isInMultiWindowMode == true

    DisposableEffect(isCinematic, isLandscapeDevice, isInMultiWindow) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (!isInMultiWindow && (isCinematic || isLandscapeDevice)) {
                controller.hide(WindowInsetsCompat.Type.statusBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            if (!isInMultiWindow && (isCinematic || isLandscapeDevice)) {
                val window = activity?.window
                if (window != null) {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.show(WindowInsetsCompat.Type.statusBars())
                }
            }
        }
    }

    val playbackManager = remember { PlaybackManager.getInstance(context) }
    LaunchedEffect(song.id) {
        playbackManager.preloadSurroundingArtwork()
    }
    val liveVisualizerData by playbackManager.visualizerData.collectAsState()
    val effectiveVisualizerData = if (visualizerData.isNotEmpty()) visualizerData else liveVisualizerData

    DisposableEffect(isPlaying, showWaveform) {
        val hasAudioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasAudioPermission && isPlaying) {
            playbackManager.startVisualizer()
        }
        onDispose {
            if (!playbackManager.isMiniPlayerVisualizerEnabled) {
                playbackManager.stopVisualizer()
            }
        }
    }

    val sheetPeekHeight = 0.dp
    val sheetFullHeight = 0.dp

    var showQueueSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistInPlayer by remember { mutableStateOf(false) }
    var showVisualizerSettings by remember { mutableStateOf(false) }
    var showCustomRepeatDialog by remember { mutableStateOf(false) }


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
    val isAmoled = isDarkTheme && useAmoledPitchBlack

    val hasBlurBackground = settingsManager.isBlurEnabled &&
        (if (isCinematic) settingsManager.isBlurCinematicMode
        else if (isDarkTheme) settingsManager.isBlurDarkMode else settingsManager.isBlurLightMode)
    val useBlurControls = hasBlurBackground && settingsManager.isBlurControlsEnabled

    val blurContainerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.35f)
    val blurPlayContainerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.50f)

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

    // Animación de rebote (bounce) en la carátula al pasar o regresar de canción
    val coroutineScope = rememberCoroutineScope()
    val coverBounceScale = remember { Animatable(1f) }
    var lastSongId by remember { mutableStateOf<Long?>(null) }
    var lastBounceTime by remember { mutableLongStateOf(0L) }
    val favoriteGlowAnim = remember { Animatable(0f) }
    var glowJob by remember { mutableStateOf<Job?>(null) }

    val triggerCoverBounce: () -> Unit = remember(coroutineScope) {
        {
            val now = System.currentTimeMillis()
            if (now - lastBounceTime > 150) {
                lastBounceTime = now
                coroutineScope.launch {
                    // Fase 1: Compresión elástica fluida sin saltos bruscos
                    coverBounceScale.animateTo(
                        targetValue = 0.92f,
                        animationSpec = tween(durationMillis = 75, easing = FastOutSlowInEasing)
                    )
                    // Fase 2: Rebote elástico continuo Material 3 Expressive
                    coverBounceScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(song.id) {
        if (lastSongId == null) {
            lastSongId = song.id
            return@LaunchedEffect
        }
        if (lastSongId != song.id) {
            lastSongId = song.id
            triggerCoverBounce()
        }
    }

    val handleNext = remember(onNext, triggerCoverBounce) {
        {
            triggerCoverBounce()
            onNext()
        }
    }

    val handlePrevious = remember(onPrevious, triggerCoverBounce) {
        {
            triggerCoverBounce()
            onPrevious()
        }
    }

    var showAudioDetailsSheet by remember { mutableStateOf(false) }
    var runtimeSampleRate by remember(song.id) { mutableStateOf(song.sampleRate) }
    var runtimeBitDepth by remember(song.id) { mutableStateOf(song.bitDepth) }
    var runtimeBitrate by remember(song.id) { mutableStateOf(song.bitrate) }

    val playingFromContext = remember(song.id, playbackManager.activePlaylistId, playbackManager.activePlaylistName, playbackManager.activeCategory) {
        when {
            playbackManager.activePlaylistId == -300L -> context.getString(R.string.playing_from_search)
            !playbackManager.activePlaylistName.isNullOrBlank() -> playbackManager.activePlaylistName!!
            playbackManager.activeCategory == "ALL" -> context.getString(R.string.tab_all)
            playbackManager.activeCategory == "FAVORITES" -> context.getString(R.string.tab_favorites)
            playbackManager.activeCategory == "RESUME" -> context.getString(R.string.tab_resume)
            playbackManager.activeCategory == "ALBUMS" -> song.album.ifBlank { context.getString(R.string.tab_albums_real) }
            playbackManager.activeCategory == "ARTISTS" -> song.artist.ifBlank { context.getString(R.string.tab_artists) }
            playbackManager.activeCategory == "FOLDERS" -> song.folderName.ifBlank { context.getString(R.string.tab_folders) }
            playbackManager.activeCategory == "PLAYLISTS" -> context.getString(R.string.playlists)
            playbackManager.activeCategory == "MIXES" -> context.getString(R.string.tab_mixes)
            playbackManager.activeCategory == "GENRES" -> song.genre ?: context.getString(R.string.tab_genres)
            song.album.isNotBlank() -> song.album
            else -> context.getString(R.string.tab_all)
        }
    }

    LaunchedEffect(song.id, song.path) {
        if ((runtimeSampleRate == null || runtimeBitDepth == null || runtimeBitrate == null) && song.path.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(song.path)
                    if (file.exists()) {
                        val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                        val header = audioFile.audioHeader
                        if (runtimeSampleRate == null) runtimeSampleRate = header.sampleRateAsNumber
                        if (runtimeBitDepth == null) runtimeBitDepth = header.bitsPerSample
                        if (runtimeBitrate == null) runtimeBitrate = (header.bitRateAsNumber * 1000).toInt()
                    }
                } catch (_: Throwable) {
                    try {
                        val extractor = android.media.MediaExtractor()
                        extractor.setDataSource(song.path)
                        for (i in 0 until extractor.trackCount) {
                            val format = extractor.getTrackFormat(i)
                            val mime = format.getString(android.media.MediaFormat.KEY_MIME)
                            if (mime?.startsWith("audio/") == true) {
                                if (runtimeSampleRate == null && format.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE)) {
                                    runtimeSampleRate = format.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE)
                                }
                                if (runtimeBitrate == null && format.containsKey(android.media.MediaFormat.KEY_BIT_RATE)) {
                                    runtimeBitrate = format.getInteger(android.media.MediaFormat.KEY_BIT_RATE)
                                }
                                break
                            }
                        }
                        extractor.release()
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val isMultiWindow = activity?.isInMultiWindowMode == true
        val isDeviceLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        // Ventana flotante / modo compacto: solo se activa si Android reporta multi-window con ventana reducida
        // o si las dimensiones de la ventana son estrictamente pequeñas (menores a 560dp de alto y 380dp de ancho)
        val isFloatingWindow = (isMultiWindow && (maxWidth < 560.dp || maxHeight < 620.dp)) ||
                               (maxHeight < 560.dp && maxWidth < 380.dp)
        val isLandscape = if (isFloatingWindow) (maxWidth > maxHeight) else isDeviceLandscape
        val isCompact = isFloatingWindow
        if (!isCinematic && hasBlurBackground) {
            Crossfade(
                targetState = song.toArtwork(),
                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
                label = "FullPlayerBlurCrossfade"
            ) { targetSong ->
                val blurRequest = remember(targetSong.id, targetSong.coverUrl) {
                    ImageRequest.Builder(context)
                        .data(targetSong.coverUrl ?: targetSong.uri)
                        .size(200, 200)
                        .fallback(R.drawable.ic_artwork_fallback)
                        .error(R.drawable.ic_artwork_fallback)
                        .build()
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = blurRequest,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(40.dp)
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

            Crossfade(
                targetState = song.toArtwork(),
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "CinematicArtCrossfade"
            ) { targetSong ->
                val request = remember(targetSong.id, targetSong.coverUrl) {
                    ImageRequest.Builder(context)
                        .data(targetSong.coverUrl ?: targetSong.uri)
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
                    Crossfade(
                        targetState = song.toArtwork(),
                        animationSpec = tween(400, easing = LinearOutSlowInEasing),
                        label = "CinematicBlurCrossfade"
                    ) { targetSong ->
                        val request = remember(targetSong.id, targetSong.coverUrl) {
                            ImageRequest.Builder(context)
                                .data(targetSong.coverUrl ?: targetSong.uri)
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

        val dynamicPrimaryColor = if (useCustomControlsColor && controlsColorPalette != 0) {
            when (controlsColorPalette) {
                1 -> if (isDarkTheme) Color(0xFFFFB4AA) else Color(0xFFB04B38)
                2 -> if (isDarkTheme) Color(0xFF9FD3B1) else Color(0xFF386B52)
                3 -> if (isDarkTheme) Color(0xFF99CCEA) else Color(0xFF2E6580)
                4 -> if (isDarkTheme) Color(0xFFE8B5D9) else Color(0xFF854B75)
                5 -> if (isDarkTheme) Color(0xFFFCBC43) else Color(0xFF825500)
                else -> MaterialTheme.colorScheme.primary
            }
        } else {
            MaterialTheme.colorScheme.primary
        }

        val favoriteGlowColor = remember(dynamicPrimaryColor, isDarkTheme) {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(dynamicPrimaryColor.toArgb(), hsl)
            if (isDarkTheme) {
                // En modo oscuro: evitar que sea muy claro o deslavado (Tone 80)
                // Reducimos la luminosidad a un tono medio y potenciamos la saturación para un resplandor rico y saturado
                hsl[1] = hsl[1].coerceAtLeast(0.85f)
                hsl[2] = 0.55f
            } else {
                // En modo claro: evitar que sea muy oscuro o parezca una sombra sucia (Tone 40)
                // Elevamos la luminosidad para que se perciba como luz brillante y colorida
                hsl[1] = hsl[1].coerceAtLeast(0.75f)
                hsl[2] = 0.62f
            }
            Color(ColorUtils.HSLToColor(hsl))
        }

        val playingFromHeaderColor = if (hasBlurBackground) {
            Color.White.copy(alpha = 0.70f)
        } else {
            dynamicPrimaryColor.copy(alpha = 0.75f)
        }

        val playingFromTextColor = if (hasBlurBackground) {
            Color.White
        } else {
            dynamicPrimaryColor
        }

        val topBarBtnBg = if (useBlurControls || hasBlurBackground) {
            blurContainerColor
        } else if (useCustomControlsColor) {
            dynamicPrimaryColor.copy(alpha = 0.16f)
        } else if (isAmoled) {
            Color(0xFF222222)
        } else if (isDarkTheme) {
            Color.Black.copy(alpha = 0.40f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        }

        val topBarBtnTint = if (useBlurControls || hasBlurBackground) {
            Color.White
        } else if (useCustomControlsColor) {
            dynamicPrimaryColor
        } else {
            MaterialTheme.colorScheme.primary
        }

        val eqIconTint = if (playbackManager.isEqEnabled) {
            topBarBtnTint
        } else {
            if (useBlurControls || hasBlurBackground) {
                Color.White.copy(alpha = 0.5f)
            } else if (useCustomControlsColor) {
                dynamicPrimaryColor.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            }
        }

        val coverSection: @Composable () -> Unit = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = if (isLandscape) {
                    Modifier.fillMaxHeight()
                } else if (isFloatingWindow) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth()
                },
                verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Top
            ) {
                var playingFromStep by remember(song.id, playingFromContext) { mutableIntStateOf(0) }

                LaunchedEffect(song.id, playingFromContext) {
                    while (isActive) {
                        playingFromStep = 0
                        delay(2600)
                        playingFromStep = 1
                        delay(5500)
                    }
                }

                val topBtnSize = if (isFloatingWindow) 36.dp else 46.dp
                val topIconSize = if (isFloatingWindow) 20.dp else 24.dp
                val playlistShape = RoundedCornerShape(if (isFloatingWindow) 10.dp else 14.dp)
                val eqShape = RoundedCornerShape(
                    topStart = if (isFloatingWindow) 18.dp else 23.dp,
                    bottomStart = if (isFloatingWindow) 18.dp else 23.dp,
                    topEnd = if (isFloatingWindow) 4.dp else 5.dp,
                    bottomEnd = if (isFloatingWindow) 4.dp else 5.dp
                )
                val optShape = RoundedCornerShape(
                    topStart = if (isFloatingWindow) 4.dp else 5.dp,
                    bottomStart = if (isFloatingWindow) 4.dp else 5.dp,
                    topEnd = if (isFloatingWindow) 18.dp else 23.dp,
                    bottomEnd = if (isFloatingWindow) 18.dp else 23.dp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isFloatingWindow) 6.dp else if (isLandscape) 8.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Add to Playlist button: Material 3 Expressive rounded square container
                    Surface(
                        onClick = { showAddToPlaylistInPlayer = true },
                        shape = playlistShape,
                        color = topBarBtnBg,
                        modifier = Modifier
                            .size(topBtnSize)
                            .bounceClick()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = stringResource(R.string.add_to_playlist),
                                tint = topBarBtnTint,
                                modifier = Modifier.size(topIconSize)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(if (isFloatingWindow) 6.dp else 10.dp))

                    // Animated Playing From "x" text: aligned to the right of the back button, WITHOUT container/background
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (isFloatingWindow) 4.dp else 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        AnimatedContent(
                            targetState = playingFromStep,
                            transitionSpec = {
                                if (targetState == 1) {
                                    (slideInVertically { height -> height / 2 } + fadeIn(tween(260)))
                                        .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(tween(220)))
                                } else {
                                    (slideInVertically { height -> -height / 2 } + fadeIn(tween(260)))
                                        .togetherWith(slideOutVertically { height -> height / 2 } + fadeOut(tween(220)))
                                }
                            },
                            label = "PlayingFromFlipTransition"
                        ) { step ->
                            if (step == 0) {
                                Text(
                                    text = stringResource(R.string.playing_from_header),
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = if (isFloatingWindow) 13.sp else 16.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = playingFromHeaderColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = playingFromContext,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = if (isFloatingWindow) 14.sp else 17.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = playingFromTextColor,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Equalizer Button
                        Surface(
                            onClick = {
                                val eqIntent = Intent(context, EqualizerActivity::class.java)
                                context.startActivity(eqIntent)
                            },
                            shape = eqShape,
                            color = topBarBtnBg,
                            modifier = Modifier
                                .size(topBtnSize)
                                .bounceClick()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = stringResource(R.string.eq_title),
                                    tint = eqIconTint,
                                    modifier = Modifier.size(topIconSize)
                                )
                            }
                        }

                        // 2. Extra Options BottomSheet Button
                        Surface(
                            onClick = { showOptionsSheet = true },
                            shape = optShape,
                            color = topBarBtnBg,
                            modifier = Modifier
                                .size(topBtnSize)
                                .bounceClick()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = playbackManager.currentOutputIcon,
                                    contentDescription = playbackManager.currentOutputName,
                                    tint = topBarBtnTint,
                                    modifier = Modifier.size(topIconSize)
                                )
                            }
                        }
                    }
                }

                val coverContent: @Composable (Modifier) -> Unit = { mod ->
                    if (isCinematic) {
                        Box(modifier = mod)
                    } else {
                        val activeShape = when (coverShape) {
                            1 -> RoundedCornerShape(0.dp)
                            2 -> CircleShape
                            else -> RoundedCornerShape(if (isFloatingWindow) 18.dp else 28.dp)
                        }
                        val glowProgress = favoriteGlowAnim.value

                        Box(
                            modifier = mod,
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = coverBounceScale.value
                                        scaleY = coverBounceScale.value
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Resplandor difuminado con degradé adaptado al Dynamic Color y forma de la carátula
                                if (glowProgress > 0.001f) {
                                    Canvas(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        val maxSpreadPx = (if (isFloatingWindow) 12.dp else 18.dp).toPx()
                                        val spreadPx = maxSpreadPx * glowProgress
                                        if (spreadPx <= 0.5f) return@Canvas

                                        val baseRadius = when (coverShape) {
                                            1 -> 0f
                                            2 -> size.minDimension / 2f
                                            else -> (if (isFloatingWindow) 18.dp else 28.dp).toPx()
                                        }

                                        val maxAlpha = 0.85f * glowProgress

                                        // Base sólida justo detrás del borde de la carátula para evitar fisuras subpíxel
                                        drawRoundRect(
                                            color = favoriteGlowColor.copy(alpha = maxAlpha),
                                            topLeft = Offset.Zero,
                                            size = size,
                                            cornerRadius = CornerRadius(baseRadius, baseRadius)
                                        )

                                        // Trazos concéntricos con decaimiento cuadrático para un degradé y difuminado continuo
                                        val steps = 24
                                        val stepPx = spreadPx / steps
                                        val strokeWidth = stepPx * 1.75f

                                        for (i in 0 until steps) {
                                            val t = i.toFloat() / steps
                                            val d = t * spreadPx
                                            val falloff = (1.0f - t).toDouble().pow(2.0).toFloat()
                                            val alpha = (maxAlpha * falloff).coerceIn(0f, 1f)

                                            if (alpha > 0.002f) {
                                                val cornerRadius = if (baseRadius > 0f) baseRadius + d else 0f
                                                drawRoundRect(
                                                    color = favoriteGlowColor.copy(alpha = alpha),
                                                    topLeft = Offset(-d, -d),
                                                    size = Size(size.width + 2 * d, size.height + 2 * d),
                                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                                    style = Stroke(width = strokeWidth)
                                                )
                                            }
                                        }
                                    }
                                }

                                Crossfade(
                                    targetState = song.toArtwork(),
                                    animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
                                    label = "FullPlayerCoverCrossfade"
                                ) { targetSong ->
                                    if (coverShape == 2 && coverVinylEffect) {
                                        VinylRecordAsyncCover(
                                            model = targetSong.coverUrl ?: targetSong.uri,
                                            rotation = if (coverSpin && isPlaying) spinRotation else 0f,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Surface(
                                            shape = activeShape,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .rotate(if (coverShape == 2 && coverSpin && isPlaying) spinRotation else 0f),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            tonalElevation = 8.dp
                                        ) {
                                            SongCoverImage(
                                                coverUrl = targetSong.coverUrl ?: targetSong.uri,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                shape = activeShape,
                                                iconScale = 0.68f
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isFloatingWindow) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val maxCoverDim = minOf(maxWidth, maxHeight)
                        val coverModifier = Modifier
                            .size(maxCoverDim)
                            .scale(coverScale)
                            .songSwipeGestures(
                                enabled = isGesturesEnabled,
                                onNext = handleNext,
                                onPrevious = handlePrevious
                            )
                        coverContent(coverModifier)
                    }
                } else {
                    val coverModifier = if (isLandscape) {
                        Modifier
                            .weight(1f, fill = false)
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .scale(coverScale)
                            .songSwipeGestures(
                                enabled = isGesturesEnabled,
                                onNext = handleNext,
                                onPrevious = handlePrevious
                            )
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .scale(coverScale)
                            .songSwipeGestures(
                                enabled = isGesturesEnabled,
                                onNext = handleNext,
                                onPrevious = handlePrevious
                            )
                    }
                    coverContent(coverModifier)
                }
            }
        }

        val controlsSection: @Composable () -> Unit = {
            val btnSize = if (isFloatingWindow) 38.dp else 48.dp
            val btnIconSize = if (isFloatingWindow) 20.dp else 24.dp
            val headerVertPadding = if (isFloatingWindow) 2.dp else 8.dp

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = headerVertPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                key(song.id) {
                    UnifiedMarqueeHeader(
                        song = song,
                        useBlurControls = useBlurControls,
                        blurContainerColor = blurContainerColor,
                        isAmoled = isAmoled,
                        isDarkTheme = isDarkTheme,
                        settingsManager = settingsManager,
                        runtimeSampleRate = runtimeSampleRate,
                        runtimeBitDepth = runtimeBitDepth,
                        runtimeBitrate = runtimeBitrate,
                        onArtistClick = onArtistClick,
                        onAudioBadgesClick = { showAudioDetailsSheet = true },
                        isCompact = isFloatingWindow,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (isFloatingWindow) 6.dp else 12.dp)
                    )
                }

                val pillBg = if (useBlurControls) blurContainerColor else if (isAmoled) Color(0xFF222222) else if (isDarkTheme) Color.Black.copy(alpha = 0.40f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                val hasLyrics = playbackManager.currentLyrics != null
                val lyricsIconColor = if (hasLyrics) {
                    if (useBlurControls) Color.White else MaterialTheme.colorScheme.primary
                } else {
                    if (useBlurControls) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { onShowLyrics() },
                        shape = RoundedCornerShape(
                            topStart = if (isFloatingWindow) 18.dp else 24.dp,
                            bottomStart = if (isFloatingWindow) 18.dp else 24.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        color = pillBg,
                        modifier = Modifier.size(btnSize).bounceClick()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lyrics,
                                contentDescription = stringResource(R.string.option_lyrics),
                                tint = lyricsIconColor,
                                modifier = Modifier.size(btnIconSize)
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            playbackManager.toggleFavorite { updatedSong ->
                                onSyncFavorite?.invoke(updatedSong.id, updatedSong.isFavorite)
                            }
                            glowJob?.cancel()
                            glowJob = coroutineScope.launch {
                                favoriteGlowAnim.snapTo(0f)
                                favoriteGlowAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                                )
                                favoriteGlowAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 1400, easing = LinearOutSlowInEasing)
                                )
                            }
                        },
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            bottomStart = 4.dp,
                            topEnd = if (isFloatingWindow) 18.dp else 24.dp,
                            bottomEnd = if (isFloatingWindow) 18.dp else 24.dp
                        ),
                        color = pillBg,
                        modifier = Modifier.size(btnSize).bounceClick()
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
                                modifier = Modifier.size(btnIconSize)
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
                    if (settingsManager.progressBarStyle == 1) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.primary,
                            trackColor = if (useBlurControls) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                            drawStopIndicator = {}
                        )
                    } else {
                        val targetAmplitude = if (isPlaying) 1f else 0f
                        val amplitudeLambda: (Float) -> Float = remember(targetAmplitude) { { _: Float -> targetAmplitude } }

                        LinearWavyProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.primary,
                            trackColor = if (useBlurControls) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                            amplitude = amplitudeLambda
                        )
                    }

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

                    val progressSliderState = remember { SliderState(progress.coerceIn(0f, 1f)) }
                    LaunchedEffect(progress) {
                        progressSliderState.value = progress.coerceIn(0f, 1f)
                    }

                    Slider(
                        state = progressSliderState,
                        onValueChange = onProgressChange,
                        modifier = Modifier.fillMaxWidth(),
                        thumb = { _ ->
                            Box(
                                modifier = Modifier
                                    .size(if (isFloatingWindow) 11.dp else 14.dp)
                                    .graphicsLayer {
                                        rotationZ = if (isPlaying) rotation else 0f
                                    }
                                    .background(color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(if (isFloatingWindow) 4.dp else 5.dp))
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
                        .padding(top = if (isFloatingWindow) 1.dp else 2.dp, bottom = if (isFloatingWindow) 2.dp else 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (useBlurControls) blurContainerColor else if (isAmoled) Color(0xFF222222) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(
                            text = formatDuration((song.duration * progress).toLong()),
                            style = if (isFloatingWindow) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp) else MaterialTheme.typography.labelSmall,
                            color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = if (isFloatingWindow) 6.dp else 8.dp, vertical = if (isFloatingWindow) 1.dp else 2.dp)
                        )
                    }

                    Surface(
                        color = if (useBlurControls) blurContainerColor else if (isAmoled) Color(0xFF222222) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(
                            text = formatDuration(song.duration),
                            style = if (isFloatingWindow) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp) else MaterialTheme.typography.labelSmall,
                            color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = if (isFloatingWindow) 6.dp else 8.dp, vertical = if (isFloatingWindow) 1.dp else 2.dp)
                        )
                    }
                }
            }

            val activePrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)
            val activeContainerColor = if (useBlurControls) {
                blurContainerColor
            } else if (useCustomControlsColor) {
                activePrimary.copy(alpha = 0.2f)
            } else if (isAmoled) {
                Color(0xFF222222)
            } else {
                if (isDarkTheme) Color.Black.copy(alpha = 0.40f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            }
            val activeIconTint = if (useBlurControls) {
                Color.White
            } else if (useCustomControlsColor) {
                activePrimary
            } else {
                MaterialTheme.colorScheme.primary
            }

            val playBgColor = if (useBlurControls) {
                blurPlayContainerColor
            } else if (useCustomControlsColor) {
                activePrimary
            } else {
                MaterialTheme.colorScheme.primary
            }

            val playIconTint = if (useBlurControls) {
                Color.White
            } else if (useCustomControlsColor) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onPrimary
            }

            var showPlayStateLabel by remember { mutableStateOf(false) }
            var isFirstPlayComposition by remember { mutableStateOf(true) }
            var labelTrigger by remember { mutableIntStateOf(0) }

            LaunchedEffect(isPlaying, labelTrigger) {
                if (isFirstPlayComposition) {
                    isFirstPlayComposition = false
                    return@LaunchedEffect
                }
                showPlayStateLabel = true
                delay(1300L)
                showPlayStateLabel = false
            }

            val targetPlayWidth = if (showPlayStateLabel) {
                if (isFloatingWindow) 110.dp else 156.dp
            } else {
                if (isFloatingWindow) 70.dp else 96.dp
            }

            val playButtonWidth by animateDpAsState(
                targetValue = targetPlayWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "PlayWidthAnim"
            )

            val playRowHeight = if (isFloatingWindow) 52.dp else 68.dp
            val skipButtonSize = if (isFloatingWindow) 52.dp else 68.dp
            val skipIconSize = if (isFloatingWindow) 24.dp else 34.dp
            val playIconSize = if (isFloatingWindow) 28.dp else 38.dp
            val skipCornerRadius = if (isFloatingWindow) 18.dp else 26.dp
            val playCornerRadius = if (isFloatingWindow) 22.dp else 30.dp
            val skipGap = if (isFloatingWindow) 8.dp else 14.dp

            val playShape = RoundedCornerShape(playCornerRadius)
            val skipShape = RoundedCornerShape(skipCornerRadius)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(playRowHeight),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = handlePrevious,
                    shape = skipShape,
                    color = activeContainerColor,
                    modifier = Modifier
                        .size(skipButtonSize)
                        .offset {
                            val halfPlay = playButtonWidth.toPx() / 2f
                            val gap = skipGap.toPx()
                            val halfSkip = (skipButtonSize.toPx()) / 2f
                            IntOffset(x = -(halfPlay + gap + halfSkip).roundToInt(), y = 0)
                        }
                        .bounceClick()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ReusableSkipIcon(
                            isNext = false,
                            controlsIconStyle = controlsIconStyle,
                            isControlsFilled = isControlsFilled,
                            tint = activeIconTint,
                            modifier = Modifier.size(skipIconSize)
                        )
                    }
                }

                @OptIn(ExperimentalAnimationGraphicsApi::class)
                Surface(
                    onClick = {
                        labelTrigger++
                        onTogglePlay()
                    },
                    shape = playShape,
                    color = playBgColor,
                    modifier = Modifier
                        .height(playRowHeight)
                        .width(playButtonWidth)
                        .bounceClick()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = if (isFloatingWindow) 4.dp else 8.dp)
                        ) {
                            val avd = AnimatedImageVector.animatedVectorResource(R.drawable.avd_play_pause_morph)
                            Icon(
                                painter = rememberAnimatedVectorPainter(avd, atEnd = isPlaying),
                                contentDescription = stringResource(R.string.cd_play_pause),
                                modifier = Modifier.size(playIconSize),
                                tint = playIconTint
                            )

                            AnimatedVisibility(
                                visible = showPlayStateLabel,
                                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                        expandHorizontally(
                                            expandFrom = Alignment.CenterHorizontally,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) +
                                        scaleIn(
                                            initialScale = 0.8f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ),
                                exit = fadeOut(animationSpec = tween(150)) +
                                       shrinkHorizontally(
                                           shrinkTowards = Alignment.CenterHorizontally,
                                           animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                       )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = if (isFloatingWindow) 4.dp else 6.dp, end = if (isFloatingWindow) 2.dp else 4.dp)
                                ) {
                                    Text(
                                        text = stringResource(if (isPlaying) R.string.play_state_play else R.string.play_state_pause),
                                        style = if (isFloatingWindow) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = playIconTint,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    onClick = handleNext,
                    shape = skipShape,
                    color = activeContainerColor,
                    modifier = Modifier
                        .size(skipButtonSize)
                        .offset {
                            val halfPlay = playButtonWidth.toPx() / 2f
                            val gap = skipGap.toPx()
                            val halfSkip = (skipButtonSize.toPx()) / 2f
                            IntOffset(x = (halfPlay + gap + halfSkip).roundToInt(), y = 0)
                        }
                        .bounceClick()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ReusableSkipIcon(
                            isNext = true,
                            controlsIconStyle = controlsIconStyle,
                            isControlsFilled = isControlsFilled,
                            tint = activeIconTint,
                            modifier = Modifier.size(skipIconSize)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isFloatingWindow) 6.dp else if (isLandscape) 8.dp else 12.dp))

            // Options Row (Shuffle, Repeat, Queue) - Material 3 Expressive Morphing Pills
            val activeAccent = if (useBlurControls || hasBlurBackground) {
                Color.White
            } else if (useCustomControlsColor) {
                activePrimary
            } else {
                MaterialTheme.colorScheme.primary
            }

            val isShuffling = playbackManager.isShuffle
            val isRepeatActive = playbackManager.repeatMode > 0
            val repeatIcon = when (playbackManager.repeatMode) {
                1 -> Icons.Default.RepeatOne
                else -> Icons.Default.Repeat
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isFloatingWindow) 2.dp else 8.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isFloatingWindow) 4.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Shuffle
                PlayerOptionPill(
                    icon = Icons.Default.Shuffle,
                    label = stringResource(R.string.option_shuffle),
                    isActive = isShuffling,
                    onClick = { playbackManager.toggleShuffle() },
                    useBlurControls = useBlurControls,
                    hasBlurBackground = hasBlurBackground,
                    blurContainerColor = blurContainerColor,
                    isAmoled = isAmoled,
                    isDarkTheme = isDarkTheme,
                    activeAccent = activeAccent,
                    isLandscape = isLandscape,
                    isCompact = isFloatingWindow,
                    modifier = Modifier.weight(1f)
                )

                // 2. Repeat
                PlayerOptionPill(
                    icon = repeatIcon,
                    label = stringResource(R.string.option_repeat),
                    isActive = isRepeatActive,
                    onClick = { playbackManager.toggleRepeatMode() },
                    onLongClick = { showCustomRepeatDialog = true },
                    useBlurControls = useBlurControls,
                    hasBlurBackground = hasBlurBackground,
                    blurContainerColor = blurContainerColor,
                    isAmoled = isAmoled,
                    isDarkTheme = isDarkTheme,
                    activeAccent = activeAccent,
                    isLandscape = isLandscape,
                    isCompact = isFloatingWindow,
                    modifier = Modifier.weight(1f)
                )

                // 3. Queue
                PlayerOptionPill(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    label = stringResource(R.string.player_queue),
                    isActive = showQueueSheet,
                    onClick = { showQueueSheet = true },
                    useBlurControls = useBlurControls,
                    hasBlurBackground = hasBlurBackground,
                    blurContainerColor = blurContainerColor,
                    isAmoled = isAmoled,
                    isDarkTheme = isDarkTheme,
                    activeAccent = activeAccent,
                    isLandscape = isLandscape,
                    isCompact = isFloatingWindow,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(if (isFloatingWindow) 4.dp else if (isLandscape) 10.dp else 16.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(swipeUpAction, onMinimize) {
                    val threshold = if (isFloatingWindow) 60.dp.toPx() else 110.dp.toPx()
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var gestureConsumed = false
                    detectDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                            totalDragY = 0f
                            gestureConsumed = false
                        },
                        onDragEnd = {
                            totalDragX = 0f
                            totalDragY = 0f
                            gestureConsumed = false
                        },
                        onDragCancel = {
                            totalDragX = 0f
                            totalDragY = 0f
                            gestureConsumed = false
                        },
                        onDrag = { _, dragAmount ->
                            if (!gestureConsumed) {
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y
                                val absY = abs(totalDragY)
                                val absX = abs(totalDragX)
                                if (absY > threshold && absY > absX * 1.8f) {
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
                                                } catch (_: Throwable) {}
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
                        .height(if (isFloatingWindow) 48.dp else 80.dp)
                        .fillMaxWidth()
                        .alpha(0.6f),
                    magnitudes = effectiveVisualizerData,
                    color = if (useBlurControls) Color.White else MaterialTheme.colorScheme.primary
                )
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (isFloatingWindow) 8.dp else 16.dp,
                            bottom = if (isFloatingWindow) 8.dp else 16.dp,
                            start = if (isFloatingWindow) 14.dp else 48.dp,
                            end = if (isFloatingWindow) 14.dp else 48.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(end = if (isFloatingWindow) 12.dp else 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        coverSection()
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        controlsSection()
                    }
                }
            } else if (isFloatingWindow) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp, bottom = 8.dp, start = 12.dp, end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        coverSection()
                    }
                    controlsSection()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 42.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
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

        if (showAudioDetailsSheet) {
            AudioDetailsBottomSheet(
                song = song,
                sampleRate = runtimeSampleRate,
                bitDepth = runtimeBitDepth,
                bitrate = runtimeBitrate,
                onDismiss = { showAudioDetailsSheet = false }
            )
        }

        if (showCustomRepeatDialog) {
            CustomRepeatDialog(
                playbackManager = playbackManager,
                currentSong = song,
                onDismiss = { showCustomRepeatDialog = false }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScallopPlayPauseButtonWithProgress(
    isPlaying: Boolean,
    progress: Float,
    onClick: () -> Unit,
    hasBlurBackground: Boolean,
    useCustomControlsColor: Boolean,
    activePrimary: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "WavyProgress"
    )

    val trackColor = if (useCustomControlsColor) {
        activePrimary.copy(alpha = 0.25f)
    } else if (hasBlurBackground) {
        Color.White.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    }

    val progressColor = if (useCustomControlsColor) {
        activePrimary
    } else if (hasBlurBackground) {
        Color.White
    } else {
        MaterialTheme.colorScheme.primary
    }

    val iconTint = if (useCustomControlsColor) {
        activePrimary
    } else if (hasBlurBackground) {
        Color.White
    } else {
        MaterialTheme.colorScheme.primary
    }

    val scaleRatio = size.value / 48f
    val playIconSize = (24 * scaleRatio).dp
    val pauseBarHeight = (16 * scaleRatio).dp
    val pauseBarWidth = (4 * scaleRatio).dp

    Box(
        modifier = modifier
            .size(size)
            .bounceClick()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = progressColor,
            trackColor = trackColor,
            stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
            trackStroke = WavyProgressIndicatorDefaults.circularTrackStroke,
            gapSize = 0.dp,
            amplitude = { if (isPlaying) 1f else 0f },
            waveSpeed = 20.dp
        )

        if (isPlaying) {
            Row(
                horizontalArrangement = Arrangement.spacedBy((4 * scaleRatio).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(pauseBarWidth)
                        .height(pauseBarHeight)
                        .clip(CircleShape)
                        .background(iconTint)
                )
                Box(
                    modifier = Modifier
                        .width(pauseBarWidth)
                        .height(pauseBarHeight)
                        .clip(CircleShape)
                        .background(iconTint)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.play_state_play),
                tint = iconTint,
                modifier = Modifier
                    .size(playIconSize)
                    .offset(x = (1.5 * scaleRatio).dp)
            )
        }
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float = 0f,
    showWaveform: Boolean,
    visualizerData: FloatArray = remember { FloatArray(0) },
    currentOutputIcon: ImageVector,
    coverShape: Int,
    coverScale: Float,
    coverSpin: Boolean,
    coverVinylEffect: Boolean,
    controlsIconStyle: Int,
    isControlsFilled: Boolean,
    useCustomControlsColor: Boolean,
    controlsColorPalette: Int,
    shape: Shape = CircleShape,
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
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Cápsula / Píldora principal interactiva (A la izquierda)
        val pillShape = CircleShape
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(pillShape)
                .songSwipeGestures(
                    enabled = true,
                    onNext = onNext,
                    onPrevious = onPrevious
                )
                .clickable { onExpand() },
            shape = pillShape,
            color = if (hasBlurBackground) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = if (hasBlurBackground) 0.dp else 6.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasBlurBackground) {
                    Crossfade(
                        targetState = song.toArtwork(),
                        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
                        label = "MiniPlayerBlurCrossfade"
                    ) { targetSong ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(30.dp)
                                    .alpha(if (isDarkTheme) 0.2f else 0.35f)
                            ) {
                                val miniBlurRequest = remember(targetSong.id, targetSong.coverUrl) {
                                    ImageRequest.Builder(miniContext)
                                        .data(targetSong.coverUrl ?: targetSong.uri)
                                        .size(150, 150)
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
                    }
                }

                if (showWaveform) {
                    MiniPlayerWaveform(
                        context = miniContext,
                        passedMagnitudes = visualizerData,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Título de la canción y debajo icono de dispositivo + artista
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = song.title,
                            modifier = Modifier.basicMarquee(),
                            color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = currentOutputIcon,
                                contentDescription = null,
                                tint = if (hasBlurBackground) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = song.artist,
                                modifier = Modifier.basicMarquee(),
                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Controles a la derecha de la cápsula
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón Sonando Ahora (solo visible cuando se está dentro de la lista que está sonando)
                        AnimatedVisibility(
                            visible = onScrollToCurrent != null,
                            enter = fadeIn(tween(200)) + expandHorizontally(tween(200)) + scaleIn(tween(200), initialScale = 0.8f),
                            exit = fadeOut(tween(150)) + shrinkHorizontally(tween(150)) + scaleOut(tween(150), targetScale = 0.8f)
                        ) {
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
                                onClick = { onScrollToCurrent?.invoke() },
                                shape = CircleShape,
                                color = pillMiniColor,
                                modifier = Modifier
                                    .size(42.dp)
                                    .bounceClick()
                                    .scale(pulseScale)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Scroll to current",
                                        tint = pillMiniIconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Botón Play/Pause con barra de progreso ondulada concéntrica Material 3 Expressive
                        ScallopPlayPauseButtonWithProgress(
                            isPlaying = isPlaying,
                            progress = progress,
                            onClick = onTogglePlay,
                            hasBlurBackground = hasBlurBackground,
                            useCustomControlsColor = useCustomControlsColor,
                            activePrimary = activePrimary,
                            size = 52.dp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 2. Cover a la derecha de la píldora (siempre circular - CircleShape, al tocarlo minimiza el miniplayer)
        Box(
            modifier = Modifier
                .size(70.dp)
                .bounceClick(),
            contentAlignment = Alignment.Center
        ) {
            val isVinylActive = coverShape == 2 && coverVinylEffect
            val isSpinActive = coverShape == 2 && coverSpin && isPlaying

            Surface(
                onClick = { onMinimize?.invoke() },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(coverScale),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 4.dp,
                shadowElevation = 2.dp
            ) {
                Crossfade(
                    targetState = song.toArtwork(),
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "MiniCoverRightCrossfade"
                ) { targetSong ->
                    if (isVinylActive) {
                        VinylRecordAsyncCover(
                            model = targetSong.coverUrl ?: targetSong.uri,
                            rotation = if (isSpinActive) spinRotation else 0f,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(if (isSpinActive) spinRotation else 0f)
                        ) {
                            SongCoverImage(
                                coverUrl = targetSong.coverUrl ?: targetSong.uri,
                                contentDescription = "Minimize player",
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                iconScale = 0.68f
                            )
                        }
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
    onExpandPlayer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember(context) { context.getSystemService(Vibrator::class.java) }
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    var hasVibrated by remember { mutableStateOf(false) }

    // Visual double-bounce hint animation when miniplayer is minimized to indicate swipe up
    LaunchedEffect(Unit) {
        delay(200L)
        offsetY.animateTo(
            targetValue = -24f,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
        )
        offsetY.animateTo(
            targetValue = -6f,
            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
        )
        offsetY.animateTo(
            targetValue = -20f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        )
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 320f)
        )
    }

    val dragModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = {
                hasVibrated = false
            },
            onDragEnd = {
                coroutineScope.launch {
                    val reachedLimit = hasVibrated || offsetY.value <= -50f
                    if (reachedLimit) {
                        if (onExpandPlayer != null) {
                            onExpandPlayer()
                        } else {
                            onRestore()
                        }
                    }
                    offsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 350f)
                    )
                    hasVibrated = false
                }
            },
            onDragCancel = {
                coroutineScope.launch {
                    offsetY.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 350f))
                    hasVibrated = false
                }
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                val rawOffset = offsetY.value + dragAmount
                val clampedOffset = if (rawOffset < -55f) {
                    -55f + (rawOffset + 55f) * 0.35f
                } else {
                    rawOffset
                }.coerceIn(-85f, 0f)

                if (clampedOffset <= -50f && !hasVibrated) {
                    hasVibrated = true
                    vibrator?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            it.vibrate(VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            it.vibrate(28)
                        }
                    }
                }

                coroutineScope.launch {
                    offsetY.snapTo(clampedOffset)
                }
            }
        )
    }

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

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onRestore,
            shape = CircleShape,
            color = if (hasBlurBackground) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = if (hasBlurBackground) 0.dp else 8.dp,
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .then(dragModifier)
                .size(70.dp)
                .scale(coverScale)
                .shadow(6.dp, CircleShape)
        ) {
        Box(contentAlignment = Alignment.Center) {
            if (hasBlurBackground) {
                Crossfade(
                    targetState = song.toArtwork(),
                    animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
                    label = "MiniCoverBlurCrossfade"
                ) { targetSong ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(25.dp)
                                .alpha(if (isDarkTheme) 0.2f else 0.35f)
                        ) {
                            val miniCtx = LocalContext.current
                            val blurRequest = remember(targetSong.id, miniCtx) {
                                ImageRequest.Builder(miniCtx)
                                    .data(targetSong.coverUrl ?: targetSong.uri)
                                    .size(150, 150)
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
                }
            }
            Crossfade(
                targetState = song.toArtwork(),
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label = "MiniPlayerCoverCrossfade"
            ) { targetSong ->
                val isVinylActive = coverShape == 2 && coverVinylEffect
                val isSpinActive = coverShape == 2 && coverSpin && isPlaying
                if (isVinylActive) {
                    VinylRecordAsyncCover(
                        model = targetSong.coverUrl ?: targetSong.uri,
                        rotation = if (isSpinActive) spinRotation else 0f,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val activeShape = CircleShape
                    Surface(
                        shape = activeShape,
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(if (isSpinActive) spinRotation else 0f),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        SongCoverImage(
                            coverUrl = targetSong.coverUrl ?: targetSong.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            shape = activeShape,
                            iconScale = 0.68f
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun MiniPlayerWaveform(
    context: Context,
    passedMagnitudes: FloatArray,
    color: Color
) {
    val activeMagnitudes = if (passedMagnitudes.isNotEmpty()) {
        passedMagnitudes
    } else {
        val pm = remember(context) { PlaybackManager.getInstance(context) }
        val pmData by pm.visualizerData.collectAsState()
        pmData
    }
    WaveformVisualizer(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.3f)
            .blur(16.dp),
        magnitudes = activeMagnitudes,
        color = color
    )
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

@Composable
fun CavaThreeDots(
    isPlaying: Boolean,
    visualizerData: FloatArray,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CavaFallbackAnim")

    val fallbackH1 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 430, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cavaBar1"
    )
    val fallbackH2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 310, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cavaBar2"
    )
    val fallbackH3 by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 510, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cavaBar3"
    )

    val hasVisualizerSignal = isPlaying && visualizerData.isNotEmpty() && visualizerData.any { it > 0.15f }

    val rawTarget1 = when {
        !isPlaying -> 0f
        hasVisualizerSignal -> ((visualizerData.getOrElse(4) { 0.1f } - 0.1f) / 0.8f).coerceIn(0f, 1f)
        else -> fallbackH1
    }

    val rawTarget2 = when {
        !isPlaying -> 0f
        hasVisualizerSignal -> ((visualizerData.getOrElse(14) { 0.1f } - 0.1f) / 0.8f).coerceIn(0f, 1f)
        else -> fallbackH2
    }

    val rawTarget3 = when {
        !isPlaying -> 0f
        hasVisualizerSignal -> ((visualizerData.getOrElse(24) { 0.1f } - 0.1f) / 0.8f).coerceIn(0f, 1f)
        else -> fallbackH3
    }

    val animSpec = if (hasVisualizerSignal) {
        spring<Dp>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    } else {
        spring<Dp>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    }

    val h1 by animateDpAsState(
        targetValue = 3.5.dp + (11.5.dp * rawTarget1),
        animationSpec = animSpec,
        label = "cavaH1"
    )
    val h2 by animateDpAsState(
        targetValue = 3.5.dp + (15.5.dp * rawTarget2),
        animationSpec = animSpec,
        label = "cavaH2"
    )
    val h3 by animateDpAsState(
        targetValue = 3.5.dp + (9.5.dp * rawTarget3),
        animationSpec = animSpec,
        label = "cavaH3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(h1)
                .background(tint, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(h2)
                .background(tint, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(h3)
                .background(tint, CircleShape)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerOptionPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    useBlurControls: Boolean = false,
    hasBlurBackground: Boolean = false,
    blurContainerColor: Color = Color.Black.copy(alpha = 0.5f),
    isAmoled: Boolean = false,
    isDarkTheme: Boolean = true,
    activeAccent: Color = MaterialTheme.colorScheme.primary,
    isLandscape: Boolean = false,
    isCompact: Boolean = false
) {
    val isBlur = useBlurControls || hasBlurBackground

    // Animated shape morphing for the inner icon badge:
    // Inactive: Circle (percent = 50), Active: Material 3 Expressive squircle/rounded square (percent = 28)
    val cornerPercent by animateIntAsState(
        targetValue = if (isActive) 28 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillIconCornerMorph"
    )
    val iconBadgeShape = RoundedCornerShape(percent = cornerPercent)

    // Animated shape morphing for the outer pill container:
    // Inactive: Capsule / Pill (percent = 50), Active: Material 3 Expressive rounded container (percent = 30)
    val pillCornerPercent by animateIntAsState(
        targetValue = if (isActive) 30 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillOuterCornerMorph"
    )
    val pillShape = RoundedCornerShape(percent = pillCornerPercent)

    // Pill container colors
    val targetPillBg = if (isBlur) {
        if (isActive) Color.White.copy(alpha = 0.22f) else blurContainerColor
    } else {
        if (isActive) {
            activeAccent.copy(alpha = 0.16f)
        } else if (isAmoled) {
            Color(0xFF222222)
        } else if (isDarkTheme) {
            Color.Black.copy(alpha = 0.40f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
        }
    }
    val pillBg by animateColorAsState(targetValue = targetPillBg, animationSpec = tween(220), label = "pillBg")

    // Inner icon badge colors
    val targetIconBadgeBg = if (isBlur) {
        if (isActive) Color.White else Color.White.copy(alpha = 0.12f)
    } else {
        if (isActive) activeAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }
    val iconBadgeBg by animateColorAsState(targetValue = targetIconBadgeBg, animationSpec = tween(220), label = "iconBadgeBg")

    val targetIconTint = if (isBlur) {
        if (isActive) Color.Black else Color.White.copy(alpha = 0.85f)
    } else {
        if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconTint by animateColorAsState(targetValue = targetIconTint, animationSpec = tween(220), label = "iconTint")

    // Label text color
    val targetTextColor = if (isBlur) {
        if (isActive) Color.White else Color.White.copy(alpha = 0.80f)
    } else {
        if (isActive) activeAccent else MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor by animateColorAsState(targetValue = targetTextColor, animationSpec = tween(220), label = "textColor")

    val pillHeight = when {
        isCompact -> 32.dp
        isLandscape -> 36.dp
        else -> 40.dp
    }
    val badgeSize = when {
        isCompact -> 22.dp
        isLandscape -> 26.dp
        else -> 28.dp
    }
    val iconSize = when {
        isCompact -> 13.dp
        isLandscape -> 15.dp
        else -> 16.dp
    }
    val textSize = when {
        isCompact -> 9.5.sp
        isLandscape -> 11.sp
        else -> 11.5.sp
    }

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Box(
        modifier = modifier
            .height(pillHeight)
            .bounceClick(0.95f)
            .clip(pillShape)
            .background(pillBg, pillShape)
            .then(clickModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (isCompact) 4.dp else if (isLandscape) 5.dp else 6.dp,
                    end = if (isCompact) 4.dp else if (isLandscape) 6.dp else 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .background(color = iconBadgeBg, shape = iconBadgeShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize)
                )
            }

            Spacer(modifier = Modifier.width(if (isCompact) 3.dp else if (isLandscape) 5.dp else 6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = textSize,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}


