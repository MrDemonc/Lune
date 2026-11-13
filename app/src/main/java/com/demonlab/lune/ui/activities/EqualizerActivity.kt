package com.demonlab.lune.ui.activities

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.demonlab.lune.R
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.components.BouncySwitch
import com.demonlab.lune.ui.components.rememberBlurSheetColors
import com.demonlab.lune.ui.theme.LuneTheme
import com.demonlab.lune.ui.theme.getControlsPrimaryColor
import com.demonlab.lune.ui.utils.bounceClick
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class EqualizerActivity : ComponentActivity() {
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
                EqualizerScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun EqSection(
    title: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun EqCard(
    modifier: Modifier = Modifier,
    hasBlur: Boolean,
    isDark: Boolean,
    hasSong: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardBg = if (hasBlur && hasSong) {
        if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        tonalElevation = if (hasBlur) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val settingsManager = SettingsManager.getInstance(context)
    val currentSong = playbackManager.currentSong
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))
    val useCustomControlsColor = settingsManager.useCustomControlsColor
    val controlsColorPalette = settingsManager.controlsColorPalette
    val activePrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showVolumeBoostDialog by remember { mutableStateOf(false) }
    var saveDialogName by remember { mutableStateOf("") }
    val isEnabled = playbackManager.isEqEnabled
    val activePreset = playbackManager.activeEqPresetName
    val isCustom = playbackManager.isCustomPreset(activePreset)

    val textColor = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondaryColor = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconTintPrimary = if (hasBlurBackground && currentSong != null) (if (useCustomControlsColor) activePrimary else Color.White) else (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primary)
    val sectionHeaderColor = if (hasBlurBackground && currentSong != null) Color.White else (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primary)
    val dividerColor = if (hasBlurBackground && currentSong != null) {
        if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    val sliderColors = SliderDefaults.colors(
        thumbColor = if (hasBlurBackground && currentSong != null && !useCustomControlsColor) Color.White else activePrimary,
        activeTrackColor = if (hasBlurBackground && currentSong != null && !useCustomControlsColor) Color.White else activePrimary,
        inactiveTrackColor = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
    )

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = if (hasBlurBackground) (if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.95f) else Color(0xFFF5F5F5).copy(alpha = 0.95f)) else MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.eq_title),
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = saveDialogName,
                    onValueChange = { saveDialogName = it },
                    label = { Text(stringResource(R.string.preset_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (saveDialogName.isNotBlank()) {
                            playbackManager.saveCustomEqPreset(saveDialogName.trim())
                            showSaveDialog = false
                        }
                    },
                    modifier = Modifier.bounceClick()
                ) {
                    Text(stringResource(R.string.save), color = iconTintPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveDialog = false },
                    modifier = Modifier.bounceClick()
                ) {
                    Text(stringResource(R.string.cancel), color = textSecondaryColor)
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showVolumeBoostDialog) {
        VolumeBoostDialog(
            initialEnabled = playbackManager.isLoudnessEnabled,
            initialGain = playbackManager.loudnessGain,
            currentSong = currentSong,
            onDismiss = { showVolumeBoostDialog = false },
            onConfirm = { enabled, gain ->
                playbackManager.setLoudnessEnabled(enabled, gain)
                showVolumeBoostDialog = false
            }
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
                            "EQ",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (hasBlurBackground && currentSong != null) Color.White else (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primary)
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
                                        tint = if (hasBlurBackground && currentSong != null) Color.White else (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { playbackManager.toggleEq() },
                            modifier = Modifier.bounceClick()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (hasBlurBackground && currentSong != null) (if (playbackManager.isEqEnabled) (if (useCustomControlsColor) activePrimary else Color.White) else Color.White.copy(alpha = 0.15f)) else (if (playbackManager.isEqEnabled) (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PowerSettingsNew,
                                        contentDescription = stringResource(R.string.cd_activate_eq),
                                        tint = if (hasBlurBackground && currentSong != null) (if (playbackManager.isEqEnabled) (if (useCustomControlsColor) Color.White else Color.Black) else Color.White) else (if (playbackManager.isEqEnabled) MaterialTheme.colorScheme.onPrimary else (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primary)),
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
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Section 1: Graphic Equalizer Card
                EqSection(
                    title = stringResource(R.string.section_equalizer),
                    textColor = sectionHeaderColor
                ) {
                    EqCard(
                        hasBlur = hasBlurBackground,
                        isDark = isDarkTheme,
                        hasSong = currentSong != null
                    ) {
                        val numBands = playbackManager.eqBandsCount
                        val bandRange = playbackManager.eqBandsRange
                        val friendlyNames = listOf(
                            stringResource(R.string.band_sub_bass),
                            stringResource(R.string.band_bass),
                            stringResource(R.string.band_mid),
                            stringResource(R.string.band_presence),
                            stringResource(R.string.band_brilliance)
                        )

                        LaunchedEffect(playbackManager.currentSong, playbackManager.isPlaying) {
                            playbackManager.refreshEqState()
                            if (playbackManager.isPlaying) {
                                while (playbackManager.eqBandsCount <= 0 || playbackManager.eqBandsRange == null) {
                                    delay(200)
                                    playbackManager.refreshEqState()
                                    if (playbackManager.eqBandsCount > 0 && playbackManager.eqBandsRange != null) break
                                }
                            }
                        }

                        // Equalizer Header Actions inside Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activePreset,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEnabled) textColor else textSecondaryColor
                                )
                                Text(
                                    text = if (isEnabled) stringResource(R.string.cd_activate_eq) else stringResource(R.string.option_repeat_off),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSecondaryColor
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCustom) {
                                    IconButton(
                                        onClick = { playbackManager.deleteCustomEqPreset(activePreset) },
                                        enabled = isEnabled,
                                        modifier = Modifier.size(36.dp).bounceClick()
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.cd_delete_preset),
                                            tint = if (isEnabled) MaterialTheme.colorScheme.error else textSecondaryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val saved = playbackManager.getSavedCustomPresets()
                                        var counter = 1
                                        val existingNames = saved.map { it.first }.toSet()
                                        while (existingNames.contains("Custom $counter")) {
                                            counter++
                                        }
                                        saveDialogName = "Custom $counter"
                                        showSaveDialog = true
                                    },
                                    enabled = isEnabled,
                                    modifier = Modifier.size(36.dp).bounceClick()
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(R.string.cd_save_preset),
                                        tint = if (isEnabled) iconTintPrimary else textSecondaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { playbackManager.resetEq() },
                                    enabled = isEnabled,
                                    modifier = Modifier.size(36.dp).bounceClick()
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.restore_defaults),
                                        tint = if (isEnabled) iconTintPrimary else textSecondaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (numBands > 0 && bandRange != null) {
                            val minLevel = bandRange[0]
                            val maxLevel = bandRange[1]

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (i in 0 until numBands) {
                                    val freq = playbackManager.getEqCenterFreq(i.toShort())
                                    val freqLabel = if (freq >= 1000000) "${freq / 1000000}k" else "${freq / 1000}"
                                    val displayLabel = friendlyNames.getOrElse(i) { freqLabel }
                                    val level = playbackManager.eqBandLevels.getOrElse(i) { 0.toShort() }
                                    val value = ((level - minLevel).toFloat() / (maxLevel - minLevel).toFloat()).coerceIn(0f, 1f)

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            displayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isEnabled) textColor else textSecondaryColor
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Box(
                                            modifier = Modifier.height(160.dp).width(56.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Slider(
                                                value = value,
                                                onValueChange = { newVal ->
                                                    val newLevel = (minLevel + newVal * (maxLevel - minLevel)).toInt().toShort()
                                                    playbackManager.setEqBandLevel(i.toShort(), newLevel)
                                                },
                                                enabled = isEnabled,
                                                colors = sliderColors,
                                                modifier = Modifier
                                                    .requiredWidth(160.dp)
                                                    .rotate(-90f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        val dbLabel = if (level > 0) "+${level / 100}" else "${level / 100}"
                                        Text(
                                            dbLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isEnabled) textColor else textSecondaryColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            val builtInPresets = playbackManager.getEqPresets()
                            val savedPresets = playbackManager.getSavedCustomPresets()
                            if (builtInPresets.isNotEmpty() || savedPresets.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(builtInPresets.size) { index ->
                                        val name = builtInPresets[index]
                                        val isActive = name == activePreset
                                        FilterChip(
                                            selected = isActive,
                                            onClick = { playbackManager.applyEqPreset(index.toShort()) },
                                            label = { Text(name) },
                                            enabled = isEnabled,
                                            border = null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (hasBlurBackground && currentSong != null) (if (useCustomControlsColor) activePrimary else Color.White.copy(alpha = 0.35f)) else (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primaryContainer),
                                                selectedLabelColor = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                                containerColor = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                labelColor = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.bounceClick()
                                        )
                                    }
                                    if (savedPresets.isNotEmpty()) {
                                        item {
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                    }
                                    items(savedPresets.size) { index ->
                                        val name = savedPresets[index].first
                                        val levels = savedPresets[index].second
                                        val isActive = name == activePreset
                                        FilterChip(
                                            selected = isActive,
                                            onClick = { playbackManager.applyCustomPreset(name, levels) },
                                            label = { Text(name) },
                                            enabled = isEnabled,
                                            border = null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (hasBlurBackground && currentSong != null) (if (useCustomControlsColor) activePrimary else Color.White.copy(alpha = 0.35f)) else (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primaryContainer),
                                                selectedLabelColor = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                                containerColor = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                labelColor = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.bounceClick()
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.height(140.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.eq_empty_hint),
                                    color = textSecondaryColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Audio Effects Card
                EqSection(
                    title = stringResource(R.string.section_audio_effects),
                    textColor = sectionHeaderColor
                ) {
                    EqCard(
                        hasBlur = hasBlurBackground,
                        isDark = isDarkTheme,
                        hasSong = currentSong != null
                    ) {
                        // Bass Boost
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (playbackManager.isBassBoostEnabled) iconTintPrimary.copy(alpha = 0.18f) else if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = if (playbackManager.isBassBoostEnabled) iconTintPrimary else textSecondaryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    stringResource(R.string.bass_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isEnabled) textColor else textSecondaryColor
                                )
                            }
                            BouncySwitch(
                                checked = playbackManager.isBassBoostEnabled,
                                onCheckedChange = { playbackManager.toggleBassBoost() },
                                enabled = isEnabled,
                                thumbContent = {
                                    Icon(
                                        imageVector = if (playbackManager.isBassBoostEnabled) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = dividerColor
                        )

                        // Spatial Audio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (playbackManager.isSpatialAudioEnabled) iconTintPrimary.copy(alpha = 0.18f) else if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SurroundSound,
                                            contentDescription = null,
                                            tint = if (playbackManager.isSpatialAudioEnabled) iconTintPrimary else textSecondaryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    stringResource(R.string.spatial_audio_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                            }
                            BouncySwitch(
                                checked = playbackManager.isSpatialAudioEnabled,
                                onCheckedChange = { playbackManager.toggleSpatialAudio() },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (playbackManager.isSpatialAudioEnabled) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = dividerColor
                        )

                        // Reverb Presets
                        val reverbNames = listOf("None", "Small Room", "Medium Room", "Large Room", "Medium Hall", "Large Hall", "Plate")
                        val reverbValues = listOf(0, 1, 2, 3, 4, 5, 6)
                        var currentReverb by remember { mutableStateOf(playbackManager.reverbPreset) }

                        LaunchedEffect(playbackManager.reverbPreset) {
                            currentReverb = playbackManager.reverbPreset
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.reverb_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (currentReverb > 0) iconTintPrimary.copy(alpha = 0.18f) else if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = reverbNames.getOrElse(reverbValues.indexOf(currentReverb)) { "None" },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentReverb > 0) iconTintPrimary else textSecondaryColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(reverbNames.size) { index ->
                                val isActive = currentReverb == reverbValues[index]
                                FilterChip(
                                    selected = isActive,
                                    onClick = {
                                        currentReverb = reverbValues[index]
                                        playbackManager.updateReverbPreset(reverbValues[index])
                                    },
                                    label = { Text(reverbNames[index]) },
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (hasBlurBackground && currentSong != null) (if (useCustomControlsColor) activePrimary else Color.White.copy(alpha = 0.35f)) else (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primaryContainer),
                                        selectedLabelColor = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        labelColor = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.bounceClick()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: Extra Volume Boost Card (Window trigger)
                EqSection(
                    title = stringResource(R.string.section_volume_gain),
                    textColor = sectionHeaderColor
                ) {
                    EqCard(
                        hasBlur = hasBlurBackground,
                        isDark = isDarkTheme,
                        hasSong = currentSong != null
                    ) {
                        val isBoostOn = playbackManager.isLoudnessEnabled
                        val currentGain = playbackManager.loudnessGain
                        val boostColor = when {
                            !isBoostOn -> textSecondaryColor
                            currentGain <= 1000 -> Color(0xFF4CAF50)
                            currentGain <= 2000 -> Color(0xFFFFA000)
                            else -> Color(0xFFE53935)
                        }

                        Surface(
                            onClick = { showVolumeBoostDialog = true },
                            color = Color.Transparent,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().bounceClick()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isBoostOn) boostColor.copy(alpha = 0.18f) else if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            tint = if (isBoostOn) boostColor else if (hasBlurBackground && currentSong != null) Color.White else (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.volume_boost_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.volume_boost_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textSecondaryColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isBoostOn) boostColor.copy(alpha = 0.2f) else if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (isBoostOn) "+${currentGain / 100} dB" else stringResource(R.string.option_repeat_off),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBoostOn) boostColor else textSecondaryColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = textSecondaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 4: Channel Balance & Pitch Card
                EqSection(
                    title = stringResource(R.string.section_balance_tuning),
                    textColor = sectionHeaderColor
                ) {
                    EqCard(
                        hasBlur = hasBlurBackground,
                        isDark = isDarkTheme,
                        hasSong = currentSong != null
                    ) {
                        // Channel Balance
                        var balanceValue by remember { mutableStateOf(playbackManager.balance) }

                        LaunchedEffect(playbackManager.balance) {
                            balanceValue = playbackManager.balance
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.balance_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (kotlin.math.abs(balanceValue - 0.5f) > 0.05f) iconTintPrimary.copy(alpha = 0.18f) else if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        when {
                                            balanceValue < 0.45f -> "L"
                                            balanceValue > 0.55f -> "R"
                                            else -> "C"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (kotlin.math.abs(balanceValue - 0.5f) > 0.05f) iconTintPrimary else textSecondaryColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (kotlin.math.abs(balanceValue - 0.5f) > 0.01f) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            balanceValue = 0.5f
                                            playbackManager.updateBalance(0.5f)
                                        },
                                        modifier = Modifier.size(32.dp).bounceClick()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = stringResource(R.string.balance_reset),
                                            tint = iconTintPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Slider(
                            value = balanceValue.coerceIn(0f, 1f),
                            onValueChange = {
                                balanceValue = it
                                playbackManager.updateBalance(it)
                            },
                            colors = sliderColors
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = dividerColor
                        )

                        // Pitch & 432Hz
                        var pitchValue by remember { mutableStateOf(playbackManager.playbackPitch) }

                        LaunchedEffect(playbackManager.playbackPitch) {
                            pitchValue = playbackManager.playbackPitch
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.pitch_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (pitchValue != 1.0f) iconTintPrimary.copy(alpha = 0.18f) else if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        "%.2fx".format(pitchValue),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pitchValue != 1.0f) iconTintPrimary else textSecondaryColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                SuggestionChip(
                                    onClick = {
                                        playbackManager.toggleTuning432()
                                    },
                                    label = { Text("432Hz") },
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (playbackManager.isTuning432) null else
                                        BorderStroke(1.dp, if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline),
                                    colors = if (playbackManager.isTuning432)
                                        SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (hasBlurBackground && currentSong != null) (if (useCustomControlsColor) activePrimary else Color.White.copy(alpha = 0.35f)) else (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primary),
                                            labelColor = Color.White
                                        ) else SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                                            labelColor = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface
                                        ),
                                    modifier = Modifier.bounceClick()
                                )

                                if (pitchValue != 1.0f) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            pitchValue = 1.0f
                                            playbackManager.updatePitch(1.0f)
                                        },
                                        modifier = Modifier.size(32.dp).bounceClick()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = stringResource(R.string.pitch_reset),
                                            tint = iconTintPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Slider(
                            value = pitchValue.coerceIn(0.5f, 2.0f),
                            onValueChange = {
                                pitchValue = it
                                playbackManager.updatePitch(pitchValue)
                            },
                            valueRange = 0.5f..2.0f,
                            colors = sliderColors
                        )
                    }
                }

                // Section 5: Dynamics Processing Card (Android P+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Spacer(modifier = Modifier.height(20.dp))

                    EqSection(
                        title = stringResource(R.string.section_dynamics_processing),
                        textColor = sectionHeaderColor
                    ) {
                        EqCard(
                            hasBlur = hasBlurBackground,
                            isDark = isDarkTheme,
                            hasSong = currentSong != null
                        ) {
                            val dynamicsNames = listOf("None", "Light", "Medium", "Strong", "Night")
                            val dynamicsValues = listOf(0, 1, 2, 3, 4)
                            var currentDynamics by remember { mutableStateOf(playbackManager.dynamicsPreset) }

                            LaunchedEffect(playbackManager.dynamicsPreset) {
                                currentDynamics = playbackManager.dynamicsPreset
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.dynamics_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (currentDynamics > 0) iconTintPrimary.copy(alpha = 0.18f) else if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = dynamicsNames.getOrElse(dynamicsValues.indexOf(currentDynamics)) { "None" },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentDynamics > 0) iconTintPrimary else textSecondaryColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(dynamicsNames.size) { index ->
                                    val isActive = currentDynamics == dynamicsValues[index]
                                    FilterChip(
                                        selected = isActive,
                                        onClick = {
                                            currentDynamics = dynamicsValues[index]
                                            playbackManager.updateDynamicsPreset(dynamicsValues[index])
                                        },
                                        label = { Text(dynamicsNames[index]) },
                                        border = null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (hasBlurBackground && currentSong != null) (if (useCustomControlsColor) activePrimary else Color.White.copy(alpha = 0.35f)) else (if (useCustomControlsColor) activePrimary else MaterialTheme.colorScheme.primaryContainer),
                                            selectedLabelColor = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                            containerColor = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            labelColor = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.bounceClick()
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun VolumeBoostDialog(
    initialEnabled: Boolean,
    initialGain: Int,
    currentSong: Song? = null,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Int) -> Unit
) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val blurColors = rememberBlurSheetColors(currentSong)

    var boostEnabled by remember { mutableStateOf(initialEnabled) }
    var boostGain by remember {
        mutableStateOf(if (initialGain > 0) initialGain else 1000)
    }

    val warningLevel = when {
        boostGain <= 1000 -> 1 // Safe: 0..10 dB (1000 mB)
        boostGain <= 2000 -> 2 // High: 11..20 dB (2000 mB)
        else -> 3              // Extreme: 21..30 dB (3000 mB)
    }

    val warningColor = when (warningLevel) {
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFFFFA000)
        else -> Color(0xFFE53935)
    }

    val warningIcon = when (warningLevel) {
        1 -> Icons.Default.CheckCircle
        2 -> Icons.Default.WarningAmber
        else -> Icons.Default.Dangerous
    }

    val gainDb = boostGain / 100

    val warningTitle = when (warningLevel) {
        1 -> stringResource(R.string.volume_boost_level_safe_title, gainDb.toString())
        2 -> stringResource(R.string.volume_boost_level_med_title, gainDb.toString())
        else -> stringResource(R.string.volume_boost_level_high_title, gainDb.toString())
    }

    val warningDesc = when (warningLevel) {
        1 -> stringResource(R.string.volume_boost_level_safe_desc)
        2 -> stringResource(R.string.volume_boost_level_med_desc)
        else -> stringResource(R.string.volume_boost_level_high_desc)
    }

    Dialog(
        onDismissRequest = {
            playbackManager.setLoudnessEnabled(initialEnabled, initialGain)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = currentSong,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(28.dp),
            isSheet = true
        ) {
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Surface(
                        shape = CircleShape,
                        color = if (boostEnabled) warningColor.copy(alpha = 0.18f) else if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (boostEnabled) warningIcon else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = if (boostEnabled) warningColor else blurColors.primaryTint,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.volume_boost_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = blurColors.textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.volume_boost_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = blurColors.textSecondaryColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Switch Row
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = blurColors.itemContainerColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (boostEnabled) Icons.Default.Bolt else Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (boostEnabled) warningColor else blurColors.textSecondaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.volume_boost_enable_switch),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = blurColors.textColor
                                )
                            }
                            BouncySwitch(
                                checked = boostEnabled,
                                onCheckedChange = { checked ->
                                    boostEnabled = checked
                                    playbackManager.setLoudnessEnabled(checked, boostGain)
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (boostEnabled) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Warning & Gain Control
                    if (!boostEnabled) {
                        // Off state: Warning of consequences
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = if (blurColors.hasBlur) 0.22f else 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.volume_boost_warning_off),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (blurColors.hasBlur && blurColors.isDark) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } else {
                        // On state: Dynamic warning level card
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = warningColor.copy(alpha = if (blurColors.hasBlur) 0.16f else 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = warningIcon,
                                        contentDescription = null,
                                        tint = warningColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = warningTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = warningColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = warningDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = blurColors.textColor.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Expressive Gain Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = warningColor.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "+$gainDb dB",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = warningColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${((boostGain.toFloat() / 3000f) * 100).toInt()}% extra)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = blurColors.textSecondaryColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick preset chips: +5, +10, +15, +20, +25, +30 dB
                        val boostPresets = listOf(500, 1000, 1500, 2000, 2500, 3000)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(boostPresets) { presetGain ->
                                val isSelected = boostGain == presetGain
                                val chipDb = presetGain / 100
                                val chipColor = when {
                                    presetGain <= 1000 -> Color(0xFF4CAF50)
                                    presetGain <= 2000 -> Color(0xFFFFA000)
                                    else -> Color(0xFFE53935)
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        boostGain = presetGain
                                        playbackManager.setLoudnessEnabled(true, presetGain)
                                    },
                                    label = {
                                        Text(
                                            text = "+${chipDb}dB",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = chipColor.copy(alpha = 0.35f),
                                        selectedLabelColor = if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onSurface,
                                        containerColor = blurColors.itemContainerColor,
                                        labelColor = blurColors.textSecondaryColor
                                    ),
                                    modifier = Modifier.bounceClick()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stepper + Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    val next = (boostGain - 100).coerceAtLeast(100)
                                    boostGain = next
                                    playbackManager.setLoudnessEnabled(true, next)
                                },
                                enabled = boostGain > 100,
                                modifier = Modifier.size(44.dp).bounceClick(),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }

                            Slider(
                                state = rememberSliderState(
                                    value = boostGain.toFloat(),
                                    steps = 28,
                                    trackRange = 100f..3000f
                                ),
                                onValueChange = { newVal ->
                                    val rounded = ((newVal / 100).roundToInt() * 100).coerceIn(100, 3000)
                                    boostGain = rounded
                                    playbackManager.setLoudnessEnabled(true, rounded)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = warningColor,
                                    activeTrackColor = warningColor,
                                    inactiveTrackColor = warningColor.copy(alpha = 0.25f)
                                )
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    val next = (boostGain + 100).coerceAtMost(3000)
                                    boostGain = next
                                    playbackManager.setLoudnessEnabled(true, next)
                                },
                                enabled = boostGain < 3000,
                                modifier = Modifier.size(44.dp).bounceClick(),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dialog Actions (Cancel / Save)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                playbackManager.setLoudnessEnabled(initialEnabled, initialGain)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.bounceClick()
                        ) {
                            Text(stringResource(R.string.cancel), color = blurColors.textSecondaryColor)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val confirmBtnColor = if (boostEnabled) warningColor else (if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary)
                        val confirmTextColor = if (blurColors.hasBlur && blurColors.isDark) Color.Black else Color.White

                        Button(
                            onClick = {
                                playbackManager.setLoudnessEnabled(boostEnabled, boostGain)
                                onConfirm(boostEnabled, boostGain)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = confirmBtnColor,
                                contentColor = confirmTextColor
                            ),
                            modifier = Modifier.bounceClick()
                        ) {
                            Text(
                                text = stringResource(R.string.save),
                                fontWeight = FontWeight.SemiBold,
                                color = confirmTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}
