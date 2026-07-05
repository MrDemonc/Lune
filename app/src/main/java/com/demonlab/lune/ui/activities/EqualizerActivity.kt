package com.demonlab.lune.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import com.demonlab.lune.ui.components.BouncySwitch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.theme.LuneTheme
import kotlinx.coroutines.delay

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveDialogName by remember { mutableStateOf("") }
    val isEnabled = playbackManager.isEqEnabled
    val activePreset = playbackManager.activeEqPresetName
    val isCustom = playbackManager.isCustomPreset(activePreset)

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.eq_title)) },
            text = {
                OutlinedTextField(
                    value = saveDialogName,
                    onValueChange = { saveDialogName = it },
                    label = { Text(stringResource(R.string.preset_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (saveDialogName.isNotBlank()) {
                            playbackManager.saveCustomEqPreset(saveDialogName.trim())
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "EQ",
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
                actions = {
                    if (isCustom) {
                        IconButton(
                            onClick = { playbackManager.deleteCustomEqPreset(activePreset) },
                            enabled = isEnabled
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cd_delete_preset),
                                        tint = if (isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
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
                        enabled = isEnabled
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.cd_save_preset),
                                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { playbackManager.resetEq() },
                        enabled = isEnabled
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.restore_defaults),
                                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { playbackManager.toggleEq() }) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PowerSettingsNew,
                                    contentDescription = stringResource(R.string.cd_activate_eq),
                                    tint = if (playbackManager.isEqEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

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
                                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(modifier = Modifier.height(170.dp).width(60.dp), contentAlignment = Alignment.Center) {
                                Slider(
                                    value = value,
                                    onValueChange = { newVal ->
                                        val newLevel = (minLevel + newVal * (maxLevel - minLevel)).toInt().toShort()
                                        playbackManager.setEqBandLevel(i.toShort(), newLevel)
                                    },
                                    enabled = isEnabled,
                                    modifier = Modifier
                                        .requiredWidth(170.dp)
                                        .rotate(-90f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            val dbLabel = if (level > 0) "+${level / 100}" else "${level / 100}"
                            Text(
                                dbLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                                border = null
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
                                border = null
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.height(170.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.eq_empty_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.bass_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.spatial_audio_label), style = MaterialTheme.typography.titleMedium)
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

            Spacer(modifier = Modifier.height(16.dp))

            var loudnessGainValue by remember { mutableStateOf(playbackManager.loudnessGain) }

            LaunchedEffect(playbackManager.loudnessGain) {
                loudnessGainValue = playbackManager.loudnessGain
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.loudness_label),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${loudnessGainValue / 100}dB",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(48.dp)
                    )
                    BouncySwitch(
                        checked = playbackManager.isLoudnessEnabled,
                        onCheckedChange = { playbackManager.toggleLoudness() },
                        thumbContent = {
                            Icon(
                                imageVector = if (playbackManager.isLoudnessEnabled) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    )
                }
            }
            if (playbackManager.isLoudnessEnabled) {
                Slider(
                    value = loudnessGainValue.toFloat().coerceIn(0f, 3000f),
                    onValueChange = {
                        loudnessGainValue = it.toInt()
                        playbackManager.updateLoudnessGain(it.toInt())
                    },
                    valueRange = 0f..3000f,
                    steps = 29
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val reverbNames = listOf("None", "Small Room", "Medium Room", "Large Room", "Medium Hall", "Large Hall", "Plate")
            val reverbValues = listOf(0, 1, 2, 3, 4, 5, 6)
            var currentReverb by remember { mutableStateOf(playbackManager.reverbPreset) }

            LaunchedEffect(playbackManager.reverbPreset) {
                currentReverb = playbackManager.reverbPreset
            }

            Text(
                stringResource(R.string.reverb_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            balanceValue < 0.45f -> "L"
                            balanceValue > 0.55f -> "R"
                            else -> "C"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(24.dp)
                    )
                    if (kotlin.math.abs(balanceValue - 0.5f) > 0.01f) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                balanceValue = 0.5f
                                playbackManager.updateBalance(0.5f)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.balance_reset),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
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
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val dynamicsNames = listOf("None", "Light", "Medium", "Strong", "Night")
            val dynamicsValues = listOf(0, 1, 2, 3, 4)
            var currentDynamics by remember { mutableStateOf(playbackManager.dynamicsPreset) }

            LaunchedEffect(playbackManager.dynamicsPreset) {
                currentDynamics = playbackManager.dynamicsPreset
            }

            Text(
                stringResource(R.string.dynamics_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "%.2fx".format(pitchValue),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(52.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SuggestionChip(
                        onClick = {
                            playbackManager.toggleTuning432()
                        },
                        label = { Text("432Hz") },
                        shape = RoundedCornerShape(16.dp),
                        border = if (playbackManager.isTuning432) null else
                            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = if (playbackManager.isTuning432)
                            SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                labelColor = MaterialTheme.colorScheme.onPrimary
                            ) else SuggestionChipDefaults.suggestionChipColors()
                    )
                    if (pitchValue != 1.0f) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                pitchValue = 1.0f
                                playbackManager.updatePitch(1.0f)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.pitch_reset),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
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
                valueRange = 0.5f..2.0f
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
