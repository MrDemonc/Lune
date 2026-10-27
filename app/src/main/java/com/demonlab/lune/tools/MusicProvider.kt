package com.demonlab.lune.tools

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.demonlab.lune.data.MusicDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import android.media.MediaScannerConnection
import android.media.MediaMetadataRetriever
import android.os.Environment
import com.demonlab.lune.R
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UriTypeAdapter : TypeAdapter<Uri>() {
    override fun write(out: JsonWriter, value: Uri?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString())
        }
    }

    override fun read(reader: JsonReader): Uri? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return reader.nextString().toUri() // Modern KTX string-to-Uri converter
    }
}

class MusicProvider(private val context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    private val database = MusicDatabase.getDatabase(context)

    private val cacheFile = File(context.filesDir, "songs_cache.json")
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter())
        .disableHtmlEscaping()
        .create()

    fun getCachedSongs(): List<Song> {
        if (!cacheFile.exists()) return emptyList()
        return try {
            val json = cacheFile.readText()
            val type = object : TypeToken<List<Song>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveToCache(songs: List<Song>) {
        try {
            val json = gson.toJson(songs)
            cacheFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSongInCache(updatedSong: Song) {
        val songs = getCachedSongs().toMutableList()
        val idx = songs.indexOfFirst { it.id == updatedSong.id }
        if (idx >= 0) {
            songs[idx] = updatedSong
            saveToCache(songs)
        } else {
            songs.add(updatedSong)
            saveToCache(songs)
        }
    }

    private fun hasReadPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun syncSongs(): List<Song> = withContext(Dispatchers.IO) {
        if (!hasReadPermission()) return@withContext emptyList()
        val songList = mutableListOf<Song>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projectionList = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStore.Audio.Media.GENRE
            } else {
                MediaStore.Audio.Media.ARTIST
            }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add(MediaStore.Audio.Media.BITRATE)
        }
        val projection = projectionList.toTypedArray()

        val audioExtensions = setOf(
            "mp3", "flac", "wav", "aac", "m4a", "ogg", "opus", "wma", "alac", "dsf", "dff"
        )

        // Include songs flagged by MediaStore as IS_MUSIC, or any audio file that is not a system sound (ringtone/alarm/notification)
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR (${MediaStore.Audio.Media.IS_RINGTONE} == 0 AND ${MediaStore.Audio.Media.IS_NOTIFICATION} == 0 AND ${MediaStore.Audio.Media.IS_ALARM} == 0))"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val overrides = database.songOverrideDao().getAllOverrides().associateBy { it.songId }

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val trackColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val genreColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
            } else -1
            val bitrateColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
            } else -1

            while (cursor.moveToNext()) {
                val data = cursor.getString(dataColumn)
                if (data.isNullOrBlank()) continue

                val extension = data.substringAfterLast(".", "").lowercase()
                if (extension !in audioExtensions) continue

                val file = File(data)
                if (file.exists() && file.length() == 0L) continue

                val id = cursor.getLong(idColumn)
                var title = cursor.getString(titleColumn) ?: ""
                var artist = cursor.getString(artistColumn) ?: ""
                var album = cursor.getString(albumColumn) ?: ""
                var duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                var trackNumber = if (trackColumn != -1) cursor.getInt(trackColumn) else 0
                var genre = if (genreColumn != -1) cursor.getString(genreColumn) else null

                // If MediaStore duration is missing/zero, attempt extraction directly
                if (duration <= 0L && file.exists()) {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(file.absolutePath)
                        duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        retriever.release()
                    } catch (_: Exception) {}
                }

                if (title.isBlank() || title.equals("<unknown>", ignoreCase = true)) {
                    title = file.nameWithoutExtension.ifBlank { "Track $id" }
                }
                if (artist.isBlank() || artist.equals("<unknown>", ignoreCase = true)) {
                    artist = context.getString(R.string.unknown_artist)
                }
                if (album.isBlank() || album.equals("<unknown>", ignoreCase = true)) {
                    album = context.getString(R.string.unknown_album)
                }

                val override = overrides[id]
                var coverUrl: String? = null
                var isFavorite = false

                if (override != null) {
                    override.title?.let { if (it.isNotBlank()) title = it }
                    override.artist?.let { if (it.isNotBlank()) artist = it }
                    override.album?.let { if (it.isNotBlank()) album = it }
                    override.genre?.let { if (it.isNotBlank()) genre = it }
                    coverUrl = override.coverUri
                    isFavorite = override.isFavorite
                }

                title = CharsetUtils.sanitizeText(title)
                artist = CharsetUtils.sanitizeText(artist)
                album = CharsetUtils.sanitizeText(album)

                val folderName = data.substringBeforeLast("/").substringAfterLast("/")
                val isHiFiFile = extension == "flac" || extension == "wav" || extension == "alac" || extension == "dsf" || extension == "dff"
                val isHiFi = settingsManager.enableHiFi && isHiFiFile
                val format = when (extension) {
                    "mp3" -> "MP3"
                    "flac" -> "FLAC"
                    "wav" -> "WAV"
                    "aac", "m4a" -> "AAC"
                    "ogg" -> "OGG"
                    "opus" -> "OPUS"
                    "wma" -> "WMA"
                    "alac" -> "ALAC"
                    "dsf", "dff" -> "DSD"
                    else -> extension.uppercase()
                }

                val mediaStoreBitrate = if (bitrateColumn != -1) {
                    val value = cursor.getInt(bitrateColumn)
                    if (value > 0) value else null
                } else null

                val calculatedBitrate = if (duration > 0 && file.exists()) {
                    ((file.length() * 8) / (duration / 1000f)).toInt()
                } else null

                val bitrate = mediaStoreBitrate ?: calculatedBitrate

                var sampleRate: Int? = null
                var bitDepth: Int? = null
                var isHiRes = false
                if (isHiFiFile && file.exists()) {
                    try {
                        val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                        val header = audioFile.audioHeader
                        sampleRate = header.sampleRateAsNumber
                        bitDepth = header.bitsPerSample
                        if ((sampleRate >= 48000 && bitDepth >= 24) || sampleRate >= 88200 || extension == "dsf" || extension == "dff") {
                            isHiRes = true
                        }

                        // Extract tags for formats where Android MediaStore does not parse metadata (e.g. WAV, DSF, DFF)
                        // or where MediaStore tags were missing/fallback unknown values
                        if (override == null) {
                            val tag = audioFile.tag
                            if (tag != null) {
                                val tagTitle = runCatching { tag.getFirst(org.jaudiotagger.tag.FieldKey.TITLE) }.getOrNull()?.trim()
                                val tagArtist = runCatching {
                                    val a = tag.getFirst(org.jaudiotagger.tag.FieldKey.ARTIST)?.trim()
                                    if (!a.isNullOrBlank()) a else tag.getFirst(org.jaudiotagger.tag.FieldKey.ALBUM_ARTIST)?.trim()
                                }.getOrNull()
                                val tagAlbum = runCatching { tag.getFirst(org.jaudiotagger.tag.FieldKey.ALBUM) }.getOrNull()?.trim()
                                val tagGenre = runCatching { tag.getFirst(org.jaudiotagger.tag.FieldKey.GENRE) }.getOrNull()?.trim()
                                val tagTrack = runCatching { tag.getFirst(org.jaudiotagger.tag.FieldKey.TRACK) }.getOrNull()?.trim()

                                if (!tagTitle.isNullOrBlank()) {
                                    title = CharsetUtils.sanitizeText(tagTitle)
                                }
                                if (!tagArtist.isNullOrBlank() && (artist.isBlank() || artist.equals("<unknown>", ignoreCase = true) || extension == "wav")) {
                                    artist = CharsetUtils.sanitizeText(tagArtist)
                                }
                                if (!tagAlbum.isNullOrBlank() && (album.isBlank() || album.equals("<unknown>", ignoreCase = true) || extension == "wav")) {
                                    album = CharsetUtils.sanitizeText(tagAlbum)
                                }
                                if (!tagGenre.isNullOrBlank() && (genre.isNullOrBlank() || extension == "wav")) {
                                    genre = CharsetUtils.sanitizeText(tagGenre)
                                }
                                if (!tagTrack.isNullOrBlank() && (trackNumber == 0 || extension == "wav")) {
                                    trackNumber = tagTrack.substringBefore("/").toIntOrNull() ?: trackNumber
                                }
                            }
                        }
                    } catch (_: Exception) {
                        if (bitrate != null && bitrate >= 2304000) {
                            isHiRes = true
                        }
                    }
                }

                val contentUri: Uri = ContentUris.withAppendedId(collection, id)

                // Use the song's specific contentUri as albumArtUri so AudioThumbnailFetcher extracts
                // the per-song embedded picture rather than leaking shared MediaStore albumart (Fixes Issue #106)
                val albumArtUri = contentUri

                songList.add(
                    Song(
                        id, albumId, title, artist, album, duration, contentUri, data,
                        dateAdded, albumArtUri, genre, folderName, isHiFi, coverUrl, isFavorite, null,
                        format, bitrate, trackNumber, isHiRes, sampleRate, bitDepth
                    )
                )
            }
        }

        // Direct disk scan fallback for audio files on storage that haven't been indexed by MediaStore yet
        val indexedPaths = songList.map { it.path.trim() }.filter { it.isNotBlank() }.toSet()
        val missingFiles = mutableSetOf<File>()

        fun scanDirForMissing(dir: File, maxDepth: Int = 8) {
            if (maxDepth <= 0 || !dir.exists() || !dir.canRead()) return
            val list = dir.listFiles() ?: return
            for (f in list) {
                if (f.isDirectory) {
                    if (!f.name.startsWith(".")) {
                        scanDirForMissing(f, maxDepth - 1)
                    }
                } else if (f.isFile && f.length() > 0L && f.extension.lowercase() in audioExtensions) {
                    val abs = f.absolutePath.trim()
                    if (!indexedPaths.contains(abs)) {
                        missingFiles.add(f)
                    }
                }
            }
        }

        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (musicDir.exists()) scanDirForMissing(musicDir)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists()) scanDirForMissing(downloadsDir)

        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documentsDir.exists()) scanDirForMissing(documentsDir)

        val audioFolderUriStr = settingsManager.musicFolderUri
        if (!audioFolderUriStr.isNullOrBlank()) {
            try {
                val uri = Uri.parse(audioFolderUriStr)
                val pathSegment = uri.lastPathSegment?.substringAfterLast(":")
                if (!pathSegment.isNullOrBlank()) {
                    val customFolder = File(Environment.getExternalStorageDirectory(), pathSegment)
                    if (customFolder.exists()) scanDirForMissing(customFolder)
                }
            } catch (_: Exception) {}
        }

        if (missingFiles.isNotEmpty()) {
            val missingPaths = mutableListOf<String>()
            for (f in missingFiles) {
                try {
                    val resolved = SongResolver.resolveSongFromUri(context, Uri.fromFile(f))
                    if (resolved != null) {
                        songList.add(resolved)
                        missingPaths.add(f.absolutePath)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (missingPaths.isNotEmpty()) {
                try {
                    MediaScannerConnection.scanFile(context, missingPaths.toTypedArray(), null, null)
                } catch (_: Exception) {}
            }
        }

        songList.sortBy { it.title.lowercase() }
        saveToCache(songList)
        songList
    }

    suspend fun refreshLibrary(): List<Song> = withContext(Dispatchers.IO) {
        if (!hasReadPermission()) return@withContext emptyList()

        val audioExtensions = setOf("mp3", "flac", "wav", "aac", "m4a", "ogg", "opus", "wma", "alac", "dsf", "dff")
        val filesToScan = mutableSetOf<String>()

        fun collectAudioFiles(dir: File, maxDepth: Int = 8) {
            if (maxDepth <= 0 || !dir.exists() || !dir.canRead()) return
            val list = dir.listFiles() ?: return
            for (f in list) {
                if (f.isDirectory) {
                    if (!f.name.startsWith(".")) {
                        collectAudioFiles(f, maxDepth - 1)
                    }
                } else if (f.isFile && f.length() > 0L && f.extension.lowercase() in audioExtensions) {
                    filesToScan.add(f.absolutePath)
                }
            }
        }

        // Recursively find all audio files in standard music directories
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (musicDir.exists()) collectAudioFiles(musicDir)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists()) collectAudioFiles(downloadsDir)

        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documentsDir.exists()) collectAudioFiles(documentsDir)

        val audioFolderUriStr = settingsManager.musicFolderUri
        if (!audioFolderUriStr.isNullOrBlank()) {
            try {
                val uri = Uri.parse(audioFolderUriStr)
                val pathSegment = uri.lastPathSegment?.substringAfterLast(":")
                if (!pathSegment.isNullOrBlank()) {
                    val customFolder = File(Environment.getExternalStorageDirectory(), pathSegment)
                    if (customFolder.exists()) collectAudioFiles(customFolder)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        getCachedSongs().forEach {
            if (it.path.isNotBlank() && File(it.path).exists()) {
                filesToScan.add(it.path)
            }
        }

        // Scan files in batches with proper latch countdown to ensure all are processed by MediaScanner
        if (filesToScan.isNotEmpty()) {
            val chunkList = filesToScan.chunked(200)
            for (chunk in chunkList) {
                val latch = CountDownLatch(chunk.size)
                MediaScannerConnection.scanFile(
                    context,
                    chunk.toTypedArray(),
                    null
                ) { _, _ -> latch.countDown() }
                latch.await(15, TimeUnit.SECONDS)
            }
        }

        syncSongs()
    }
}