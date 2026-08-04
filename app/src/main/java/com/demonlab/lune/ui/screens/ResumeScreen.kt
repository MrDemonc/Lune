package com.demonlab.lune.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.screens.resume.HeroSection
import com.demonlab.lune.ui.screens.resume.PlaylistGridSection
import com.demonlab.lune.ui.screens.resume.RecommendationSection
import com.demonlab.lune.ui.screens.resume.RecentlyAddedSection
import com.demonlab.lune.ui.screens.resume.SectionHeader
import com.demonlab.lune.ui.viewmodels.MusicViewModel

@Composable
fun ResumeScreen(
    viewModel: MusicViewModel,
    allSongs: List<Song>,
    allPlaylists: List<Playlist>,
    bottomPadding: Dp,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onExpandPlayer: () -> Unit,
    onPlayToggle: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }

    val dailyListeningTimeMs = playbackManager.dailyListeningTime
    val dailyListeningTimeStr = remember(dailyListeningTimeMs) {
        val hours = (dailyListeningTimeMs / (1000 * 60 * 60)).toInt()
        val minutes = ((dailyListeningTimeMs / (1000 * 60)) % 60).toInt()
        val seconds = ((dailyListeningTimeMs / 1000) % 60).toInt()
        when {
            hours > 0 -> context.getString(R.string.stats_hours_unit, hours)
            minutes > 0 -> context.getString(R.string.stats_minutes_unit, minutes)
            else -> context.getString(R.string.stats_seconds_unit, seconds)
        }
    }

    val sUnknownArtist = stringResource(R.string.unknown_artist)
    val sUnknownSong = stringResource(R.string.unknown_song)

    val fallbackTopArtistName = remember(allSongs) {
        allSongs.filter { it.artist.isNotBlank() && it.artist != "<unknown>" }
            .groupingBy { it.artist }
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

    val topArtistStat = viewModel.topArtistStats.firstOrNull()
    val realTopArtistName = remember(topArtistStat, fallbackTopArtistName) {
        topArtistStat?.id?.replace("ARTIST_", "") ?: (fallbackTopArtistName ?: sUnknownArtist)
    }

    val recommendations = remember(allSongs) {
        val topArtists = allSongs.groupingBy { it.artist }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        allSongs.filter { topArtists.contains(it.artist) }
            .distinctBy { it.id }
            .shuffled()
            .take(20)
    }

    val topPlaylists = remember(allPlaylists) {
        allPlaylists.take(10)
    }

    val recentlyAdded = remember(allSongs) {
        allSongs.sortedByDescending { it.dateAdded }.take(10)
    }

    val favoriteCount = remember(allSongs) {
        allSongs.count { it.isFavorite }
    }

    val settingsManager = remember { com.demonlab.lune.tools.SettingsManager.getInstance(context) }
    val prefs = remember(context) { context.getSharedPreferences("lune_settings", Context.MODE_PRIVATE) }
    var showHeroSection by remember { mutableStateOf(settingsManager.showHeroSection) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "show_hero_section") {
                showHeroSection = settingsManager.showHeroSection
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
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
                dailyListeningTimeStr = dailyListeningTimeStr,
                totalSongs = allSongs.size,
                playlistsCount = allPlaylists.size,
                favoriteCount = favoriteCount,
                topArtist = realTopArtistName,
                showGreetingCard = showHeroSection,
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
                    onPlaylistClick = onPlaylistClick
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
                    onSongClick = { song -> onSongClick(song, allSongs) }
                )
            }
        }
    }
}
