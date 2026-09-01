package com.demonlab.lune.ui.utils

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import java.util.Locale

fun Vibrator.triggerLightVibration() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    } else {
        @Suppress("DEPRECATION")
        this.vibrate(20)
    }
}

fun formatDuration(duration: Long): String {
    val minutes = (duration / 1000) / 60
    val seconds = (duration / 1000) % 60
    return "%d:%02d".format(Locale.getDefault(), minutes, seconds)
}

fun formatDurationCompact(durationInMillis: Long): String {
    val totalSeconds = durationInMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
    } else {
        "${minutes}m"
    }
}

fun formatLongDuration(durationInMillis: Long): String {
    val totalSeconds = durationInMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(Locale.getDefault(), hours, minutes, seconds)
    } else {
        "%02d:%02d".format(Locale.getDefault(), minutes, seconds)
    }
}

fun Modifier.bounceClick(
    scaleDown: Float = 0.94f,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMedium
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(scaleDown, dampingRatio, stiffness) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                val animJob = coroutineScope.launch {
                    scale.animateTo(
                        targetValue = scaleDown,
                        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness)
                    )
                }
                waitForUpOrCancellation()
                animJob.cancel()
                coroutineScope.launch {
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness)
                    )
                }
            }
        }
}

fun Modifier.songSwipeGestures(
    enabled: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit
): Modifier = this.then(
    if (enabled) {
        Modifier.pointerInput(Unit) {
            var totalDragX = 0f
            var gestureConsumed = false
            detectDragGestures(
                onDragStart = {
                    totalDragX = 0f
                    gestureConsumed = false
                },
                onDrag = { _, dragAmount ->
                    if (!gestureConsumed) {
                        totalDragX += dragAmount.x
                        val absX = kotlin.math.abs(totalDragX)
                        val absY = kotlin.math.abs(dragAmount.y)
                        if (absX > 60 && absX > absY * 1.5f) {
                            if (totalDragX < 0) onNext() else onPrevious()
                            gestureConsumed = true
                        }
                    }
                }
            )
        }
    } else {
        Modifier
    }
)
