package com.demonlab.lune.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demonlab.lune.R
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.tools.SongResolver
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.components.VinylRecordAsyncCover
import com.demonlab.lune.ui.components.rememberBlurSheetColors
import com.demonlab.lune.ui.player.ReusableSkipIcon
import com.demonlab.lune.ui.theme.LuneTheme
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.utils.formatDuration
import kotlinx.coroutines.delay

class QuickPlayerActivity : ComponentActivity() {

    private var currentUriState = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        handleIntent(intent)

        setContent {
            val context = LocalContext.current
            val settingsManager = remember { SettingsManager.getInstance(context) }
            val playbackManager = remember { PlaybackManager.getInstance(context) }

            val isDarkTheme = when (settingsManager.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            val targetUri = currentUriState.value

            // Resolve target song
            var resolvedSong by remember(targetUri) {
                mutableStateOf<Song?>(null)
            }
            var playlist by remember(targetUri) {
                mutableStateOf<List<Song>>(emptyList())
            }

            LaunchedEffect(targetUri) {
                if (targetUri != null) {
                    val s = SongResolver.resolveSongFromUri(context, targetUri)
                    if (s != null) {
                        resolvedSong = s
                        val siblings = SongResolver.resolveSiblingSongs(context, s)
                        playlist = siblings

                        // If not already playing this song, start playback immediately
                        if (playbackManager.currentSong?.uri != s.uri || !playbackManager.isPlaying) {
                            playbackManager.play(s, playlist, playlistName = s.folderName)
                        }
                    }
                }
            }

            val activeSong = playbackManager.currentSong ?: resolvedSong

            LuneTheme(
                darkTheme = isDarkTheme,
                dynamicColor = true,
                useCustomColors = settingsManager.useCustomColors,
                customColorPalette = settingsManager.customColorPalette,
                useAmoledPitchBlack = settingsManager.useAmoledPitchBlack
            ) {
                BackHandler {
                    finish()
                }

                QuickPlayerDialogScreen(
                    song = activeSong,
                    playbackManager = playbackManager,
                    settingsManager = settingsManager,
                    isDarkTheme = isDarkTheme,
                    hasSiblings = playlist.size > 1,
                    onClose = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: intent?.clipData?.let {
            if (it.itemCount > 0) it.getItemAt(0).uri else null
        }
        if (uri != null) {
            // Take persistable permission if available
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}

            currentUriState.value = uri
        }
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun QuickPlayerDialogScreen(
    song: Song?,
    playbackManager: PlaybackManager,
    settingsManager: SettingsManager,
    isDarkTheme: Boolean,
    hasSiblings: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isPlaying = playbackManager.isPlaying
    var currentProgress by remember { mutableStateOf(playbackManager.getProgress()) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }

    // Live progress synchronization
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                if (!isDragging) {
                    currentProgress = playbackManager.getProgress()
                }
                delay(200)
            }
        } else {
            if (!isDragging) {
                currentProgress = playbackManager.getProgress()
            }
        }
    }

    val blurColors = rememberBlurSheetColors(song)
    val hasBlur = blurColors.hasBlur

    val activePrimary = getControlsPrimaryColor(
        settingsManager.useCustomControlsColor,
        settingsManager.controlsColorPalette,
        isDarkTheme
    )

    // Animated entrance
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val infiniteSpinTransition = rememberInfiniteTransition(label = "QuickPlayerSpin")
    val spinRotation by infiniteSpinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAnimation"
    )

    // Backdrop Scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClose()
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut()
        ) {
            // Card Container (docked at bottom with margins)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .navigationBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Catch clicks inside */ }
            ) {
                AppBlurBackdrop(
                    hasBlurBackground = hasBlur,
                    isDarkTheme = isDarkTheme,
                    currentSong = song,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    val cardBorder = if (hasBlur) {
                        BorderStroke(1.dp, blurColors.itemBorderColor ?: Color.White.copy(alpha = 0.12f))
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        color = if (hasBlur) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = cardBorder,
                        tonalElevation = if (hasBlur) 0.dp else 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 1. Header Bar: Brand / Title + Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_logo_diamonds),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = activePrimary
                                    )
                                    Text(
                                        text = stringResource(R.string.quick_player),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = blurColors.textColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Open in Lune
                                    IconButton(
                                        onClick = {
                                            val fullAppIntent = Intent(context, Lune::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                                putExtra(Lune.EXTRA_EXPAND_PLAYER, true)
                                            }
                                            context.startActivity(fullAppIntent)
                                            onClose()
                                        },
                                        modifier = Modifier.size(36.dp).bounceClick()
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = stringResource(R.string.open_in_lune),
                                            tint = blurColors.textSecondaryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Close Button
                                    IconButton(
                                        onClick = onClose,
                                        modifier = Modifier.size(36.dp).bounceClick()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.close),
                                            tint = blurColors.textSecondaryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. Song Info Row: Artwork + Title & Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Cover Art respecting customization settings
                                val coverShape = settingsManager.coverShape
                                val coverScale = settingsManager.coverScale
                                val coverSpin = settingsManager.coverSpin
                                val coverVinylEffect = settingsManager.coverVinylEffect

                                val activeShape = when (coverShape) {
                                    1 -> RoundedCornerShape(0.dp)
                                    2 -> CircleShape
                                    else -> RoundedCornerShape(18.dp)
                                }
                                val isVinylActive = coverShape == 2 && coverVinylEffect
                                val isSpinActive = coverShape == 2 && coverSpin && isPlaying

                                val artModel = song?.coverUrl?.let { Uri.parse(it) }
                                    ?: song?.albumArtUri
                                    ?: song?.uri

                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .scale(coverScale.coerceIn(0.6f, 1.4f))
                                        .shadow(
                                            elevation = if (isVinylActive) 8.dp else 6.dp,
                                            shape = activeShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isVinylActive) {
                                        VinylRecordAsyncCover(
                                            model = artModel,
                                            rotation = if (isSpinActive) spinRotation else 0f,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .rotate(if (isSpinActive) spinRotation else 0f),
                                            shape = activeShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            tonalElevation = if (hasBlur) 0.dp else 4.dp
                                        ) {
                                            SongCoverImage(
                                                coverUrl = artModel,
                                                contentDescription = song?.title,
                                                modifier = Modifier.fillMaxSize(),
                                                shape = activeShape,
                                                iconScale = 0.65f
                                            )
                                        }
                                    }
                                }

                                // Title, Artist & Badges
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = song?.title ?: stringResource(R.string.unknown_artist),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = blurColors.textColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = song?.artist ?: stringResource(R.string.unknown_artist),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = blurColors.textSecondaryColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Format Badges Row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!song?.format.isNullOrBlank()) {
                                            Surface(
                                                color = if (hasBlur) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = song.format,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (hasBlur) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (song?.isHiFi == true) {
                                            Surface(
                                                color = activePrimary.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "HI-FI",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = activePrimary,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (song?.bitrate != null && song.bitrate > 0) {
                                            Surface(
                                                color = if (hasBlur) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${song.bitrate / 1000} kbps",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = blurColors.textSecondaryColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 3. Progress Slider & Timestamps
                            val totalDurationMs = if (song != null && song.duration > 0) {
                                song.duration
                            } else {
                                playbackManager.duration().toLong().coerceAtLeast(0L)
                            }
                            val displayProgress = if (isDragging) dragProgress else currentProgress

                            val sliderState = remember { SliderState(displayProgress.coerceIn(0f, 1f)) }
                            LaunchedEffect(displayProgress) {
                                if (!isDragging) {
                                    sliderState.value = displayProgress.coerceIn(0f, 1f)
                                }
                            }

                            Slider(
                                state = sliderState,
                                onValueChange = {
                                    isDragging = true
                                    dragProgress = it
                                },
                                onValueChangeFinished = {
                                    isDragging = false
                                    playbackManager.seekTo(dragProgress)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp),
                                thumb = { _ ->
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                color = if (hasBlur) Color.White else activePrimary,
                                                shape = CircleShape
                                            )
                                    )
                                },
                                colors = SliderDefaults.colors(
                                    activeTrackColor = if (hasBlur) Color.White else activePrimary,
                                    inactiveTrackColor = if (hasBlur) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentMs = (totalDurationMs * displayProgress).toLong()
                                Text(
                                    text = formatDuration(currentMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = blurColors.textSecondaryColor
                                )
                                Text(
                                    text = formatDuration(totalDurationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = blurColors.textSecondaryColor
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4. Playback Controls Row
                            val controlContainerColor = if (hasBlur) {
                                Color.White.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            }
                            val controlIconTint = blurColors.textColor

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Repeat mode toggle
                                IconButton(
                                    onClick = {
                                        playbackManager.repeatMode = (playbackManager.repeatMode + 1) % 3
                                    },
                                    modifier = Modifier.size(42.dp).bounceClick()
                                ) {
                                    val repeatIcon = when (playbackManager.repeatMode) {
                                        1 -> Icons.Default.RepeatOne
                                        else -> Icons.Default.Repeat
                                    }
                                    val repeatTint = if (playbackManager.repeatMode != 0) activePrimary else blurColors.textSecondaryColor
                                    Icon(
                                        imageVector = repeatIcon,
                                        contentDescription = null,
                                        tint = repeatTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Previous / -10s
                                Surface(
                                    onClick = {
                                        if (hasSiblings) {
                                            playbackManager.playPreviousFromService()
                                        } else {
                                            val currentMs = playbackManager.currentPosition().toLong()
                                            playbackManager.seekToTimeMs((currentMs - 10000L).coerceAtLeast(0L))
                                        }
                                    },
                                    shape = CircleShape,
                                    color = controlContainerColor,
                                    modifier = Modifier.size(48.dp).bounceClick()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (hasSiblings) {
                                            ReusableSkipIcon(
                                                isNext = false,
                                                controlsIconStyle = settingsManager.controlsIconStyle,
                                                isControlsFilled = settingsManager.isControlsFilled,
                                                tint = controlIconTint,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Replay10,
                                                contentDescription = null,
                                                tint = controlIconTint,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                // Central Play / Pause Button
                                Surface(
                                    onClick = {
                                        if (isPlaying) {
                                            playbackManager.pause()
                                        } else {
                                            playbackManager.resume()
                                        }
                                    },
                                    shape = CircleShape,
                                    color = if (hasBlur) Color.White.copy(alpha = 0.22f) else activePrimary,
                                    modifier = Modifier.size(68.dp).bounceClick()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val avd = AnimatedImageVector.animatedVectorResource(R.drawable.avd_play_pause_morph)
                                        Icon(
                                            painter = rememberAnimatedVectorPainter(avd, atEnd = isPlaying),
                                            contentDescription = stringResource(R.string.cd_play_pause),
                                            modifier = Modifier.size(34.dp),
                                            tint = if (hasBlur) Color.White else MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                // Next / +10s
                                Surface(
                                    onClick = {
                                        if (hasSiblings) {
                                            playbackManager.playNextFromService()
                                        } else {
                                            val currentMs = playbackManager.currentPosition().toLong()
                                            playbackManager.seekToTimeMs((currentMs + 10000L).coerceAtMost(totalDurationMs))
                                        }
                                    },
                                    shape = CircleShape,
                                    color = controlContainerColor,
                                    modifier = Modifier.size(48.dp).bounceClick()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (hasSiblings) {
                                            ReusableSkipIcon(
                                                isNext = true,
                                                controlsIconStyle = settingsManager.controlsIconStyle,
                                                isControlsFilled = settingsManager.isControlsFilled,
                                                tint = controlIconTint,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Forward10,
                                                contentDescription = null,
                                                tint = controlIconTint,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                // Favorite Toggle
                                IconButton(
                                    onClick = {
                                        song?.let { playbackManager.toggleFavorite(it) }
                                    },
                                    modifier = Modifier.size(42.dp).bounceClick()
                                ) {
                                    val isFav = song?.isFavorite ?: false
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isFav) Color.Red else blurColors.textSecondaryColor,
                                        modifier = Modifier.size(24.dp)
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
