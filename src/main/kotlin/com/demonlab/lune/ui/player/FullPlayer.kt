package com.demonlab.lune.ui.player

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@Composable
fun FullPlayer(
    playbackManager: PlaybackManager,
    settingsManager: SettingsManager,
    onCollapse: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong = playbackManager.currentSong ?: return
    var showLyrics by remember { mutableStateOf(false) }

    val controlsColor = getControlsPrimaryColor(
        useCustomControlsColor = settingsManager.useCustomControlsColor,
        controlsColorPalette = settingsManager.controlsColorPalette
    )

    val progress = if (playbackManager.currentDurationMs > 0) {
        (playbackManager.currentPositionMs.toFloat() / playbackManager.currentDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = if (showLyrics) L10n.lyrics else L10n.get("now_playing"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row {
                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = L10n.lyrics,
                            tint = if (showLyrics) controlsColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = L10n.equalizer,
                            tint = if (settingsManager.isEqEnabled) controlsColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenQueue) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = L10n.queue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Content (Cover Art OR Lyrics)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (showLyrics) {
                    LyricsViewer(
                        playbackManager = playbackManager,
                        controlsColor = controlsColor
                    )
                } else {
                    // Artwork with selected shape/animation
                    val coverSizeFraction = when (settingsManager.coverSize) {
                        0 -> 0.65f
                        1 -> 0.78f
                        else -> 0.90f
                    }

                    when (settingsManager.coverShape) {
                        2 -> { // Circular / Vinyl
                            VinylRecordCover(
                                coverPath = currentSong.coverPath,
                                isPlaying = playbackManager.isPlaying,
                                isVinylEffect = settingsManager.coverVinyl,
                                isSpinning = settingsManager.coverSpin,
                                modifier = Modifier
                                    .fillMaxWidth(coverSizeFraction)
                                    .aspectRatio(1f)
                            )
                        }
                        1 -> { // Square
                            CoverArtwork(
                                coverPath = currentSong.coverPath,
                                modifier = Modifier
                                    .fillMaxWidth(coverSizeFraction)
                                    .aspectRatio(1f)
                                    .shadow(16.dp, RoundedCornerShape(4.dp)),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                        else -> { // Default Rounded
                            CoverArtwork(
                                coverPath = currentSong.coverPath,
                                modifier = Modifier
                                    .fillMaxWidth(coverSizeFraction)
                                    .aspectRatio(1f)
                                    .shadow(20.dp, RoundedCornerShape(24.dp)),
                                shape = RoundedCornerShape(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Area (Title, Artist, Album, Badges)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentSong.artist} • ${currentSong.album}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { playbackManager.toggleFavorite(currentSong) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (currentSong.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentSong.isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Waveform Visualizer
            if (settingsManager.isVisualizerEnabled) {
                WaveformVisualizer(
                    frequencies = playbackManager.visualizerFrequencies,
                    isPlaying = playbackManager.isPlaying,
                    primaryColor = controlsColor,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(36.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Slider
            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    val newPos = (newProgress * playbackManager.currentDurationMs).toLong()
                    playbackManager.seekTo(newPos)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = controlsColor,
                    activeTrackColor = controlsColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Duration labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(playbackManager.currentPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(playbackManager.currentDurationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { playbackManager.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (settingsManager.isShuffle) controlsColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Previous
                IconButton(
                    onClick = { playbackManager.playPrevious() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(controlsColor)
                        .bounceClick { playbackManager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackManager.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = { playbackManager.playNext() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Mode
                IconButton(onClick = { playbackManager.toggleRepeatMode() }) {
                    Icon(
                        imageVector = when (settingsManager.repeatMode) {
                            1 -> Icons.Default.RepeatOne
                            2 -> Icons.Default.Repeat
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (settingsManager.repeatMode > 0) controlsColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Volume Control
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeMute,
                    contentDescription = "Mute",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = settingsManager.volume,
                    onValueChange = { playbackManager.setVolume(it) },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = controlsColor,
                        activeTrackColor = controlsColor
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Full Volume",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun LyricsViewer(
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 40.dp)
        ) {
            itemsIndexed(lyrics.lines) { index, line ->
                val isHighlighted = index == playbackManager.currentLyricIndex
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (isHighlighted) 22.sp else 18.sp
                    ),
                    color = if (isHighlighted) controlsColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (lyrics.isSynced) {
                                playbackManager.seekTo(line.timestampMs)
                            }
                        }
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}
