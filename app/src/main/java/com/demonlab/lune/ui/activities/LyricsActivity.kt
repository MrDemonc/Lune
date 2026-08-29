package com.demonlab.lune.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.theme.LuneTheme
import androidx.compose.foundation.isSystemInDarkTheme
import com.demonlab.lune.tools.SettingsManager
import kotlinx.coroutines.delay
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.request.ImageRequest
import android.os.Vibrator
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.utils.triggerLightVibration
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import java.util.regex.Pattern

data class LyricWord(val timeMs: Long, val text: String)
data class LyricsLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null,
    val words: List<LyricWord> = emptyList()
)

class LyricsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsManager = SettingsManager.getInstance(this)
        enableEdgeToEdge()
        setContent {
            val themeMode = settingsManager.themeMode
            val systemInDarkTheme = isSystemInDarkTheme()
            val targetDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> systemInDarkTheme
            }
            val useCustomColors = settingsManager.useCustomColors
            val customColorPalette = settingsManager.customColorPalette

            LuneTheme(
                darkTheme = targetDarkTheme,
                useCustomColors = useCustomColors,
                customColorPalette = customColorPalette
            ) {
                LyricsScreen(onBack = { finish() }, isDarkTheme = targetDarkTheme)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsScreen(onBack: () -> Unit, isDarkTheme: Boolean = false) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val song = playbackManager.currentSong ?: return
    val rawLyrics = playbackManager.currentLyrics
    
    // Auto-close if lyrics are missing for too long after song change
    LaunchedEffect(song.id) {
        delay(2000) // Grace period for extraction
        if (playbackManager.currentLyrics == null) {
            onBack()
        }
    }
    
    val isPlaying = playbackManager.isPlaying
    
    var currentProgress by remember { mutableStateOf(playbackManager.getProgress()) }
    val currentPositionMs = (song.duration * currentProgress).toLong()
    
    var userOffsetMs by remember(song.id) { mutableLongStateOf(0L) }
    var showOffsetControl by remember { mutableStateOf(false) }

    val lyricsLines = remember(rawLyrics, userOffsetMs) { 
        val lines = parseLyrics(rawLyrics, userOffsetMs)
        Log.i("LyricsActivity", "Parsed ${lines.size} synced lines")
        lines
    }
    val lyricsSettings = remember { SettingsManager.getInstance(context) }
    val listState = rememberLazyListState()
    val vibrator = remember(context) { context.getSystemService(Vibrator::class.java) }
    var textAlignIndex by remember { mutableIntStateOf(lyricsSettings.lyricsTextAlignment) }
    var speedIndex by remember { mutableIntStateOf(lyricsSettings.lyricsSpeedIndex) }
    val alignments = listOf(TextAlign.Start, TextAlign.Center)
    val speedOptions = listOf("1", "2", "3", "5")
    var isLyricsMiniPlayerMinimized by remember { mutableStateOf(false) }
    val playNext: () -> Unit = { playbackManager.playNextFromService() }
    val playPrevious: () -> Unit = { playbackManager.playPreviousFromService() }
    var keepScreenOn by remember { mutableStateOf(lyricsSettings.keepScreenOn) }

    LaunchedEffect(keepScreenOn) {
        val window = (context as? androidx.activity.ComponentActivity)?.window
        if (keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    // Sync progress periodically. This runs constantly to keep the playback position updated.
    LaunchedEffect(Unit) {
        while (true) {
            currentProgress = playbackManager.getProgress()
            delay(50)
        }
    }
    
    // Auto-scroll to current line
    val speedMultiplier = speedOptions[speedIndex].toFloat()
    val adjustedPositionMs = (currentPositionMs * speedMultiplier).toLong().coerceAtMost(song.duration)
    val activeIndex = remember(adjustedPositionMs, lyricsLines) {
        lyricsLines.indexOfLast { it.timeMs <= adjustedPositionMs }.coerceAtLeast(0)
    }
    
    LaunchedEffect(activeIndex) {
        if (lyricsLines.isNotEmpty()) {
            listState.animateScrollToItem(
                (activeIndex - 2).coerceAtLeast(0)
            )
        }
    }

    val isBlurActive = lyricsSettings.isBlurEnabled && if (isDarkTheme) lyricsSettings.isBlurDarkMode else lyricsSettings.isBlurLightMode
    val lyricsTextColor = if (isBlurActive) Color.White else MaterialTheme.colorScheme.onSurface
    val lyricsMutedColor = if (isBlurActive) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant
    val lyricsMuted2Color = if (isBlurActive) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val lyricsMuted08Color = if (isBlurActive) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface

    Box(modifier = Modifier.fillMaxSize()) {
        if (isBlurActive) {
            // Blurred Background
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = song.coverUrl ?: song.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                        .alpha(0.5f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            )
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Surface(
                        shape = CircleShape,
                        color = if (isBlurActive) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isBlurActive) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = lyricsTextColor,
                        maxLines = 1
                    )
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = lyricsMuted2Color,
                        maxLines = 1
                    )
                }
                IconButton(onClick = { showOffsetControl = !showOffsetControl }) {
                    Surface(
                        shape = CircleShape,
                        color = if (showOffsetControl || userOffsetMs != 0L) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else if (isBlurActive) {
                            Color.White.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Ajustar sincronización",
                                tint = if (showOffsetControl || userOffsetMs != 0L) MaterialTheme.colorScheme.onPrimaryContainer else if (isBlurActive) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showOffsetControl,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isBlurActive) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Offset: ${if (userOffsetMs > 0) "+$userOffsetMs" else "$userOffsetMs"} ms",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isBlurActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            if (userOffsetMs != 0L) {
                                TextButton(onClick = { userOffsetMs = 0L }) {
                                    Text("Reset", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { userOffsetMs -= 500L },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("-0.5s", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { userOffsetMs -= 100L },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("-0.1s", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { userOffsetMs += 100L },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+0.1s", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { userOffsetMs += 500L },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+0.5s", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                val isTransitioning = playbackManager.isTransitioning
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = isTransitioning,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val infiniteTransition = rememberInfiniteTransition(label = "MixingAnimation")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "LogoRotation"
                            )
                            val bounceOffset by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = -20f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "TextBounce"
                            )
                            
                            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = com.demonlab.lune.R.drawable.ic_logo_diamonds),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }
                                )
                                Icon(
                                    painter = painterResource(id = com.demonlab.lune.R.drawable.ic_logo_note),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxSize(0.6f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            val transitionText = if (playbackManager.isAutomix) {
                                stringResource(com.demonlab.lune.R.string.transition_mixing)
                            } else {
                                stringResource(com.demonlab.lune.R.string.transition_crossfade)
                            }
                            
                            Text(
                                text = transitionText,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.graphicsLayer {
                                    translationY = bounceOffset
                                }
                            )
                        }
                    }
                }
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isTransitioning,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {

            if (lyricsLines.size < 2 && !rawLyrics.isNullOrBlank()) {
                // If we found 0 or only 1 synced line, but we have a raw string, show it.
                val displayLines = rawLyrics.lines()
                    .map { TIME_TAG_PATTERN.matcher(it).replaceAll("").trim() }
                    .filter { it.isNotBlank() }
                
                Box(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        displayLines.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 20.sp,
                                    lineHeight = 28.sp,
                                    textAlign = alignments[textAlignIndex]
                                ),
                                color = lyricsMuted08Color,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            } else if (lyricsLines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No lyrics found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = lyricsMuted2Color
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 100.dp, bottom = 300.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    itemsIndexed(lyricsLines) { index, line ->
                        val isActive = index == activeIndex
                        val color by animateColorAsState(
                            targetValue = if (isActive) lyricsTextColor else lyricsMutedColor,
                            animationSpec = tween(300),
                            label = "LyricColor"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isActive) 1.1f else 1.0f,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy),
                            label = "LyricScale"
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
                                    .padding(vertical = 16.dp)
                                    .bounceClick(0.95f)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { onLineSeek() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(32.dp).graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                )
                            }
                        } else if (isActive && line.words.isNotEmpty()) {
                            val annotatedText = buildAnnotatedString {
                                for (word in line.words) {
                                    val isPassed = word.timeMs <= adjustedPositionMs
                                    withStyle(
                                        SpanStyle(
                                            color = if (isPassed) lyricsTextColor else lyricsMuted2Color,
                                            fontWeight = if (isPassed) FontWeight.Bold else FontWeight.Medium
                                        )
                                    ) {
                                        append(word.text)
                                    }
                                }
                            }
                            Text(
                                text = annotatedText,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 24.sp,
                                    textAlign = alignments[textAlignIndex]
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .bounceClick(0.95f)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { onLineSeek() }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .bounceClick(0.95f)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { onLineSeek() },
                                horizontalAlignment = if (alignments[textAlignIndex] == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start
                            ) {
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = 24.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = alignments[textAlignIndex]
                                    ),
                                    color = color,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (!line.translation.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = line.translation,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal,
                                            textAlign = alignments[textAlignIndex]
                                        ),
                                        color = color.copy(alpha = if (isActive) 0.85f else 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
        }

        // Mini Player
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedContent(
                targetState = isLyricsMiniPlayerMinimized,
                transitionSpec = {
                    fadeIn(tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300)) togetherWith
                    fadeOut(tween(150)) + scaleOut(targetScale = 0.8f, animationSpec = tween(250)) using
                    SizeTransform(clip = false) { _, _ -> tween(300) }
                },
                modifier = Modifier.fillMaxWidth(),
                label = "lyricsMiniPlayerTransition"
            ) { minimized ->
                if (minimized) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            onClick = { isLyricsMiniPlayerMinimized = false },
                            shape = CircleShape,
                            color = if (isBlurActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = if (isBlurActive) 0.dp else 8.dp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 24.dp)
                                .size(52.dp)
                                .shadow(6.dp, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isBlurActive) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(40.dp)
                                            .alpha(if (isDarkTheme) 0.2f else 0.35f)
                                    ) {
                                        val miniCtx = LocalContext.current
                                        val blurRequest = remember(song.id, miniCtx) {
                                            ImageRequest.Builder(miniCtx)
                                                .data(song.coverUrl ?: song.uri)
                                                .crossfade(true)
                                                .build()
                                        }
                                        AsyncImage(
                                            model = blurRequest,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    if (!isDarkTheme) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.28f))
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isBlurActive) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = if (isBlurActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = if (isBlurActive) 0.dp else 8.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (isBlurActive) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(80.dp)
                                            .alpha(if (isDarkTheme) 0.2f else 0.35f)
                                    ) {
                                        val blurReq = remember(song.id, song.coverUrl) {
                                            ImageRequest.Builder(context)
                                                .data(song.coverUrl ?: song.uri)
                                                .crossfade(true)
                                                .build()
                                        }
                                        AsyncImage(
                                            model = blurReq,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    if (!isDarkTheme) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.28f))
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    // Progress bar area
                                    Box(modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                LinearWavyProgressIndicator(
                                                    progress = { currentProgress },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 4.dp),
                                                    color = if (isBlurActive) Color.White else MaterialTheme.colorScheme.primary,
                                                    trackColor = if (isBlurActive) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                                                    amplitude = { 1f }
                                                )
                                                Slider(
                                                    value = currentProgress,
                                                    onValueChange = { playbackManager.seekTo(it) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    thumb = {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .background(
                                                                    color = if (isBlurActive) Color.White else MaterialTheme.colorScheme.primary,
                                                                    shape = RoundedCornerShape(5.dp)
                                                                )
                                                        )
                                                    },
                                                    colors = SliderDefaults.colors(
                                                        activeTrackColor = Color.Transparent,
                                                        inactiveTrackColor = Color.Transparent
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                        }

                                        IconButton(
                                            onClick = { isLyricsMiniPlayerMinimized = true },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Minimize",
                                                tint = if (isBlurActive) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Controls row
                                    val activePrimary = getControlsPrimaryColor(
                                        useCustomControlsColor = false,
                                        controlsColorPalette = 0
                                    )
                                    val pillMiniColor = if (isBlurActive) {
                                        if (isDarkTheme) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.4f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    }
                                    val pillMiniIconTint = if (isBlurActive) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Text align button
                                        Surface(
                                            onClick = {
                                                val next = (textAlignIndex + 1) % alignments.size
                                                textAlignIndex = next
                                                lyricsSettings.lyricsTextAlignment = next
                                            },
                                            shape = CircleShape,
                                            color = pillMiniColor,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .bounceClick()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (alignments[textAlignIndex] == TextAlign.Start) Icons.AutoMirrored.Filled.FormatAlignLeft else Icons.Default.FormatAlignCenter,
                                                    contentDescription = null,
                                                    tint = pillMiniIconTint,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        // Prev / PlayPause / Next
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                onClick = playPrevious,
                                                shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                                                color = pillMiniColor,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .bounceClick()
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.SkipPrevious,
                                                        contentDescription = null,
                                                        tint = pillMiniIconTint,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                onClick = { if (isPlaying) playbackManager.pause() else playbackManager.resume() },
                                                shape = RoundedCornerShape(4.dp),
                                                color = pillMiniColor,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .bounceClick()
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                                        contentDescription = null,
                                                        tint = pillMiniIconTint,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                onClick = playNext,
                                                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp),
                                                color = pillMiniColor,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .bounceClick()
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.SkipNext,
                                                        contentDescription = null,
                                                        tint = pillMiniIconTint,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        // Speed button
                                        Surface(
                                            onClick = {
                                                val next = (speedIndex + 1) % speedOptions.size
                                                speedIndex = next
                                                lyricsSettings.lyricsSpeedIndex = next
                                            },
                                            shape = CircleShape,
                                            color = pillMiniColor,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .bounceClick()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${speedOptions[speedIndex]}x",
                                                    color = pillMiniIconTint,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
private val TIME_TAG_PATTERN = Pattern.compile("[\\[<](\\d{1,2}):(\\d{2})[.:](\\d{1,3})[\\]>]")
private val METADATA_TAG_PATTERN = Pattern.compile("^\\[[a-zA-Z]+:.*?\\]$")

private fun parseTime(minStr: String, secStr: String, msStr: String): Long {
    val m = minStr.toLongOrNull() ?: 0L
    val s = secStr.toLongOrNull() ?: 0L
    val ms = when (msStr.length) {
        1 -> (msStr.toLongOrNull() ?: 0L) * 100
        2 -> (msStr.toLongOrNull() ?: 0L) * 10
        else -> msStr.take(3).toLongOrNull() ?: 0L
    }
    return (m * 60 * 1000) + (s * 1000) + ms
}

private fun parseLyrics(raw: String?, offsetAdjustmentMs: Long = 0L): List<LyricsLine> {
    if (raw.isNullOrBlank()) return emptyList()

    val offsetMatcher = Pattern.compile("\\[offset:\\s*([+-]?\\d+)\\]", Pattern.CASE_INSENSITIVE).matcher(raw)
    val fileOffset = if (offsetMatcher.find()) offsetMatcher.group(1)?.toLongOrNull() ?: 0L else 0L
    val totalOffset = fileOffset + offsetAdjustmentMs

    val lines = mutableListOf<LyricsLine>()
    var lastTimeMs = 0L

    raw.lines().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || METADATA_TAG_PATTERN.matcher(line).matches()) {
            return@forEach
        }

        val matcher = TIME_TAG_PATTERN.matcher(line)
        val matches = mutableListOf<Triple<Int, Int, Long>>() // startIdx, endIdx, timeMs
        while (matcher.find()) {
            val t = parseTime(matcher.group(1) ?: "0", matcher.group(2) ?: "0", matcher.group(3) ?: "0")
            val adjusted = (t + totalOffset).coerceAtLeast(0L)
            matches.add(Triple(matcher.start(), matcher.end(), adjusted))
        }

        if (matches.isEmpty()) {
            if (!line.startsWith("[")) {
                lines.add(LyricsLine(lastTimeMs, line))
            }
            return@forEach
        }

        val cleanText = TIME_TAG_PATTERN.matcher(line).replaceAll("").trim()

        // Check if all tags are at the very beginning (e.g. [01:00.00][02:00.00]Chorus)
        var allTagsAtStart = true
        var curPos = 0
        val startTimes = mutableListOf<Long>()
        for (m in matches) {
            val prefix = line.substring(curPos, m.first).trim()
            if (prefix.isEmpty()) {
                startTimes.add(m.third)
                curPos = m.second
            } else {
                allTagsAtStart = false
                break
            }
        }

        val rest = line.substring(curPos).trim()
        val hasTagsInRest = TIME_TAG_PATTERN.matcher(rest).find()

        if (allTagsAtStart && !hasTagsInRest && cleanText.isNotEmpty()) {
            for (st in startTimes) {
                lines.add(LyricsLine(st, cleanText))
                lastTimeMs = st
            }
            return@forEach
        }

        // Instrumental / pause line without text (e.g. [01:00.00])
        if (cleanText.isEmpty()) {
            val t = matches.first().third
            lines.add(LyricsLine(t, ""))
            lastTimeMs = t
            return@forEach
        }

        // Inline word-by-word timestamps (Enhanced LRC)
        val words = mutableListOf<LyricWord>()
        var lastEnd = 0
        var currentTime: Long? = null

        for (m in matches) {
            val textBefore = line.substring(lastEnd, m.first)
            val matchTime = m.third
            if (textBefore.isNotEmpty()) {
                val assignedTime = currentTime ?: matchTime
                words.add(LyricWord(assignedTime, textBefore))
            }
            currentTime = matchTime
            lastEnd = m.second
        }

        val textAfter = line.substring(lastEnd)
        if (textAfter.isNotEmpty() && currentTime != null) {
            words.add(LyricWord(currentTime, textAfter))
        }

        val lineStart = words.firstOrNull()?.timeMs ?: matches.first().third
        lines.add(LyricsLine(lineStart, cleanText, words = words))
        lastTimeMs = lineStart
    }

    val sorted = lines.sortedBy { it.timeMs }.toMutableList()

    // Grouping for multi-track lyrics (original + translation / romaji)
    val merged = mutableListOf<LyricsLine>()
    for (line in sorted) {
        val last = merged.lastOrNull()
        if (last != null && Math.abs(last.timeMs - line.timeMs) <= 300 && line.text.isNotBlank() && last.text.isNotBlank()) {
            if (last.translation == null) {
                merged[merged.size - 1] = last.copy(translation = line.text)
            } else {
                merged[merged.size - 1] = last.copy(translation = "${last.translation}\n${line.text}")
            }
        } else {
            merged.add(line)
        }
    }

    if (merged.isNotEmpty() && merged[0].timeMs > 2000) {
        merged.add(0, LyricsLine(0, ""))
    }

    return merged
}