package com.demonlab.lune.tools

import android.content.Context
import com.demonlab.lune.data.MusicDatabase
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.data.PlaylistSong
import com.demonlab.lune.data.PlaybackStats
import com.demonlab.lune.data.SongOverride
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

data class PlaylistExportData(
    val version: Int = 2,
    val playlists: List<PlaylistData> = emptyList(),
    val settings: SettingsBackupData? = null,
    val playbackStats: List<PlaybackStats>? = null,
    val songOverrides: List<SongOverride>? = null
)

data class PlaylistData(
    val name: String,
    val songs: List<SongMetadata>,
)

data class SongMetadata(
    val title: String,
    val artist: String,
    val duration: Long,
    val dateAdded: Long = 0,
)

private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

class PlaylistBackupManager(private val context: Context) {

    private val db = MusicDatabase.getDatabase(context)
    private val dao = db.playlistDao()
    private val statsDao = db.playbackStatsDao()
    private val overrideDao = db.songOverrideDao()
    private val musicProvider = MusicProvider(context)
    private val settings = SettingsManager.getInstance(context)

    suspend fun exportPlaylists(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val songsMap = musicProvider.getCachedSongs()
                .ifEmpty { musicProvider.syncSongs() }
                .associateBy { it.id }

            // 1. Fetch Playlists
            val playlistsBackup = dao.getAllPlaylists().map { playlist ->
                PlaylistData(
                    name = playlist.name,
                    songs = dao.getSongIdsForPlaylist(playlist.id).mapNotNull { id ->
                        songsMap[id]?.let { SongMetadata(it.title, it.artist, it.duration, it.dateAdded) }
                    },
                )
            }

            // 2. Fetch SharedPreferences State
            val settingsBackup = SettingsBackupData(
                optionsOrder = settings.optionsOrder,
                hiddenFolders = settings.hiddenFolders.toList(),
                enableHiFi = settings.enableHiFi,
                themeMode = settings.themeMode,
                forceDarkMode = settings.forceDarkMode,
                sortOption = settings.sortOption,
                albumViewStyle = settings.albumViewStyle,
                isSortAscending = settings.isSortAscending,
                isShuffle = settings.isShuffle,
                isCrossfade = settings.isCrossfade,
                isAutomix = settings.isAutomix,
                repeatMode = settings.repeatMode,
                isEqEnabled = settings.isEqEnabled,
                eqBandLevels = settings.eqBandLevels,
                activeEqPresetName = settings.activeEqPresetName,
                lastEqPresetName = settings.lastEqPresetName,
                lastEqBandLevels = settings.lastEqBandLevels,
                customEqPresetsJson = settings.customEqPresetsJson,
                isBassBoostEnabled = settings.isBassBoostEnabled,
                bassBoostLevel = settings.bassBoostLevel,
                isSpatialAudioEnabled = settings.isSpatialAudioEnabled,
                language = settings.language,
                customTitle = settings.customTitle,
                isFullPlayerVisualizerEnabled = settings.isFullPlayerVisualizerEnabled,
                isMiniPlayerVisualizerEnabled = settings.isMiniPlayerVisualizerEnabled,
                isCinematicPlayerEnabled = settings.isCinematicPlayerEnabled,
                isHapticVibrationEnabled = settings.isHapticVibrationEnabled,
                isSongInfoEnabled = settings.isSongInfoEnabled,
                isBlurEnabled = settings.isBlurEnabled,
                isBlurDarkMode = settings.isBlurDarkMode,
                isBlurLightMode = settings.isBlurLightMode,
                isBlurCinematicMode = settings.isBlurCinematicMode,
                isBlurControlsEnabled = settings.isBlurControlsEnabled,
                lyricsTextAlignment = settings.lyricsTextAlignment,
                lyricsSpeedIndex = settings.lyricsSpeedIndex,
                isGesturesEnabled = settings.isGesturesEnabled,
                swipeUpAction = settings.swipeUpAction,
                dailyListeningTime = settings.dailyListeningTime,
                lastStatsResetTimestamp = settings.lastStatsResetTimestamp,
                showAllFoldersOnStart = settings.showAllFoldersOnStart,
                useCustomColors = settings.useCustomColors,
                customColorPalette = settings.customColorPalette,
                showBackupWarning = settings.showBackupWarning,
                useAmoledPitchBlack = settings.useAmoledPitchBlack,
                coverShape = settings.coverShape,
                coverScale = settings.coverScale,
                coverSpin = settings.coverSpin,
                coverVinylEffect = settings.coverVinylEffect,
                controlsIconStyle = settings.controlsIconStyle,
                isControlsFilled = settings.isControlsFilled,
                useCustomControlsColor = settings.useCustomControlsColor,
                controlsColorPalette = settings.controlsColorPalette,
                playbackSpeed = settings.playbackSpeed,
                playbackPitch = settings.playbackPitch,
                reverbPreset = settings.reverbPreset,
                balance = settings.balance,
                dynamicsPreset = settings.dynamicsPreset,
                isLoudnessEnabled = settings.isLoudnessEnabled,
                loudnessGain = settings.loudnessGain
            )

            // 3. Fetch Playback Statistics from Room
            val playbackStatsBackup = statsDao.getAllStats()

            // 4. Fetch Metadata Overrides & Favorite Statuses from Room
            val songOverridesBackup = overrideDao.getAllOverrides()

            // 5. Package everything into v2 Backup Container
            val exportData = PlaylistExportData(
                version = 2,
                playlists = playlistsBackup,
                settings = settingsBackup,
                playbackStats = playbackStatsBackup,
                songOverrides = songOverridesBackup
            )

            outputStream.bufferedWriter().use { it.write(gson.toJson(exportData)) }
        }.isSuccess
    }

    suspend fun importPlaylists(inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val exportData = inputStream.bufferedReader()
                .use { gson.fromJson(it, PlaylistExportData::class.java) }
                ?: return@withContext false

            // 1. Restore Playlists (Compatible with v1 and v2)
            val allSongs = musicProvider.syncSongs()

            exportData.playlists.forEach { playlistData ->
                val existing = dao.getPlaylistByName(playlistData.name)
                if (existing != null) {
                    dao.deletePlaylist(existing)
                }
                val playlistId = dao.insertPlaylist(Playlist(name = playlistData.name))

                val songsToAdd = playlistData.songs.mapNotNull { meta ->
                    allSongs.find { song ->
                        song.title == meta.title &&
                                song.artist == meta.artist &&
                                kotlin.math.abs(song.duration - meta.duration) < 2_000L
                    }?.let { PlaylistSong(playlistId, it.id) }
                }

                if (songsToAdd.isNotEmpty()) dao.addSongsToPlaylist(songsToAdd)
            }

            // 2. Restore Lune Settings (v2 Only)
            exportData.settings?.let { backup ->
                backup.optionsOrder?.let { settings.optionsOrder = it }
                backup.hiddenFolders?.let { settings.hiddenFolders = it.toSet() }
                backup.enableHiFi?.let { settings.enableHiFi = it }
                backup.themeMode?.let { settings.themeMode = it }
                backup.forceDarkMode?.let { settings.forceDarkMode = it }
                backup.sortOption?.let { settings.sortOption = it }
                backup.albumViewStyle?.let { settings.albumViewStyle = it }
                backup.isSortAscending?.let { settings.isSortAscending = it }
                backup.isShuffle?.let { settings.isShuffle = it }
                backup.isCrossfade?.let { settings.isCrossfade = it }
                backup.isAutomix?.let { settings.isAutomix = it }
                backup.repeatMode?.let { settings.repeatMode = it }
                backup.isEqEnabled?.let { settings.isEqEnabled = it }
                backup.eqBandLevels?.let { settings.eqBandLevels = it }
                backup.activeEqPresetName?.let { settings.activeEqPresetName = it }
                backup.lastEqPresetName?.let { settings.lastEqPresetName = it }
                backup.lastEqBandLevels?.let { settings.lastEqBandLevels = it }
                backup.customEqPresetsJson?.let { settings.customEqPresetsJson = it }
                backup.isBassBoostEnabled?.let { settings.isBassBoostEnabled = it }
                backup.bassBoostLevel?.let { settings.bassBoostLevel = it }
                backup.isSpatialAudioEnabled?.let { settings.isSpatialAudioEnabled = it }
                backup.language?.let { settings.language = it }
                backup.customTitle?.let { settings.customTitle = it }
                backup.isFullPlayerVisualizerEnabled?.let { settings.isFullPlayerVisualizerEnabled = it }
                backup.isMiniPlayerVisualizerEnabled?.let { settings.isMiniPlayerVisualizerEnabled = it }
                backup.isCinematicPlayerEnabled?.let { settings.isCinematicPlayerEnabled = it }
                backup.isHapticVibrationEnabled?.let { settings.isHapticVibrationEnabled = it }
                backup.isSongInfoEnabled?.let { settings.isSongInfoEnabled = it }
                backup.isBlurEnabled?.let { settings.isBlurEnabled = it }
                backup.isBlurDarkMode?.let { settings.isBlurDarkMode = it }
                backup.isBlurLightMode?.let { settings.isBlurLightMode = it }
                backup.isBlurCinematicMode?.let { settings.isBlurCinematicMode = it }
                backup.isBlurControlsEnabled?.let { settings.isBlurControlsEnabled = it }
                backup.lyricsTextAlignment?.let { settings.lyricsTextAlignment = it }
                backup.lyricsSpeedIndex?.let { settings.lyricsSpeedIndex = it }
                backup.isGesturesEnabled?.let { settings.isGesturesEnabled = it }
                backup.swipeUpAction?.let { settings.swipeUpAction = it }
                backup.dailyListeningTime?.let { settings.dailyListeningTime = it }
                backup.lastStatsResetTimestamp?.let { settings.lastStatsResetTimestamp = it }
                backup.showAllFoldersOnStart?.let { settings.showAllFoldersOnStart = it }
                backup.useCustomColors?.let { settings.useCustomColors = it }
                backup.customColorPalette?.let { settings.customColorPalette = it }
                backup.showBackupWarning?.let { settings.showBackupWarning = it }
                backup.useAmoledPitchBlack?.let { settings.useAmoledPitchBlack = it }
                backup.coverShape?.let { settings.coverShape = it }
                backup.coverScale?.let { settings.coverScale = it }
                backup.coverSpin?.let { settings.coverSpin = it }
                backup.coverVinylEffect?.let { settings.coverVinylEffect = it }
                backup.controlsIconStyle?.let { settings.controlsIconStyle = it }
                backup.isControlsFilled?.let { settings.isControlsFilled = it }
                backup.useCustomControlsColor?.let { settings.useCustomControlsColor = it }
                backup.controlsColorPalette?.let { settings.controlsColorPalette = it }
                backup.playbackSpeed?.let { settings.playbackSpeed = it }
                backup.playbackPitch?.let { settings.playbackPitch = it }
                backup.reverbPreset?.let { settings.reverbPreset = it }
                backup.balance?.let { settings.balance = it }
                backup.dynamicsPreset?.let { settings.dynamicsPreset = it }
                backup.isLoudnessEnabled?.let { settings.isLoudnessEnabled = it }
                backup.loudnessGain?.let { settings.loudnessGain = it }
            }

            // 3. Restore Playback Statistics (v2 Only)
            exportData.playbackStats?.let { statsList ->
                if (statsList.isNotEmpty()) {
                    statsDao.insertStatsList(statsList)
                }
            }

            // 4. Restore Song Overrides and Likes (v2 Only)
            exportData.songOverrides?.let { overridesList ->
                if (overridesList.isNotEmpty()) {
                    overrideDao.insertOverrideList(overridesList)
                }
            }
        }.isSuccess
    }
}