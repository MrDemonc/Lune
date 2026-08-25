package com.demonlab.lune.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demonlab.lune.data.Song
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.utils.formatDuration
import org.jetbrains.skia.Image
import java.io.File

// Cached image loader for Compose Desktop
@Composable
fun rememberCoverBitmap(coverPath: String?): ImageBitmap? {
    if (coverPath.isNullOrBlank()) return null
    val cached = remember(coverPath) { com.demonlab.lune.tools.CoverCache.getFromMemory(coverPath) }
    if (cached != null) return cached

    return produceState<ImageBitmap?>(initialValue = null, key1 = coverPath) {
        value = com.demonlab.lune.tools.CoverCache.loadCover(coverPath)
    }.value
}

@Composable
fun CoverArtwork(
    coverPath: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    contentDescription: String? = null
) {
    val bitmap = rememberCoverBitmap(coverPath)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Default cover",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SongItem(
    song: Song,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onOptionsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable { onPlay() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Art
        CoverArtwork(
            coverPath = song.coverPath,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (song.isHiFi) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Hi-Res",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Duration
        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Favorite button
        IconButton(
            onClick = onFavoriteToggle,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (song.isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        if (onOptionsClick != null) {
            IconButton(
                onClick = onOptionsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Highly optimized Single-Canvas GPU Waveform Visualizer (Zero Box overhead)
@Composable
fun WaveformVisualizer(
    frequencies: FloatArray,
    isPlaying: Boolean,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val count = frequencies.size
        if (count == 0) return@Canvas
        val totalWidth = size.width
        val maxHeight = size.height
        val barWidth = (totalWidth / (count * 1.6f)).coerceIn(2f, 6f)
        val gap = (totalWidth - (barWidth * count)) / (count - 1).coerceAtLeast(1).toFloat()

        val brush = Brush.verticalGradient(
            listOf(primaryColor, primaryColor.copy(alpha = 0.35f))
        )

        for (i in 0 until count) {
            val freq = if (isPlaying) frequencies[i].coerceIn(0.08f, 1.0f) else 0.08f
            val barHeight = (freq * maxHeight).coerceIn(3f, maxHeight)
            val left = i * (barWidth + gap)
            val top = (maxHeight - barHeight) / 2f

            drawRoundRect(
                brush = brush,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun VinylRecordCover(
    coverPath: String?,
    isPlaying: Boolean,
    isVinylEffect: Boolean = true,
    isSpinning: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylRotation"
    )

    val currentRotation = if (isPlaying && isSpinning) rotation else 0f

    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = currentRotation }
            .shadow(16.dp, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isVinylEffect) {
            // Vinyl outer grooved disc
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color(0xFF181818))
                // Grooves
                for (r in 1..4) {
                    drawCircle(
                        color = Color(0xFF282828),
                        radius = size.minDimension / 2f * (0.6f + r * 0.08f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                }
            }
        }

        // Center Album Art (Record Label)
        CoverArtwork(
            coverPath = coverPath,
            modifier = Modifier
                .fillMaxSize(0.62f)
                .clip(CircleShape),
            shape = CircleShape
        )

        // Center Spindle Hole
        Surface(
            modifier = Modifier.size(14.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface
        ) {}
    }
}

@Composable
fun SettingSegmentedChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

