package com.demonlab.lune.tools

import android.content.Context
import android.net.Uri
import android.util.Log
import com.demonlab.lune.data.MusicDatabase
import com.demonlab.lune.data.SongOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MetadataManager(private val context: Context) {
    private val database = MusicDatabase.getDatabase(context)

    suspend fun updateSongMetadata(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        genre: String?,
        coverUri: String?
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val existing = database.songOverrideDao().getOverrideForSong(songId)
            val override = SongOverride(
                songId = songId,
                title = title,
                artist = artist,
                album = album,
                genre = genre,
                coverUri = coverUri,
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
        return@withContext try {
            val existing = database.songOverrideDao().getOverrideForSong(songId)
            if (existing != null) {
                database.songOverrideDao().insertOverride(existing.copy(isFavorite = isFavorite))
            } else {
                database.songOverrideDao().insertOverride(SongOverride(songId = songId, isFavorite = isFavorite))
            }
            true
        } catch (e: Exception) {
            Log.e("MetadataManager", "Error updating favorite status", e)
            false
        }
    }

    suspend fun clearMetadataOverride(songId: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val existing = database.songOverrideDao().getOverrideForSong(songId)
            if (existing != null) {
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

    suspend fun updateCoverUrl(songId: Long, coverUrl: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val existing = database.songOverrideDao().getOverrideForSong(songId)
            if (existing != null) {
                database.songOverrideDao().insertOverride(existing.copy(coverUri = coverUrl))
            } else {
                database.songOverrideDao().insertOverride(SongOverride(songId = songId, coverUri = coverUrl))
            }
            true
        } catch (e: Exception) {
            Log.e("MetadataManager", "Error updating cover url", e)
            false
        }
    }

    // Stub for future custom cover persistence logic if needed
    fun saveCustomCover(songId: Long, imageUri: Uri): Boolean {
        return true 
    }
}
