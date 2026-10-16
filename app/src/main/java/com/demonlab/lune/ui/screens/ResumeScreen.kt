package com.demonlab.lune.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.screens.resume.HeroSection
import com.demonlab.lune.ui.screens.resume.PlaylistGridSection
import com.demonlab.lune.ui.screens.resume.RecommendationSection
import com.demonlab.lune.ui.screens.resume.RecentlyAddedSection
import com.demonlab.lune.ui.screens.resume.SectionHeader
import com.demonlab.lune.ui.screens.resume.TopArtistsSection
import com.demonlab.lune.ui.screens.resume.ArtistItem
import com.demonlab.lune.ui.screens.resume.TopGenresSection
import com.demonlab.lune.ui.screens.resume.GenreItem
import com.demonlab.lune.ui.viewmodels.MusicViewModel

@Composable
fun ResumeScreen(
    viewModel: MusicViewModel,
    allSongs: List<Song>,
    allPlaylists: List<Playlist>,
    bottomPadding: Dp,
    currentSong: Song?,
    isPlaying: Boolean,
    playbackProgress: Float = 0f,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onArtistClick: (String) -> Unit,
    onGenreClick: (String) -> Unit,
    onExpandPlayer: () -> Unit,
    onPlayToggle: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val sUnknownArtist = stringResource(R.string.unknown_artist)
    val sUnknownSong = stringResource(R.string.unknown_song)

    val fallbackTopArtistName = remember(allSongs) {
        allSongs.filter { it.artist.isNotBlank() && !it.artist.equals("<unknown>", ignoreCase = true) }
            .groupingBy { it.artist.trim() }
            .eachCount()
            .maxByOrNull { it.value }?.key
    }

    val fallbackTopPlaylist = remember(allPlaylists, viewModel.playlistMappings) {
        allPlaylists.maxByOrNull { playlist ->
            viewModel.getSongsForPlaylistSync(playlist.id).size
        }
    }

    val top3Songs = remember(viewModel.topSongStats, allSongs) {
        viewModel.topSongStats.mapNotNull { stat ->
            val idStr = stat.id.replace("SONG_", "")
            val id = idStr.toLongOrNull()
            allSongs.find { it.id == id }
        }.take(3)
    }

    val topSong = top3Songs.firstOrNull()

    val realTopArtistName = remember(viewModel.topArtistStats, viewModel.topSongStats, allSongs, fallbackTopArtistName) {
        val artistPlaysMap = mutableMapOf<String, Long>()

        // 1. Accumulate plays from top played songs
        for (stat in viewModel.topSongStats) {
            val sId = stat.id.removePrefix("SONG_").toLongOrNull()
            if (sId != null && stat.playCount > 0) {
                val s = allSongs.find { it.id == sId }
                val art = s?.artist?.trim()
                if (!art.isNullOrBlank() && !art.equals("<unknown>", ignoreCase = true)) {
                    artistPlaysMap[art] = (artistPlaysMap[art] ?: 0L) + stat.playCount
                }
            }
        }

        // 2. Combine with direct ARTIST_ stats
        for (stat in viewModel.topArtistStats) {
            val art = stat.id.removePrefix("ARTIST_").trim()
            if (art.isNotBlank() && !art.equals("<unknown>", ignoreCase = true) && stat.playCount > 0) {
                val existing = artistPlaysMap[art] ?: 0L
                artistPlaysMap[art] = maxOf(existing, stat.playCount)
            }
        }

        // 3. Find artist with highest playCount
        val bestByPlays = artistPlaysMap.filter { it.value > 0 }.maxByOrNull { it.value }?.key

        bestByPlays
            ?: viewModel.topArtistStats.firstOrNull { it.playCount > 0 }?.id?.removePrefix("ARTIST_")?.trim()
            ?: fallbackTopArtistName
            ?: sUnknownArtist
    }

    val recommendations = remember(allSongs) {
        val topArtists = allSongs.groupingBy { it.artist.trim() }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        allSongs.filter { topArtists.contains(it.artist.trim()) }
            .distinctBy { it.id }
            .shuffled()
            .take(20)
    }

    val topPlaylists = remember(allPlaylists) {
        allPlaylists.take(10)
    }

    val topArtistsList = remember(allSongs, viewModel.topArtistStats, viewModel.topSongStats) {
        val artistPlaysMap = mutableMapOf<String, Long>()
        for (stat in viewModel.topSongStats) {
            val sId = stat.id.removePrefix("SONG_").toLongOrNull()
            if (sId != null && stat.playCount > 0) {
                val s = allSongs.find { it.id == sId }
                val art = s?.artist?.trim()
                if (!art.isNullOrBlank() && !art.equals("<unknown>", ignoreCase = true)) {
                    artistPlaysMap[art] = (artistPlaysMap[art] ?: 0L) + stat.playCount
                }
            }
        }
        for (stat in viewModel.topArtistStats) {
            val art = stat.id.removePrefix("ARTIST_").trim()
            if (art.isNotBlank() && !art.equals("<unknown>", ignoreCase = true) && stat.playCount > 0) {
                val existing = artistPlaysMap[art] ?: 0L
                artistPlaysMap[art] = maxOf(existing, stat.playCount)
            }
        }

        allSongs.filter { it.artist.isNotBlank() && !it.artist.equals("<unknown>", ignoreCase = true) }
            .groupBy { it.artist.trim() }
            .entries
            .map { (artistName, songs) ->
                val playCount: Int = (artistPlaysMap[artistName] ?: songs.size.toLong()).toInt()
                val coverSong = songs.firstOrNull { !it.coverUrl.isNullOrEmpty() || it.albumArtUri != null } ?: songs.firstOrNull()
                val coverStr: String? = coverSong?.coverUrl ?: coverSong?.albumArtUri?.toString()
                ArtistItem(
                    name = artistName,
                    playCount = playCount,
                    coverUrl = coverStr,
                    songs = songs
                )
            }
            .sortedByDescending { it.playCount }
    }

    val topGenresList = remember(allSongs) {
        allSongs.groupBy { song ->
            val g = song.genre?.trim()
            if (g.isNullOrEmpty() || g.equals("<unknown>", ignoreCase = true) || g.equals("unknown", ignoreCase = true)) {
                "Desconocido"
            } else {
                g
            }
        }.entries.map { (genreName, songs) ->
            val coverSong = songs.firstOrNull { !it.coverUrl.isNullOrEmpty() || it.albumArtUri != null } ?: songs.firstOrNull()
            val coverStr: String? = coverSong?.coverUrl ?: coverSong?.albumArtUri?.toString()
            GenreItem(
                name = genreName,
                songCount = songs.size,
                coverUrl = coverStr,
                songs = songs
            )
        }.sortedByDescending { it.songCount }
    }

    val recentlyAdded = remember(allSongs) {
        allSongs.sortedByDescending { it.dateAdded }.take(10)
    }

    val topArtistSongs = remember(allSongs, realTopArtistName) {
        allSongs.filter { it.artist.trim().equals(realTopArtistName.trim(), ignoreCase = true) }
    }

    val topGenreStat = viewModel.topGenreStats.firstOrNull()
    val realTopGenreName = remember(topGenreStat, topGenresList) {
        topGenreStat?.id?.replace("GENRE_", "") ?: (topGenresList.firstOrNull()?.name ?: "")
    }

    val topSongStat = viewModel.topSongStats.firstOrNull()
    val totalPlays = remember(viewModel.topSongStats) {
        viewModel.topSongStats.sumOf { it.playCount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 8.dp, bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically { it / 2 }
        ) {
            HeroSection(
                currentSong = currentSong,
                isPlaying = isPlaying,
                topArtist = realTopArtistName,
                topArtistSongs = topArtistSongs,
                topSong = topSong,
                topSongPlayCount = topSongStat?.playCount ?: 0L,
                topGenre = realTopGenreName,
                totalPlays = totalPlays,
                onArtistClick = onArtistClick,
                onSongClick = { song -> onSongClick(song, allSongs) },
                hasBlurBackground = hasBlurBackground,
                isDarkTheme = isDarkTheme,
                useCustomControlsColor = useCustomControlsColor,
                controlsColorPalette = controlsColorPalette,
                playbackProgress = playbackProgress,
                onContinueListening = onExpandPlayer,
                onPlayToggle = onPlayToggle
            )
        }

        if (recommendations.isNotEmpty()) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                RecommendationSection(
                    title = stringResource(R.string.resume_recommendations),
                    songs = recommendations,
                    hasBlurBackground = hasBlurBackground,
                    isDarkTheme = isDarkTheme,
                    useCustomControlsColor = useCustomControlsColor,
                    controlsColorPalette = controlsColorPalette,
                    onSongClick = { song -> onSongClick(song, allSongs) }
                )
            }
        }

        if (topPlaylists.isNotEmpty()) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                PlaylistGridSection(
                    viewModel = viewModel,
                    playlists = topPlaylists,
                    hasBlurBackground = hasBlurBackground,
                    onPlaylistClick = onPlaylistClick
                )
            }
        }

        if (topArtistsList.isNotEmpty()) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                TopArtistsSection(
                    artists = topArtistsList,
                    hasBlurBackground = hasBlurBackground,
                    onArtistClick = onArtistClick
                )
            }
        }

        if (topGenresList.isNotEmpty()) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                TopGenresSection(
                    genres = topGenresList,
                    hasBlurBackground = hasBlurBackground,
                    onGenreClick = onGenreClick
                )
            }
        }

        if (recentlyAdded.isNotEmpty()) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                RecentlyAddedSection(
                    songs = recentlyAdded,
                    hasBlurBackground = hasBlurBackground,
                    onSongClick = { song -> onSongClick(song, allSongs) }
                )
            }
        }
    }
}
