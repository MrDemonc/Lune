package com.demonlab.lune.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demonlab.lune.R
import com.demonlab.lune.tools.LuneWidgetProvider
import com.demonlab.lune.tools.MusicService
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.components.BouncySwitch
import com.demonlab.lune.ui.activities.SectionPosition
import com.demonlab.lune.ui.activities.SettingsPreferenceItem
import com.demonlab.lune.ui.activities.SettingsSection
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.theme.LuneTheme
import androidx.compose.foundation.isSystemInDarkTheme

class WidgetCustomizationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsManager = SettingsManager.getInstance(this)

        setContent {
            var themeMode by remember { mutableIntStateOf(settingsManager.themeMode) }
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
                        themeMode = settingsManager.themeMode
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
                WidgetCustomizationScreen(
                    onBack = { finish() },
                    settingsManager = settingsManager
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCustomizationScreen(
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

    var widgetUseSolidBackground by remember { mutableStateOf(settingsManager.widgetUseSolidBackground) }
    var widgetLightBackgroundColor by remember { mutableIntStateOf(settingsManager.widgetLightBackgroundColor) }
    var widgetDarkBackgroundColor by remember { mutableIntStateOf(settingsManager.widgetDarkBackgroundColor) }
    var widgetBackgroundDarkness by remember { mutableFloatStateOf(settingsManager.widgetBackgroundDarkness) }
    var widgetBackgroundBlur by remember { mutableIntStateOf(settingsManager.widgetBackgroundBlur) }
    var widgetCircularCover by remember { mutableStateOf(settingsManager.widgetCircularCover) }
    var widgetVinylCover by remember { mutableStateOf(settingsManager.widgetVinylCover) }

    // Toggle for live preview between Light and Dark mode
    var previewIsNight by remember {
        mutableStateOf(
            (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun notifyWidgetUpdate() {
        try {
            val serviceIntent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_UPDATE_WIDGET
            }
            context.startService(serviceIntent)
        } catch (_: Exception) {}

        val broadcastIntent = Intent("com.demonlab.lune.WIDGET_UPDATE").apply {
            `package` = context.packageName
        }
        context.sendBroadcast(broadcastIntent)
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
                            stringResource(R.string.widget_customization),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
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
                // Live Preview Card
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.cover_preview),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                        // Mode switcher for preview (Light / Dark)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.clickable { previewIsNight = !previewIsNight }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (previewIsNight) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (previewIsNight) stringResource(R.string.theme_dark) else stringResource(R.string.theme_light),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated 1:1 Live Widget Preview
                    val activeSolidColor = if (previewIsNight) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                    val isLightPreview = widgetUseSolidBackground && LuneWidgetProvider.isColorLight(activeSolidColor)

                    val previewTextColor = if (isLightPreview) Color(0xFF151515) else Color.White
                    val previewSubtextColor = if (isLightPreview) Color(0xFF606060) else Color(0xFFD0D0D0)
                    val previewControlTint = if (isLightPreview) Color(0xFF151515) else Color.White

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        shadowElevation = 8.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Background layer
                            if (widgetUseSolidBackground) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(activeSolidColor))
                                )
                            } else {
                                // Default dark gradient + blurred simulation
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF2A292E), Color(0xFF1B1A1E), Color(0xFF111013))
                                            )
                                        )
                                    )
                                // Simulated album art blurred glow with darkness and blur
                                val mockCover = painterResource(R.drawable.ic_launcher_foreground)
                                Image(
                                    painter = mockCover,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur((widgetBackgroundBlur * 0.4f).dp)
                                        .background(
                                            (if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer).copy(
                                                alpha = (1f - (widgetBackgroundDarkness * 0.7f)).coerceIn(0.1f, 0.9f)
                                            )
                                        )
                                )
                                // Darkness overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = widgetBackgroundDarkness))
                                )
                            }

                            // Widget contents
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cover Art Container
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .padding(end = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (widgetCircularCover && widgetVinylCover) {
                                        // Vinyl record preview
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(Color(0xFF101010)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(0.92f).border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape))
                                            Box(modifier = Modifier.fillMaxSize(0.85f).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape))
                                            Box(modifier = Modifier.fillMaxSize(0.78f).border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape))
                                            Box(modifier = Modifier.fillMaxSize(0.70f).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape))
                                            Box(modifier = Modifier.fillMaxSize(0.63f).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape))

                                            Surface(
                                                shape = CircleShape,
                                                modifier = Modifier.fillMaxSize(0.55f),
                                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                                                border = BorderStroke(2.dp, Color(0xFF252525))
                                            ) {
                                                Image(
                                                    painter = painterResource(R.drawable.ic_launcher_foreground),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF101010))
                                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                            )
                                        }
                                    } else if (widgetCircularCover) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.fillMaxSize(),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                        ) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }

                                // Right side info & controls
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Top output badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isLightPreview) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Speaker,
                                                    contentDescription = null,
                                                    tint = previewControlTint,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Speaker",
                                                    color = previewTextColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Title and Artist
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Song Title",
                                            color = previewTextColor,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Artist Name",
                                            color = previewSubtextColor,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Controls row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.SkipPrevious,
                                            contentDescription = null,
                                            tint = previewControlTint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = previewControlTint,
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Icon(
                                            Icons.Default.SkipNext,
                                            contentDescription = null,
                                            tint = previewControlTint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= SECTION 1: BACKGROUND CUSTOMIZATION =================
            SettingsSection(title = stringResource(R.string.widget_background_section)) {
                // Option 1: Solid Color Background Switch
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.widget_solid_bg),
                    supportingText = stringResource(R.string.widget_solid_bg_desc),
                    icon = Icons.Default.FormatColorFill,
                    position = if (widgetUseSolidBackground) SectionPosition.SINGLE else SectionPosition.FIRST,
                    trailingContent = {
                        BouncySwitch(
                            checked = widgetUseSolidBackground,
                            onCheckedChange = {
                                widgetUseSolidBackground = it
                                settingsManager.widgetUseSolidBackground = it
                                notifyWidgetUpdate()
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (widgetUseSolidBackground) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                // Sub-options when Solid Background is disabled: Blur Slider & Darkness Slider
                if (!widgetUseSolidBackground) {
                    // Option 2: Background Blur Slider (MIDDLE)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.BlurOn,
                                        contentDescription = null,
                                        tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.widget_blur),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.widget_blur_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "${widgetBackgroundBlur}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Slider(
                                value = widgetBackgroundBlur.toFloat(),
                                onValueChange = {
                                    widgetBackgroundBlur = it.toInt()
                                    settingsManager.widgetBackgroundBlur = it.toInt()
                                    notifyWidgetUpdate()
                                },
                                valueRange = 10f..100f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                    activeTrackColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = if (hasBlurBackground) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                    activeTickColor = if (hasBlurBackground) Color.Black.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onPrimary,
                                    inactiveTickColor = if (hasBlurBackground) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("10%", style = MaterialTheme.typography.bodySmall, color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("50%", style = MaterialTheme.typography.bodySmall, color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("100%", style = MaterialTheme.typography.bodySmall, color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Option 3: Background Darkness Slider (LAST)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                        color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Opacity,
                                        contentDescription = null,
                                        tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.widget_darkness),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.widget_darkness_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "${(widgetBackgroundDarkness * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Slider(
                                value = widgetBackgroundDarkness,
                                onValueChange = {
                                    widgetBackgroundDarkness = it
                                    settingsManager.widgetBackgroundDarkness = it
                                    notifyWidgetUpdate()
                                },
                                valueRange = 0.10f..0.90f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                    activeTrackColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = if (hasBlurBackground) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                    activeTickColor = if (hasBlurBackground) Color.Black.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onPrimary,
                                    inactiveTickColor = if (hasBlurBackground) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("10%", style = MaterialTheme.typography.bodySmall, color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("50%", style = MaterialTheme.typography.bodySmall, color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("90%", style = MaterialTheme.typography.bodySmall, color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= SECTION 2: COVER ART CUSTOMIZATION =================
            SettingsSection(title = stringResource(R.string.widget_cover_section)) {
                // Option 3: Circular Cover Switch
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.widget_circular_cover),
                    supportingText = stringResource(R.string.widget_circular_cover_desc),
                    icon = Icons.Default.Circle,
                    position = if (widgetCircularCover) SectionPosition.FIRST else SectionPosition.SINGLE,
                    trailingContent = {
                        BouncySwitch(
                            checked = widgetCircularCover,
                            onCheckedChange = {
                                widgetCircularCover = it
                                settingsManager.widgetCircularCover = it
                                notifyWidgetUpdate()
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (widgetCircularCover) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                // Sub-option: Vinyl Record Effect
                AnimatedVisibility(
                    visible = widgetCircularCover,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.widget_vinyl_cover),
                        supportingText = stringResource(R.string.widget_vinyl_cover_desc),
                        icon = Icons.Default.SurroundSound,
                        position = SectionPosition.LAST,
                        trailingContent = {
                            BouncySwitch(
                                checked = widgetVinylCover,
                                onCheckedChange = {
                                    widgetVinylCover = it
                                    settingsManager.widgetVinylCover = it
                                    notifyWidgetUpdate()
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (widgetVinylCover) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}
