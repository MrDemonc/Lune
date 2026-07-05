package com.demonlab.lune.tools

import android.content.Context
import android.net.Uri
import android.util.Log
import com.demonlab.lune.data.MusicDatabase
import com.demonlab.lune.data.SongOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import java.io.File

class MetadataManager(private val context: Context) {
    private val database = MusicDatabase.getDatabase(context)
    private val coversDir = File(context.filesDir, "covers")

    suspend fun updateSongMetadata(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        genre: String?,
        coverUri: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = database.songOverrideDao().getOverrideForSong(songId)

            val finalCoverUri = if (coverUri != null) {
                if (!coverUri.startsWith("file://${context.filesDir}")) {
                    saveCustomCover(songId, coverUri.toUri())?.toString() ?: coverUri
                } else {
                    coverUri
                }
            } else {
                existing?.coverUri
            }

            val override = SongOverride(
                songId = songId,
                title = title,
                artist = artist,
                album = album,
                genre = genre,
                coverUri = finalCoverUri,
                isFavorite = existing?.isFavorite ?: false
            )
            database.songOverrideDao().insertOverride(override)
            true
        } catch (e: Exception) {
            Log.e("MetadataManager", "Error saving metadata to Room", e)
            false
        }
    }

    suspend fun updateFavoriteStatus(songId: Long, isFavorite: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = database.songOverrideDao().getOverrideForSong(songId)
            val override = existing?.copy(isFavorite = isFavorite)
                ?: SongOverride(songId = songId, isFavorite = isFavorite)

            database.songOverrideDao().insertOverride(override)
            true
        } catch (e: Exception) {
            Log.e("MetadataManager", "Error updating favorite status", e)
            false
        }
    }

    suspend fun clearMetadataOverride(songId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = database.songOverrideDao().getOverrideForSong(songId)
            if (existing != null) {
                // Delete custom cover if it exists
                File(coversDir, "cover_$songId.jpg").takeIf { it.exists() }?.delete()

                // Keep only the songId and isFavorite status, clear other metadata
                val clearedOverride = SongOverride(
                    songId = songId,
                    isFavorite = existing.isFavorite
                )
                database.songOverrideDao().insertOverride(clearedOverride)
            }
            true
        } catch (e: Exception) {
            Log.e("MetadataManager", "Error clearing metadata override", e)
            false
        }
    }

    private fun saveCustomCover(songId: Long, imageUri: Uri): Uri? {
        return try {
            if (!coversDir.exists()) {
                coversDir.mkdirs()
            }
            val coverFile = File(coversDir, "cover_$songId.jpg")
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                coverFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(coverFile)
        } catch (e: Exception) {
            Log.e("MetadataManager", "Error saving custom cover", e)
            null
        }
    }
}