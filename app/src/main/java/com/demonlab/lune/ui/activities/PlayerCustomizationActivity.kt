package com.demonlab.lune.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.components.BouncySwitch
import com.demonlab.lune.ui.player.ReusableSkipIcon
import com.demonlab.lune.ui.theme.LuneTheme
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import com.demonlab.lune.ui.utils.bounceClick
import kotlinx.coroutines.delay

class PlayerCustomizationActivity : ComponentActivity() {
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

            var useCustomColors by remember { mutableStateOf(settingsManager.useCustomColors) }
            var customColorPalette by remember { mutableIntStateOf(settingsManager.customColorPalette) }
            var useAmoledPitchBlack by remember { mutableStateOf(settingsManager.useAmoledPitchBlack) }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        useCustomColors = settingsManager.useCustomColors
                        customColorPalette = settingsManager.customColorPalette
                        useAmoledPitchBlack = settingsManager.useAmoledPitchBlack
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LuneTheme(
                darkTheme = targetDarkTheme,
                useCustomColors = useCustomColors,
                customColorPalette = customColorPalette,
                useAmoledPitchBlack = useAmoledPitchBlack
            ) {
                PlayerCustomizationScreen(
                    onBack = { finish() },
                    settingsManager = settingsManager
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerCustomizationScreen(
    onBack: () -> Unit,
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))

    var coverShape by remember { mutableIntStateOf(settingsManager.coverShape) }
    var coverScale by remember { mutableFloatStateOf(settingsManager.coverScale) }
    var coverSpin by remember { mutableStateOf(settingsManager.coverSpin) }
    var coverVinylEffect by remember { mutableStateOf(settingsManager.coverVinylEffect) }

    var progressBarStyle by remember { mutableIntStateOf(settingsManager.progressBarStyle) }

    var controlsIconStyle by remember { mutableIntStateOf(settingsManager.controlsIconStyle) }
    var isControlsFilled by remember { mutableStateOf(settingsManager.isControlsFilled) }
    var useCustomControlsColor by remember { mutableStateOf(settingsManager.useCustomControlsColor) }
    var controlsColorPalette by remember { mutableIntStateOf(settingsManager.controlsColorPalette) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val infiniteTransition = rememberInfiniteTransition(label = "CoverSpinRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAnimation"
    )

    val activePrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)
    val activeContainerColor = if (useCustomControlsColor) {
        activePrimary.copy(alpha = 0.2f)
    } else if (hasBlurBackground) {
        Color.White.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    }

    val activeIconTint = if (useCustomControlsColor) {
        activePrimary
    } else if (hasBlurBackground) {
        Color.White
    } else {
        MaterialTheme.colorScheme.primary
    }

    AppBlurBackdrop(
        hasBlurBackground = hasBlurBackground,
        isDarkTheme = isDarkTheme,
        currentSong = currentSong
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = if (hasBlurBackground && currentSong != null) Color.Transparent else MaterialTheme.colorScheme.surface,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.player_customization),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.bounceClick()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.cd_back),
                                        tint = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = if (hasBlurBackground && currentSong != null) Color.Transparent else MaterialTheme.colorScheme.surface,
                        titleContentColor = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Unified Live Preview Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.cover_preview),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Live Cover Preview
                        val mockCoverPainter = painterResource(R.drawable.ic_launcher_foreground)
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .scale(coverScale),
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverShape == 2 && coverVinylEffect) {
                                VinylRecordCover(
                                    painter = mockCoverPainter,
                                    rotation = if (coverSpin) rotation else 0f,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val activeShape = when (coverShape) {
                                    1 -> RoundedCornerShape(0.dp)
                                    2 -> CircleShape
                                    else -> RoundedCornerShape(24.dp)
                                }
                                Surface(
                                    shape = activeShape,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .rotate(if (coverShape == 2 && coverSpin) rotation else 0f),
                                    color = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                                    tonalElevation = if (hasBlurBackground) 0.dp else 8.dp
                                ) {
                                    Image(
                                        painter = mockCoverPainter,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Song Info
                        Text(
                            text = "Starlight Sonata",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Lune MrDemonc",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Progress Bar Preview
                        val progressColor = if (hasBlurBackground) Color.White else activePrimary
                        val progressTrackColor = if (hasBlurBackground) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                        val targetAmplitude: (Float) -> Float = { 1f }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (progressBarStyle == 1) {
                                LinearProgressIndicator(
                                    progress = { 0.42f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    color = progressColor,
                                    trackColor = progressTrackColor,
                                    drawStopIndicator = {}
                                )
                            } else {
                                LinearWavyProgressIndicator(
                                    progress = { 0.42f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    color = progressColor,
                                    trackColor = progressTrackColor,
                                    amplitude = targetAmplitude
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Time stamps
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1:24",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "3:45",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Live Controls Preview
                        var mockIsPlaying by remember { mutableStateOf(false) }
                        var mockShowLabel by remember { mutableStateOf(false) }
                        var mockFirstComp by remember { mutableStateOf(true) }
                        var mockTrigger by remember { mutableIntStateOf(0) }

                        LaunchedEffect(mockIsPlaying, mockTrigger) {
                            if (mockFirstComp) {
                                mockFirstComp = false
                                return@LaunchedEffect
                            }
                            mockShowLabel = true
                            delay(1300L)
                            mockShowLabel = false
                        }

                        val mockPlayWidth by animateDpAsState(
                            targetValue = if (mockShowLabel) 156.dp else 96.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "MockPlayWidth"
                        )

                        val previewSkipShape = RoundedCornerShape(26.dp)
                        val previewPlayShape = RoundedCornerShape(30.dp)
                        val previewSkipBorder = BorderStroke(
                            width = 1.dp,
                            color = if (hasBlurBackground) {
                                if (isDarkTheme) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)
                            } else if (useCustomControlsColor) {
                                activePrimary.copy(alpha = 0.20f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            }
                        )
                        val previewPlayBorder = BorderStroke(
                            width = 1.dp,
                            color = if (hasBlurBackground) {
                                if (isDarkTheme) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f)
                            } else if (useCustomControlsColor) {
                                activePrimary.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            }
                        )

                        val previewPlayBg = if (useCustomControlsColor) {
                            activePrimary
                        } else if (hasBlurBackground) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.primary
                        }

                        val previewPlayTint = if (useCustomControlsColor) {
                            Color.White
                        } else if (hasBlurBackground) {
                            Color.Black
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = previewSkipShape,
                                color = activeContainerColor,
                                border = previewSkipBorder,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    ReusableSkipIcon(
                                        isNext = false,
                                        controlsIconStyle = controlsIconStyle,
                                        isControlsFilled = isControlsFilled,
                                        tint = activeIconTint,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    mockTrigger++
                                    mockIsPlaying = !mockIsPlaying
                                },
                                shape = previewPlayShape,
                                color = previewPlayBg,
                                border = previewPlayBorder,
                                modifier = Modifier
                                    .height(68.dp)
                                    .width(mockPlayWidth)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (mockIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = previewPlayTint,
                                            modifier = Modifier.size(36.dp)
                                        )

                                        AnimatedVisibility(
                                            visible = mockShowLabel,
                                            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                                    expandHorizontally(
                                                        expandFrom = Alignment.Start,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    ) +
                                                    scaleIn(
                                                        initialScale = 0.8f,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    ),
                                            exit = fadeOut(animationSpec = tween(150)) +
                                                    shrinkHorizontally(
                                                        shrinkTowards = Alignment.Start,
                                                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                                    )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(start = 6.dp, end = 4.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(if (mockIsPlaying) R.string.play_state_play else R.string.play_state_pause),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = previewPlayTint,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Clip
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = previewSkipShape,
                                color = activeContainerColor,
                                border = previewSkipBorder,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    ReusableSkipIcon(
                                        isNext = true,
                                        controlsIconStyle = controlsIconStyle,
                                        isControlsFilled = isControlsFilled,
                                        tint = activeIconTint,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Cover Shape
                SettingsSection(title = stringResource(R.string.cover_shape_title)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val shapes = listOf(
                                0 to (stringResource(R.string.shape_default) to RoundedCornerShape(12.dp)),
                                1 to (stringResource(R.string.shape_square) to RoundedCornerShape(0.dp)),
                                2 to (stringResource(R.string.shape_circular) to CircleShape)
                            )

                            shapes.forEach { (index, data) ->
                                val (label, shape) = data
                                val isSelected = coverShape == index

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .bounceClick()
                                        .clickable {
                                            coverShape = index
                                            settingsManager.coverShape = index
                                        }
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) {
                                            if (hasBlurBackground) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            if (hasBlurBackground) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        },
                                        border = BorderStroke(2.dp, if (isSelected) (if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary) else Color.Transparent),
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Surface(
                                                shape = shape,
                                                color = if (isSelected) {
                                                    if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                                } else {
                                                    if (hasBlurBackground) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {}
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) (if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary) else (if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Cover Size
                SettingsSection(title = stringResource(R.string.cover_size)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.size_small), style = MaterialTheme.typography.labelSmall, color = if (coverScale == 0.70f) (if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary) else (if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant))
                                Text(stringResource(R.string.size_medium), style = MaterialTheme.typography.labelSmall, color = if (coverScale == 0.85f) (if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary) else (if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant))
                                Text(stringResource(R.string.size_large), style = MaterialTheme.typography.labelSmall, color = if (coverScale == 1.0f) (if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary) else (if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(if (hasBlurBackground) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val sizes = listOf(
                                    0.70f to "70%",
                                    0.85f to "85%",
                                    1.0f to "100%"
                                )
                                sizes.forEach { (scaleVal, text) ->
                                    val isSelected = coverScale == scaleVal
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(if (isSelected) (if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary) else Color.Transparent)
                                            .bounceClick()
                                            .clickable {
                                                coverScale = scaleVal
                                                settingsManager.coverScale = scaleVal
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = text,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) (if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary) else (if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSecondaryContainer)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Circular Effects (visible when coverShape == 2)
                AnimatedVisibility(
                    visible = coverShape == 2,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsSection(title = stringResource(R.string.circular_effects)) {
                            SettingsPreferenceItem(
                                headlineText = stringResource(R.string.cover_spin),
                                supportingText = stringResource(R.string.cover_spin_desc),
                                icon = Icons.Default.Refresh,
                                position = SectionPosition.FIRST,
                                trailingContent = {
                                    BouncySwitch(
                                        checked = coverSpin,
                                        onCheckedChange = {
                                            coverSpin = it
                                            settingsManager.coverSpin = it
                                        },
                                        thumbContent = {
                                            Icon(
                                                imageVector = if (coverSpin) Icons.Default.Check else Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize)
                                            )
                                        }
                                    )
                                }
                            )

                            SettingsPreferenceItem(
                                headlineText = stringResource(R.string.cover_vinyl),
                                supportingText = stringResource(R.string.cover_vinyl_desc),
                                icon = Icons.Default.Album,
                                position = SectionPosition.LAST,
                                trailingContent = {
                                    BouncySwitch(
                                        checked = coverVinylEffect,
                                        onCheckedChange = {
                                            coverVinylEffect = it
                                            settingsManager.coverVinylEffect = it
                                        },
                                        thumbContent = {
                                            Icon(
                                                imageVector = if (coverVinylEffect) Icons.Default.Check else Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize)
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Progress Bar Style (Wave vs Straight/Linear)
                SettingsSection(title = stringResource(R.string.progress_bar_style_title)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val cardShape = RoundedCornerShape(20.dp)

                            // Option 0: Wave (Onda)
                            val isWaveSelected = progressBarStyle == 0
                            Surface(
                                onClick = {
                                    progressBarStyle = 0
                                    settingsManager.progressBarStyle = 0
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClick()
                                    .clip(cardShape),
                                shape = cardShape,
                                color = if (isWaveSelected) {
                                    if (hasBlurBackground) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    if (hasBlurBackground) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                },
                                border = BorderStroke(
                                    width = 2.dp,
                                    color = if (isWaveSelected) {
                                        if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.progress_bar_wave),
                                            fontWeight = if (isWaveSelected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isWaveSelected) {
                                                if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                            } else {
                                                if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isWaveSelected) {
                                                if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.Transparent
                                            },
                                            border = if (!isWaveSelected) BorderStroke(1.5.dp, if (hasBlurBackground) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline) else null,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            if (isWaveSelected) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Mini wavy bar
                                    LinearWavyProgressIndicator(
                                        progress = { 0.65f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        color = if (isWaveSelected) {
                                            if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                        } else {
                                            if (hasBlurBackground) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        },
                                        trackColor = if (hasBlurBackground) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        amplitude = { 1f }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            // Option 1: Linear / Straight (Recta)
                            val isLinearSelected = progressBarStyle == 1
                            Surface(
                                onClick = {
                                    progressBarStyle = 1
                                    settingsManager.progressBarStyle = 1
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClick()
                                    .clip(cardShape),
                                shape = cardShape,
                                color = if (isLinearSelected) {
                                    if (hasBlurBackground) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    if (hasBlurBackground) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                },
                                border = BorderStroke(
                                    width = 2.dp,
                                    color = if (isLinearSelected) {
                                        if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.progress_bar_line),
                                            fontWeight = if (isLinearSelected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isLinearSelected) {
                                                if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                            } else {
                                                if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isLinearSelected) {
                                                if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.Transparent
                                            },
                                            border = if (!isLinearSelected) BorderStroke(1.5.dp, if (hasBlurBackground) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline) else null,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            if (isLinearSelected) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Mini straight linear bar
                                    LinearProgressIndicator(
                                        progress = { 0.65f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        color = if (isLinearSelected) {
                                            if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                        } else {
                                            if (hasBlurBackground) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        },
                                        trackColor = if (hasBlurBackground) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        drawStopIndicator = {}
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Controls Button Style
                SettingsSection(title = stringResource(R.string.controls_style_title)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val styles = listOf(
                                0 to stringResource(R.string.shape_default),
                                1 to "Play 2",
                                2 to "Play 3"
                            )

                            styles.forEach { (index, label) ->
                                val isSelected = controlsIconStyle == index

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .bounceClick()
                                        .clickable {
                                            controlsIconStyle = index
                                            settingsManager.controlsIconStyle = index
                                        }
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) {
                                            if (hasBlurBackground) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            if (hasBlurBackground) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        },
                                        border = BorderStroke(2.dp, if (isSelected) (if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary) else Color.Transparent),
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            ReusableSkipIcon(
                                                isNext = true,
                                                controlsIconStyle = index,
                                                isControlsFilled = isControlsFilled,
                                                tint = if (isSelected) (if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary) else (if (hasBlurBackground) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) (if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary) else (if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Controls Settings (Filled & Custom Colors)
                SettingsSection(title = stringResource(R.string.categories)) {
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.controls_filled),
                        supportingText = stringResource(R.string.controls_filled_desc),
                        icon = Icons.Default.CheckCircle,
                        position = SectionPosition.FIRST,
                        trailingContent = {
                            BouncySwitch(
                                checked = isControlsFilled,
                                onCheckedChange = {
                                    isControlsFilled = it
                                    settingsManager.isControlsFilled = it
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (isControlsFilled) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    )

                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.controls_custom_color),
                        supportingText = stringResource(R.string.controls_custom_color_desc),
                        icon = Icons.Default.Palette,
                        position = if (useCustomControlsColor) SectionPosition.MIDDLE else SectionPosition.LAST,
                        trailingContent = {
                            BouncySwitch(
                                checked = useCustomControlsColor,
                                onCheckedChange = {
                                    useCustomControlsColor = it
                                    settingsManager.useCustomControlsColor = it
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (useCustomControlsColor) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    )

                    if (useCustomControlsColor) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                            color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = stringResource(R.string.color_palette),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val palettes = listOf(
                                        0 to MaterialTheme.colorScheme.primary, // App default / Material You
                                        1 to Color(0xFFB04B38), // Sunset Peach
                                        2 to Color(0xFF386B52), // Sage Green
                                        3 to Color(0xFF2E6580), // Ocean Breeze
                                        4 to Color(0xFF6E568F), // Lavender Mist
                                        5 to Color(0xFF7F5700)  // Warm Amber
                                    )
                                    palettes.forEach { (index, color) ->
                                        val isSelected = controlsColorPalette == index
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .bounceClick()
                                                .clickable {
                                                    controlsColorPalette = index
                                                    settingsManager.controlsColorPalette = index
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
