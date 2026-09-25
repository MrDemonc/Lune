package com.demonlab.lune.audio

import android.media.audiofx.DynamicsProcessing
import android.os.Build
import java.lang.reflect.Constructor

class DynamicsEffect {
    private var dynamics: DynamicsProcessing? = null
    private var secondaryDynamics: DynamicsProcessing? = null
    private var currentPreset: Int = 0

    fun setup(sessionId: Int, isSecondary: Boolean, preset: Int) {
        release(isSecondary)
        currentPreset = preset
        try {
            val d = createDynamics(sessionId)
            if (isSecondary) {
                secondaryDynamics = d
            } else {
                dynamics = d
            }
            setPresetOnEffect(d, preset)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createDynamics(sessionId: Int): DynamicsProcessing {
        return try {
            if (Build.VERSION.SDK_INT >= 28) {
                val ctor = DynamicsProcessing::class.java.getConstructor(Int::class.java)
                @Suppress("DEPRECATION")
                ctor.newInstance(sessionId) as DynamicsProcessing
            } else {
                val ctor = DynamicsProcessing::class.java.getConstructor(Int::class.java, Int::class.java)
                @Suppress("DEPRECATION")
                ctor.newInstance(0, sessionId) as DynamicsProcessing
            }
        } catch (e: Exception) {
            throw RuntimeException("Cannot create DynamicsProcessing", e)
        }
    }

    fun setPreset(preset: Int) {
        currentPreset = preset
        dynamics?.let { setPresetOnEffect(it, preset) }
        secondaryDynamics?.let { setPresetOnEffect(it, preset) }
    }

    private fun setPresetOnEffect(d: DynamicsProcessing, preset: Int) {
        if (preset == 0) {
            d.enabled = false
            return
        }
        try {
            val getChannel = DynamicsProcessing::class.java.getMethod("getChannel", Int::class.java)
            val channel = getChannel.invoke(d, 0)

            val getMbc = channel::class.java.getMethod("getMbc")
            val mbc = getMbc.invoke(channel)

            val setMbcEnabled = mbc::class.java.getMethod("setEnabled", Boolean::class.java)
            val getBandCount = mbc::class.java.getMethod("getBandCount")
            val setBand = mbc::class.java.getMethod("setBand", Int::class.java, DynamicsProcessing.MbcBand::class.java)

            val bandCount = getBandCount.invoke(mbc) as Int
            val mbcBandCtor = DynamicsProcessing.MbcBand::class.java.getConstructor(
                Boolean::class.java, Float::class.java, Float::class.java, Float::class.java,
                Float::class.java, Float::class.java, Float::class.java, Float::class.java,
                Float::class.java, Float::class.java, Float::class.java
            )

            val params = when (preset) {
                1 -> floatArrayOf(0.002f, 0.05f, 1.5f, -20f, 0f, 0f)
                2 -> floatArrayOf(0.003f, 0.08f, 3f, -30f, 0f, 2f)
                3 -> floatArrayOf(0.004f, 0.1f, 5f, -40f, 3f, 5f)
                4 -> floatArrayOf(0.005f, 0.15f, 8f, -50f, 6f, 8f)
                else -> floatArrayOf(0.002f, 0.05f, 1.5f, -20f, 0f, 0f)
            }
            val attack = params[0]; val release = params[1]; val ratio = params[2]
            val threshold = params[3]; val inputGain = params[4]; val makeUpGain = params[5]

            val getEq = channel::class.java.getMethod("getEq")
            val eq = getEq.invoke(channel)

            val setInputGain = channel::class.java.getMethod("setInputGain", Float::class.java)
            val setOutputGain = channel::class.java.getMethod("setOutputGain", Float::class.java)
            val setEqEnabled = eq::class.java.getMethod("setEnabled", Boolean::class.java)
            setEqEnabled.invoke(eq, false)

            for (b in 0 until bandCount) {
                val band = mbcBandCtor.newInstance(
                    true,
                    attack, release, ratio,
                    threshold, 0f, -80f,
                    0f, 1f, inputGain, makeUpGain
                )
                setBand.invoke(mbc, b, band)
            }
            setMbcEnabled.invoke(mbc, true)

            val getLimiter = channel::class.java.getMethod("getLimiter")
            val limiter = getLimiter.invoke(channel)
            val setLimiterEnabled = limiter::class.java.getMethod("setEnabled", Boolean::class.java)
            val setLimiterLimit = limiter::class.java.getMethod("setLimit", Float::class.java)
            val setLimiterAttack = limiter::class.java.getMethod("setAttackTime", Float::class.java)
            val setLimiterRelease = limiter::class.java.getMethod("setReleaseTime", Float::class.java)

            if (preset >= 2) {
                setLimiterEnabled.invoke(limiter, true)
                val limitDb = when (preset) { 2 -> -2f; 3 -> -3f; else -> -5f }
                setLimiterLimit.invoke(limiter, limitDb)
                setLimiterAttack.invoke(limiter, 0.001f)
                setLimiterRelease.invoke(limiter, if (preset == 4) 0.15f else 0.1f)
            } else {
                setLimiterEnabled.invoke(limiter, false)
            }

            setInputGain.invoke(channel, inputGain)
            setOutputGain.invoke(channel, makeUpGain)
            d.enabled = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun handover() {
        dynamics?.release()
        dynamics = secondaryDynamics
        secondaryDynamics = null
    }

    fun release(isSecondary: Boolean = false) {
        if (isSecondary) {
            secondaryDynamics?.release()
            secondaryDynamics = null
        } else {
            dynamics?.release()
            dynamics = null
        }
    }

    fun releaseAll() {
        dynamics?.release()
        dynamics = null
        secondaryDynamics?.release()
        secondaryDynamics = null
    }

    companion object {
        val presetNames = listOf("None", "Light", "Medium", "Strong", "Night")
        val presetValues = listOf(0, 1, 2, 3, 4)
    }
}
