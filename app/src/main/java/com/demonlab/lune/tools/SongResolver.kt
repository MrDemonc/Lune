package com.demonlab.lune.tools

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.demonlab.lune.R
import java.io.File
import kotlin.math.abs

object SongResolver {
    private const val TAG = "SongResolver"

    fun resolveSongFromUri(context: Context, uri: Uri): Song? {
        // 1. Check if it's already a MediaStore URI with ID
        val parsedId = runCatching {
            if (uri.authority == "media" || uri.toString().contains("audio/media")) {
                ContentUris.parseId(uri)
            } else null
        }.getOrNull()

        val provider = MusicProvider(context)
        val cachedSongs = runCatching { provider.getCachedSongs() }.getOrDefault(emptyList())

        if (parsedId != null && parsedId > 0) {
            val cached = cachedSongs.find { it.id == parsedId }
            if (cached != null) return cached
        }

        // 2. Check if cached by matching URI or file path
        val uriStr = uri.toString()
        val pathStr = uri.path
        val cachedMatch = cachedSongs.find { 
            it.uri == uri || (pathStr != null && it.path.isNotBlank() && it.path == pathStr) 
        }
        if (cachedMatch != null) return cachedMatch

        // 3. Fallback: Parse directly via MediaMetadataRetriever and ContentResolver
        return parseFromMetadata(context, uri, parsedId)
    }

    private fun parseFromMetadata(context: Context, uri: Uri, existingId: Long?): Song? {
        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var duration = 0L
        var genre: String? = null
        var bitrate: Int? = null
        var trackNumber = 0
        var mimeType: String? = null

        try {
            if (uri.scheme == "file") {
                val path = uri.path
                if (path != null && File(path).exists()) {
                    retriever.setDataSource(path)
                } else {
                    retriever.setDataSource(context, uri)
                }
            } else {
                retriever.setDataSource(context, uri)
            }

            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull() ?: 0
            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        } catch (e: Exception) {
            Log.w(TAG, "Failed extracting metadata with retriever for $uri", e)
        } finally {
            runCatching { retriever.release() }
        }

        // Determine file name and extension
        var fileName: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed resolving display name from content resolver", e)
            }
        }
        if (fileName.isNullOrBlank()) {
            fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "audio_track"
        }

        val nameWithoutExt = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".", "").lowercase()

        val finalTitle = if (!title.isNullOrBlank()) CharsetUtils.sanitizeText(title) else nameWithoutExt
        val finalArtist = if (!artist.isNullOrBlank()) CharsetUtils.sanitizeText(artist) else context.getString(R.string.unknown_artist)
        val finalAlbum = if (!album.isNullOrBlank()) CharsetUtils.sanitizeText(album) else context.getString(R.string.unknown_album)

        val filePath = if (uri.scheme == "file") uri.path ?: "" else ""
        val folderName = if (filePath.isNotBlank()) {
            filePath.substringBeforeLast("/").substringAfterLast("/").ifEmpty { "Downloads" }
        } else {
            "Downloads"
        }

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
            else -> when {
                mimeType?.contains("flac", ignoreCase = true) == true -> "FLAC"
                mimeType?.contains("ogg", ignoreCase = true) == true -> "OGG"
                mimeType?.contains("mp4", ignoreCase = true) == true || mimeType?.contains("m4a", ignoreCase = true) == true -> "AAC"
                mimeType?.contains("wav", ignoreCase = true) == true -> "WAV"
                else -> extension.uppercase().ifEmpty { "AUDIO" }
            }
        }

        val isHiFi = extension in listOf("flac", "wav", "alac", "dsf", "dff") || (bitrate != null && bitrate > 320000)

        // Stable non-zero synthetic ID for external files
        val id = existingId ?: -abs(uri.toString().hashCode().toLong().let { if (it == 0L) 1L else it })

        return Song(
            id = id,
            albumId = -1L,
            title = finalTitle,
            artist = finalArtist,
            album = finalAlbum,
            duration = duration,
            uri = uri,
            path = filePath,
            dateAdded = System.currentTimeMillis(),
            albumArtUri = uri,
            genre = genre,
            folderName = folderName,
            isHiFi = isHiFi,
            coverUrl = null,
            isFavorite = false,
            lyrics = null,
            format = format,
            bitrate = bitrate,
            trackNumber = trackNumber,
            isHiRes = isHiFi
        )
    }

    fun resolveSiblingSongs(context: Context, currentSong: Song): List<Song> {
        if (currentSong.path.isNotBlank()) {
            val file = File(currentSong.path)
            val parent = file.parentFile
            if (parent != null && parent.isDirectory && parent.canRead()) {
                val audioExtensions = setOf("mp3", "flac", "wav", "m4a", "ogg", "opus", "aac", "wma", "alac")
                val files = parent.listFiles { f ->
                    f.isFile && f.extension.lowercase() in audioExtensions
                }?.sortedBy { it.name } ?: emptyList()

                if (files.size > 1) {
                    return files.map { f ->
                        if (f.absolutePath == file.absolutePath) {
                            currentSong
                        } else {
                            val uri = Uri.fromFile(f)
                            resolveSongFromUri(context, uri) ?: Song(
                                id = -abs(uri.toString().hashCode().toLong().let { if (it == 0L) 1L else it }),
                                albumId = -1L,
                                title = f.nameWithoutExtension,
                                artist = context.getString(R.string.unknown_artist),
                                album = context.getString(R.string.unknown_album),
                                duration = 0L,
                                uri = uri,
                                path = f.absolutePath,
                                albumArtUri = uri,
                                folderName = parent.name,
                                format = f.extension.uppercase()
                            )
                        }
                    }
                }
            }
        }
        return listOf(currentSong)
    }
}
