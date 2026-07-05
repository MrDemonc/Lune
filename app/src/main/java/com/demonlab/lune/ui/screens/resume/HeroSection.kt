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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun HeroSection(
    currentSong: Song?,
    isPlaying: Boolean,
    dailyListeningTimeStr: String,
    totalSongs: Int,
    playlistsCount: Int,
    favoriteCount: Int,
    topArtist: String,
    onContinueListening: () -> Unit,
    onPlayToggle: () -> Unit,
) {
    val context = LocalContext.current
    val (greeting, timeIcon, solidColor) = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
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
        val color = when (hour) {
            in 0..5 -> Color(0xFF1A1A2E)
            in 6..11 -> Color(0xFF29B6F6)
            in 12..13 -> Color(0xFF4FC3F7)
            in 14..17 -> Color(0xFFFBC02D)
            in 18..19 -> Color(0xFF3949AB)
            else -> Color(0xFF121212)
        }
        Triple(greeting, icon, color)
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(color = solidColor)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.stats_music_unit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = timeIcon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
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
                        modifier = Modifier.weight(1f),
                        onClick = { infoCardType = "time" }
                    )
                    StatChip(
                        icon = Icons.Default.MusicNote,
                        value = totalSongs.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = { infoCardType = "songs" }
                    )
                    StatChip(
                        icon = Icons.Default.Favorite,
                        value = favoriteCount.toString(),
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
                } else if (pName != null) {
                    "$cat: $pName"
                } else {
                    cat
                }
            }
            PlayingFromCard(sourceLabel = sourceLabel)
            ContinueListeningCard(
                song = currentSong,
                isPlaying = isPlaying,
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.15f),
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
                tint = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
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
    onClick: () -> Unit,
    onPlayToggle: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            AsyncImage(
                model = song.coverUrl ?: song.uri,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = onPlayToggle,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun PlayingFromCard(
    sourceLabel: String,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
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
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.playing_from, sourceLabel),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
