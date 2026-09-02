package com.demonlab.lune.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.demonlab.lune.ui.utils.bounceClick
import androidx.compose.ui.draw.blur
import com.demonlab.lune.R
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.utils.formatDuration
import com.demonlab.lune.ui.utils.formatDurationCompact
import com.demonlab.lune.ui.utils.formatLongDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppBlurBackdrop(
    hasBlurBackground: Boolean,
    isDarkTheme: Boolean,
    currentSong: Song? = null,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val baseBgColor = if (isDarkTheme) Color(0xFF141416) else MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (hasBlurBackground && currentSong != null) baseBgColor else MaterialTheme.colorScheme.surface)
    ) {
        if (hasBlurBackground && currentSong != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(80.dp)
                    .alpha(if (isDarkTheme) 0.35f else 0.45f)
            ) {
                val req = remember(currentSong.id, currentSong.coverUrl) {
                    coil.request.ImageRequest.Builder(context)
                        .data(currentSong.coverUrl ?: currentSong.albumArtUri ?: currentSong.uri)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = req,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = if (isDarkTheme) 0.50f else 0.20f))
            )
        }
        content()
    }
}

data class BlurSheetColors(
    val hasBlur: Boolean,
    val isDark: Boolean,
    val containerColor: Color,
    val itemContainerColor: Color,
    val textColor: Color,
    val textSecondaryColor: Color,
    val primaryTint: Color,
    val itemBorderColor: Color? = null,
    val useCustomControlsColor: Boolean = false,
    val controlsColorPalette: Int = 0
)

@Composable
fun rememberBlurSheetColors(currentSong: Song? = null): BlurSheetColors {
    val context = LocalContext.current
    val settingsManager = SettingsManager.getInstance(context)
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val hasBlur = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))
    val useCustomControlsColor = settingsManager.useCustomControlsColor
    val controlsColorPalette = settingsManager.controlsColorPalette
    val primaryTint = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)

    return if (hasBlur) {
        BlurSheetColors(
            hasBlur = true,
            isDark = isDarkTheme,
            containerColor = if (currentSong != null) Color.Transparent else (if (isDarkTheme) Color(0xFF1C1C1E).copy(alpha = 0.96f) else Color(0xFFF2F2F7).copy(alpha = 0.96f)),
            itemContainerColor = Color.White.copy(alpha = 0.12f),
            textColor = Color.White,
            textSecondaryColor = Color.White.copy(alpha = 0.70f),
            primaryTint = if (useCustomControlsColor) primaryTint else Color.White,
            itemBorderColor = Color.White.copy(alpha = 0.08f),
            useCustomControlsColor = useCustomControlsColor,
            controlsColorPalette = controlsColorPalette
        )
    } else {
        BlurSheetColors(
            hasBlur = false,
            isDark = isDarkTheme,
            containerColor = MaterialTheme.colorScheme.surface,
            itemContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            textColor = MaterialTheme.colorScheme.onSurface,
            textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant,
            primaryTint = if (useCustomControlsColor) primaryTint else MaterialTheme.colorScheme.primary,
            itemBorderColor = null,
            useCustomControlsColor = useCustomControlsColor,
            controlsColorPalette = controlsColorPalette
        )
    }
}

@Composable
fun ResponsiveText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    targetTextSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1
) {
    var textSize by remember(text) { mutableStateOf(targetTextSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        color = color,
        fontWeight = fontWeight,
        fontSize = textSize,
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow && textSize.value > 10f) {
                textSize = (textSize.value * 0.9f).sp
            } else {
                readyToDraw = true
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayingSongDiamondsIndicator(
    isPlaying: Boolean,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PlayingIndicatorRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    Box(
        modifier = Modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_logo_diamonds),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .size(60.dp)
                .then(if (isPlaying) Modifier.rotate(rotation) else Modifier)
        )
        Icon(
            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItem(
    song: Song,
    currentlyPlaying: Boolean,
    isPlaying: Boolean = false,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    hasBlurBackground: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onOptionsClick: (() -> Unit)? = null,
    onFavoriteClick: ((Song) -> Unit)? = null
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val themeMode = settingsManager.themeMode
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }

    val activePrimary = if (hasBlurBackground && !useCustomControlsColor) Color.White else com.demonlab.lune.ui.theme.getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)

    val shape = if (isFirst && isLast) {
        RoundedCornerShape(28.dp)
    } else if (isFirst) {
        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    } else if (isLast) {
        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
    } else {
        RoundedCornerShape(4.dp)
    }

    val cardBg = if (hasBlurBackground) {
        if (currentlyPlaying) {
            if (useCustomControlsColor) activePrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.20f)
        } else {
            Color.White.copy(alpha = 0.10f)
        }
    } else {
        if (currentlyPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    }

    val itemBorder = if (hasBlurBackground && currentlyPlaying) {
        BorderStroke(1.dp, (if (useCustomControlsColor) activePrimary else Color.White).copy(alpha = 0.45f))
    } else null

    val titleColor = if (currentlyPlaying) activePrimary else if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (currentlyPlaying) activePrimary.copy(alpha = 0.85f) else if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val bitrateColor = if (hasBlurBackground) Color.White.copy(alpha = 0.60f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val favoriteIconTint = if (song.isFavorite) activePrimary else if (hasBlurBackground) Color.White.copy(alpha = 0.70f) else MaterialTheme.colorScheme.onSurfaceVariant
    val optionsBg = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val optionsTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 1.dp)
            .bounceClick(scaleDown = 0.96f),
        onClick = onClick ?: {},
        shape = shape,
        color = cardBg,
        border = itemBorder
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            supportingContent = {
                if (settingsManager.isBitrateOnList && (song.bitrate != null || song.format.isNotEmpty())) {
                    Column {
                        Text(
                            "${formatDuration(song.duration)} • ${song.artist}",
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = subtitleColor
                        )
                        val bitrateText = if (song.bitrate != null) "${song.format} | ${song.bitrate / 1000}kbps" else song.format
                        Text(
                            bitrateText,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall,
                            color = bitrateColor
                        )
                    }
                } else {
                    Text(
                        "${formatDuration(song.duration)} • ${song.artist}",
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = subtitleColor
                    )
                }
            },
            leadingContent = {
                Box(contentAlignment = Alignment.Center) {
                    SongCoverImage(
                        coverUrl = song.coverUrl ?: song.uri,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        shape = MaterialTheme.shapes.medium
                    )
                    if (currentlyPlaying) {
                        PlayingSongDiamondsIndicator(isPlaying = isPlaying, tint = activePrimary)
                    }
                }
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val favBg = if (song.isFavorite) {
                        if (hasBlurBackground) Color.White.copy(alpha = 0.28f) else (if (useCustomControlsColor) activePrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    } else {
                        if (hasBlurBackground) Color.White.copy(alpha = 0.10f) else Color.Transparent
                    }
                    val favTint = if (song.isFavorite) {
                        if (hasBlurBackground) Color.White else (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primary)
                    } else {
                        favoriteIconTint
                    }

                    Surface(
                        onClick = { onFavoriteClick?.invoke(song) },
                        shape = CircleShape,
                        color = favBg,
                        modifier = Modifier
                            .size(32.dp)
                            .bounceClick(scaleDown = 0.80f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(R.string.option_favorite),
                                tint = favTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onOptionsClick ?: {},
                        modifier = Modifier.size(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = optionsBg,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = stringResource(R.string.player_options),
                                    tint = optionsTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.isHiRes) {
                    val hiResBg = if (hasBlurBackground) Color(0xFF2E2400).copy(alpha = 0.70f) else Color(0xFF2E2400)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = hiResBg,
                        border = BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
                        )
                    ) {
                        Text(
                            "Hi-Res",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (song.isHiFi) {
                    val hifiBg = if (hasBlurBackground) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.tertiaryContainer
                    val hifiTextColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onTertiaryContainer
                    val hifiBorder = if (hasBlurBackground) BorderStroke(0.8.dp, Color.White.copy(alpha = 0.35f)) else null
                    Surface(
                        color = hifiBg,
                        border = hifiBorder,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "HI-FI",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = hifiTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    song.title,
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false).basicMarquee(),
                    color = titleColor
                )
            }
        }
    }
}

@Composable
fun SongGridItem(
    song: Song,
    currentlyPlaying: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    hasBlurBackground: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0,
    onOptionsClick: (() -> Unit)? = null,
    onFavoriteClick: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activePrimary = com.demonlab.lune.ui.theme.getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)
    val cardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    val coverShape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick()
            .clip(cardShape)
            .clickable(onClick = onClick)
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth(),
                shape = coverShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                SongCoverImage(
                    coverUrl = song.coverUrl ?: song.albumArtUri ?: song.uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    shape = coverShape,
                    iconScale = 0.68f
                )
            }

            if (currentlyPlaying) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f), coverShape),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = activePrimary,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            if (song.isHiRes) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (hasBlurBackground) Color(0xFF2E2400).copy(alpha = 0.75f) else Color(0xFF2E2400).copy(alpha = 0.9f),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        "Hi-Res",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Black
                    )
                }
            } else if (song.isHiFi) {
                val hifiBg = if (hasBlurBackground) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                val hifiTextColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onTertiaryContainer
                val hifiBorder = if (hasBlurBackground) BorderStroke(0.8.dp, Color.White.copy(alpha = 0.40f)) else null
                Surface(
                    color = hifiBg,
                    border = hifiBorder,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        "HI-FI",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = hifiTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (onFavoriteClick != null) {
                val favBg = if (song.isFavorite) {
                    if (hasBlurBackground) Color.White.copy(alpha = 0.32f) else (if (useCustomControlsColor) activePrimary.copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.55f))
                } else {
                    if (hasBlurBackground) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.45f)
                }
                Surface(
                    onClick = { onFavoriteClick(song) },
                    shape = CircleShape,
                    color = favBg,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .bounceClick(scaleDown = 0.80f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.option_favorite),
                            tint = if (song.isFavorite) (if (hasBlurBackground) Color.White else activePrimary) else Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            if (currentlyPlaying) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(activePrimary, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                color = if (currentlyPlaying) activePrimary else if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (onOptionsClick != null) {
                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.player_options),
                        tint = if (hasBlurBackground) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun Modifier.headerWaveBackground(
    strokeWidth: androidx.compose.ui.unit.Dp = 1.2.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    hasBlurBackground: Boolean = false,
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "HeaderWaveTransition")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase2"
    )

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    return this.drawWithCache {
        val strokePx = strokeWidth.toPx()
        val radiusPx = cornerRadius.toPx()

        val clipPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
            )
        }

        val wavePath1 = Path()
        val wavePath2 = Path()

        val baseH = 22.dp.toPx()
        val amp1 = 7.dp.toPx()
        val amp2 = 9.dp.toPx()

        val waveBrush1 = if (hasBlurBackground) {
            Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.11f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(
                    primary.copy(alpha = 0.30f),
                    tertiary.copy(alpha = 0.40f),
                    secondary.copy(alpha = 0.35f),
                    primary.copy(alpha = 0.30f)
                )
            )
        }

        val waveBrush2 = if (hasBlurBackground) {
            Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.24f),
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.10f)
                )
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(
                    primaryContainer.copy(alpha = 0.40f),
                    primary.copy(alpha = 0.55f),
                    tertiary.copy(alpha = 0.45f),
                    primaryContainer.copy(alpha = 0.38f)
                )
            )
        }

        val borderBrush = if (hasBlurBackground) {
            Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.32f),
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.18f)
                )
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(
                    primary.copy(alpha = 0.35f),
                    tertiary.copy(alpha = 0.50f),
                    primaryContainer.copy(alpha = 0.45f),
                    primary.copy(alpha = 0.35f)
                )
            )
        }

        onDrawWithContent {
            val w = size.width
            val h = size.height

            // Wave 1: Back wave layer with gentle undulating slopes
            wavePath1.reset()
            wavePath1.moveTo(0f, h)
            val wl1 = w * 1.15f
            var x = 0f
            val step = 8f
            while (x <= w) {
                val y = h - (baseH + amp1 * kotlin.math.sin(2 * Math.PI * (x / wl1) + phase1).toFloat())
                wavePath1.lineTo(x, y)
                x += step
            }
            val endY1 = h - (baseH + amp1 * kotlin.math.sin(2 * Math.PI * (w / wl1) + phase1).toFloat())
            wavePath1.lineTo(w, endY1)
            wavePath1.lineTo(w, h)
            wavePath1.close()

            // Wave 2: Front wave layer overlapping Wave 1
            wavePath2.reset()
            wavePath2.moveTo(0f, h)
            val wl2 = w * 0.88f
            x = 0f
            while (x <= w) {
                val y = h - ((baseH * 0.88f) + amp2 * kotlin.math.sin(2 * Math.PI * (x / wl2) + phase2).toFloat())
                wavePath2.lineTo(x, y)
                x += step
            }
            val endY2 = h - ((baseH * 0.88f) + amp2 * kotlin.math.sin(2 * Math.PI * (w / wl2) + phase2).toFloat())
            wavePath2.lineTo(w, endY2)
            wavePath2.lineTo(w, h)
            wavePath2.close()

            // 1. Draw waves in the BACKGROUND (behind the content, clipped to rounded corners)
            clipPath(clipPath) {
                drawPath(path = wavePath1, brush = waveBrush1)
                drawPath(path = wavePath2, brush = waveBrush2)
            }

            // 2. Draw content ON TOP of the waves!
            // Icons, text, options and buttons are drawn pristine, 100% uncovered
            drawContent()

            // 3. Sleek subtle border stroke framing the header
            drawRoundRect(
                brush = borderBrush,
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size(w - strokePx, h - strokePx),
                cornerRadius = CornerRadius((radiusPx - strokePx / 2f).coerceAtLeast(0f)),
                style = Stroke(width = strokePx)
            )
        }
    }
}

@Composable
fun Modifier.headerWaveBorder(
    strokeWidth: androidx.compose.ui.unit.Dp = 1.2.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    hasBlurBackground: Boolean = false,
): Modifier = headerWaveBackground(strokeWidth, cornerRadius, hasBlurBackground)

@Composable
fun HeaderSurface(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    hasBlurBackground: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var isWaveEnabled by remember { mutableStateOf(settingsManager.isHeaderWaveEffectEnabled) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isWaveEnabled = settingsManager.isHeaderWaveEffectEnabled
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = if (hasBlurBackground) Color.Black.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (!isWaveEnabled && hasBlurBackground) BorderStroke(1.2.dp, Color.White.copy(alpha = 0.16f)) else null,
        tonalElevation = if (hasBlurBackground) 0.dp else 4.dp,
        shadowElevation = 0.dp
    ) {
        if (isWaveEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .headerWaveBackground(cornerRadius = cornerRadius, hasBlurBackground = hasBlurBackground)
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
fun AlbumsListHeader(
    albumCount: Int,
    viewStyle: Int,
    onToggleViewStyle: () -> Unit,
    isAlbumView: Boolean,
    onToggleAlbumView: (() -> Unit)? = null,
    title: String? = null,
    icon: ImageVector? = null,
    hasBlurBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayTitle = title ?: if (isAlbumView) stringResource(R.string.tab_albums_real) else stringResource(R.string.tab_artists)
    val displayIcon = icon ?: if (isAlbumView) Icons.Default.Album else Icons.Default.Person

    val iconContainerBg = if (hasBlurBackground) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
    val iconTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
    val titleColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
    val countColor = if (hasBlurBackground) Color.White.copy(alpha = 0.80f) else MaterialTheme.colorScheme.onSurfaceVariant
    val actionBtnBg = if (hasBlurBackground) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
    val actionBtnTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = { onToggleAlbumView?.invoke() },
                enabled = onToggleAlbumView != null,
                shape = RoundedCornerShape(12.dp),
                color = iconContainerBg,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        displayIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                Text(
                    text = albumCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = countColor
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onToggleAlbumView != null) {
                Surface(
                    onClick = onToggleAlbumView,
                    shape = CircleShape,
                    color = if (isAlbumView) MaterialTheme.colorScheme.primary else actionBtnBg,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isAlbumView) Icons.Default.Person else Icons.Default.Album,
                            contentDescription = null,
                            tint = if (isAlbumView) MaterialTheme.colorScheme.onPrimary else actionBtnTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Surface(
                onClick = onToggleViewStyle,
                shape = CircleShape,
                color = actionBtnBg,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (viewStyle == 0) Icons.Default.ViewCarousel else Icons.Default.GridView,
                        contentDescription = "Toggle View Style",
                        tint = actionBtnTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SongsListHeader(
    songs: List<Song>,
    isShuffleActive: Boolean,
    isCurrentListPlaying: Boolean,
    isSortActive: Boolean,
    onSortClick: () -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier,
    folderName: String = "",
    isGridLayout: Boolean = false,
    hasBlurBackground: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0,
    onToggleLayout: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val isPlaying = playbackManager.isPlaying

    val activePrimary = com.demonlab.lune.ui.theme.getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)

    val iconContainerBg = if (hasBlurBackground) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
    val iconTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
    val titleColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (hasBlurBackground) Color.White.copy(alpha = 0.80f) else MaterialTheme.colorScheme.onSurfaceVariant
    val actionBtnInactiveBg = if (hasBlurBackground) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
    val actionBtnInactiveTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
    val actionBtnActiveBg = if (useCustomControlsColor) activePrimary else if (hasBlurBackground) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary
    val actionBtnActiveTint = Color.White

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val leftModifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
        if (folderName == "FAVORITES") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = leftModifier) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconContainerBg,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.tab_favorites),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val totalDuration = songs.sumOf { it.duration }
                    Text(
                        text = "${songs.size} · ${formatDurationCompact(totalDuration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 1
                    )
                }
            }
        } else if (folderName == "ALL") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = leftModifier) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconContainerBg,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.tab_all),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val totalDuration = songs.sumOf { it.duration }
                    Text(
                        text = "${songs.size} · ${formatDurationCompact(totalDuration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 1
                    )
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.Start, modifier = leftModifier) {
                val totalDuration = songs.sumOf { it.duration }
                Text(
                    text = formatLongDuration(totalDuration),
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = iconTint
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val songsLabel = if (songs.size == 1) stringResource(R.string.song_singular) else stringResource(R.string.song_plural)
                    Text(
                        text = "${songs.size} $songsLabel",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (onToggleLayout != null) {
                Surface(
                    onClick = onToggleLayout,
                    shape = CircleShape,
                    color = if (isGridLayout) actionBtnActiveBg else actionBtnInactiveBg,
                    modifier = Modifier
                        .size(36.dp)
                        .bounceClick()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isGridLayout) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                            contentDescription = "Toggle Layout",
                            tint = if (isGridLayout) actionBtnActiveTint else actionBtnInactiveTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Surface(
                onClick = onSortClick,
                shape = CircleShape,
                color = if (isSortActive) actionBtnActiveBg else actionBtnInactiveBg,
                modifier = Modifier
                    .size(36.dp)
                    .bounceClick()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSortActive) Icons.Default.Schedule else Icons.Default.SortByAlpha,
                        contentDescription = null,
                        tint = if (isSortActive) actionBtnActiveTint else actionBtnInactiveTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onShuffleClick,
                    shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                    color = if (isShuffleActive) actionBtnActiveBg else actionBtnInactiveBg,
                    modifier = Modifier
                        .size(44.dp)
                        .bounceClick()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = if (isShuffleActive) actionBtnActiveTint else actionBtnInactiveTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Surface(
                    onClick = onPlayClick,
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp),
                    color = actionBtnActiveBg,
                    modifier = Modifier
                        .size(44.dp)
                        .bounceClick()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isCurrentListPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = actionBtnActiveTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FolderFilterContent(
    allFolders: List<String>,
    hiddenFolders: MutableState<Set<String>>,
    selectedFolder: String,
    onSelectedFolderChange: (String) -> Unit
) {
    val context = LocalContext.current
    val settingsManager = SettingsManager.getInstance(context)
    val blurColors = rememberBlurSheetColors()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(R.string.filter_folders),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = blurColors.textColor,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        )

        allFolders.forEachIndexed { index, folder ->
            val isHidden = hiddenFolders.value.contains(folder)
            val isFirst = index == 0
            val isLast = index == allFolders.lastIndex
            val shape = when {
                allFolders.size == 1 -> RoundedCornerShape(28.dp)
                isFirst -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                isLast -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                else -> RoundedCornerShape(4.dp)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 1.dp)
                    .bounceClick(),
                shape = shape,
                color = blurColors.itemContainerColor,
                border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) }
            ) {
                ListItem(
                    trailingContent = {
                        BouncySwitch(
                            checked = !isHidden,
                            onCheckedChange = { isVisible ->
                                val newHidden = hiddenFolders.value.toMutableSet()
                                if (isVisible) newHidden.remove(folder) else newHidden.add(folder)
                                hiddenFolders.value = newHidden
                                settingsManager.hiddenFolders = newHidden

                                // If we hidden current selected folder, reset to "Todo"
                                if (!isVisible && selectedFolder == folder) {
                                    onSelectedFolderChange("ALL")
                                }
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (!isHidden) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) {
                    Text(folder, color = blurColors.textColor)
                }
            }
        }
    }
}

@Composable
fun WaveformVisualizer(
    modifier: Modifier = Modifier,
    magnitudes: FloatArray,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        magnitudes.forEach { magnitude ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(magnitude)
                    .background(color, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OptionButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    sublabel: String? = null,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val blurColors = rememberBlurSheetColors()
    val activeBg = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary
    val inactiveBg = if (blurColors.hasBlur) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
    val activeTint = if (blurColors.hasBlur) (if (blurColors.isDark) Color.Black else Color.White) else MaterialTheme.colorScheme.onPrimary
    val inactiveTint = if (blurColors.hasBlur) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
    val labelColor = if (blurColors.hasBlur) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val sublabelBg = if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer
    val sublabelColor = if (blurColors.hasBlur) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.bounceClick()
    ) {
        Surface(
            shape = CircleShape,
            color = if (active) activeBg else inactiveBg,
            modifier = Modifier
                .size(56.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .clip(CircleShape)
                .then(
                    if (onLongClick != null) {
                        Modifier.combinedClickable(
                            enabled = enabled,
                            onClick = onClick,
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongClick()
                            }
                        )
                    } else {
                        Modifier.clickable(enabled = enabled, onClick = onClick)
                    }
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (active) activeTint else inactiveTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor
        )
        if (sublabel != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = sublabelBg
            ) {
                Text(
                    sublabel,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = sublabelColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ScrollToCurrentButton(
    listState: LazyListState,
    targetIndex: Int,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }

    val isVisible by remember(targetIndex) {
        derivedStateOf {
            if (targetIndex == -1) false
            else {
                val layoutInfo = listState.layoutInfo
                val visibleIndices = layoutInfo.visibleItemsInfo.map { it.index }
                !visibleIndices.contains(targetIndex)
            }
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            isExpanded = true
            delay(4000)
            isExpanded = false
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Surface(
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(targetIndex)
                }
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            tonalElevation = 8.dp,
            modifier = Modifier
                .height(56.dp)
                .shadow(8.dp, CircleShape)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = if (isExpanded && !label.isNullOrEmpty()) 20.dp else 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                if (isExpanded && !label.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun VinylRecordAsyncCover(
    model: Any?,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .rotate(rotation)
            .clip(CircleShape)
            .background(Color(0xFF101010)),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize(0.9f).border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape))
        Box(modifier = Modifier.fillMaxSize(0.8f).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape))
        Box(modifier = Modifier.fillMaxSize(0.7f).border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape))
        Box(modifier = Modifier.fillMaxSize(0.6f).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape))

        Surface(
            shape = CircleShape,
            modifier = Modifier.fillMaxSize(0.55f),
            border = BorderStroke(2.dp, Color(0xFF202020))
        ) {
            SongCoverImage(
                coverUrl = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                iconScale = 0.65f
            )
        }

        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Color(0xFF101010))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        )
    }
}

@Composable
fun BouncySwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors? = null,
    thumbContent: @Composable (() -> Unit)? = null
) {
    val scale = remember { Animatable(initialValue = 1f) }
    val context = LocalContext.current
    val settingsManager = remember { com.demonlab.lune.tools.SettingsManager.getInstance(context) }
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))

    val switchColors = colors ?: if (hasBlurBackground) {
        SwitchDefaults.colors(
            checkedThumbColor = Color.Black,
            checkedTrackColor = Color.White,
            checkedBorderColor = Color.White,
            checkedIconColor = Color.White,
            uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
            uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
            uncheckedBorderColor = Color.White.copy(alpha = 0.35f),
            uncheckedIconColor = Color.Black
        )
    } else {
        SwitchDefaults.colors(
            checkedIconColor = Color.White,
            checkedThumbColor = MaterialTheme.colorScheme.primary,
            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
        )
    }

    LaunchedEffect(checked) {
        scale.snapTo(1f)
        scale.animateTo(
            targetValue = 1.12f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.graphicsLayer(scaleX = scale.value, scaleY = scale.value),
        enabled = enabled,
        colors = switchColors,
        thumbContent = thumbContent
    )
}

@Composable
fun SongCoverImage(
    coverUrl: Any?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
    contentScale: ContentScale = ContentScale.Crop,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    iconScale: Float = 0.68f,
    onError: (() -> Unit)? = null
) {
    var isError by remember(coverUrl) { mutableStateOf(coverUrl == null) }

    LaunchedEffect(coverUrl) {
        if (coverUrl == null) {
            onError?.invoke()
        }
    }

    Surface(
        shape = shape,
        color = backgroundColor,
        modifier = modifier
    ) {
        if (coverUrl != null && !isError) {
            AsyncImage(
                model = coverUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onError = {
                    isError = true
                    onError?.invoke()
                }
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.fillMaxSize(iconScale)
                )
            }
        }
    }
}
