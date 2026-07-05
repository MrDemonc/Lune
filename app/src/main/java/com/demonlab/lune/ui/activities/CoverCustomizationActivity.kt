package com.demonlab.lune.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.demonlab.lune.ui.components.BouncySwitch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.theme.LuneTheme

class CoverCustomizationActivity : ComponentActivity() {
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
                CoverCustomizationScreen(
                    onBack = { finish() },
                    settingsManager = settingsManager
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverCustomizationScreen(
    onBack: () -> Unit,
    settingsManager: SettingsManager
) {
    var coverShape by remember { mutableIntStateOf(settingsManager.coverShape) }
    var coverScale by remember { mutableFloatStateOf(settingsManager.coverScale) }
    var coverSpin by remember { mutableStateOf(settingsManager.coverSpin) }
    var coverVinylEffect by remember { mutableStateOf(settingsManager.coverVinylEffect) }

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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.cover_player),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
            // Live Preview Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.cover_preview),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    val mockCoverPainter = painterResource(R.drawable.ic_launcher_foreground)

                    Box(
                        modifier = Modifier
                            .size(200.dp)
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
                                color = MaterialTheme.colorScheme.primaryContainer,
                                tonalElevation = 8.dp
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

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Mock player controls
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Starlight Sonata",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Lune MrDemonc",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shape Selection
            SettingsSection(title = stringResource(R.string.cover_shape_title)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                modifier = Modifier.clickable {
                                    coverShape = index
                                    settingsManager.coverShape = index
                                }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    border = BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Surface(
                                            shape = shape,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(36.dp)
                                        ) {}
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scale / Size Selection
            SettingsSection(title = stringResource(R.string.cover_size)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.size_small), style = MaterialTheme.typography.labelSmall, color = if (coverScale == 0.70f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.size_medium), style = MaterialTheme.typography.labelSmall, color = if (coverScale == 0.85f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.size_large), style = MaterialTheme.typography.labelSmall, color = if (coverScale == 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
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
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            coverScale = scaleVal
                                            settingsManager.coverScale = scaleVal
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = text,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (coverShape == 2) {
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
    }
}

@Composable
fun VinylRecordCover(
    painter: Painter,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .rotate(rotation)
            .clip(CircleShape)
            .background(Color(0xFF101010)),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize(0.9f).border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape))
        Box(modifier = Modifier.fillMaxSize(0.8f).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape))
        Box(modifier = Modifier.fillMaxSize(0.7f).border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape))
        Box(modifier = Modifier.fillMaxSize(0.6f).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape))

        Surface(
            shape = CircleShape,
            modifier = Modifier.fillMaxSize(0.55f),
            border = BorderStroke(2.dp, Color(0xFF202020))
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Color(0xFF101010))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        )
    }
}
