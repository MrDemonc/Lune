package com.demonlab.lune.tools

import com.demonlab.lune.data.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.security.MessageDigest
import java.util.logging.Level
import java.util.logging.Logger

class MusicProvider private constructor() {
    private val dataManager = DataManager.instance
    private val settings = SettingsManager.instance
    private val cacheDir = File(System.getProperty("user.home"), ".cache/lune").apply { mkdirs() }
    private val cacheFile = File(cacheDir, "songs_cache.json")
    private val gson = Gson()

    private val supportedExtensions = setOf("mp3", "flac", "ogg", "wav", "m4a", "aac", "opus", "alac", "wma")

    init {
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    fun getCachedSongs(): List<Song> {
        if (!cacheFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<Song>>() {}.type
            val list: List<Song> = gson.fromJson(cacheFile.readText(), type) ?: emptyList()
            val favorites = dataManager.getFavorites()
            val overrides = dataManager.getOverrides()

            list.distinctBy { it.path }.map { song ->
                val ov = overrides[song.id]
                song.copy(
                    title = ov?.title ?: song.title,
                    artist = ov?.artist ?: song.artist,
                    album = ov?.album ?: song.album,
                    genre = ov?.genre ?: song.genre,
                    coverPath = ov?.coverPath ?: song.coverPath,
                    isFavorite = favorites.contains(song.id)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveToCache(songs: List<Song>) {
        try {
            cacheFile.writeText(gson.toJson(songs))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun scanLibrary(directories: List<String> = settings.musicDirectories): List<Song> = withContext(Dispatchers.IO) {
        val scannedSongs = mutableListOf<Song>()
        val favorites = dataManager.getFavorites()
        val overrides = dataManager.getOverrides()

        for (dirPath in directories) {
            val root = File(dirPath)
            if (!root.exists() || !root.isDirectory) continue

            root.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in supportedExtensions }
                .forEach { file ->
                    try {
                        val song = extractSongMetadata(file, favorites, overrides)
                        scannedSongs.add(song)
                    } catch (e: Exception) {
                        val id = (file.canonicalPath.hashCode().toLong() and 0xFFFFFFFFL)
                        scannedSongs.add(
                            Song(
                                id = id,
                                title = file.nameWithoutExtension,
                                artist = "Unknown Artist",
                                album = file.parentFile?.name ?: "Unknown Album",
                                duration = 0L,
                                path = file.canonicalPath,
                                dateAdded = file.lastModified(),
                                folderName = file.parentFile?.name ?: "",
                                format = file.extension.uppercase(),
                                isFavorite = favorites.contains(id)
                            )
                        )
                    }
                }
        }

        val distinctSongs = scannedSongs.distinctBy { it.path }
        saveToCache(distinctSongs)
        distinctSongs
    }

    private fun extractSongMetadata(
        file: File,
        favorites: Set<Long>,
        overrides: Map<Long, SongOverride>
    ): Song {
        val canonicalPath = file.canonicalPath
        val id = (canonicalPath.hashCode().toLong() and 0xFFFFFFFFL)
        var title = file.nameWithoutExtension
        var artist = "Unknown Artist"
        var album = file.parentFile?.name ?: "Unknown Album"
        var genre: String? = null
        var duration = 0L
        var trackNumber = 0
        var year: String? = null
        var bitrate: Int? = null
        var sampleRate: Int? = null
        var isHiFi = false
        var lyrics: String? = null
        var coverPath: String? = null

        val format = file.extension.uppercase()

        try {
            val audioFile = AudioFileIO.read(file)
            val header = audioFile.audioHeader
            if (header != null) {
                duration = (header.trackLength * 1000).toLong()
                bitrate = header.bitRateAsNumber.toInt().takeIf { it > 0 }
                sampleRate = header.sampleRateAsNumber.takeIf { it > 0 }

                if (format in listOf("FLAC", "ALAC", "WAV", "DSD") || (sampleRate ?: 0) >= 48000 || (bitrate ?: 0) > 320) {
                    isHiFi = true
                }
            }

            val tag = audioFile.tag
            if (tag != null) {
                tag.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() }?.let { title = it }
                tag.getFirst(FieldKey.ARTIST)?.takeIf { it.isNotBlank() }?.let { artist = it }
                tag.getFirst(FieldKey.ALBUM)?.takeIf { it.isNotBlank() }?.let { album = it }
                tag.getFirst(FieldKey.GENRE)?.takeIf { it.isNotBlank() }?.let { genre = it }
                tag.getFirst(FieldKey.TRACK)?.toIntOrNull()?.let { trackNumber = it }
                tag.getFirst(FieldKey.YEAR)?.takeIf { it.isNotBlank() }?.let { year = it }
                tag.getFirst(FieldKey.LYRICS)?.takeIf { it.isNotBlank() }?.let { lyrics = it }

                val artwork = tag.firstArtwork
                if (artwork != null && artwork.binaryData != null) {
                    val hash = md5(artwork.binaryData)
                    val coverFile = File(dataManager.coversCacheDir, "$hash.jpg")
                    if (!coverFile.exists()) {
                        coverFile.writeBytes(artwork.binaryData)
                    }
                    coverPath = coverFile.absolutePath
                }
            }
        } catch (e: Exception) {}

        if (lyrics == null) {
            lyrics = LyricsParser.findLrcForSong(canonicalPath)
        }

        val ov = overrides[id]
        return Song(
            id = id,
            albumId = (album.hashCode().toLong() and 0xFFFFFFFFL),
            title = ov?.title ?: title,
            artist = ov?.artist ?: artist,
            album = ov?.album ?: album,
            genre = ov?.genre ?: genre,
            duration = duration,
            path = canonicalPath,
            dateAdded = file.lastModified(),
            coverPath = ov?.coverPath ?: coverPath,
            folderName = file.parentFile?.name ?: "",
            isHiFi = isHiFi,
            isFavorite = favorites.contains(id),
            lyrics = lyrics,
            format = format,
            bitrate = bitrate,
            sampleRate = sampleRate,
            trackNumber = trackNumber,
            year = year
        )
    }

    private fun md5(data: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun groupAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { "${it.album}||${it.artist}" }
            .map { (key, albumSongs) ->
                val firstWithCover = albumSongs.firstOrNull { it.coverPath != null }
                val firstSong = albumSongs.first()
                Album(
                    id = (key.hashCode().toLong() and 0xFFFFFFFFL),
                    name = firstSong.album,
                    artist = firstSong.artist,
                    year = firstSong.year,
                    songCount = albumSongs.size,
                    coverPath = firstWithCover?.coverPath,
                    songs = albumSongs.sortedBy { it.trackNumber }
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun groupFolders(songs: List<Song>): List<Folder> {
        return songs.groupBy { File(it.path).parentFile?.absolutePath ?: "" }
            .map { (folderPath, folderSongs) ->
                val file = File(folderPath)
                Folder(
                    name = file.name.ifEmpty { folderPath },
                    path = folderPath,
                    songs = folderSongs.sortedBy { it.title.lowercase() }
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    companion object {
        val instance: MusicProvider by lazy { MusicProvider() }
    }
}
