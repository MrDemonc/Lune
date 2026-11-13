package com.demonlab.lune.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.components.BouncySwitch
import androidx.compose.ui.graphics.Color
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.theme.LuneTheme
import com.demonlab.lune.ui.utils.bounceClick

class AudioSettingsActivity : ComponentActivity() {
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

            LuneTheme(
                darkTheme = targetDarkTheme,
                useCustomColors = useCustomColors,
                customColorPalette = customColorPalette,
                useAmoledPitchBlack = useAmoledPitchBlack
            ) {
                AudioSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong
    val themeMode = settingsManager.themeMode
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))

    var enableHiFi by remember { mutableStateOf(settingsManager.enableHiFi) }
    var is32BitFloatEnabled by remember { mutableStateOf(settingsManager.is32BitFloatEnabled) }
    var isExclusiveModeEnabled by remember { mutableStateOf(settingsManager.isExclusiveModeEnabled) }
    var isCrossfade by remember { mutableStateOf(settingsManager.isCrossfade) }
    var crossfadeDurationSeconds by remember { mutableIntStateOf(settingsManager.crossfadeDurationSeconds) }
    var seamlessLooping by remember { mutableStateOf(settingsManager.seamlessLooping) }
    var stopOnTaskRemoved by remember { mutableStateOf(settingsManager.stopOnTaskRemoved) }
    var showCrossfadeDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (showCrossfadeDialog) {
        var tempCrossfade by remember { mutableIntStateOf(crossfadeDurationSeconds) }
        AlertDialog(
            onDismissRequest = { showCrossfadeDialog = false },
            containerColor = if (hasBlurBackground) (if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.95f) else Color(0xFFF5F5F5).copy(alpha = 0.95f)) else MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.crossfade_time),
                    fontWeight = FontWeight.Bold,
                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (tempCrossfade > 0) "${tempCrossfade}s" else stringResource(R.string.disabled),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        state = rememberSliderState(
                            value = tempCrossfade.toFloat(),
                            steps = 11,
                            trackRange = 0f..12f
                        ),
                        onValueChange = { tempCrossfade = it.toInt() },
                        colors = SliderDefaults.colors(
                            thumbColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                            activeTrackColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = if (hasBlurBackground) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        crossfadeDurationSeconds = tempCrossfade
                        settingsManager.crossfadeDurationSeconds = tempCrossfade
                        val isEnabled = tempCrossfade > 0
                        isCrossfade = isEnabled
                        settingsManager.isCrossfade = isEnabled
                        showCrossfadeDialog = false
                    },
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                        contentColor = if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCrossfadeDialog = false },
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
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
                            stringResource(R.string.audio_settings),
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
                                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
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
            SettingsSection(title = stringResource(R.string.audio_settings)) {
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.hifi_badge),
                    supportingText = stringResource(R.string.hifi_badge_desc),
                    icon = Icons.Default.MusicNote,
                    position = SectionPosition.FIRST,
                    trailingContent = {
                        BouncySwitch(
                            checked = enableHiFi,
                            onCheckedChange = {
                                enableHiFi = it
                                settingsManager.enableHiFi = it
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (enableHiFi) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.audio_32bit_float),
                    supportingText = stringResource(R.string.audio_32bit_float_desc),
                    icon = Icons.Default.Equalizer,
                    position = SectionPosition.MIDDLE,
                    trailingContent = {
                        BouncySwitch(
                            checked = is32BitFloatEnabled,
                            onCheckedChange = {
                                is32BitFloatEnabled = it
                                settingsManager.is32BitFloatEnabled = it
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (is32BitFloatEnabled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.audio_exclusive_mode),
                    supportingText = stringResource(R.string.audio_exclusive_mode_desc),
                    icon = Icons.Default.HighQuality,
                    position = SectionPosition.MIDDLE,
                    trailingContent = {
                        BouncySwitch(
                            checked = isExclusiveModeEnabled,
                            onCheckedChange = {
                                isExclusiveModeEnabled = it
                                settingsManager.isExclusiveModeEnabled = it
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isExclusiveModeEnabled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.crossfade_time),
                    supportingText = if (crossfadeDurationSeconds > 0) "${crossfadeDurationSeconds}s" else stringResource(R.string.disabled),
                    icon = Icons.Default.Tune,
                    position = SectionPosition.MIDDLE,
                    onClick = { showCrossfadeDialog = true }
                )

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.seamless_looping),
                    supportingText = stringResource(R.string.seamless_looping_desc),
                    icon = Icons.Default.AllInclusive,
                    position = SectionPosition.MIDDLE,
                    trailingContent = {
                        BouncySwitch(
                            checked = seamlessLooping,
                            onCheckedChange = {
                                seamlessLooping = it
                                settingsManager.seamlessLooping = it
                                playbackManager.updateLoopingState()
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (seamlessLooping) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.stop_on_task_removed),
                    supportingText = stringResource(R.string.stop_on_task_removed_desc),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    position = SectionPosition.LAST,
                    trailingContent = {
                        BouncySwitch(
                            checked = stopOnTaskRemoved,
                            onCheckedChange = {
                                stopOnTaskRemoved = it
                                settingsManager.stopOnTaskRemoved = it
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (stopOnTaskRemoved) Icons.Default.Check else Icons.Default.Close,
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
