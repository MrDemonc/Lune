package com.demonlab.lune.tools

import com.demonlab.lune.tools.Song

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.net.Uri
import com.demonlab.lune.data.MusicDatabase
import com.demonlab.lune.data.SongOverride
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.File
import kotlinx.coroutines.runBlocking

class UriTypeAdapter : TypeAdapter<Uri>() {
    override fun write(out: JsonWriter, value: Uri?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString())
        }
    }

    override fun read(inReader: JsonReader): Uri? {
        if (inReader.peek() == com.google.gson.stream.JsonToken.NULL) {
            inReader.nextNull()
            return null
        }
        return Uri.parse(inReader.nextString())
    }
}

class MusicProvider(private val context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    private val database = MusicDatabase.getDatabase(context)
    
    private val cacheFile = File(context.filesDir, "songs_cache.json")
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter())
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

    fun syncSongs(): List<Song> {
        val showWhatsapp = settingsManager.showWhatsappAudio
        val songList = mutableListOf<Song>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                MediaStore.Audio.Media.GENRE
            } else {
                MediaStore.Audio.Media.ARTIST // Fallback if genre not easily available
            }
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val overrides = runBlocking { database.songOverrideDao().getAllOverrides() }.associateBy { it.songId }

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
            val genreColumn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                var title = cursor.getString(titleColumn)
                var artist = cursor.getString(artistColumn)
                var album = cursor.getString(albumColumn)
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val data = cursor.getString(dataColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                var genre = if (genreColumn != -1) cursor.getString(genreColumn) else null

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

                val folderName = data.substringBeforeLast("/").substringAfterLast("/")
                val extension = data.substringAfterLast(".").lowercase()
                val isHiFiFile = extension == "flac" || extension == "wav" || extension == "alac"
                val isHiFi = settingsManager.enableHiFi && isHiFiFile

                val contentUri: Uri = ContentUris.withAppendedId(
                    collection,
                    id
                )
                
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                songList.add(Song(id, albumId, title, artist, album, duration, contentUri, data, dateAdded, albumArtUri, genre, folderName, isHiFi, coverUrl, isFavorite))
            }
        }
        saveToCache(songList)
        return songList
    }
}
