package com.demonlab.lune.tools

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
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


    var themeMode: Int
        get() = prefs.getInt("theme_mode", 0) // 0: Auto, 1: Light, 2: Dark
        set(value) = prefs.edit().putInt("theme_mode", value).apply()

    var forceDarkMode: Boolean
        get() = prefs.getBoolean("force_dark_mode", false)
        set(value) = prefs.edit().putBoolean("force_dark_mode", value).apply()

    var sortOption: String
        get() = prefs.getString("sort_option", "ALPHABETICAL") ?: "ALPHABETICAL"
        set(value) = prefs.edit().putString("sort_option", value).apply()

    fun getSortOption(key: String): String {
        return prefs.getString("sort_option_$key", sortOption) ?: sortOption
    }

    fun setSortOption(key: String, option: String) {
        prefs.edit().putString("sort_option_$key", option).apply()
    }

    var albumViewStyle: Int
        get() = prefs.getInt("album_view_style", 0) // 0 = Grid, 1 = Carousel
        set(value) = prefs.edit().putInt("album_view_style", value).apply()

    var albumBrowseMode: Boolean
        get() = prefs.getBoolean("album_browse_mode", false) // false = Artists, true = Albums
        set(value) = prefs.edit().putBoolean("album_browse_mode", value).apply()

    var isSectionCustomizationEnabled: Boolean
        get() = prefs.getBoolean("section_customization_enabled", false)
        set(value) = prefs.edit().putBoolean("section_customization_enabled", value).apply()

    var hiddenSectionTabs: Set<String>
        get() = prefs.getStringSet("hidden_section_tabs", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("hidden_section_tabs", value).apply()

    var isSortAscending: Boolean
        get() = prefs.getBoolean("is_sort_ascending", true)
        set(value) = prefs.edit().putBoolean("is_sort_ascending", value).apply()

    fun getIsSortAscending(key: String): Boolean {
        return prefs.getBoolean("is_sort_ascending_$key", isSortAscending)
    }

    fun setIsSortAscending(key: String, ascending: Boolean) {
        prefs.edit().putBoolean("is_sort_ascending_$key", ascending).apply()
    }

    var isShuffle: Boolean
        get() = prefs.getBoolean("is_shuffle", false)
        set(value) = prefs.edit().putBoolean("is_shuffle", value).apply()

    var isCrossfade: Boolean
        get() = prefs.getBoolean("is_crossfade", false)
        set(value) = prefs.edit().putBoolean("is_crossfade", value).apply()

    var isCrossfadeCustomDuration: Boolean
        get() = prefs.getBoolean("is_crossfade_custom_duration", false)
        set(value) = prefs.edit().putBoolean("is_crossfade_custom_duration", value).apply()

    var crossfadeDurationSeconds: Int
        get() = prefs.getInt("crossfade_duration_seconds", 12)
        set(value) = prefs.edit().putInt("crossfade_duration_seconds", value).apply()

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

    var lastEqPresetName: String
        get() = prefs.getString("last_eq_preset_name", "") ?: ""
        set(value) = prefs.edit().putString("last_eq_preset_name", value).apply()

    var lastEqBandLevels: String
        get() = prefs.getString("last_eq_band_levels", "") ?: ""
        set(value) = prefs.edit().putString("last_eq_band_levels", value).apply()

    var customEqPresetsJson: String
        get() = prefs.getString("custom_eq_presets", "[]") ?: "[]"
        set(value) = prefs.edit().putString("custom_eq_presets", value).apply()

    var isBassBoostEnabled: Boolean
        get() = prefs.getBoolean("is_bass_enabled", false)
        set(value) = prefs.edit().putBoolean("is_bass_enabled", value).apply()

    var bassBoostLevel: Int
        get() = prefs.getInt("bass_boost_level", 900)
        set(value) = prefs.edit().putInt("bass_boost_level", value).apply()

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
        
    var isHapticVibrationEnabled: Boolean
        get() = prefs.getBoolean("is_haptic_vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("is_haptic_vibration_enabled", value).apply()

    var isSongInfoEnabled: Boolean
        get() = prefs.getBoolean("is_song_info_enabled", false)
        set(value) = prefs.edit().putBoolean("is_song_info_enabled", value).apply()

    private val _isBitrateOnList = mutableStateOf(prefs.getBoolean("is_bitrate_on_list", true))
    var isBitrateOnList: Boolean
        get() = _isBitrateOnList.value
        set(value) {
            _isBitrateOnList.value = value
            prefs.edit().putBoolean("is_bitrate_on_list", value).apply()
        }

    private val _isBitrateOnPlayer = mutableStateOf(prefs.getBoolean("is_bitrate_on_player", true))
    var isBitrateOnPlayer: Boolean
        get() = _isBitrateOnPlayer.value
        set(value) {
            _isBitrateOnPlayer.value = value
            prefs.edit().putBoolean("is_bitrate_on_player", value).apply()
        }

    private val _isOptionsBarVisible = mutableStateOf(prefs.getBoolean("is_options_bar_visible", true))
    var isOptionsBarVisible: Boolean
        get() = _isOptionsBarVisible.value
        set(value) {
            _isOptionsBarVisible.value = value
            prefs.edit().putBoolean("is_options_bar_visible", value).apply()
        }

    private val _isMiniPlayerMinimized = mutableStateOf(prefs.getBoolean("is_mini_player_minimized", false))
    var isMiniPlayerMinimized: Boolean
        get() = _isMiniPlayerMinimized.value
        set(value) {
            _isMiniPlayerMinimized.value = value
            prefs.edit().putBoolean("is_mini_player_minimized", value).apply()
        }

    var isBlurEnabled: Boolean
        get() = prefs.getBoolean("is_blur_enabled", true)
        set(value) = prefs.edit().putBoolean("is_blur_enabled", value).apply()

    var isBlurDarkMode: Boolean
        get() = prefs.getBoolean("is_blur_dark_mode", true)
        set(value) = prefs.edit().putBoolean("is_blur_dark_mode", value).apply()

    var isBlurLightMode: Boolean
        get() = prefs.getBoolean("is_blur_light_mode", false)
        set(value) = prefs.edit().putBoolean("is_blur_light_mode", value).apply()

    var isBlurCinematicMode: Boolean
        get() = prefs.getBoolean("is_blur_cinematic_mode", false)
        set(value) = prefs.edit().putBoolean("is_blur_cinematic_mode", value).apply()

    var isBlurControlsEnabled: Boolean
        get() = prefs.getBoolean("is_blur_controls_enabled", false)
        set(value) = prefs.edit().putBoolean("is_blur_controls_enabled", value).apply()

    var lyricsTextAlignment: Int
        get() = prefs.getInt("lyrics_text_alignment", 0)
        set(value) = prefs.edit().putInt("lyrics_text_alignment", value).apply()

    var lyricsSpeedIndex: Int
        get() = prefs.getInt("lyrics_speed_index", 0)
        set(value) = prefs.edit().putInt("lyrics_speed_index", value).apply()

    var isGesturesEnabled: Boolean
        get() = prefs.getBoolean("is_gestures_enabled", false)
        set(value) = prefs.edit().putBoolean("is_gestures_enabled", value).apply()

    var swipeUpAction: Int
        get() = prefs.getInt("swipe_up_action", 0)
        set(value) = prefs.edit().putInt("swipe_up_action", value).apply()

    var dailyListeningTime: Long
        get() = prefs.getLong("daily_listening_time", 0L)
        set(value) = prefs.edit().putLong("daily_listening_time", value).apply()

    var lastStatsResetTimestamp: Long
        get() = prefs.getLong("last_stats_reset_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_stats_reset_timestamp", value).apply()

    var musicFolderUri: String?
        get() = prefs.getString("music_folder_uri", null)
        set(value) = prefs.edit().putString("music_folder_uri", value).apply()
        
    var isInitialFolderScanPending: Boolean
        get() = prefs.getBoolean("is_initial_folder_scan_pending", false)
        set(value) = prefs.edit().putBoolean("is_initial_folder_scan_pending", value).apply()

    var showAllFoldersOnStart: Boolean
        get() = prefs.getBoolean("show_all_folders_on_start", false)
        set(value) = prefs.edit().putBoolean("show_all_folders_on_start", value).apply()

    var folderHierarchyMode: Boolean
        get() = prefs.getBoolean("folder_hierarchy_mode", false)
        set(value) = prefs.edit().putBoolean("folder_hierarchy_mode", value).apply()

    var useCustomColors: Boolean
        get() = prefs.getBoolean("use_custom_colors", false)
        set(value) = prefs.edit().putBoolean("use_custom_colors", value).apply()

    var customColorPalette: Int
        get() = prefs.getInt("custom_color_palette", 0)
        set(value) = prefs.edit().putInt("custom_color_palette", value).apply()

    var showBackupWarning: Boolean
        get() = prefs.getBoolean("show_backup_warning", true)
        set(value) = prefs.edit().putBoolean("show_backup_warning", value).apply()

    var useAmoledPitchBlack: Boolean
        get() = prefs.getBoolean("use_amoled_pitch_black", false)
        set(value) = prefs.edit().putBoolean("use_amoled_pitch_black", value).apply()

    var coverShape: Int
        get() = prefs.getInt("cover_shape", 0)
        set(value) = prefs.edit().putInt("cover_shape", value).apply()

    var coverScale: Float
        get() = prefs.getFloat("cover_scale", 1.0f)
        set(value) = prefs.edit().putFloat("cover_scale", value).apply()

    var coverSpin: Boolean
        get() = prefs.getBoolean("cover_spin", true)
        set(value) = prefs.edit().putBoolean("cover_spin", value).apply()

    var coverVinylEffect: Boolean
        get() = prefs.getBoolean("cover_vinyl_effect", false)
        set(value) = prefs.edit().putBoolean("cover_vinyl_effect", value).apply()

    var controlsIconStyle: Int
        get() = prefs.getInt("controls_icon_style", 0) // 0: Default Android, 1: play_2, 2: play_3
        set(value) = prefs.edit().putInt("controls_icon_style", value).apply()

    var isControlsFilled: Boolean
        get() = prefs.getBoolean("is_controls_filled", false)
        set(value) = prefs.edit().putBoolean("is_controls_filled", value).apply()

    var useCustomControlsColor: Boolean
        get() = prefs.getBoolean("use_custom_controls_color", false)
        set(value) = prefs.edit().putBoolean("use_custom_controls_color", value).apply()

    var controlsColorPalette: Int
        get() = prefs.getInt("controls_color_palette", 0) // 0: Material You/App default, 1: Sunset Peach, 2: Sage Green, etc.
        set(value) = prefs.edit().putInt("controls_color_palette", value).apply()

    var playbackSpeed: Float
        get() = prefs.getFloat("playback_speed", 1.0f)
        set(value) = prefs.edit().putFloat("playback_speed", value).apply()

    var playbackPitch: Float
        get() = prefs.getFloat("playback_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("playback_pitch", value).apply()

    var isTuning432: Boolean
        get() = prefs.getBoolean("tuning_432", false)
        set(value) = prefs.edit().putBoolean("tuning_432", value).apply()

    var reverbPreset: Int
        get() = prefs.getInt("reverb_preset", 0)
        set(value) = prefs.edit().putInt("reverb_preset", value).apply()

    var balance: Float
        get() = prefs.getFloat("balance", 0.5f)
        set(value) = prefs.edit().putFloat("balance", value).apply()

    var dynamicsPreset: Int
        get() = prefs.getInt("dynamics_preset", 0)
        set(value) = prefs.edit().putInt("dynamics_preset", value).apply()

    var isLoudnessEnabled: Boolean
        get() = prefs.getBoolean("is_loudness_enabled", false)
        set(value) = prefs.edit().putBoolean("is_loudness_enabled", value).apply()

    var loudnessGain: Int
        get() = prefs.getInt("loudness_gain", 0)
        set(value) = prefs.edit().putInt("loudness_gain", value).apply()

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