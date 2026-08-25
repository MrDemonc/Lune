package com.demonlab.lune.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demonlab.lune.data.Song
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.components.CoverArtwork
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import com.demonlab.lune.ui.utils.bounceClick

@Composable
fun MiniPlayer(
    playbackManager: PlaybackManager,
    settingsManager: SettingsManager,
    onExpandFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong = playbackManager.currentSong ?: return
    val progress = if (playbackManager.currentDurationMs > 0) {
        (playbackManager.currentPositionMs.toFloat() / playbackManager.currentDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val controlsColor = getControlsPrimaryColor(
        useCustomControlsColor = settingsManager.useCustomControlsColor,
        controlsColorPalette = settingsManager.controlsColorPalette
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clickable { onExpandFullPlayer() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Column {
            // Top Linear Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = controlsColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover Art
                CoverArtwork(
                    coverPath = currentSong.coverPath,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Favorite
                IconButton(
                    onClick = { playbackManager.toggleFavorite(currentSong) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (currentSong.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentSong.isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Prev
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
                        .size(42.dp)
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
            }
        }
    }
}
