package com.demonlab.lune.ai.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.demonlab.lune.ai.model.SongInteraction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AiTelemetryStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lune_ai_telemetry", Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder().create()
    private val storageScope = CoroutineScope(Dispatchers.IO + Job())
    private val memoryCache = ConcurrentHashMap<Long, SongInteraction>()
    private var saveJob: Job? = null

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            val json = prefs.getString("interactions_map", null)
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<Map<String, SongInteraction>>() {}.type
                val loaded: Map<String, SongInteraction>? = gson.fromJson(json, type)
                if (loaded != null) {
                    for ((k, v) in loaded) {
                        val songId = k.toLongOrNull() ?: v.songId
                        if (songId > 0L) {
                            if (v.nextSongTransitions == null) {
                                v.nextSongTransitions = mutableMapOf()
                            }
                            memoryCache[songId] = v
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AiTelemetryStorage", "Error loading telemetry, resetting storage: ${e.message}", e)
            try {
                prefs.edit().remove("interactions_map").apply()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun getInteraction(songId: Long): SongInteraction {
        return memoryCache.computeIfAbsent(songId) { SongInteraction(songId = it) }.also {
            if (it.nextSongTransitions == null) {
                it.nextSongTransitions = mutableMapOf()
            }
        }
    }

    fun getAllInteractions(): Map<Long, SongInteraction> {
        return memoryCache
    }

    fun recordInteraction(songId: Long, block: (SongInteraction) -> Unit) {
        val interaction = getInteraction(songId)
        synchronized(interaction) {
            if (interaction.nextSongTransitions == null) {
                interaction.nextSongTransitions = mutableMapOf()
            }
            block(interaction)
        }
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = storageScope.launch {
            delay(1500L) // Debounce writes
            try {
                val snapshot = HashMap<String, SongInteraction>(memoryCache.size)
                for ((id, interaction) in memoryCache) {
                    snapshot[id.toString()] = interaction
                }
                val json = gson.toJson(snapshot)
                prefs.edit().putString("interactions_map", json).apply()
            } catch (e: Exception) {
                Log.e("AiTelemetryStorage", "Error saving telemetry: ${e.message}", e)
            }
        }
    }

    fun clearAll() {
        memoryCache.clear()
        prefs.edit().clear().apply()
    }
}
