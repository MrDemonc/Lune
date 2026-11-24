package com.demonlab.lune.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.components.BouncySwitch
import com.demonlab.lune.ui.theme.LuneTheme
import com.demonlab.lune.ui.utils.bounceClick

class ThemeSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsManager = SettingsManager.getInstance(this)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableIntStateOf(settingsManager.themeMode) }
            var useCustomColors by remember { mutableStateOf(settingsManager.useCustomColors) }
            var customColorPalette by remember { mutableIntStateOf(settingsManager.customColorPalette) }
            var useAmoledPitchBlack by remember { mutableStateOf(settingsManager.useAmoledPitchBlack) }

            val systemInDarkTheme = isSystemInDarkTheme()
            val targetDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> systemInDarkTheme
            }

            LuneTheme(
                darkTheme = targetDarkTheme,
                useCustomColors = useCustomColors,
                customColorPalette = customColorPalette,
                useAmoledPitchBlack = useAmoledPitchBlack
            ) {
                ThemeSettingsScreen(
                    onBack = { finish() },
                    themeMode = themeMode,
                    onThemeModeChanged = { themeMode = it },
                    useCustomColors = useCustomColors,
                    onCustomColorsChanged = { useCustomColors = it },
                    customColorPalette = customColorPalette,
                    onColorPaletteChanged = { customColorPalette = it },
                    useAmoledPitchBlack = useAmoledPitchBlack,
                    onAmoledChanged = { useAmoledPitchBlack = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    themeMode: Int,
    onThemeModeChanged: (Int) -> Unit,
    useCustomColors: Boolean,
    onCustomColorsChanged: (Boolean) -> Unit,
    customColorPalette: Int,
    onColorPaletteChanged: (Int) -> Unit,
    useAmoledPitchBlack: Boolean,
    onAmoledChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong

    val isDarkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    var isBlurEnabled by remember { mutableStateOf(settingsManager.isBlurEnabled) }
    var isBlurDarkMode by remember { mutableStateOf(settingsManager.isBlurDarkMode) }
    var isBlurLightMode by remember { mutableStateOf(settingsManager.isBlurLightMode) }

    val hasBlurBackground = isBlurEnabled && ((isDarkTheme && isBlurDarkMode) || (!isDarkTheme && isBlurLightMode))
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                            stringResource(R.string.theme),
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
                        containerColor = if (hasBlurBackground && currentSong != null) Color.Transparent else MaterialTheme.colorScheme.surface,
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
                // 1. Modo del Tema (Claro / Oscuro / Sistema)
                SettingsSection(title = stringResource(R.string.theme_mode_title)) {
                    val options = listOf(
                        Triple(0, stringResource(R.string.theme_auto), Icons.Outlined.BrightnessAuto),
                        Triple(1, stringResource(R.string.theme_light), Icons.Default.LightMode),
                        Triple(2, stringResource(R.string.theme_dark), Icons.Default.DarkMode)
                    )

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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            options.forEach { (mode, label, icon) ->
                                val isSelected = themeMode == mode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .bounceClick()
                                        .clickable {
                                            settingsManager.themeMode = mode
                                            onThemeModeChanged(mode)
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) {
                                        if (hasBlurBackground) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        if (hasBlurBackground) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) {
                                            if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                        } else {
                                            if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) {
                                                if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                            } else {
                                                if (hasBlurBackground) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Modo AMOLED (Negro Puro)
                SettingsSection(title = stringResource(R.string.amoled_pitch_black)) {
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.amoled_pitch_black),
                        supportingText = stringResource(R.string.amoled_pitch_black_desc),
                        icon = Icons.Default.DarkMode,
                        position = SectionPosition.SINGLE,
                        trailingContent = {
                            BouncySwitch(
                                checked = useAmoledPitchBlack,
                                onCheckedChange = {
                                    settingsManager.useAmoledPitchBlack = it
                                    onAmoledChanged(it)
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (useAmoledPitchBlack) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Colores personalizados (Custom Colors)
                SettingsSection(title = stringResource(R.string.color_palette)) {
                    val position = if (useCustomColors) SectionPosition.FIRST else SectionPosition.SINGLE
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.use_custom_colors),
                        supportingText = stringResource(R.string.use_custom_colors_desc),
                        icon = Icons.Default.Palette,
                        position = position,
                        trailingContent = {
                            BouncySwitch(
                                checked = useCustomColors,
                                onCheckedChange = {
                                    settingsManager.useCustomColors = it
                                    onCustomColorsChanged(it)
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (useCustomColors) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    )

                    AnimatedVisibility(
                        visible = useCustomColors,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp)
                                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp, topStart = 4.dp, topEnd = 4.dp)),
                            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp, topStart = 4.dp, topEnd = 4.dp),
                            color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val palettes = listOf(
                                        0 to Color(0xFF6650a4),
                                        1 to Color(0xFFB04B38),
                                        2 to Color(0xFF386B52),
                                        3 to Color(0xFF2E6580),
                                        4 to Color(0xFF6E568F),
                                        5 to Color(0xFF7F5700),
                                        6 to (if (isDarkTheme) Color(0xFF1E1E22) else Color(0xFF141316))
                                    )
                                    palettes.forEach { (index, color) ->
                                        val isSelected = customColorPalette == index
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .bounceClick()
                                                .clickable {
                                                    settingsManager.customColorPalette = index
                                                    onColorPaletteChanged(index)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = color,
                                                modifier = Modifier.fillMaxSize(),
                                                border = if (isSelected) BorderStroke(
                                                    3.dp,
                                                    if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                                                ) else if (index == 6) BorderStroke(
                                                    1.5.dp,
                                                    if (hasBlurBackground) Color.White.copy(alpha = 0.45f)
                                                    else (if (isDarkTheme) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.20f))
                                                ) else null
                                            ) {
                                                if (isSelected) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Brush,
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
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Efecto Blur (Desenfoque)
                SettingsSection(title = stringResource(R.string.blur)) {
                    val masterPosition = if (isBlurEnabled) SectionPosition.FIRST else SectionPosition.SINGLE
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.enable_blur),
                        supportingText = stringResource(R.string.enable_blur_desc),
                        icon = Icons.Default.BlurOn,
                        position = masterPosition,
                        trailingContent = {
                            BouncySwitch(
                                checked = isBlurEnabled,
                                onCheckedChange = {
                                    isBlurEnabled = it
                                    settingsManager.isBlurEnabled = it
                                    if (it) {
                                        settingsManager.isBlurCinematicMode = true
                                        settingsManager.isBlurControlsEnabled = true
                                    }
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (isBlurEnabled) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    )

                    AnimatedVisibility(
                        visible = isBlurEnabled,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            SettingsPreferenceItem(
                                headlineText = stringResource(R.string.blur_dark_mode),
                                supportingText = stringResource(R.string.blur_dark_mode_desc),
                                icon = Icons.Default.DarkMode,
                                position = SectionPosition.MIDDLE,
                                trailingContent = {
                                    BouncySwitch(
                                        checked = isBlurDarkMode,
                                        onCheckedChange = {
                                            isBlurDarkMode = it
                                            settingsManager.isBlurDarkMode = it
                                            if (!it && !isBlurLightMode) {
                                                isBlurEnabled = false
                                                settingsManager.isBlurEnabled = false
                                            }
                                        },
                                        thumbContent = {
                                            Icon(
                                                imageVector = if (isBlurDarkMode) Icons.Default.Check else Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize)
                                            )
                                        }
                                    )
                                }
                            )

                            SettingsPreferenceItem(
                                headlineText = stringResource(R.string.blur_light_mode),
                                supportingText = stringResource(R.string.blur_light_mode_desc),
                                icon = Icons.Default.LightMode,
                                position = SectionPosition.LAST,
                                trailingContent = {
                                    BouncySwitch(
                                        checked = isBlurLightMode,
                                        onCheckedChange = {
                                            isBlurLightMode = it
                                            settingsManager.isBlurLightMode = it
                                            if (!it && !isBlurDarkMode) {
                                                isBlurEnabled = false
                                                settingsManager.isBlurEnabled = false
                                            }
                                        },
                                        thumbContent = {
                                            Icon(
                                                imageVector = if (isBlurLightMode) Icons.Default.Check else Icons.Default.Close,
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
