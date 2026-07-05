package com.demonlab.lune.tools

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class SavedPlaybackState(
    val currentSongId: Long = -1L,
    val playbackPositionMs: Long = 0L,
    val queueIds: List<Long> = emptyList(),
    val playlistId: Long? = null,
    val playlistName: String? = null,
    val playlistCategory: String? = null,
    val shuffledIndices: List<Int> = emptyList(),
    val shufflePosition: Int = -1,
    val frontQueueInsertCount: Int = 0,
    val wasPlaying: Boolean = false,
    val queueSections: List<QueuedSection> = emptyList(),
)

class PlaybackStateSaver(private val prefs: SharedPreferences) {

    private val gson = Gson()

    fun save(state: SavedPlaybackState) {
        prefs.edit().putString(KEY_SAVED_STATE, gson.toJson(state)).apply()
    }

    fun restore(): SavedPlaybackState? {
        val json = prefs.getString(KEY_SAVED_STATE, null) ?: return null
        return try {
            gson.fromJson(json, SavedPlaybackState::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_SAVED_STATE).apply()
    }

    companion object {
        private const val KEY_SAVED_STATE = "saved_playback_state"
    }
}
