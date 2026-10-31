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
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            enhancer?.enabled = enabled
            secondaryEnhancer?.enabled = enabled
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun setTargetGain(gain: Int) {
        try {
            targetGain = gain
            enhancer?.setTargetGain(gain)
            secondaryEnhancer?.setTargetGain(gain)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun handover() {
        try {
            enhancer?.release()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        enhancer = secondaryEnhancer
        secondaryEnhancer = null
    }

    fun release(isSecondary: Boolean = false) {
        try {
            if (isSecondary) {
                secondaryEnhancer?.release()
                secondaryEnhancer = null
            } else {
                enhancer?.release()
                enhancer = null
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun releaseAll() {
        try {
            enhancer?.release()
            enhancer = null
            secondaryEnhancer?.release()
            secondaryEnhancer = null
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}
