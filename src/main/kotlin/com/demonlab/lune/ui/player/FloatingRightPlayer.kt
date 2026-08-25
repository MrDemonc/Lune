package com.demonlab.lune.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demonlab.lune.data.L10n
import com.demonlab.lune.data.Song
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.components.CoverArtwork
import com.demonlab.lune.ui.components.VinylRecordCover
import com.demonlab.lune.ui.components.WaveformVisualizer
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.utils.formatDuration
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun WavyTrackProgressIndicator(
    progress: Float,
    isPlaying: Boolean,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveOffset")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(14.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val activeWidth = width * progress.coerceIn(0f, 1f)

        // Draw background inactive track
        val trackPath = Path().apply {
            moveTo(0f, centerY)
            lineTo(width, centerY)
        }
        drawPath(
            path = trackPath,
            color = trackColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw animated active wavy path
        if (activeWidth > 0f) {
            val wavePath = Path()
            val waveLength = 26.dp.toPx()
            val amplitude = if (isPlaying) 3.5.dp.toPx() else 1.dp.toPx()

            var x = 0f
            var first = true
            while (x <= activeWidth) {
                val currentOffset = if (isPlaying) waveOffset else 0f
                val y = centerY + amplitude * sin((x / waveLength) * (2 * Math.PI).toFloat() + currentOffset)
                if (first) {
                    wavePath.moveTo(x, y)
                    first = false
                } else {
                    wavePath.lineTo(x, y)
                }
                x += 2f
            }

            drawPath(
                path = wavePath,
                color = color,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingRightPlayer(
    playbackManager: PlaybackManager,
    settingsManager: SettingsManager,
    onOpenEqualizer: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong = playbackManager.currentSong ?: return
    var showLyrics by remember { mutableStateOf(false) }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val hasLyrics = playbackManager.lyrics.lines.isNotEmpty()
    val isExpanded = showLyrics && hasLyrics

    val controlsColor = getControlsPrimaryColor(
        useCustomControlsColor = settingsManager.useCustomControlsColor,
        controlsColorPalette = settingsManager.controlsColorPalette
    )

    val actualProgress = if (playbackManager.currentDurationMs > 0) {
        (playbackManager.currentPositionMs.toFloat() / playbackManager.currentDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayProgress = if (isDragging) dragProgress else actualProgress
    val displayPositionMs = if (isDragging) {
        (dragProgress * playbackManager.currentDurationMs).toLong()
    } else {
        playbackManager.currentPositionMs
    }

    val coverDp = when (settingsManager.coverSize) {
        0 -> 130.dp
        1 -> 150.dp
        else -> 170.dp
    }

    Surface(
        modifier = modifier
            .width(330.dp)
            .fillMaxHeight()
            .padding(top = 8.dp, bottom = 16.dp, end = 20.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row (Now Playing / Lyrics / EQ / Queue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showLyrics) "Lyrics" else "Now Playing",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showLyrics = !showLyrics }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = "Toggle Lyrics",
                            tint = if (showLyrics) controlsColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onOpenEqualizer, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizer",
                            tint = if (settingsManager.isEqEnabled) controlsColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Center Content: Cover Art OR Vertically Expanded Lyrics Viewer
            if (showLyrics) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingLyricsViewer(
                        playbackManager = playbackManager,
                        controlsColor = controlsColor
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(coverDp),
                    contentAlignment = Alignment.Center
                ) {
                    when (settingsManager.coverShape) {
                        2 -> { // Circular / Vinyl
                            VinylRecordCover(
                                coverPath = currentSong.coverPath,
                                isPlaying = playbackManager.isPlaying,
                                isVinylEffect = settingsManager.coverVinyl,
                                isSpinning = settingsManager.coverSpin,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        1 -> { // Square
                            CoverArtwork(
                                coverPath = currentSong.coverPath,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(8.dp, RoundedCornerShape(4.dp)),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                        else -> { // Default Rounded
                            CoverArtwork(
                                coverPath = currentSong.coverPath,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(10.dp, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Song Info (Title & Artist) + Favorite Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${currentSong.artist} • ${currentSong.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { playbackManager.toggleFavorite(currentSong) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (playbackManager.isFavorite(currentSong.id)) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (playbackManager.isFavorite(currentSong.id)) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Waveform Visualizer
            if (settingsManager.isVisualizerEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                WaveformVisualizer(
                    frequencies = playbackManager.visualizerFrequencies,
                    isPlaying = playbackManager.isPlaying,
                    primaryColor = controlsColor,
                    modifier = Modifier.fillMaxWidth().height(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // WAVY WAYBAR PROGRESS
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                WavyTrackProgressIndicator(
                    progress = displayProgress,
                    isPlaying = playbackManager.isPlaying,
                    color = controlsColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
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
                    value = displayProgress,
                    onValueChange = { newProgress ->
                        isDragging = true
                        dragProgress = newProgress
                    },
                    onValueChangeFinished = {
                        val newPos = (dragProgress * playbackManager.currentDurationMs).toLong()
                        playbackManager.seekTo(newPos)
                        isDragging = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .graphicsLayer {
                                    rotationZ = if (playbackManager.isPlaying) rotation else 0f
                                }
                                .background(color = controlsColor, shape = RoundedCornerShape(4.dp))
                        )
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }

            // Duration labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(displayPositionMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(playbackManager.currentDurationMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { playbackManager.toggleShuffle() }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (settingsManager.isShuffle) controlsColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = { playbackManager.playPrevious() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play / Pause
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(controlsColor)
                        .bounceClick { playbackManager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackManager.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = { playbackManager.playNext() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Repeat Mode
                IconButton(onClick = { playbackManager.toggleRepeatMode() }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = when (settingsManager.repeatMode) {
                            1 -> Icons.Default.RepeatOne
                            2 -> Icons.Default.Repeat
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (settingsManager.repeatMode > 0) controlsColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Volume Control Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                    contentDescription = "Mute",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Slider(
                    value = settingsManager.volume,
                    onValueChange = { playbackManager.setVolume(it) },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = controlsColor,
                        activeTrackColor = controlsColor
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Full Volume",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }

            // QUEUE VIEW BENEATH PLAYER (When lyrics is not showing)
            if (!showLyrics) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${L10n.queue} (${playbackManager.activeQueue.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (playbackManager.activeQueue.isNotEmpty()) {
                        TextButton(
                            onClick = { playbackManager.clearQueue() },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(L10n.clear, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (playbackManager.activeQueue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(L10n.queueEmpty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(playbackManager.activeQueue) { index, song ->
                            val isCurrent = index == playbackManager.currentIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else Color.Transparent
                                    )
                                    .clickable { playbackManager.playQueueIndex(index) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoverArtwork(
                                    coverPath = song.coverPath,
                                    modifier = Modifier.size(32.dp),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { playbackManager.removeFromQueue(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun FloatingLyricsViewer(
    playbackManager: PlaybackManager,
    controlsColor: Color,
    modifier: Modifier = Modifier
) {
    val lyrics = playbackManager.lyrics
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(playbackManager.currentLyricIndex) {
        if (playbackManager.currentLyricIndex >= 0 && lyrics.isSynced) {
            scope.launch {
                listState.animateScrollToItem(
                    index = (playbackManager.currentLyricIndex - 2).coerceAtLeast(0)
                )
            }
        }
    }

    if (lyrics.lines.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = L10n.noLyrics,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize().padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            itemsIndexed(lyrics.lines) { index, line ->
                val isHighlighted = index == playbackManager.currentLyricIndex
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (isHighlighted) 15.sp else 12.sp
                    ),
                    color = if (isHighlighted) controlsColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            if (lyrics.isSynced) {
                                playbackManager.seekTo(line.timestampMs)
                            }
                        }
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}
