package com.demonlab.lune.ui.data

import android.net.Uri
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
import com.demonlab.lune.R
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.SongCoverImage

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
    activePlaylistId: Long? = null
) {
    val initialIndex = remember(activePlaylistId, albums) {
        if (activePlaylistId != null) {
            val idx = albums.indexOfFirst { it.id == activePlaylistId }
            if (idx >= 0) idx else 0
        } else 0
    }
    val gridState = rememberLazyGridState()

    LaunchedEffect(activePlaylistId) {
        if (initialIndex > 0) {
            gridState.scrollToItem(initialIndex)
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding + 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(albums) { index, album ->
            val isFirst = index == 0
            val isLast = index == albums.lastIndex
            val isPlaying = album.id == activePlaylistId
            AlbumCard(album = album, onClick = { onAlbumClick(album) }, isPlaying = isPlaying)
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit, isPlaying: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                SongCoverImage(
                    coverUrl = album.coverUrl ?: album.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    iconScale = 0.68f
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
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
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
