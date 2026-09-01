package com.demonlab.lune.ui.screens.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.utils.bounceClick

data class GenreItem(
    val name: String,
    val songCount: Int,
    val coverUrl: String?,
    val songs: List<Song>
)

@Composable
fun TopGenresSection(
    genres: List<GenreItem>,
    hasBlurBackground: Boolean = false,
    onGenreClick: (String) -> Unit,
) {
    if (genres.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        SectionHeader(
            title = stringResource(R.string.resume_top_genres),
            hasBlurBackground = hasBlurBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(genres, key = { it.name }) { genre ->
                GenreCard(
                    genre = genre,
                    onClick = { onGenreClick(genre.name) }
                )
            }
        }
    }
}

@Composable
private fun GenreCard(
    genre: GenreItem,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .width(160.dp)
            .height(110.dp)
            .bounceClick()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SongCoverImage(
                coverUrl = genre.coverUrl,
                contentDescription = genre.name,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.88f)
                            ),
                            startY = 0f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = genre.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${genre.songCount} ${stringResource(R.string.tab_songs).lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.80f)
                )
            }
        }
    }
}
