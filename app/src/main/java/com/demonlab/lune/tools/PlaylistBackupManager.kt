package com.demonlab.lune.tools

import android.content.Context
import com.demonlab.lune.data.MusicDatabase
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.data.PlaylistSong
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter

data class PlaylistExportData(
    val version: Int = 1,
    val playlists: List<PlaylistData>
)

data class PlaylistData(
    val name: String,
    val songs: List<SongMetadata>
)

data class SongMetadata(
    val title: String,
    val artist: String,
    val duration: Long
)

class PlaylistBackupManager(private val context: Context) {
    private val database = MusicDatabase.getDatabase(context)
    private val musicProvider = MusicProvider(context)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportPlaylists(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val allSongs = musicProvider.getCachedSongs().ifEmpty { musicProvider.syncSongs() }
            val songsMap = allSongs.associateBy { it.id }
            
            val playlists = database.playlistDao().getAllPlaylists()
            val exportList = playlists.map { playlist ->
                val songIds = database.playlistDao().getSongIdsForPlaylist(playlist.id)
                val songsMetadata = songIds.mapNotNull { id ->
                    songsMap[id]?.let { song ->
                        SongMetadata(song.title, song.artist, song.duration)
                    }
                }
                PlaylistData(playlist.name, songsMetadata)
            }

            val exportData = PlaylistExportData(playlists = exportList)
            val json = gson.toJson(exportData)
            
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(json)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importPlaylists(inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = inputStream.bufferedReader().use { it.readText() }
            val exportData = gson.fromJson(json, PlaylistExportData::class.java) ?: return@withContext false
            
            val allSongs = musicProvider.syncSongs() // Re-sync to find current songs
            
            exportData.playlists.forEach { playlistData ->
                // Create playlist
                val playlistId = database.playlistDao().insertPlaylist(Playlist(name = playlistData.name))
                
                // Match songs
                val songsToAdd = mutableListOf<PlaylistSong>()
                playlistData.songs.forEach { songMeta ->
                    val matchedSong = allSongs.find { 
                        it.title == songMeta.title && 
                        it.artist == songMeta.artist && 
                        Math.abs(it.duration - songMeta.duration) < 2000 // 2 seconds tolerance
                    }
                    
                    matchedSong?.let { 
                        songsToAdd.add(PlaylistSong(playlistId, it.id))
                    }
                }
                
                if (songsToAdd.isNotEmpty()) {
                    database.playlistDao().addSongsToPlaylist(songsToAdd)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
