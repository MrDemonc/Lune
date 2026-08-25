package com.demonlab.lune.ui.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

fun formatDuration(durationInMillis: Long): String {
    val totalSeconds = (durationInMillis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

fun formatDurationCompact(durationInMillis: Long): String {
    val totalSeconds = (durationInMillis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return "${minutes}:${String.format("%02d", remainingSeconds)}"
}

fun formatLongDuration(durationInMillis: Long): String {
    val totalSeconds = (durationInMillis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

@Composable
fun Modifier.bounceClick(
    scaleDown: Float = 0.92f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
