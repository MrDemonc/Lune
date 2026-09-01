package com.demonlab.lune.ui.screens.resume

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import com.demonlab.lune.R
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.utils.formatDuration

@Composable
fun RecentlyAddedSection(
    songs: List<Song>,
    hasBlurBackground: Boolean = false,
    onSongClick: (Song) -> Unit,
) {
    if (songs.isEmpty()) return

    Column {
        SectionHeader(
            title = stringResource(R.string.resume_recently_added),
            hasBlurBackground = hasBlurBackground
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            itemsIndexed(songs.take(5), key = { _, s -> s.id }) { _, song ->
                RecentlyAddedRow(
                    song = song,
                    hasBlurBackground = hasBlurBackground,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun RecentlyAddedRow(
    song: Song,
    hasBlurBackground: Boolean = false,
    onClick: () -> Unit,
) {
    val titleColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
    val artistColor = if (hasBlurBackground) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val durationColor = if (hasBlurBackground) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val rowBg = if (hasBlurBackground) Color.Black.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = rowBg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            SongCoverImage(
                coverUrl = song.coverUrl ?: song.albumArtUri,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
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
            Text(
                text = formatDuration(song.duration),
                style = MaterialTheme.typography.labelSmall,
                color = durationColor
            )
        }
    }
}


