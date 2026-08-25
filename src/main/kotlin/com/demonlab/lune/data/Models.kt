package com.demonlab.lune.data

import java.io.File

data class Song(
    val id: Long,
    val albumId: Long = 0L,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val dateAdded: Long = 0L,
    val coverPath: String? = null,
    val genre: String? = null,
    val folderName: String = "",
    val isHiFi: Boolean = false,
    val isFavorite: Boolean = false,
    val lyrics: String? = null,
    val format: String = "",
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val trackNumber: Int = 0,
    val year: String? = null
) {
    val file: File get() = File(path)
    val exists: Boolean get() = file.exists()
}

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val year: String? = null,
    val songCount: Int = 0,
    val coverPath: String? = null,
    val songs: List<Song> = emptyList()
)

data class Playlist(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val songIds: MutableList<Long> = mutableListOf(),
    val customCoverPath: String? = null
)

data class Folder(
    val name: String,
    val path: String,
    val songs: List<Song> = emptyList(),
    val subFolders: List<Folder> = emptyList()
)

data class PlaybackStats(
    val id: String, // e.g., "SONG_123", "PLAYLIST_456", "ARTIST_Queen"
    val type: String, // "SONG", "PLAYLIST", "ARTIST"
    var playCount: Long = 0,
    var totalTimeMs: Long = 0,
    var lastPlayed: Long = System.currentTimeMillis()
)

data class SongOverride(
    val songId: Long,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val coverPath: String? = null,
    val isFavorite: Boolean = false
)

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class ParsedLyrics(
    val lines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = false,
    val rawText: String = ""
)
