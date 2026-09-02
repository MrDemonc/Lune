package com.demonlab.lune.ai.storage

import android.content.Context
import android.content.SharedPreferences
import com.demonlab.lune.ai.model.SongInteraction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AiTelemetryStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lune_ai_telemetry", Context.MODE_PRIVATE)
    private val gson = Gson()
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
                val type = object : TypeToken<Map<Long, SongInteraction>>() {}.type
                val loaded: Map<Long, SongInteraction>? = gson.fromJson(json, type)
                if (loaded != null) {
                    memoryCache.putAll(loaded)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getInteraction(songId: Long): SongInteraction {
        return memoryCache.computeIfAbsent(songId) { SongInteraction(songId = it) }
    }

    fun getAllInteractions(): Map<Long, SongInteraction> {
        return memoryCache
    }

    fun recordInteraction(songId: Long, block: (SongInteraction) -> Unit) {
        val interaction = memoryCache.computeIfAbsent(songId) { SongInteraction(songId = it) }
        synchronized(interaction) {
            block(interaction)
        }
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = storageScope.launch {
            delay(1500L) // Debounce writes
            try {
                val snapshot = HashMap(memoryCache)
                val json = gson.toJson(snapshot)
                prefs.edit().putString("interactions_map", json).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAll() {
        memoryCache.clear()
        prefs.edit().clear().apply()
    }
}
