package com.demonlab.lune.ui.screens.resume

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.utils.bounceClick

data class ArtistItem(
    val name: String,
    val playCount: Int,
    val coverUrl: String?,
    val songs: List<Song>
)

@Composable
fun TopArtistsSection(
    artists: List<ArtistItem>,
    hasBlurBackground: Boolean = false,
    onArtistClick: (String) -> Unit,
) {
    if (artists.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        SectionHeader(
            title = stringResource(R.string.resume_top_artists),
            hasBlurBackground = hasBlurBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(artists, key = { it.name }) { artist ->
                ArtistCircleCard(
                    artist = artist,
                    onClick = { onArtistClick(artist.name) }
                )
            }
        }
    }
}

@Composable
private fun ArtistCircleCard(
    artist: ArtistItem,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(120.dp)
            .bounceClick()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            SongCoverImage(
                coverUrl = artist.coverUrl,
                contentDescription = artist.name,
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                iconScale = 0.65f
            )
        }
    }
}
