package com.demonlab.lune.ui.screens.resume

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demonlab.lune.R
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.player.ScallopPlayPauseButtonWithProgress
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import com.demonlab.lune.ui.utils.bounceClick

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
    val artistCoverSong = remember(topArtist, topArtistSongs) {
        val withCover = topArtistSongs.filter { !it.coverUrl.isNullOrEmpty() || it.albumArtUri != null }
        if (withCover.isNotEmpty()) {
            withCover.random()
        } else {
            topArtistSongs.firstOrNull()
        }
    }

    val expressiveShape = MaterialShapes.Flower.toShape()

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Large artist cover in M3 Expressive shape
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(expressiveShape)
                .background(chipBgColor)
                .clickable(enabled = onArtistClick != null) {
                    onArtistClick?.invoke(topArtist)
                },
            contentAlignment = Alignment.Center
        ) {
            val coverObj = artistCoverSong?.coverUrl ?: artistCoverSong?.albumArtUri
            if (coverObj != null) {
                SongCoverImage(
                    coverUrl = coverObj,
                    shape = expressiveShape,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = contentColor
                )
            }
        }

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
