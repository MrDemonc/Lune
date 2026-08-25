package com.demonlab.lune.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class SettingsManager private constructor() {
    private val configDir = File(System.getProperty("user.home"), ".config/lune").apply { mkdirs() }
    private val configFile = File(configDir, "settings.json")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    data class SettingsData(
        var themeMode: String = "auto", // auto, dark, light
        var useCustomColors: Boolean = false,
        var customColorPalette: Int = 0, // 0: Default Purple, 1: Sunset Peach, 2: Sage Green, 3: Ocean Breeze, 4: Lavender Mist, 5: Warm Amber
        var useAmoledPitchBlack: Boolean = false,
        var useBlur: Boolean = true,
        var customTitle: String = "Lune",
        var musicDirectories: List<String> = listOf(File(System.getProperty("user.home"), "Music").absolutePath),
        var isShuffle: Boolean = false,
        var repeatMode: Int = 0, // 0: off, 1: one, 2: all
        var isCrossfade: Boolean = false,
        var isAutomix: Boolean = false,
        var crossfadeDurationSec: Int = 5,
        var playbackSpeed: Float = 1.0f,
        var playbackPitch: Float = 1.0f,
        var isHiFiEnabled: Boolean = true,
        // Equalizer & Audio Effects
        var isEqEnabled: Boolean = false,
        var eqPreset: String = "Flat",
        var eqBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        var isBassBoostEnabled: Boolean = false,
        var bassBoostGain: Float = 0f,
        var isSpatialAudioEnabled: Boolean = false,
        var loudnessGain: Int = 0, // 0 to 30 dB
        var balance: Float = 0.5f, // 0.0 (Left) -> 0.5 (Center) -> 1.0 (Right)
        var reverbPreset: Int = 0, // 0: None, 1: Small Room, 2: Med Room, 3: Large Room, 4: Med Hall, 5: Large Hall, 6: Plate
        var dynamicsPreset: Int = 0, // 0: None, 1: Light, 2: Medium, 3: Strong, 4: Night
        // UI Customization
        var coverShape: Int = 0, // 0: Default Rounded, 1: Square, 2: Circular
        var coverSize: Int = 2, // 0: Small, 1: Medium, 2: Large
        var coverSpin: Boolean = false,
        var coverVinyl: Boolean = false,
        var useCustomControlsColor: Boolean = false,
        var controlsColorPalette: Int = 0,
        var controlsFilled: Boolean = false,
        var isVisualizerEnabled: Boolean = true,
        var showHeroGreeting: Boolean = true,
        var sortOption: Int = 0, // 0: Title, 1: Artist, 2: Duration, 3: Date, 4: Track
        var isSortAscending: Boolean = true,
        var volume: Float = 1.0f,
        var language: String = "system"
    )

    private var data = SettingsData()

    // Observable states for Compose UI
    var themeMode by mutableStateOf(data.themeMode)
    var language by mutableStateOf(data.language)
    var useCustomColors by mutableStateOf(data.useCustomColors)
    var customColorPalette by mutableIntStateOf(data.customColorPalette)
    var useAmoledPitchBlack by mutableStateOf(data.useAmoledPitchBlack)
    var useBlur by mutableStateOf(data.useBlur)
    var customTitle by mutableStateOf(data.customTitle)
    var musicDirectories by mutableStateOf(data.musicDirectories)
    var isShuffle by mutableStateOf(data.isShuffle)
    var repeatMode by mutableIntStateOf(data.repeatMode)
    var isCrossfade by mutableStateOf(data.isCrossfade)
    var isAutomix by mutableStateOf(data.isAutomix)
    var crossfadeDurationSec by mutableIntStateOf(data.crossfadeDurationSec)
    var playbackSpeed by mutableFloatStateOf(data.playbackSpeed)
    var playbackPitch by mutableFloatStateOf(data.playbackPitch)
    var isHiFiEnabled by mutableStateOf(data.isHiFiEnabled)
    var isEqEnabled by mutableStateOf(data.isEqEnabled)
    var eqPreset by mutableStateOf(data.eqPreset)
    var eqBands by mutableStateOf(data.eqBands)
    var isBassBoostEnabled by mutableStateOf(data.isBassBoostEnabled)
    var bassBoostGain by mutableFloatStateOf(data.bassBoostGain)
    var isSpatialAudioEnabled by mutableStateOf(data.isSpatialAudioEnabled)
    var loudnessGain by mutableIntStateOf(data.loudnessGain)
    var balance by mutableFloatStateOf(data.balance)
    var reverbPreset by mutableIntStateOf(data.reverbPreset)
    var dynamicsPreset by mutableIntStateOf(data.dynamicsPreset)
    var coverShape by mutableIntStateOf(data.coverShape)
    var coverSize by mutableIntStateOf(data.coverSize)
    var coverSpin by mutableStateOf(data.coverSpin)
    var coverVinyl by mutableStateOf(data.coverVinyl)
    var useCustomControlsColor by mutableStateOf(data.useCustomControlsColor)
    var controlsColorPalette by mutableIntStateOf(data.controlsColorPalette)
    var controlsFilled by mutableStateOf(data.controlsFilled)
    var isVisualizerEnabled by mutableStateOf(data.isVisualizerEnabled)
    var showHeroGreeting by mutableStateOf(data.showHeroGreeting)
    var sortOption by mutableIntStateOf(data.sortOption)
    var isSortAscending by mutableStateOf(data.isSortAscending)
    var volume by mutableFloatStateOf(data.volume)

    init {
        loadSettings()
    }

    fun loadSettings() {
        if (configFile.exists()) {
            try {
                val json = configFile.readText()
                val loaded = gson.fromJson(json, SettingsData::class.java)
                if (loaded != null) {
                    data = loaded
                    syncFromData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun syncFromData() {
        themeMode = data.themeMode
        useCustomColors = data.useCustomColors
        customColorPalette = data.customColorPalette
        useAmoledPitchBlack = data.useAmoledPitchBlack
        useBlur = data.useBlur
        customTitle = data.customTitle
        musicDirectories = data.musicDirectories
        isShuffle = data.isShuffle
        repeatMode = data.repeatMode
        isCrossfade = data.isCrossfade
        isAutomix = data.isAutomix
        crossfadeDurationSec = data.crossfadeDurationSec
        playbackSpeed = data.playbackSpeed
        playbackPitch = data.playbackPitch
        isHiFiEnabled = data.isHiFiEnabled
        isEqEnabled = data.isEqEnabled
        eqPreset = data.eqPreset
        eqBands = data.eqBands
        isBassBoostEnabled = data.isBassBoostEnabled
        bassBoostGain = data.bassBoostGain
        isSpatialAudioEnabled = data.isSpatialAudioEnabled
        loudnessGain = data.loudnessGain
        balance = data.balance
        reverbPreset = data.reverbPreset
        dynamicsPreset = data.dynamicsPreset
        coverShape = data.coverShape
        coverSize = data.coverSize
        coverSpin = data.coverSpin
        coverVinyl = data.coverVinyl
        useCustomControlsColor = data.useCustomControlsColor
        controlsColorPalette = data.controlsColorPalette
        controlsFilled = data.controlsFilled
        isVisualizerEnabled = data.isVisualizerEnabled
        showHeroGreeting = data.showHeroGreeting
        sortOption = data.sortOption
        isSortAscending = data.isSortAscending
        volume = data.volume
        language = data.language
    }

    fun saveSettings() {
        data.themeMode = themeMode
        data.language = language
        data.useCustomColors = useCustomColors
        data.customColorPalette = customColorPalette
        data.useAmoledPitchBlack = useAmoledPitchBlack
        data.useBlur = useBlur
        data.customTitle = customTitle
        data.musicDirectories = musicDirectories
        data.isShuffle = isShuffle
        data.repeatMode = repeatMode
        data.isCrossfade = isCrossfade
        data.isAutomix = isAutomix
        data.crossfadeDurationSec = crossfadeDurationSec
        data.playbackSpeed = playbackSpeed
        data.playbackPitch = playbackPitch
        data.isHiFiEnabled = isHiFiEnabled
        data.isEqEnabled = isEqEnabled
        data.eqPreset = eqPreset
        data.eqBands = eqBands
        data.isBassBoostEnabled = isBassBoostEnabled
        data.bassBoostGain = bassBoostGain
        data.isSpatialAudioEnabled = isSpatialAudioEnabled
        data.loudnessGain = loudnessGain
        data.balance = balance
        data.reverbPreset = reverbPreset
        data.dynamicsPreset = dynamicsPreset
        data.coverShape = coverShape
        data.coverSize = coverSize
        data.coverSpin = coverSpin
        data.coverVinyl = coverVinyl
        data.useCustomControlsColor = useCustomControlsColor
        data.controlsColorPalette = controlsColorPalette
        data.controlsFilled = controlsFilled
        data.isVisualizerEnabled = isVisualizerEnabled
        data.showHeroGreeting = showHeroGreeting
        data.sortOption = sortOption
        data.isSortAscending = isSortAscending
        data.volume = volume

        try {
            configFile.writeText(gson.toJson(data))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val instance: SettingsManager by lazy { SettingsManager() }
    }
}
