package com.demonlab.lune.ui.data

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.utils.fastSmoothScrollToItem
import com.demonlab.lune.R
import com.demonlab.lune.tools.CategoryCoverManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.components.ScrollToTopPill
import com.demonlab.lune.ui.components.rememberScrollToTopVisibility
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val albumArtUri: Uri?,
    val coverUrl: String?,
    val songs: List<Song>
)

@Composable
fun AlbumGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    bottomPadding: Dp,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    customActiveColor: Color? = null,
    activePlaylistId: Long? = null,
    category: String = "ALBUMS"
) {
    val initialIndex = remember(activePlaylistId, albums) {
        if (activePlaylistId != null) {
            val idx = albums.indexOfFirst { it.id == activePlaylistId }
            if (idx >= 0) idx else 0
        } else 0
    }
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    val isScrollToTopVisible = rememberScrollToTopVisibility(gridState, settingsManager.isScrollToTopEnabled)

    LaunchedEffect(activePlaylistId) {
        if (initialIndex > 0) {
            gridState.scrollToItem(initialIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding + 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                albums,
                key = { _, album -> album.id },
                contentType = { _, _ -> "album_card" }
            ) { index, album ->
                val isFirst = index == 0
                val isLast = index == albums.lastIndex
                val isPlaying = album.id == activePlaylistId
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album) },
                    hasBlurBackground = hasBlurBackground,
                    isPlaying = isPlaying,
                    category = category
                )
            }
        }

        ScrollToTopPill(
            visible = isScrollToTopVisible.value,
            onClick = {
                scope.launch {
                    gridState.fastSmoothScrollToItem(0)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 14.dp),
            hasBlurBackground = hasBlurBackground,
            isDarkTheme = isDarkTheme,
            customActiveColor = customActiveColor
        )
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    hasBlurBackground: Boolean = false,
    isPlaying: Boolean = false,
    category: String = "ALBUMS"
) {
    val context = LocalContext.current
    val categoryUpdates by CategoryCoverManager.updates.collectAsState()
    val customCovers = remember(album.name, category, categoryUpdates) {
        CategoryCoverManager.getCustomCovers(context, category, album.name)
    }

    val activeCovers = remember(customCovers, album.coverUrl, album.albumArtUri) {
        if (customCovers.isNotEmpty()) customCovers else listOfNotNull(album.coverUrl ?: album.albumArtUri)
    }

    var currentCoverIndex by remember(activeCovers) { mutableIntStateOf(0) }

    LaunchedEffect(activeCovers) {
        if (activeCovers.size > 1) {
            while (isActive) {
                delay(6000L)
                val nextIndex = if (activeCovers.size == 2) {
                    1 - currentCoverIndex
                } else {
                    var candidate = Random.nextInt(activeCovers.size)
                    while (candidate == currentCoverIndex) {
                        candidate = Random.nextInt(activeCovers.size)
                    }
                    candidate
                }
                currentCoverIndex = nextIndex
            }
        }
    }

    val currentCover = activeCovers.getOrNull(currentCoverIndex)

    val cardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    val coverShape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
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
                Crossfade(
                    targetState = currentCover,
                    animationSpec = tween(durationMillis = 600),
                    label = "albumCardCoverCrossfade"
                ) { cover ->
                    SongCoverImage(
                        coverUrl = cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        shape = coverShape,
                        iconScale = 0.68f
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}
