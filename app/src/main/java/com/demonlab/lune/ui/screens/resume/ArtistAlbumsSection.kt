package com.demonlab.lune.ui.screens.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.data.Album
import com.demonlab.lune.ui.utils.bounceClick

data class ArtistAlbumsSectionData(
    val artistName: String,
    val coverUrl: Any?,
    val albums: List<Album>
)

@Composable
fun ArtistAlbumsSection(
    section: ArtistAlbumsSectionData,
    currentSong: Song? = null,
    hasBlurBackground: Boolean = false,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    if (section.albums.isEmpty()) return

    Column(
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        // Section Header: Circular artist photo/cover + artist name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onArtistClick(section.artistName) }
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(42.dp)
                    .bounceClick()
            ) {
                SongCoverImage(
                    coverUrl = section.coverUrl,
                    contentDescription = section.artistName,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    iconScale = 0.65f
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = section.artistName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (hasBlurBackground) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal scroll with available albums of the artist
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            items(section.albums, key = { it.id }) { album ->
                val isPlaying = currentSong?.album?.hashCode()?.toLong() == album.id
                ArtistAlbumCard(
                    album = album,
                    isPlaying = isPlaying,
                    hasBlurBackground = hasBlurBackground,
                    onClick = { onAlbumClick(album) }
                )
            }
        }
    }
}

@Composable
private fun ArtistAlbumCard(
    album: Album,
    isPlaying: Boolean,
    hasBlurBackground: Boolean,
    onClick: () -> Unit
) {
    val coverShape = RoundedCornerShape(18.dp)
    val titleColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (hasBlurBackground) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .width(136.dp)
            .bounceClick()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(136.dp),
            shape = coverShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp
        ) {
            SongCoverImage(
                coverUrl = album.coverUrl ?: album.albumArtUri,
                contentDescription = album.name,
                modifier = Modifier.fillMaxSize(),
                shape = coverShape,
                iconScale = 0.65f
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = titleColor
            )
        }

        Text(
            text = stringResource(R.string.card_artist_songs, album.songs.size),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = subColor,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}
