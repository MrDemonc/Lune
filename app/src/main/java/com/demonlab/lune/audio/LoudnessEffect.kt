package com.demonlab.lune.audio

import android.media.audiofx.LoudnessEnhancer

class LoudnessEffect {
    private var enhancer: LoudnessEnhancer? = null
    private var secondaryEnhancer: LoudnessEnhancer? = null
    private var targetGain: Int = 0

    fun setup(sessionId: Int, isSecondary: Boolean, enabled: Boolean, gain: Int) {
        release(isSecondary)
        try {
            val e = LoudnessEnhancer(sessionId).apply {
                setTargetGain(gain)
                this.enabled = enabled
            }
            targetGain = gain
            if (isSecondary) {
                secondaryEnhancer = e
            } else {
                enhancer = e
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEnabled(enabled: Boolean) {
        enhancer?.enabled = enabled
        secondaryEnhancer?.enabled = enabled
    }

    fun setTargetGain(gain: Int) {
        targetGain = gain
        enhancer?.setTargetGain(gain)
        secondaryEnhancer?.setTargetGain(gain)
    }

    fun handover() {
        enhancer?.release()
        enhancer = secondaryEnhancer
        secondaryEnhancer = null
    }

    fun release(isSecondary: Boolean = false) {
        if (isSecondary) {
            secondaryEnhancer?.release()
            secondaryEnhancer = null
        } else {
            enhancer?.release()
            enhancer = null
        }
    }

    fun releaseAll() {
        enhancer?.release()
        enhancer = null
        secondaryEnhancer?.release()
        secondaryEnhancer = null
    }
}
