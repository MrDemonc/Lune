package com.demonlab.lune.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.theme.LuneTheme
import kotlinx.coroutines.delay
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import java.util.regex.Pattern

data class LyricsLine(val timeMs: Long, val text: String)

class LyricsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LuneTheme {
                LyricsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun LyricsScreen(onBack: () -> Unit) {
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
    
    val lyricsLines = remember(rawLyrics) { 
        val lines = parseLyrics(rawLyrics)
        Log.i("LyricsActivity", "Parsed ${lines.size} synced lines")
        lines
    }
    val listState = rememberLazyListState()
    
    // Sync progress periodically. This runs constantly to keep the playback position updated.
    LaunchedEffect(Unit) {
        while (true) {
            currentProgress = playbackManager.getProgress()
            delay(250)
        }
    }
    
    // Auto-scroll to current line
    val activeIndex = remember(currentPositionMs, lyricsLines) {
        lyricsLines.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
    }
    
    LaunchedEffect(activeIndex) {
        if (lyricsLines.isNotEmpty()) {
            listState.animateScrollToItem(
                (activeIndex - 2).coerceAtLeast(0)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred Background
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = song.coverUrl ?: song.albumArtUri,
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

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
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
                                    textAlign = TextAlign.Center
                                ),
                                color = Color.White.copy(alpha = 0.8f),
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
                        color = Color.White.copy(alpha = 0.7f)
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
                            targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.3f),
                            animationSpec = tween(300),
                            label = "LyricColor"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isActive) 1.1f else 1.0f,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy),
                            label = "LyricScale"
                        )

                        if (line.text.isBlank()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
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
                        } else {
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 24.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Start
                                ),
                                color = color,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
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

private fun parseLyrics(raw: String?): List<LyricsLine> {
    if (raw == null) return emptyList()
    
    val lines = mutableListOf<LyricsLine>()
    // More lenient pattern to match [00:00.00] or [00:00:00] or [00:00]
    val pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})?\\](.*)")
    
    raw.lines().forEach { line ->
        val matcher = pattern.matcher(line)
        if (matcher.find()) {
            val min = matcher.group(1)?.toLong() ?: 0L
            val sec = matcher.group(2)?.toLong() ?: 0L
            val msPart = matcher.group(3) ?: "00"
            val text = matcher.group(4)?.trim() ?: ""
            
            val ms = when (msPart.length) {
                1 -> msPart.toLong() * 100
                2 -> msPart.toLong() * 10
                else -> msPart.toLong()
            }
            val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
            
            // Only add if it's not a metadata tag (redundant check but safer)
            if (!text.startsWith("[ti:") && !text.startsWith("[ar:") && !text.startsWith("[al:") && !text.startsWith("[by:")) {
                lines.add(LyricsLine(totalMs, text))
            }
        }
    }
    
    val sorted = lines.sortedBy { it.timeMs }.toMutableList()
    // Add instrumental intro if there's a gap at start
    if (sorted.isNotEmpty() && sorted[0].timeMs > 2000) {
        sorted.add(0, LyricsLine(0, ""))
    }
    
    return sorted
}
