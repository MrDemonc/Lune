package com.demonlab.lune.tools

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lune_settings", Context.MODE_PRIVATE)

    private val _customTitleFlow = MutableStateFlow(prefs.getString("custom_title", "") ?: "")
    val customTitleFlow: StateFlow<String> = _customTitleFlow.asStateFlow()

    var isFirstRun: Boolean
        get() = prefs.getBoolean("is_first_run", true)
        set(value) = prefs.edit().putBoolean("is_first_run", value).apply()

    var optionsOrder: String
        get() = prefs.getString("options_order", "like,shuffle,repeat,crossfade,automix,timer,eq") ?: "like,shuffle,repeat,crossfade,automix,timer,eq"
        set(value) = prefs.edit().putString("options_order", value).apply()

    var hiddenFolders: Set<String>
        get() = prefs.getStringSet("hidden_folders", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("hidden_folders", value).apply()

    // Deprecated: used for backward compatibility if needed, but we use hiddenFolders now
    var showWhatsappAudio: Boolean
        get() = !hiddenFolders.contains("WhatsApp Audio")
        set(value) {
            val folders = hiddenFolders.toMutableSet()
            if (value) folders.remove("WhatsApp Audio") else folders.add("WhatsApp Audio")
            hiddenFolders = folders
        }

    var enableHiFi: Boolean
        get() = prefs.getBoolean("enable_hifi", true)
        set(value) = prefs.edit().putBoolean("enable_hifi", value).apply()

    var downloadCovers: Boolean
        get() = prefs.getBoolean("download_covers", true)
        set(value) = prefs.edit().putBoolean("download_covers", value).apply()

    var themeMode: Int
        get() = prefs.getInt("theme_mode", 0) // 0: Auto, 1: Light, 2: Dark
        set(value) = prefs.edit().putInt("theme_mode", value).apply()

    var forceDarkMode: Boolean
        get() = prefs.getBoolean("force_dark_mode", false)
        set(value) = prefs.edit().putBoolean("force_dark_mode", value).apply()

    var isShuffle: Boolean
        get() = prefs.getBoolean("is_shuffle", false)
        set(value) = prefs.edit().putBoolean("is_shuffle", value).apply()

    var isCrossfade: Boolean
        get() = prefs.getBoolean("is_crossfade", false)
        set(value) = prefs.edit().putBoolean("is_crossfade", value).apply()

    var isAutomix: Boolean
        get() = prefs.getBoolean("is_automix", false)
        set(value) = prefs.edit().putBoolean("is_automix", value).apply()

    var repeatMode: Int
        get() = prefs.getInt("repeat_mode", 0) // 0: Off, 1: One, 2: All
        set(value) = prefs.edit().putInt("repeat_mode", value).apply()

    var isEqEnabled: Boolean
        get() = prefs.getBoolean("is_eq_enabled", false)
        set(value) = prefs.edit().putBoolean("is_eq_enabled", value).apply()

    var eqBandLevels: String
        get() = prefs.getString("eq_band_levels", "") ?: ""
        set(value) = prefs.edit().putString("eq_band_levels", value).apply()

    var activeEqPresetName: String
        get() = prefs.getString("active_eq_preset_name", "") ?: ""
        set(value) = prefs.edit().putString("active_eq_preset_name", value).apply()

    var isBassBoostEnabled: Boolean
        get() = prefs.getBoolean("is_bass_enabled", false)
        set(value) = prefs.edit().putBoolean("is_bass_enabled", value).apply()

    var isSpatialAudioEnabled: Boolean
        get() = prefs.getBoolean("is_spatial_audio_enabled", false)
        set(value) {
            prefs.edit().putBoolean("is_spatial_audio_enabled", value).apply()
        }

    var language: String
        get() = prefs.getString("language", "system") ?: "system"
        set(value) = prefs.edit().putString("language", value).apply()

    var customTitle: String
        get() = prefs.getString("custom_title", "") ?: ""
        set(value) {
            prefs.edit().putString("custom_title", value).apply()
            _customTitleFlow.value = value
        }

    var lastPlayedSongId: Long
        get() = prefs.getLong("last_played_song_id", -1L)
        set(value) = prefs.edit().putLong("last_played_song_id", value).apply()

    var lastCategory: String
        get() = prefs.getString("last_category", "") ?: ""
        set(value) = prefs.edit().putString("last_category", value).apply()

    var lastPlaylistId: Long
        get() = prefs.getLong("last_playlist_id", -1L)
        set(value) = prefs.edit().putLong("last_playlist_id", value).apply()

    var activeCategory: String
        get() = prefs.getString("active_category", "") ?: ""
        set(value) = prefs.edit().putString("active_category", value).apply()

    var isFullPlayerVisualizerEnabled: Boolean
        get() = prefs.getBoolean("is_full_player_visualizer_enabled", false)
        set(value) = prefs.edit().putBoolean("is_full_player_visualizer_enabled", value).apply()

    var isMiniPlayerVisualizerEnabled: Boolean
        get() = prefs.getBoolean("is_mini_player_visualizer_enabled", false)
        set(value) = prefs.edit().putBoolean("is_mini_player_visualizer_enabled", value).apply()

    var isCinematicPlayerEnabled: Boolean
        get() = prefs.getBoolean("is_cinematic_player_enabled", false)
        set(value) = prefs.edit().putBoolean("is_cinematic_player_enabled", value).apply()

    var dailyListeningTime: Long
        get() = prefs.getLong("daily_listening_time", 0L)
        set(value) = prefs.edit().putLong("daily_listening_time", value).apply()

    var lastStatsResetTimestamp: Long
        get() = prefs.getLong("last_stats_reset_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_stats_reset_timestamp", value).apply()

    fun getPlaylistShuffle(playlistId: Long): Boolean {
        return prefs.getBoolean("shuffle_playlist_$playlistId", false)
    }

    fun setPlaylistShuffle(playlistId: Long, value: Boolean) {
        prefs.edit().putBoolean("shuffle_playlist_$playlistId", value).apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
