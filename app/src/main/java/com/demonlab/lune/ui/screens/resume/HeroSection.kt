package com.demonlab.lune.ui.screens.resume

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.demonlab.lune.R
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.player.ScallopPlayPauseButtonWithProgress
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import com.demonlab.lune.ui.utils.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HeroSection(
    currentSong: Song?,
    isPlaying: Boolean,
    topArtist: String,
    topArtistSongs: List<Song> = emptyList(),
    topSong: Song? = null,
    topSongPlayCount: Long = 0L,
    topGenre: String = "",
    totalPlays: Long = 0L,
    onArtistClick: ((String) -> Unit)? = null,
    onSongClick: ((Song) -> Unit)? = null,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0,
    playbackProgress: Float = 0f,
    onContinueListening: () -> Unit,
    onPlayToggle: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    val cardBrush = remember(hasBlurBackground, isDarkTheme, colorScheme) {
        if (hasBlurBackground) {
            Brush.linearGradient(
                colors = listOf(
                    if (isDarkTheme) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.32f),
                    if (isDarkTheme) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.18f)
                )
            )
        } else {
            val isDark = colorScheme.surface.luminance() < 0.5f
            if (isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.secondaryContainer,
                        colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.secondaryContainer,
                        colorScheme.primaryContainer.copy(alpha = 0.65f)
                    )
                )
            }
        }
    }

    val cardContentColor = if (hasBlurBackground) {
        Color.White
    } else {
        colorScheme.onSecondaryContainer
    }

    val cardChipBgColor = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.22f)
    } else {
        colorScheme.surface.copy(alpha = 0.50f)
    }

    Column(
        modifier = Modifier.animateContentSize(
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tops Summary Hero Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(190.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(brush = cardBrush)
                    .padding(16.dp)
            ) {
                TopStatsCardContent(
                    topArtist = topArtist,
                    topArtistSongs = topArtistSongs,
                    topSong = topSong,
                    topSongPlayCount = topSongPlayCount,
                    topGenre = topGenre,
                    totalPlays = totalPlays,
                    contentColor = cardContentColor,
                    chipBgColor = cardChipBgColor,
                    onArtistClick = onArtistClick,
                    onSongClick = onSongClick
                )
            }
        }

        if (currentSong != null) {
            ContinueListeningCard(
                song = currentSong,
                isPlaying = isPlaying,
                playbackProgress = playbackProgress,
                hasBlurBackground = hasBlurBackground,
                isDarkTheme = isDarkTheme,
                useCustomControlsColor = useCustomControlsColor,
                controlsColorPalette = controlsColorPalette,
                onClick = onContinueListening,
                onPlayToggle = onPlayToggle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private class MorphOutlineShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        // startAngle = 0 ensures canonical orientation without any atan2 cubic-0 alignment jumps
        morph.toPath(progress, path, 0)

        // Scale proportionally to fill ~94% of the container
        val scale = minOf(size.width, size.height) * 0.94f
        val matrix = Matrix()
        matrix.scale(scale, scale)
        path.transform(matrix)

        // Center precisely in the box
        val pathBounds = path.getBounds()
        val dx = (size.width / 2f) - pathBounds.center.x
        val dy = (size.height / 2f) - pathBounds.center.y
        path.translate(Offset(dx, dy))

        return Outline.Generic(path)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object ExpressiveMorphCache {
    private val expressivePolygons: List<RoundedPolygon> by lazy {
        listOf(
            MaterialShapes.Flower,
            MaterialShapes.Clover4Leaf,
            MaterialShapes.SoftBurst,
            MaterialShapes.Pill,
            MaterialShapes.Sunny,
            MaterialShapes.Cookie4Sided,
            MaterialShapes.Oval,
            MaterialShapes.Heart
        ).map { it.normalized() }
    }

    @Volatile
    private var cachedMorphSequence: List<Morph>? = null

    suspend fun getMorphSequence(): List<Morph> {
        cachedMorphSequence?.let { return it }
        return withContext(Dispatchers.Default) {
            cachedMorphSequence ?: run {
                val polygons = expressivePolygons
                val sequence = polygons.indices.map { i ->
                    Morph(polygons[i], polygons[(i + 1) % polygons.size])
                }
                cachedMorphSequence = sequence
                sequence
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AnimatedTopArtistCover(
    topArtist: String,
    topArtistSongs: List<Song>,
    contentColor: Color,
    chipBgColor: Color,
    onClick: (() -> Unit)?
) {
    val defaultShape = MaterialShapes.Flower.toShape()
    var morphSequence by remember { mutableStateOf<List<Morph>?>(null) }

    LaunchedEffect(Unit) {
        morphSequence = ExpressiveMorphCache.getMorphSequence()
    }

    val artistCoverObjects = remember(topArtist, topArtistSongs) {
        if (topArtist.isBlank()) return@remember emptyList()
        val withCover = topArtistSongs.filter { !it.coverUrl.isNullOrEmpty() || it.albumArtUri != null }
        val uniqueCovers = withCover
            .mapNotNull { it.coverUrl ?: it.albumArtUri }
            .distinct()
        if (uniqueCovers.isNotEmpty()) uniqueCovers else emptyList()
    }

    val animProgress = remember(topArtist) { Animatable(0f) }
    val numCovers = artistCoverObjects.size
    val numMorphs = morphSequence?.size ?: 0

    LaunchedEffect(topArtist, numMorphs, numCovers) {
        if (numMorphs == 0) return@LaunchedEffect
        while (true) {
            delay(3500L)
            animProgress.animateTo(
                targetValue = animProgress.value + 1f,
                animationSpec = tween(
                    durationMillis = 1100,
                    easing = FastOutSlowInEasing
                )
            )
            // Periodic wrap-around to keep numbers bounded
            if (animProgress.value >= 1000f) {
                val cycleLength = if (numCovers > 0) numMorphs * numCovers else numMorphs
                animProgress.snapTo(animProgress.value % cycleLength)
            }
        }
    }

    val totalProgress = animProgress.value
    val currentStep = totalProgress.toInt()
    val progress = (totalProgress - currentStep).coerceIn(0f, 1f)

    val currentMorphShape: Shape = remember(morphSequence, currentStep, progress) {
        val sequence = morphSequence
        if (sequence != null && sequence.isNotEmpty()) {
            val currentMorph = sequence[currentStep % sequence.size]
            MorphOutlineShape(currentMorph, progress)
        } else {
            null
        }
    } ?: defaultShape

    val currentCover = if (numCovers > 0) artistCoverObjects[currentStep % numCovers] else null
    val nextCover = if (numCovers > 0) artistCoverObjects[(currentStep + 1) % numCovers] else null

    val pulseScale = if (progress > 0.001f) 1f - 0.04f * kotlin.math.sin(progress * Math.PI.toFloat()) else 1f

    Box(
        modifier = Modifier
            .size(140.dp)
            .bounceClick()
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                shape = currentMorphShape
                clip = true
            }
            .background(chipBgColor)
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        if (currentCover != null) {
            SongCoverImage(
                coverUrl = currentCover,
                shape = RectangleShape,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        if (nextCover != null && nextCover != currentCover && progress > 0.001f) {
            SongCoverImage(
                coverUrl = nextCover,
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = progress
                    },
                contentScale = ContentScale.Crop
            )
        }
        if (currentCover == null && nextCover == null) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopStatsCardContent(
    topArtist: String,
    topArtistSongs: List<Song>,
    topSong: Song?,
    topSongPlayCount: Long,
    topGenre: String,
    totalPlays: Long,
    contentColor: Color,
    chipBgColor: Color,
    onArtistClick: ((String) -> Unit)?,
    onSongClick: ((Song) -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Large artist cover in animated morphing M3 Expressive shape with cycling covers
        AnimatedTopArtistCover(
            topArtist = topArtist,
            topArtistSongs = topArtistSongs,
            contentColor = contentColor,
            chipBgColor = chipBgColor,
            onClick = if (onArtistClick != null) { { onArtistClick.invoke(topArtist) } } else null
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Right side: All "top" stats vertically aligned with basicMarquee
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Artist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onArtistClick != null) {
                        onArtistClick?.invoke(topArtist)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = contentColor.copy(alpha = 0.85f)
                    )
                    Text(
                        text = stringResource(R.string.card_top_artist).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = topArtist,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                val songsCountStr = if (topArtistSongs.isNotEmpty()) {
                    stringResource(R.string.card_artist_songs, topArtistSongs.size)
                } else if (totalPlays > 0) {
                    stringResource(R.string.card_plays_count, totalPlays)
                } else {
                    ""
                }
                if (songsCountStr.isNotEmpty()) {
                    Text(
                        text = songsCountStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            // 2. Top Song
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = topSong != null && onSongClick != null) {
                        topSong?.let { onSongClick?.invoke(it) }
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = contentColor.copy(alpha = 0.85f)
                    )
                    Text(
                        text = stringResource(R.string.card_top_song).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                    if (topSongPlayCount > 0) {
                        Text(
                            text = "• " + if (topSongPlayCount == 1L) {
                                stringResource(R.string.card_play_count_single, topSongPlayCount)
                            } else {
                                stringResource(R.string.card_plays_count, topSongPlayCount)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.65f),
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = topSong?.title ?: stringResource(R.string.card_no_plays),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            // 3. Top Genre
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = contentColor.copy(alpha = 0.85f)
                    )
                    Text(
                        text = stringResource(R.string.card_top_genre).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = topGenre.ifBlank { stringResource(R.string.unknown_genre) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

@Composable
private fun ContinueListeningCard(
    song: Song,
    isPlaying: Boolean,
    playbackProgress: Float = 0f,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0,
    onClick: () -> Unit,
    onPlayToggle: () -> Unit,
) {
    val activePrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)

    val cardBg = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val titleColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
    val artistColor = if (hasBlurBackground) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .bounceClick()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            SongCoverImage(
                coverUrl = song.coverUrl ?: song.uri,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = titleColor
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = artistColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            ScallopPlayPauseButtonWithProgress(
                isPlaying = isPlaying,
                progress = playbackProgress,
                onClick = onPlayToggle,
                hasBlurBackground = hasBlurBackground,
                useCustomControlsColor = useCustomControlsColor,
                activePrimary = activePrimary,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
