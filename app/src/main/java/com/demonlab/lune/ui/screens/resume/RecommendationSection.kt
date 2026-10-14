package com.demonlab.lune.ui.screens.resume

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import com.demonlab.lune.ui.utils.bounceClick
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationSection(
    title: String,
    songs: List<Song>,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0,
    onSongClick: (Song) -> Unit,
) {
    if (songs.isEmpty()) return

    val coroutineScope = rememberCoroutineScope()
    val carouselState = rememberCarouselState { songs.size }

    val activePrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)
    val playBtnBg = if (useCustomControlsColor) {
        activePrimary
    } else if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val playBtnTint = if (useCustomControlsColor) {
        if (activePrimary.luminance() > 0.45f) Color(0xFF141316) else Color.White
    } else if (hasBlurBackground) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    val playBtnBorder = if (hasBlurBackground) {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.40f))
    } else {
        null
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val screenWidth = maxWidth
            val preferredItemWidth = (screenWidth - 80.dp).coerceIn(240.dp, 520.dp)

            HorizontalMultiBrowseCarousel(
                state = carouselState,
                preferredItemWidth = preferredItemWidth,
                itemSpacing = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) { page ->
                val song = songs[page]
                val isSelected = carouselState.currentItem == page
                val contentAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    label = "recommendationContentAlpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(RoundedCornerShape(24.dp))
                        .clickable {
                            if (carouselState.currentItem != page) {
                                coroutineScope.launch {
                                    carouselState.animateScrollToItem(page)
                                }
                            }
                            onSongClick(song)
                        }
                ) {
                    // Full cover image filling the item
                    SongCoverImage(
                        coverUrl = song.coverUrl ?: song.albumArtUri,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(0.dp),
                        contentScale = ContentScale.Crop
                    )

                    // Overlay shown only when expanded / focused
                    if (contentAlpha > 0.02f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = contentAlpha }
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.25f),
                                            Color.Black.copy(alpha = 0.78f),
                                            Color.Black.copy(alpha = 0.94f)
                                        ),
                                        startY = 40f
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth(0.78f)
                            ) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = Color.White,
                                    modifier = Modifier.basicMarquee()
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    color = Color.White.copy(alpha = 0.82f),
                                    modifier = Modifier.basicMarquee()
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = playBtnBg,
                                border = playBtnBorder,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(44.dp)
                                    .bounceClick()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = playBtnTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    hasBlurBackground: Boolean = false
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
    )
}
