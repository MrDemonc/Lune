package com.demonlab.lune.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun <T> FastScrollbar(
    listState: LazyListState,
    items: List<T>,
    modifier: Modifier = Modifier,
    headerItemCount: Int = 0,
    itemKeyOrLetter: (T) -> String,
    hasBlurBackground: Boolean = false,
    useCustomControlsColor: Boolean = false,
    controlsColorPalette: Int = 0,
    thumbColor: Color = if (useCustomControlsColor) {
        getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette).copy(alpha = 0.85f)
    } else if (hasBlurBackground) {
        Color.White.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.primary
    },
    bubbleColor: Color = if (useCustomControlsColor) {
        getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)
    } else if (hasBlurBackground) {
        Color.White
    } else {
        MaterialTheme.colorScheme.primaryContainer
    },
    bubbleTextColor: Color = if (useCustomControlsColor) {
        Color.White
    } else if (hasBlurBackground) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
) {
    if (items.size <= 15) return

    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    var isDragging by remember { mutableStateOf(false) }
    var dragThumbY by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }
    var currentLetter by remember { mutableStateOf("") }
    var frozenTopPx by remember { mutableFloatStateOf(0f) }

    val thumbHeightDp = 48.dp
    val density = LocalDensity.current
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

    val currentItems by rememberUpdatedState(items)
    val currentHeaderCount by rememberUpdatedState(headerItemCount)
    val currentItemKeyOrLetter by rememberUpdatedState(itemKeyOrLetter)

    val thumbWidth by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 4.dp,
        animationSpec = tween(durationMillis = 150),
        label = "thumbWidth"
    )
    val thumbAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.4f,
        animationSpec = tween(durationMillis = 150),
        label = "thumbAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp)
            .onSizeChanged { size -> containerHeight = size.height.toFloat() }
            .pointerInput(listState) {
                var scrollJob: Job? = null
                var lastTargetIndex = -1

                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()

                    val currentHeaderOffset = if (currentHeaderCount > 0) {
                        val headerInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentHeaderCount - 1 }
                        if (headerInfo != null) {
                            (headerInfo.offset + headerInfo.size).toFloat().coerceAtLeast(0f)
                        } else 0f
                    } else 0f
                    frozenTopPx = currentHeaderOffset
                    isDragging = true

                    fun updatePosition(y: Float) {
                        val totalSongs = currentItems.size
                        if (totalSongs <= 1) return
                        val availableHeight = (containerHeight - frozenTopPx - thumbHeightPx).coerceAtLeast(0f)
                        val targetThumbY = (y - thumbHeightPx / 2f).coerceIn(frozenTopPx, frozenTopPx + availableHeight)
                        dragThumbY = targetThumbY
                        val dragProgress = if (availableHeight > 0f) {
                            ((targetThumbY - frozenTopPx) / availableHeight).coerceIn(0f, 1f)
                        } else 0f
                        val targetSongIndex = (dragProgress * (totalSongs - 1)).roundToInt().coerceIn(0, totalSongs - 1)
                        val targetIndex = if (dragProgress == 0f && currentHeaderCount > 0) 0 else currentHeaderCount + targetSongIndex

                        val actualItemIndex = targetIndex - currentHeaderCount
                        val newLetter = if (actualItemIndex in currentItems.indices) {
                            val str = currentItemKeyOrLetter(currentItems[actualItemIndex]).trim()
                            if (str.isNotEmpty()) str.take(1).uppercase() else ""
                        } else ""

                        if (newLetter != currentLetter) {
                            currentLetter = newLetter
                            if (newLetter.isNotBlank()) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }

                        if (targetIndex != lastTargetIndex) {
                            lastTargetIndex = targetIndex
                            scrollJob?.cancel()
                            scrollJob = coroutineScope.launch {
                                listState.scrollToItem(targetIndex)
                            }
                        }
                    }

                    updatePosition(down.position.y)

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                change.consume()
                                updatePosition(change.position.y)
                            } else {
                                break
                            }
                        }
                    } finally {
                        isDragging = false
                        lastTargetIndex = -1
                    }
                }
            }
    ) {
        // Thumb
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val thumbY = if (isDragging) {
                        dragThumbY
                    } else {
                        val currentHeaderOffset = if (headerItemCount > 0) {
                            val headerInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == headerItemCount - 1 }
                            if (headerInfo != null) {
                                (headerInfo.offset + headerInfo.size).toFloat().coerceAtLeast(0f)
                            } else 0f
                        } else 0f

                        val availableHeight = (containerHeight - currentHeaderOffset - thumbHeightPx).coerceAtLeast(0f)
                        val firstSongVisible = (listState.firstVisibleItemIndex - headerItemCount).coerceAtLeast(0)
                        val songItemsCount = items.size
                        val progress = if (songItemsCount > 1) {
                            val firstItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index >= headerItemCount }
                            val offsetFraction = if (firstItem != null && firstItem.size > 0) {
                                (-firstItem.offset.toFloat() / firstItem.size.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            ((firstSongVisible + offsetFraction) / (songItemsCount - 1).toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        currentHeaderOffset + (progress * availableHeight)
                    }
                    IntOffset(-4.dp.roundToPx(), thumbY.roundToInt())
                }
                .width(thumbWidth)
                .height(thumbHeightDp)
                .clip(RoundedCornerShape(percent = 50))
                .background(thumbColor.copy(alpha = thumbAlpha))
                .then(
                    if (hasBlurBackground && isDragging) {
                        Modifier.border(
                            1.dp,
                            Color.White.copy(alpha = 0.35f),
                            RoundedCornerShape(percent = 50)
                        )
                    } else {
                        Modifier
                    }
                )
        )

        // Bubble Indicator (Vertical Pill)
        AnimatedVisibility(
            visible = isDragging && currentLetter.isNotEmpty(),
            enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(1f, 0.5f)),
            exit = fadeOut() + scaleOut(transformOrigin = TransformOrigin(1f, 0.5f)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    IntOffset(
                        -56.dp.roundToPx(),
                        (dragThumbY - 12.dp.toPx()).roundToInt()
                    )
                }
        ) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = bubbleColor,
                shadowElevation = if (hasBlurBackground) 4.dp else 8.dp,
                border = if (hasBlurBackground) BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null,
                modifier = Modifier.width(48.dp).height(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = currentLetter,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = bubbleTextColor
                    )
                }
            }
        }
    }
}
