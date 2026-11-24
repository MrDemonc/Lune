package com.demonlab.lune.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demonlab.lune.R
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.lyrics.TIME_TAG_PATTERN
import com.demonlab.lune.ui.lyrics.parseLyrics
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.utils.triggerLightVibration
import kotlinx.coroutines.delay

@Composable
fun CoverLyricsView(
    song: Song,
    playbackManager: PlaybackManager,
    isDarkTheme: Boolean,
    hasBlurBackground: Boolean,
    isGradientEnabled: Boolean,
    isAmoled: Boolean,
    onOpenFullLyrics: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember(context) {
        context.getSystemService(android.os.Vibrator::class.java)
    }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var textAlignIndex by remember { mutableIntStateOf(settingsManager.lyricsTextAlignment) }

    LaunchedEffect(settingsManager.lyricsTextAlignment) {
        textAlignIndex = settingsManager.lyricsTextAlignment
    }

    val currentTextAlign = if (textAlignIndex == 1) TextAlign.Center else TextAlign.Start
    val currentHorizontalAlignment = if (textAlignIndex == 1) Alignment.CenterHorizontally else Alignment.Start
    val currentContentAlignment = if (textAlignIndex == 1) Alignment.Center else Alignment.CenterStart
    val currentTransformOrigin = if (textAlignIndex == 1) TransformOrigin(0.5f, 0.5f) else TransformOrigin(0f, 0.5f)

    // Modo Claro puro (sin blur de portada ni fondo oscuro)
    val isLightMode = !isDarkTheme && !hasBlurBackground

    // Color del texto activo: blanco nítido en fondo oscuro/blur/gradient, o negro/carbón profundo en modo claro
    val activeTextColor = if (isLightMode) MaterialTheme.colorScheme.onSurface else Color.White

    // Color del texto inactivo atenuado estilo Apple Music (jerarquía visual clara)
    val inactiveTextColor = if (isLightMode) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else {
        Color.White.copy(alpha = 0.38f)
    }

    // Sombra suave en modo oscuro/blur/gradient para garantizar legibilidad 100% sobre portadas o gradientes dinámicos
    val textShadow = if (isLightMode) null else Shadow(
        color = Color.Black.copy(alpha = 0.50f),
        offset = Offset(0f, 2f),
        blurRadius = 8f
    )

    val rawLyrics = playbackManager.currentLyrics
    val lyricsLines = remember(rawLyrics, song.id) { parseLyrics(rawLyrics) }

    var currentProgress by remember { mutableFloatStateOf(playbackManager.getProgress()) }
    LaunchedEffect(song.id, playbackManager.isPlaying) {
        while (true) {
            currentProgress = playbackManager.getProgress()
            delay(50)
        }
    }
    val currentPositionMs = (song.duration * currentProgress).toLong()

    val activeIndex = remember(currentPositionMs, lyricsLines) {
        if (lyricsLines.isEmpty()) -1
        else lyricsLines.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
    }

    val listState = rememberLazyListState()
    var isUserInteracting by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isUserInteracting = true
            lastInteractionTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && lyricsLines.isNotEmpty()) {
            val now = System.currentTimeMillis()
            if (!isUserInteracting || (now - lastInteractionTime > 3500L)) {
                isUserInteracting = false
                listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
            }
        }
    }

    // Contenedor 100% transparente integrado con el fondo del reproductor (sin marcos ni recuadros)
    Box(modifier = modifier.fillMaxSize()) {
        // Contenido de las letras con desvanecimiento alfa en extremos mediante BlendMode.DstIn
        if (lyricsLines.size < 2 && !rawLyrics.isNullOrBlank()) {
            val displayLines = remember(rawLyrics) {
                rawLyrics.lines()
                    .map { TIME_TAG_PATTERN.matcher(it).replaceAll("").trim() }
                    .filter { it.isNotBlank() }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.00f to Color.Transparent,
                                0.10f to Color.Black,
                                0.90f to Color.Black,
                                1.00f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .verticalScroll(rememberScrollState()),
                contentAlignment = currentContentAlignment
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp, bottom = 44.dp, start = 8.dp, end = 8.dp),
                    horizontalAlignment = currentHorizontalAlignment
                ) {
                    displayLines.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 20.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = currentTextAlign,
                                shadow = textShadow
                            ),
                            color = activeTextColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        } else if (lyricsLines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isLightMode) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.14f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lyrics,
                                contentDescription = null,
                                tint = activeTextColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.no_lyrics_title),
                        style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
                        fontWeight = FontWeight.Bold,
                        color = activeTextColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.no_lyrics_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = inactiveTextColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = onOpenFullLyrics,
                        modifier = Modifier.bounceClick(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isLightMode) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.20f),
                            contentColor = if (isLightMode) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                        )
                    ) {
                        Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.open_full_lyrics),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.00f to Color.Transparent,
                                0.10f to Color.Black,
                                0.90f to Color.Black,
                                1.00f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    },
                contentPadding = PaddingValues(top = 44.dp, bottom = 44.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = currentHorizontalAlignment
            ) {
                itemsIndexed(lyricsLines) { index, line ->
                    val isActive = index == activeIndex
                    val lineColor by animateColorAsState(
                        targetValue = if (isActive) activeTextColor else inactiveTextColor,
                        animationSpec = tween(240),
                        label = "CoverLyricColor"
                    )
                    val lineScale by animateFloatAsState(
                        targetValue = if (isActive) 1.03f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "CoverLyricScale"
                    )

                    val onLineSeek = {
                        playbackManager.seekToTimeMs(line.timeMs)
                        if (song.duration > 0) {
                            currentProgress = (line.timeMs.toFloat() / song.duration.toFloat()).coerceIn(0f, 1f)
                        }
                        vibrator?.triggerLightVibration()
                    }

                    if (line.text.isBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .bounceClick(0.95f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onLineSeek() },
                            contentAlignment = currentContentAlignment
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = lineColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        scaleX = lineScale
                                        scaleY = lineScale
                                        transformOrigin = currentTransformOrigin
                                    }
                            )
                        }
                    } else if (isActive && line.words.isNotEmpty()) {
                        val annotatedText = buildAnnotatedString {
                            for (word in line.words) {
                                val isPassed = word.timeMs <= currentPositionMs
                                withStyle(
                                    SpanStyle(
                                        color = if (isPassed) activeTextColor else activeTextColor.copy(alpha = 0.55f),
                                        fontWeight = if (isPassed) FontWeight.Bold else FontWeight.SemiBold,
                                        shadow = textShadow
                                    )
                                ) {
                                    append(word.text)
                                }
                            }
                        }
                        Text(
                            text = annotatedText,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 23.sp,
                                lineHeight = 31.sp,
                                textAlign = currentTextAlign
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = lineScale
                                    scaleY = lineScale
                                    transformOrigin = currentTransformOrigin
                                }
                                .bounceClick(0.97f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onLineSeek() }
                        )
                    } else {
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = if (isActive) 23.sp else 19.sp,
                                lineHeight = if (isActive) 31.sp else 27.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                textAlign = currentTextAlign,
                                shadow = textShadow
                            ),
                            color = lineColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = lineScale
                                    scaleY = lineScale
                                    transformOrigin = currentTransformOrigin
                                }
                                .bounceClick(0.97f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onLineSeek() }
                        )
                    }
                }
            }
        }

        // Botones flotantes sutiles estilo iOS en la esquina superior derecha
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Botón para alinear letras (izquierda / centrada)
            Surface(
                onClick = {
                    val next = (textAlignIndex + 1) % 2
                    textAlignIndex = next
                    settingsManager.lyricsTextAlignment = next
                    vibrator?.triggerLightVibration()
                },
                shape = CircleShape,
                color = if (isLightMode) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.16f),
                contentColor = activeTextColor,
                shadowElevation = if (isLightMode) 2.dp else 0.dp,
                modifier = Modifier
                    .size(32.dp)
                    .bounceClick()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (textAlignIndex == 0) Icons.AutoMirrored.Filled.FormatAlignLeft else Icons.Default.FormatAlignCenter,
                        contentDescription = stringResource(R.string.align_lyrics),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Botón para acceder a la actividad de letras completa
            Surface(
                onClick = onOpenFullLyrics,
                shape = CircleShape,
                color = if (isLightMode) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.16f),
                contentColor = activeTextColor,
                shadowElevation = if (isLightMode) 2.dp else 0.dp,
                modifier = Modifier
                    .size(32.dp)
                    .bounceClick()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = stringResource(R.string.open_full_lyrics),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
