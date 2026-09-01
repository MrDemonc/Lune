package com.demonlab.lune.ui.utils

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

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

class MaterialExpressiveScallopShape(
    private val lobes: Int = 8,
    private val amplitude: Float = 0.07f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = minOf(size.width, size.height) / 2f / (1f + amplitude)
        val steps = lobes * 16
        val angleStep = (2.0 * Math.PI / steps).toFloat()

        for (stepIndex in 0..steps) {
            val theta = stepIndex * angleStep - (Math.PI / 2.0).toFloat()
            val r = baseRadius * (1f + amplitude * cos(lobes * theta))
            val x = centerX + r * cos(theta)
            val y = centerY + r * sin(theta)

            if (stepIndex == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}

fun Modifier.songSwipeGestures(
    enabled: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    val coroutineScope = rememberCoroutineScope()
    var dragTranslationX by remember { mutableFloatStateOf(0f) }
    var dragScale by remember { mutableFloatStateOf(1f) }
    val animOffsetX = remember { Animatable(0f) }
    val animScale = remember { Animatable(1f) }
    var isDragging by remember { mutableStateOf(false) }

    this
        .graphicsLayer {
            translationX = if (isDragging) dragTranslationX else animOffsetX.value
            scaleX = if (isDragging) dragScale else animScale.value
            scaleY = if (isDragging) dragScale else animScale.value
        }
        .pointerInput(Unit) {
            var gestureConsumed = false
            detectDragGestures(
                onDragStart = {
                    isDragging = true
                    dragTranslationX = 0f
                    dragScale = 1f
                    gestureConsumed = false
                },
                onDragEnd = {
                    isDragging = false
                    val currentX = dragTranslationX
                    val currentS = dragScale
                    coroutineScope.launch {
                        animOffsetX.snapTo(currentX)
                        animScale.snapTo(currentS)
                        if (gestureConsumed) {
                            animScale.animateTo(0.94f, tween(50))
                        }
                        launch {
                            animOffsetX.animateTo(
                                0f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                        launch {
                            animScale.animateTo(
                                1f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    }
                },
                onDragCancel = {
                    isDragging = false
                    val currentX = dragTranslationX
                    val currentS = dragScale
                    coroutineScope.launch {
                        animOffsetX.snapTo(currentX)
                        animScale.snapTo(currentS)
                        launch {
                            animOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                        }
                        launch {
                            animScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                        }
                    }
                },
                onDrag = { change, dragAmount ->
                    dragTranslationX = (dragTranslationX + dragAmount.x * 0.35f).coerceIn(-75f, 75f)
                    val absX = kotlin.math.abs(dragTranslationX)
                    val absY = kotlin.math.abs(dragAmount.y)
                    dragScale = (1f - (absX / 1600f)).coerceIn(0.96f, 1f)

                    if (!gestureConsumed && absX > 45 && absX > absY * 1.2f) {
                        change.consume()
                        gestureConsumed = true
                        if (dragTranslationX < 0) onNext() else onPrevious()
                    }
                }
            )
        }
}
