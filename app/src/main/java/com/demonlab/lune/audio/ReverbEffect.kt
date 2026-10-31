package com.demonlab.lune.audio

import android.media.audiofx.PresetReverb
import android.media.audiofx.EnvironmentalReverb
import android.os.Build
import android.util.Log

class ReverbEffect {
    private var reverbLegacy: PresetReverb? = null
    private var reverb: EnvironmentalReverb? = null
    private var secondaryReverbLegacy: PresetReverb? = null
    private var secondaryReverb: EnvironmentalReverb? = null
    private var currentPreset: Int = 0
    private val tag = "ReverbEffect"

    fun setup(sessionId: Int, isSecondary: Boolean, preset: Int) {
        release(isSecondary)
        currentPreset = preset
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    val r = EnvironmentalReverb(0, sessionId).apply {
                        applyPreset(preset)
                        enabled = preset != 0
                    }
                    if (isSecondary) {
                        secondaryReverb = r
                    } else {
                        reverb = r
                    }
                    return
                } catch (t: Throwable) {
                    Log.w(tag, "EnvironmentalReverb not supported, falling back to PresetReverb: ${t.message}")
                }
            }
            val r = PresetReverb(0, sessionId).apply {
                setPreset(preset.toShort())
                enabled = preset != 0
            }
            if (isSecondary) {
                secondaryReverbLegacy = r
            } else {
                reverbLegacy = r
            }
        } catch (t: Throwable) {
            Log.e(tag, "Failed to create reverb effect", t)
        }
    }

    fun setPreset(preset: Int) {
        try {
            currentPreset = preset
            val enabled = preset != 0
            if (Build.VERSION.SDK_INT >= 31) {
                reverb?.let {
                    it.applyPreset(preset)
                    it.enabled = enabled
                }
                secondaryReverb?.let {
                    it.applyPreset(preset)
                    it.enabled = enabled
                }
            } else {
                reverbLegacy?.let {
                    it.setPreset(preset.toShort())
                    it.enabled = enabled
                }
                secondaryReverbLegacy?.let {
                    it.setPreset(preset.toShort())
                    it.enabled = enabled
                }
            }
        } catch (t: Throwable) {
            Log.e(tag, "Failed to set reverb preset", t)
        }
    }

    private fun EnvironmentalReverb.applyPreset(preset: Int) {
        when (preset) {
            1 -> { // Small Room
                decayTime = 500
                setDensity(300.toShort())
                setDiffusion(300.toShort())
                setReverbLevel((-2000).toShort())
                setRoomLevel((-1000).toShort())
            }
            2 -> { // Medium Room
                decayTime = 1000
                setDensity(500.toShort())
                setDiffusion(500.toShort())
                setReverbLevel((-1000).toShort())
                setRoomLevel((-500).toShort())
            }
            3 -> { // Large Room
                decayTime = 1500
                setDensity(700.toShort())
                setDiffusion(700.toShort())
                setReverbLevel((-500).toShort())
                setRoomLevel((-200).toShort())
            }
            4 -> { // Medium Hall
                decayTime = 2200
                setDensity(800.toShort())
                setDiffusion(800.toShort())
                setReverbLevel(0.toShort())
                setRoomLevel(0.toShort())
            }
            5 -> { // Large Hall
                decayTime = 3500
                setDensity(900.toShort())
                setDiffusion(900.toShort())
                setReverbLevel(0.toShort())
                setRoomLevel(0.toShort())
            }
            6 -> { // Plate
                decayTime = 1500
                setDensity(500.toShort())
                setDiffusion(1000.toShort())
                setReverbLevel((-500).toShort())
                setRoomLevel((-1000).toShort())
            }
        }
    }

    fun handover() {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                reverb?.release()
                reverb = secondaryReverb
                secondaryReverb = null
            } else {
                reverbLegacy?.release()
                reverbLegacy = secondaryReverbLegacy
                secondaryReverbLegacy = null
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun release(isSecondary: Boolean = false) {
        try {
            if (isSecondary) {
                secondaryReverbLegacy?.release()
                secondaryReverbLegacy = null
                secondaryReverb?.release()
                secondaryReverb = null
            } else {
                reverbLegacy?.release()
                reverbLegacy = null
                reverb?.release()
                reverb = null
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun releaseAll() {
        try {
            reverbLegacy?.release()
            reverbLegacy = null
            reverb?.release()
            reverb = null
            secondaryReverbLegacy?.release()
            secondaryReverbLegacy = null
            secondaryReverb?.release()
            secondaryReverb = null
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    companion object {
        val presetNames = listOf("None", "Small Room", "Medium Room", "Large Room", "Medium Hall", "Large Hall", "Plate")
        val presetValues = listOf(0, 1, 2, 3, 4, 5, 6)
    }
}
