package com.demonlab.lune.tools

import android.content.Context
import com.demonlab.lune.data.MusicDatabase
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.data.PlaylistSong
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

data class PlaylistExportData(
    val version: Int = 2,
    val playlists: List<PlaylistData>,
    val lyrics: List<LyricBackupItem> = emptyList(),
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

    private val dao = MusicDatabase.getDatabase(context).playlistDao()
    private val musicProvider = MusicProvider(context)

    suspend fun exportPlaylists(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val songsMap = musicProvider.getCachedSongs()
                .ifEmpty { musicProvider.syncSongs() }
                .associateBy { it.id }

            val lyricsStorage = LyricsStorageManager.getInstance(context)
            val customLyrics = lyricsStorage.getAllCustomLyrics()

            val exportData = PlaylistExportData(
                version = 2,
                playlists = dao.getAllPlaylists().map { playlist ->
                    PlaylistData(
                        name = playlist.name,
                        songs = dao.getSongIdsForPlaylist(playlist.id).mapNotNull { id ->
                            songsMap[id]?.let { SongMetadata(it.title, it.artist, it.duration, it.dateAdded) }
                        },
                    )
                },
                lyrics = customLyrics,
            )

            outputStream.bufferedWriter().use { it.write(gson.toJson(exportData)) }
        }.isSuccess
    }

    suspend fun importPlaylists(inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val exportData = inputStream.bufferedReader()
                .use { gson.fromJson(it, PlaylistExportData::class.java) }
                ?: return@withContext false

            val allSongs = musicProvider.syncSongs()

            exportData.playlists.forEach { playlistData ->
                val existing = dao.getPlaylistByName(playlistData.name)
                if (existing != null) {
                    dao.deletePlaylist(existing)
                }
                val playlistId = dao.insertPlaylist(Playlist(name = playlistData.name))

                val songsToAdd = playlistData.songs.mapIndexedNotNull { index, meta ->
                    allSongs.find { song ->
                        song.title == meta.title &&
                                song.artist == meta.artist &&
                                kotlin.math.abs(song.duration - meta.duration) < 2_000L
                    }?.let { PlaylistSong(playlistId, it.id, position = index) }
                }

                if (songsToAdd.isNotEmpty()) dao.addSongsToPlaylist(songsToAdd)
            }

            if (!exportData.lyrics.isNullOrEmpty()) {
                val lyricsStorage = LyricsStorageManager.getInstance(context)
                lyricsStorage.restoreCustomLyrics(exportData.lyrics, allSongs)
            }
        }.isSuccess
    }

    suspend fun syncBackupIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val settings = SettingsManager.getInstance(context)
        if (!settings.isAutoSyncBackupEnabled) return@withContext false
        val uriString = settings.autoSyncBackupUri ?: return@withContext false
        try {
            val uri = android.net.Uri.parse(uriString)
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                exportPlaylists(outputStream)
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun triggerAutoSync() {
        CoroutineScope(Dispatchers.IO).launch {
            syncBackupIfNeeded()
        }
    }
}