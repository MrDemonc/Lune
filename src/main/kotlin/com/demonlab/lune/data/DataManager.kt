package com.demonlab.lune.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

class DataManager private constructor() {
    private val dataDir = File(System.getProperty("user.home"), ".local/share/lune").apply { mkdirs() }
    private val cacheDir = File(System.getProperty("user.home"), ".cache/lune").apply { mkdirs() }

    private val playlistsFile = File(dataDir, "playlists.json")
    private val favoritesFile = File(dataDir, "favorites.json")
    private val overridesFile = File(dataDir, "overrides.json")
    private val statsFile = File(dataDir, "stats.json")
    private val stateFile = File(dataDir, "state.json")

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    val coversCacheDir = File(cacheDir, "covers").apply { mkdirs() }

    // Playlists
    fun getPlaylists(): MutableList<Playlist> {
        if (!playlistsFile.exists()) return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<Playlist>>() {}.type
            gson.fromJson(playlistsFile.readText(), type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun savePlaylists(playlists: List<Playlist>) {
        try {
            playlistsFile.writeText(gson.toJson(playlists))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Favorites
    fun getFavorites(): MutableSet<Long> {
        if (!favoritesFile.exists()) return mutableSetOf()
        return try {
            val type = object : TypeToken<MutableSet<Long>>() {}.type
            gson.fromJson(favoritesFile.readText(), type) ?: mutableSetOf()
        } catch (e: Exception) {
            mutableSetOf()
        }
    }

    fun saveFavorites(favorites: Set<Long>) {
        try {
            favoritesFile.writeText(gson.toJson(favorites))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Song Overrides (tag edits, cover overrides)
    fun getOverrides(): MutableMap<Long, SongOverride> {
        if (!overridesFile.exists()) return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<Long, SongOverride>>() {}.type
            gson.fromJson(overridesFile.readText(), type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    fun saveOverrides(overrides: Map<Long, SongOverride>) {
        try {
            overridesFile.writeText(gson.toJson(overrides))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Playback Stats
    fun getStats(): MutableMap<String, PlaybackStats> {
        if (!statsFile.exists()) return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<String, PlaybackStats>>() {}.type
            gson.fromJson(statsFile.readText(), type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    fun recordPlay(songId: Long, durationMs: Long, artistName: String) {
        val stats = getStats()
        // Song stat
        val songKey = "SONG_$songId"
        val songStat = stats.getOrPut(songKey) { PlaybackStats(songKey, "SONG") }
        songStat.playCount += 1
        songStat.totalTimeMs += durationMs
        songStat.lastPlayed = System.currentTimeMillis()

        // Artist stat
        if (artistName.isNotBlank()) {
            val artistKey = "ARTIST_$artistName"
            val artistStat = stats.getOrPut(artistKey) { PlaybackStats(artistKey, "ARTIST") }
            artistStat.playCount += 1
            artistStat.totalTimeMs += durationMs
            artistStat.lastPlayed = System.currentTimeMillis()
        }

        try {
            statsFile.writeText(gson.toJson(stats))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Playback State persistence
    data class SavedPlaybackState(
        val lastSongId: Long? = null,
        val lastPositionMs: Long = 0L,
        val queueSongIds: List<Long> = emptyList(),
        val queueIndex: Int = 0
    )

    fun getSavedState(): SavedPlaybackState? {
        if (!stateFile.exists()) return null
        return try {
            gson.fromJson(stateFile.readText(), SavedPlaybackState::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun savePlaybackState(state: SavedPlaybackState) {
        try {
            stateFile.writeText(gson.toJson(state))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val instance: DataManager by lazy { DataManager() }
    }
}
