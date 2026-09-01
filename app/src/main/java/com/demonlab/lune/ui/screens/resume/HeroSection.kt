package com.demonlab.lune.ui.screens.resume

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.demonlab.lune.R
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.SongCoverImage
import kotlinx.coroutines.delay
import java.util.Calendar

import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import com.demonlab.lune.ui.player.ScallopPlayPauseButtonWithProgress

private data class HeroGreetingTheme(
    val greeting: String,
    val icon: ImageVector,
    val brush: Brush,
    val contentColor: Color,
    val iconBgColor: Color,
    val iconTint: Color
)

private data class ThemeColors(
    val brush: Brush,
    val contentColor: Color,
    val iconBgColor: Color,
    val iconTint: Color
)

@Composable
fun HeroSection(
    currentSong: Song?,
    isPlaying: Boolean,
    dailyListeningTimeStr: String,
    totalSongs: Int,
    playlistsCount: Int,
    favoriteCount: Int,
    topArtist: String,
    showGreetingCard: Boolean = true,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0,
    playbackProgress: Float = 0f,
    onContinueListening: () -> Unit,
    onPlayToggle: () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    val heroTheme = remember(hour, colorScheme, hasBlurBackground, isDarkTheme) {
        val isDark = if (hasBlurBackground) isDarkTheme else colorScheme.surface.luminance() < 0.5f

        val greetingStr = when (hour) {
            in 0..5 -> context.getString(R.string.welcome_early_morning)
            in 6..11 -> context.getString(R.string.welcome_morning)
            in 12..13 -> context.getString(R.string.welcome_noon)
            in 14..17 -> context.getString(R.string.welcome_afternoon)
            in 18..19 -> context.getString(R.string.welcome_evening)
            else -> context.getString(R.string.welcome_night)
        }
        val icon = when (hour) {
            in 0..5 -> Icons.Default.NightsStay
            in 6..11 -> Icons.Default.WbSunny
            in 12..13 -> Icons.Default.LightMode
            in 14..17 -> Icons.Default.WbSunny
            in 18..19 -> Icons.Default.WbTwilight
            else -> Icons.Default.NightsStay
        }
        val colors = if (hasBlurBackground) {
            val b = Brush.linearGradient(
                colors = listOf(
                    if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.28f),
                    if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.16f)
                )
            )
            ThemeColors(b, Color.White, Color.White.copy(alpha = 0.20f), Color.White)
        } else when (hour) {
            // Madrugada (0..5h)
            in 0..5 -> {
                if (isDark) {
                    val b = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.tertiaryContainer,
                            colorScheme.surfaceContainerHighest
                        )
                    )
                    ThemeColors(b, colorScheme.onTertiaryContainer, colorScheme.tertiary, colorScheme.onTertiary)
                } else {
                    val b = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                            colorScheme.secondaryContainer
                        )
                    )
                    ThemeColors(b, colorScheme.onTertiaryContainer, colorScheme.tertiary, colorScheme.onTertiary)
                }
            }
            // Mañana (6..11h)
            in 6..11 -> {
                if (isDark) {
                    val b = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.primaryContainer,
                            colorScheme.surfaceContainerHighest
                        )
                    )
                    ThemeColors(b, colorScheme.onPrimaryContainer, colorScheme.primary, colorScheme.onPrimary)
                } else {
                    val b = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.primaryContainer,
                            colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        )
                    )
                    ThemeColors(b, colorScheme.onPrimaryContainer, colorScheme.primary, colorScheme.onPrimary)
                }
            }
            // Mediodía (12..13h)
            in 12..13 -> {
                if (isDark) {
                    val b = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.primaryContainer,
                            colorScheme.secondaryContainer
                        )
                    )
                    ThemeColors(b, colorScheme.onPrimaryContainer, colorScheme.primary, colorScheme.onPrimary)
                } else {
                    val b = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.primaryContainer,
                            colorScheme.surfaceTint.copy(alpha = 0.35f)
                        )
                    )
                    ThemeColors(b, colorScheme.onPrimaryContainer, colorScheme.primary, colorScheme.onPrimary)
                }
            }
            // Tarde (14..17h)
            in 14..17 -> {
                if (isDark) {
                    val b = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.secondaryContainer,
                            colorScheme.surfaceContainerHighest
                        )
                    )
                    ThemeColors(b, colorScheme.onSecondaryContainer, colorScheme.secondary, colorScheme.onSecondary)
                } else {
                    val b = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.secondaryContainer,
                            colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                    ThemeColors(b, colorScheme.onSecondaryContainer, colorScheme.secondary, colorScheme.onSecondary)
                }
            }
            // Atardecer (18..19h)
            in 18..19 -> {
                if (isDark) {
                    val b = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.tertiaryContainer,
                            colorScheme.secondaryContainer
                        )
                    )
                    ThemeColors(b, colorScheme.onTertiaryContainer, colorScheme.tertiary, colorScheme.onTertiary)
                } else {
                    val b = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.tertiaryContainer,
                            colorScheme.secondaryContainer
                        )
                    )
                    ThemeColors(b, colorScheme.onTertiaryContainer, colorScheme.tertiary, colorScheme.onTertiary)
                }
            }
            // Noche (20..23h)
            else -> {
                if (isDark) {
                    val b = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.surfaceContainerHighest,
                            colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        )
                    )
                    ThemeColors(b, colorScheme.onSurface, colorScheme.primary, colorScheme.onPrimary)
                } else {
                    val b = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.surfaceVariant,
                            colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        )
                    )
                    ThemeColors(b, colorScheme.onSurfaceVariant, colorScheme.primary, colorScheme.onPrimary)
                }
            }
        }
        HeroGreetingTheme(greetingStr, icon, colors.brush, colors.contentColor, colors.iconBgColor, colors.iconTint)
    }

    var infoCardType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(infoCardType) {
        if (infoCardType != null) {
            delay(4000)
            infoCardType = null
        }
    }

    Column(
        modifier = Modifier.animateContentSize(
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showGreetingCard) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(brush = heroTheme.brush)
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = heroTheme.greeting,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = heroTheme.contentColor,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.stats_music_unit),
                                style = MaterialTheme.typography.bodyMedium,
                                color = heroTheme.contentColor.copy(alpha = 0.75f)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = heroTheme.iconBgColor,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = heroTheme.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = heroTheme.iconTint
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(
                            icon = Icons.Default.History,
                            value = dailyListeningTimeStr,
                            contentColor = heroTheme.contentColor,
                            iconBgColor = heroTheme.iconBgColor,
                            modifier = Modifier.weight(1f),
                            onClick = { infoCardType = "time" }
                        )
                        StatChip(
                            icon = Icons.Default.MusicNote,
                            value = totalSongs.toString(),
                            contentColor = heroTheme.contentColor,
                            iconBgColor = heroTheme.iconBgColor,
                            modifier = Modifier.weight(1f),
                            onClick = { infoCardType = "songs" }
                        )
                        StatChip(
                            icon = Icons.Default.Favorite,
                            value = favoriteCount.toString(),
                            contentColor = heroTheme.contentColor,
                            iconBgColor = heroTheme.iconBgColor,
                            modifier = Modifier.weight(1f),
                            onClick = { infoCardType = "favorites" }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = infoCardType != null,
                enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                    slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { it / 2 },
                exit = fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                    slideOutVertically(animationSpec = tween(500, easing = FastOutSlowInEasing)) { it / 2 }
            ) {
                InfoCard(
                    type = infoCardType ?: "",
                    dailyListeningTimeStr = dailyListeningTimeStr,
                    totalSongs = totalSongs,
                    favoriteCount = favoriteCount
                )
            }
        }

        if (currentSong != null) {
            val pm = com.demonlab.lune.tools.PlaybackManager.getInstance(context)
            val cat = pm.activeCategory ?: ""
            val pId = pm.activePlaylistId
            val pName = pm.activePlaylistName
            val sourceLabel = remember(cat, pId, pName) {
                if (pId == -300L) {
                    context.getString(R.string.playing_from_search)
                } else if (cat == "ALL") {
                    context.getString(R.string.tab_all)
                } else if (cat == "FAVORITES") {
                    context.getString(R.string.tab_favorites)
                } else if (cat == "RESUME") {
                    context.getString(R.string.tab_resume)
                } else if (pName != null) {
                    "$cat: $pName"
                } else {
                    cat
                }
            }

            PlayingFromCard(
                sourceLabel = sourceLabel,
                hasBlurBackground = hasBlurBackground,
                isDarkTheme = isDarkTheme
            )
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

@Composable
private fun InfoCard(
    type: String,
    dailyListeningTimeStr: String,
    totalSongs: Int,
    favoriteCount: Int,
) {
    val context = LocalContext.current
    val (emoji, message) = remember(type, dailyListeningTimeStr, totalSongs, favoriteCount) {
        when (type) {
            "time" -> "🎉" to context.getString(R.string.info_card_time, dailyListeningTimeStr)
            "songs" -> "🎵" to context.getString(R.string.info_card_songs, totalSongs)
            "favorites" -> "⭐" to if (favoriteCount > 0) {
                context.getString(R.string.info_card_favorites_has, favoriteCount)
            } else {
                context.getString(R.string.info_card_favorites_none)
            }
            else -> "" to ""
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "emoji")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 34.sp,
                modifier = Modifier.graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationY = bounce
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    value: String,
    contentColor: Color,
    iconBgColor: Color = contentColor.copy(alpha = 0.18f),
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = iconBgColor.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f)),
        modifier = modifier
            .height(40.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                modifier = Modifier.size(46.dp)
            )
        }
    }
}

@Composable
private fun PlayingFromCard(
    sourceLabel: String,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
) {
    val cardBg = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    }
    val contentColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.playing_from, sourceLabel),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
