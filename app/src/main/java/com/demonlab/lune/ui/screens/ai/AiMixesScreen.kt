package com.demonlab.lune.ui.screens.ai

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.demonlab.lune.R
import com.demonlab.lune.ai.LuneAiEngine
import com.demonlab.lune.ai.model.AiMix
import com.demonlab.lune.ai.model.MixCategory
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.components.rememberBlurSheetColors
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.viewmodels.MusicViewModel
import kotlin.math.sin

/**
 * Material 3 Expressive Asymmetric Card Shapes
 */
val M3ExpressiveBannerShape = RoundedCornerShape(topStart = 32.dp, topEnd = 16.dp, bottomEnd = 32.dp, bottomStart = 16.dp)
val M3ExpressiveCollageShape1 = RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 8.dp)
val M3ExpressiveCollageShape2 = RoundedCornerShape(topStart = 12.dp, topEnd = 28.dp, bottomEnd = 12.dp, bottomStart = 28.dp)
val M3ExpressiveTileShape = RoundedCornerShape(22.dp)

/**
 * Material 3 Expressive Wavy Progress Indicator
 */
@Composable
fun M3WavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    waveColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 5.dp,
    amplitude: Float = 6f,
    wavelength: Float = 36f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_wave_progress")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.height(18.dp)) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val strokePx = strokeWidth.toPx()
        val activeWidth = (width * progress.coerceIn(0f, 1f))

        // Inactive background track (straight line)
        drawLine(
            color = trackColor,
            start = Offset(0f, midY),
            end = Offset(width, midY),
            strokeWidth = strokePx * 0.8f,
            cap = StrokeCap.Round
        )

        // Active Wavy progress line
        if (activeWidth > 2f) {
            val wavePath = Path()
            wavePath.moveTo(0f, midY)

            var x = 0f
            val step = 3f
            while (x <= activeWidth) {
                val waveRatio = (x / activeWidth).coerceIn(0f, 1f)
                val currentAmp = amplitude * waveRatio // Smooth ramp up at the start
                val y = midY + (sin((x / wavelength) * (2 * Math.PI).toFloat() + phase) * currentAmp)
                wavePath.lineTo(x, y)
                x += step
            }

            drawPath(
                path = wavePath,
                color = waveColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Leading Indicator Dot
            drawCircle(
                color = waveColor,
                radius = strokePx * 1.2f,
                center = Offset(activeWidth, midY)
            )
        }
    }
}

@Composable
fun AiMixesScreen(
    allSongs: List<Song>,
    playbackManager: PlaybackManager,
    viewModel: MusicViewModel,
    hasBlurBackground: Boolean,
    isDarkTheme: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val aiEngine = remember { LuneAiEngine.getInstance(context) }
    val mixes by aiEngine.aiMixes.collectAsState()
    val blurColors = rememberBlurSheetColors(playbackManager.currentSong)

    LaunchedEffect(allSongs) {
        aiEngine.refreshMixes(allSongs)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = bottomPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Material 3 Expressive Collage Header Banner
        item {
            AiExpressiveCollageBanner(
                allSongs = allSongs,
                hasBlur = hasBlurBackground,
                isDark = isDarkTheme,
                blurColors = blurColors,
                onSmartShuffleClick = {
                    if (allSongs.isNotEmpty()) {
                        val shuffled = aiEngine.generateSmartShuffle(allSongs, playbackManager.currentSong)
                        playbackManager.play(shuffled.first(), shuffled, category = "MIXES", playlistName = "Smart Shuffle")
                    }
                }
            )
        }

        // 2. Quick Action Feature Cards (Smart Shuffle & Radio Inteligente)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Discovery Mode Card
                AiActionCard(
                    title = stringResource(R.string.ai_discovery_title),
                    subtitle = stringResource(R.string.ai_discovery_desc),
                    icon = Icons.Default.Explore,
                    gradient = listOf(Color(0xFF0D9488), Color(0xFF10B981)),
                    modifier = Modifier.weight(1f),
                    hasBlur = hasBlurBackground,
                    onClick = {
                        if (allSongs.isNotEmpty()) {
                            val discoveryQueue = aiEngine.generateDiscoveryQueue(allSongs, playbackManager.currentSong)
                            if (discoveryQueue.isNotEmpty()) {
                                playbackManager.play(discoveryQueue.first(), discoveryQueue, category = "MIXES", playlistName = discoveryQueue.first().title)
                            }
                        }
                    }
                )

                // Instant Flow Card
                AiActionCard(
                    title = stringResource(R.string.ai_smart_radio_title),
                    subtitle = stringResource(R.string.ai_smart_radio_desc),
                    icon = Icons.Default.AutoAwesome,
                    gradient = listOf(Color(0xFFEC4899), Color(0xFFF43F5E)),
                    modifier = Modifier.weight(1f),
                    hasBlur = hasBlurBackground,
                    onClick = {
                        val seed = playbackManager.currentSong ?: allSongs.maxByOrNull { aiEngine.getSongAffinity(it.id) } ?: allSongs.firstOrNull()
                        if (seed != null) {
                            val smartQueue = aiEngine.generateSmartShuffle(allSongs, seed)
                            playbackManager.play(seed, smartQueue, category = "MIXES", playlistName = seed.title)
                        }
                    }
                )
            }
        }

        // 3. Featured AI Mixes Showcase
        if (mixes.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.ai_custom_mixes_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            items(mixes, key = { it.id }) { mix ->
                AiMixCard(
                    mix = mix,
                    hasBlur = hasBlurBackground,
                    isDark = isDarkTheme,
                    blurColors = blurColors,
                    onPlayClick = {
                        if (mix.songs.isNotEmpty()) {
                            playbackManager.play(mix.songs.first(), mix.songs, category = "MIXES", playlistName = mix.title)
                        }
                    }
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.ai_analyzing_library),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Expressive Collage Header with Wavy Progress Indicator
 */
@Composable
private fun AiExpressiveCollageBanner(
    allSongs: List<Song>,
    hasBlur: Boolean,
    isDark: Boolean,
    blurColors: com.demonlab.lune.ui.components.BlurSheetColors,
    onSmartShuffleClick: () -> Unit
) {
    val topSongs = remember(allSongs) { allSongs.take(4) }
    val analyzedPercent = if (allSongs.isNotEmpty()) 0.92f else 0.40f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = if (hasBlur) 0.dp else 4.dp,
                shape = M3ExpressiveBannerShape,
                spotColor = Color(0xFF6366F1).copy(alpha = 0.20f)
            ),
        shape = M3ExpressiveBannerShape,
        color = if (hasBlur) {
            Color.White.copy(alpha = 0.12f)
        } else if (isDark) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = if (hasBlur) {
            BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Row: Expressive Badge + Status + Dynamic Collage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Title and M3 Expressive Pill Badge
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasBlur) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (hasBlur) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.ai_core_title),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (hasBlur) Color.White else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.ai_musical_intelligence),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (hasBlur) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.ai_local_learning_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasBlur) Color.White.copy(alpha = 0.70f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right: Material 3 Expressive Covers Collage
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 75.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Tile 3 (Background right, angled)
                    if (topSongs.size >= 3) {
                        Surface(
                            shape = M3ExpressiveCollageShape1,
                            border = BorderStroke(1.5.dp, if (hasBlur) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .size(50.dp)
                                .offset(x = 28.dp, y = (-4).dp)
                                .rotate(10f)
                                .shadow(4.dp, M3ExpressiveCollageShape1)
                        ) {
                            SongCoverImage(
                                coverUrl = topSongs[2].coverUrl ?: topSongs[2].uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                shape = M3ExpressiveCollageShape1
                            )
                        }
                    }

                    // Tile 2 (Background left, angled)
                    if (topSongs.size >= 2) {
                        Surface(
                            shape = M3ExpressiveCollageShape2,
                            border = BorderStroke(1.5.dp, if (hasBlur) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .size(54.dp)
                                .offset(x = (-24).dp, y = 4.dp)
                                .rotate(-8f)
                                .shadow(4.dp, M3ExpressiveCollageShape2)
                        ) {
                            SongCoverImage(
                                coverUrl = topSongs[1].coverUrl ?: topSongs[1].uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                shape = M3ExpressiveCollageShape2
                            )
                        }
                    }

                    // Tile 1 (Front center, main expressive squircle)
                    if (topSongs.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(2.dp, if (hasBlur) Color.White else MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .size(58.dp)
                                .shadow(8.dp, RoundedCornerShape(18.dp))
                        ) {
                            SongCoverImage(
                                coverUrl = topSongs[0].coverUrl ?: topSongs[0].uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(18.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (hasBlur) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Material 3 Wavy Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.ai_affinity_flow),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (hasBlur) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.ai_songs_learned, allSongs.size),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasBlur) Color.White else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                M3WavyProgressIndicator(
                    progress = analyzedPercent,
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = if (hasBlur) Color.White.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant,
                    waveColor = if (hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // CTA Button with Material 3 Expressive Styling
            Button(
                onClick = onSmartShuffleClick,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary,
                    contentColor = if (hasBlur) Color.Black else MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick()
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ai_start_smart_shuffle),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AiActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    hasBlur: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (hasBlur) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainer,
        border = if (hasBlur) BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)) else null,
        modifier = modifier
            .height(115.dp)
            .bounceClick()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = gradient.first().copy(alpha = 0.25f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (hasBlur) Color.White else gradient.first(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasBlur) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasBlur) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AiMixCard(
    mix: AiMix,
    hasBlur: Boolean,
    isDark: Boolean,
    blurColors: com.demonlab.lune.ui.components.BlurSheetColors,
    onPlayClick: () -> Unit
) {
    Surface(
        onClick = onPlayClick,
        shape = RoundedCornerShape(24.dp),
        color = if (hasBlur) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainer,
        border = if (hasBlur) BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .bounceClick(scaleDown = 0.97f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Top Banner with Cover Collage & Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Brush.horizontalGradient(mix.gradientColors))
            ) {
                // Background covers collage preview
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy((-16).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val previewSongs = mix.songs.take(4)
                    previewSongs.forEachIndexed { index, song ->
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .size(70.dp)
                                .offset(x = (index * 6).dp)
                                .shadow(4.dp, CircleShape)
                        ) {
                            SongCoverImage(
                                coverUrl = song.coverUrl ?: song.uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape
                            )
                        }
                    }
                }

                // Play Button floating on the right
                FilledIconButton(
                    onClick = onPlayClick,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 18.dp)
                        .size(54.dp)
                        .shadow(8.dp, CircleShape)
                        .bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Mix Details & Track List preview
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mix.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (hasBlur) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasBlur) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = stringResource(R.string.ai_mix_songs_count, mix.songs.size),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasBlur) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mix.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasBlur) Color.White.copy(alpha = 0.70f) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Song Preview list (first 3 songs)
                mix.songs.take(3).forEach { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (hasBlur) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${song.title} • ${song.artist}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasBlur) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
