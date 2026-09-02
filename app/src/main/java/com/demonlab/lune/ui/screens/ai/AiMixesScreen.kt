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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
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
 * Expressive Wave Shape for Material 3 Cards & Headers
 */
val ExpressiveWaveShape: Shape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    val cornerRadius = 28f

    moveTo(cornerRadius, 0f)
    cubicTo(width * 0.25f, 0f, width * 0.35f, 16f, width * 0.5f, 12f)
    cubicTo(width * 0.65f, 8f, width * 0.75f, 0f, width - cornerRadius, 0f)
    quadraticTo(width, 0f, width, cornerRadius)
    lineTo(width, height - cornerRadius)
    quadraticTo(width, height, width - cornerRadius, height)
    lineTo(cornerRadius, height)
    quadraticTo(0f, height, 0f, height - cornerRadius)
    lineTo(0f, cornerRadius)
    quadraticTo(0f, 0f, cornerRadius, 0f)
    close()
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
        // 1. Interactive AI Wave Header Banner
        item {
            AiWaveHeaderBanner(
                totalSongs = allSongs.size,
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

        // 2. Quick Action Feature Cards (Smart Shuffle & Daily Flow)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Smart Shuffle Card
                AiActionCard(
                    title = "Smart Shuffle",
                    subtitle = "Transiciones armónicas",
                    icon = Icons.Default.Shuffle,
                    gradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                    modifier = Modifier.weight(1f),
                    hasBlur = hasBlurBackground,
                    onClick = {
                        if (allSongs.isNotEmpty()) {
                            val shuffled = aiEngine.generateSmartShuffle(allSongs, playbackManager.currentSong)
                            playbackManager.play(shuffled.first(), shuffled, category = "MIXES", playlistName = "Smart Shuffle")
                        }
                    }
                )

                // Instant Flow Card
                AiActionCard(
                    title = "Radio Inteligente",
                    subtitle = "Mix infinito para ti",
                    icon = Icons.Default.AutoAwesome,
                    gradient = listOf(Color(0xFFEC4899), Color(0xFFF43F5E)),
                    modifier = Modifier.weight(1f),
                    hasBlur = hasBlurBackground,
                    onClick = {
                        val seed = playbackManager.currentSong ?: allSongs.maxByOrNull { aiEngine.getSongAffinity(it.id) } ?: allSongs.firstOrNull()
                        if (seed != null) {
                            val smartQueue = aiEngine.generateSmartShuffle(allSongs, seed)
                            playbackManager.play(seed, smartQueue, category = "MIXES", playlistName = "Radio Inteligente")
                        }
                    }
                )
            }
        }

        // 3. Featured AI Mixes Showcase
        if (mixes.isNotEmpty()) {
            item {
                Text(
                    text = "Tus Mixes Personalizados",
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
                        text = "Analizando tu música para generar Mixes personalizados...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AiWaveHeaderBanner(
    totalSongs: Int,
    hasBlur: Boolean,
    isDark: Boolean,
    blurColors: com.demonlab.lune.ui.components.BlurSheetColors,
    onSmartShuffleClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 6.dp, shape = ExpressiveWaveShape, spotColor = Color(0xFF6366F1).copy(alpha = 0.35f)),
        shape = ExpressiveWaveShape,
        color = if (hasBlur) {
            Color.White.copy(alpha = 0.12f)
        } else if (isDark) {
            Color(0xFF1E1B2E)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        },
        border = if (hasBlur) BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            // Live Ambient Wave Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val waveColor1 = Color(0xFF6366F1).copy(alpha = if (isDark || hasBlur) 0.22f else 0.14f)
                val waveColor2 = Color(0xFFEC4899).copy(alpha = if (isDark || hasBlur) 0.18f else 0.10f)

                val path1 = Path()
                val path2 = Path()
                val width = size.width
                val height = size.height
                val midY = height * 0.65f

                path1.moveTo(0f, height)
                path1.lineTo(0f, midY)
                path2.moveTo(0f, height)
                path2.lineTo(0f, midY + 10f)

                val steps = 40
                for (i in 0..steps) {
                    val x = (width / steps) * i
                    val rad = (i.toFloat() / steps.toFloat()) * (4 * Math.PI).toFloat() + wavePhase
                    val y1 = midY + (sin(rad) * 16f)
                    val y2 = (midY + 12f) + (sin(rad + 1.2f) * 14f)
                    path1.lineTo(x, y1)
                    path2.lineTo(x, y2)
                }
                path1.lineTo(width, height)
                path1.close()
                path2.lineTo(width, height)
                path2.close()

                drawPath(path1, waveColor1)
                drawPath(path2, waveColor2)
            }

            // Header Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1).copy(alpha = 0.25f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (hasBlur) Color.White else Color(0xFF818CF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Lune AI Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (hasBlur) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "IA local • Aprendizaje de hábitos sin conexión",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasBlur) Color.White.copy(alpha = 0.70f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (hasBlur) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "$totalSongs pistas",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasBlur) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // CTA Button
                Button(
                    onClick = onSmartShuffleClick,
                    shape = RoundedCornerShape(16.dp),
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
                        text = "Reproducir con Smart Shuffle",
                        fontWeight = FontWeight.Bold
                    )
                }
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
                            text = "${mix.songs.size} canciones",
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
