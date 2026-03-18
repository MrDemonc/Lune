package com.demonlab.lune.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.demonlab.lune.tools.MusicProvider
import com.demonlab.lune.tools.Song
import com.demonlab.lune.tools.MetadataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val musicProvider = MusicProvider(application)
    private val metadataManager = MetadataManager(application)

    var allSongs by mutableStateOf<List<Song>>(emptyList())
        private set

    val visuallyDeletedIds = androidx.compose.runtime.mutableStateListOf<Long>()

    val filteredSongs: List<Song>
        get() = allSongs.filter { it.id !in visuallyDeletedIds }

    var isLoading by mutableStateOf(false)
        private set

    fun loadSongs() {
        val cached = musicProvider.getCachedSongs()
        if (cached.isNotEmpty()) {
            allSongs = cached
        } else {
            isLoading = true
        }

        viewModelScope.launch {
            syncSongsInternal()
            isLoading = false
        }
    }

    private suspend fun syncSongsInternal() {
        val synced = withContext(Dispatchers.IO) {
            musicProvider.syncSongs()
        }
        if (synced != allSongs) {
            allSongs = synced
        }
    }

    fun updateMetadata(
        song: Song,
        title: String,
        artist: String,
        album: String,
        genre: String?,
        coverUri: android.net.Uri?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val success = metadataManager.updateSongMetadata(
                songId = song.id,
                title = title,
                artist = artist,
                album = album,
                genre = genre,
                coverUri = coverUri?.toString()
            )
            if (success) {
                syncSongsInternal()
                onSuccess()
            }
        }
    } // added missing brace

    fun restoreOriginalMetadata(song: Song, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = metadataManager.clearMetadataOverride(song.id)
            if (success) {
                syncSongsInternal()
                onSuccess()
            }
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val success = metadataManager.updateFavoriteStatus(song.id, !song.isFavorite)
            if (success) {
                loadSongs()
            }
        }
    }

    // PLAYLISTS
    var playlists by mutableStateOf<List<com.demonlab.lune.data.Playlist>>(emptyList())
        private set

    var playlistMappings by mutableStateOf<List<com.demonlab.lune.data.PlaylistSong>>(emptyList())
        private set

    fun loadPlaylists() {
        viewModelScope.launch {
            val (newPlaylists, newMappings) = withContext(Dispatchers.IO) {
                val db = com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication())
                Pair(db.playlistDao().getAllPlaylists(), db.playlistDao().getAllPlaylistMappings())
            }
            playlists = newPlaylists
            playlistMappings = newMappings
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication()).playlistDao().insertPlaylist(
                    com.demonlab.lune.data.Playlist(name = name)
                )
            }
            loadPlaylists()
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication()).playlistDao().addSongToPlaylist(
                    com.demonlab.lune.data.PlaylistSong(playlistId, songId)
                )
            }
            loadPlaylists()
            onComplete?.invoke()
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val db = com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication())
                val playlistSongs = songIds.map { com.demonlab.lune.data.PlaylistSong(playlistId, it) }
                db.playlistDao().addSongsToPlaylist(playlistSongs)
            }
            loadPlaylists()
            onComplete?.invoke()
        }
    }
    fun removeSongsFromPlaylist(playlistId: Long, songIds: List<Long>, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val db = com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication())
                db.playlistDao().removeSongsFromPlaylist(playlistId, songIds)
            }
            loadPlaylists()
            onComplete?.invoke()
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication()).playlistDao().removeSongFromPlaylist(playlistId, songId)
            }
            loadPlaylists()
            onComplete?.invoke()
        }
    }

    fun getPlaylistsContainingSong(songId: Long, callback: (List<Long>) -> Unit) {
        viewModelScope.launch {
            // This is a bit inefficient but works for now: check all playlists
            val all = withContext(Dispatchers.IO) {
                com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication()).playlistDao().getAllPlaylists()
            }
            val containingIds = mutableListOf<Long>()
            withContext(Dispatchers.IO) {
                val dao = com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication()).playlistDao()
                for (playlist in all) {
                    val songIds = dao.getSongIdsForPlaylist(playlist.id)
                    if (songIds.contains(songId)) {
                        containingIds.add(playlist.id)
                    }
                }
            }
            callback(containingIds)
        }
    }

    fun getSongsForPlaylist(playlistId: Long, callback: (List<Song>) -> Unit) {
        viewModelScope.launch {
            val ids = withContext(Dispatchers.IO) {
                com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication()).playlistDao().getSongIdsForPlaylist(playlistId)
            }
            callback(allSongs.filter { it.id in ids })
        }
    }

    fun getSongsForPlaylistSync(playlistId: Long): List<Song> {
        val songIds = playlistMappings.filter { it.playlistId == playlistId }.sortedBy { it.addedAt }.map { it.songId }
        val songMap = allSongs.associateBy { it.id }
        return songIds.mapNotNull { songMap[it] }
    }

    fun deletePlaylist(playlist: com.demonlab.lune.data.Playlist, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication()).playlistDao().deletePlaylist(playlist)
            }
            loadPlaylists()
            onComplete?.invoke()
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val db = com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication())
                val playlist = db.playlistDao().getAllPlaylists().find { it.id == playlistId }
                if (playlist != null) {
                    db.playlistDao().updatePlaylist(playlist.copy(name = newName))
                }
            }
            loadPlaylists()
            onComplete?.invoke()
        }
    }

    fun getPlaylistPreviewCovers(playlistId: Long, callback: (List<String?>) -> Unit) {
        viewModelScope.launch {
            val ids = withContext(Dispatchers.IO) {
                com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication()).playlistDao().getSongIdsForPlaylist(playlistId)
            }
            val covers = allSongs.filter { it.id in ids.take(4) }.map { it.coverUrl ?: it.albumArtUri?.toString() }
            callback(covers)
        }
    }

    fun getPlaylistInfo(playlistId: Long, callback: (Int, Long) -> Unit) {
        viewModelScope.launch {
            val ids = withContext(Dispatchers.IO) {
                com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication()).playlistDao().getSongIdsForPlaylist(playlistId)
            }
            val playlistSongs = allSongs.filter { it.id in ids }
            val count = playlistSongs.size
            val totalDuration = playlistSongs.sumOf { it.duration }
            callback(count, totalDuration)
        }
    }

    fun prepareDeleteSong(song: Song) {
        if (!visuallyDeletedIds.contains(song.id)) {
            visuallyDeletedIds.add(song.id)
        }
    }

    fun undoDeleteSong(song: Song) {
        visuallyDeletedIds.remove(song.id)
    }

    fun deleteSongPermanently(songId: Long, songData: String, songUri: android.net.Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // 1. Delete actual file
                    val file = java.io.File(songData)
                    if (file.exists()) {
                        file.delete()
                    }
                    
                    // 2. Delete from MediaStore
                    getApplication<Application>().contentResolver.delete(
                        songUri,
                        null,
                        null
                    )
                    
                    // 3. Delete metadata overrides
                    val db = com.demonlab.lune.data.MusicDatabase.getDatabase(getApplication())
                    val override = db.songOverrideDao().getOverrideForSong(songId)
                    if (override != null) {
                        db.songOverrideDao().deleteOverride(override)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            visuallyDeletedIds.remove(songId)
            loadSongs()
        }
    }

    init {
        loadSongs()
        loadPlaylists()
    }
}


