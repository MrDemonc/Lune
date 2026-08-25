package com.demonlab.lune.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demonlab.lune.data.L10n
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerDialog(
    settingsManager: SettingsManager,
    onDismiss: () -> Unit
) {
    val presets = listOf("Flat", "Rock", "Pop", "Jazz", "Electronic", "Classical", "Bass Boost", "Vocal")
    val bandFrequencies = listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(L10n.equalizerEffects, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Switch(
                    checked = settingsManager.isEqEnabled,
                    onCheckedChange = {
                        settingsManager.isEqEnabled = it
                        settingsManager.saveSettings()
                    }
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Preset Selector
                Text(L10n.presets, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { preset ->
                        val isSelected = settingsManager.eqPreset == preset
                        com.demonlab.lune.ui.components.SettingSegmentedChip(
                            selected = isSelected,
                            label = preset,
                            onClick = {
                                settingsManager.eqPreset = preset
                                applyEqPreset(preset, settingsManager)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 10-Band EQ Sliders
                Text(L10n.tenBandEq, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val bands = settingsManager.eqBands.toMutableList()
                while (bands.size < 10) bands.add(0f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    bands.forEachIndexed { index, gain ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text(
                                text = "${gain.toInt()}dB",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Slider(
                                value = gain,
                                onValueChange = { newVal ->
                                    bands[index] = newVal
                                    settingsManager.eqBands = bands.toList()
                                    settingsManager.eqPreset = "Custom"
                                    settingsManager.saveSettings()
                                },
                                valueRange = -12f..12f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp),
                                enabled = settingsManager.isEqEnabled
                            )

                            Text(
                                text = bandFrequencies.getOrElse(index) { "${index + 1}" },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Audio Effects Grid
                Text(L10n.equalizerEffects, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Bass Boost
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(L10n.extraBass, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settingsManager.isBassBoostEnabled,
                        onCheckedChange = {
                            settingsManager.isBassBoostEnabled = it
                            settingsManager.saveSettings()
                        }
                    )
                }

                // Spatial Audio
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(L10n.spatialAudio, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settingsManager.isSpatialAudioEnabled,
                        onCheckedChange = {
                            settingsManager.isSpatialAudioEnabled = it
                            settingsManager.saveSettings()
                        }
                    )
                }

                // Loudness Enhancer
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(L10n.loudness, style = MaterialTheme.typography.bodyMedium)
                        Text("${settingsManager.loudnessGain} dB", style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = settingsManager.loudnessGain.toFloat(),
                        onValueChange = {
                            settingsManager.loudnessGain = it.toInt()
                            settingsManager.saveSettings()
                        },
                        valueRange = 0f..30f
                    )
                }

                // Balance Pan (L - C - R)
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.balance, style = MaterialTheme.typography.bodyMedium)
                        val balLabel = when {
                            settingsManager.balance < 0.45f -> "L (${((0.5f - settingsManager.balance) * 200).toInt()}%)"
                            settingsManager.balance > 0.55f -> "R (${((settingsManager.balance - 0.5f) * 200).toInt()}%)"
                            else -> "Center"
                        }
                        TextButton(
                            onClick = {
                                PlaybackManager.instance.setBalance(0.5f)
                            }
                        ) {
                            Text(balLabel)
                        }
                    }
                    Slider(
                        value = settingsManager.balance,
                        onValueChange = {
                            PlaybackManager.instance.setBalance(it)
                        },
                        valueRange = 0.0f..1.0f
                    )
                }

                // Playback Speed & Pitch
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.speedPitch, style = MaterialTheme.typography.bodyMedium)
                        TextButton(
                            onClick = {
                                settingsManager.playbackSpeed = 1.0f
                                settingsManager.saveSettings()
                            }
                        ) {
                            Text("${String.format("%.2f", settingsManager.playbackSpeed)}x (${L10n.reset})")
                        }
                    }
                    Slider(
                        value = settingsManager.playbackSpeed,
                        onValueChange = {
                            settingsManager.playbackSpeed = it
                            settingsManager.saveSettings()
                        },
                        valueRange = 0.5f..2.0f
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.close)
            }
        }
    )
}

private fun applyEqPreset(preset: String, settingsManager: SettingsManager) {
    val gains = when (preset) {
        "Rock" -> listOf(4f, 3f, 1f, 0f, -1f, 1f, 2f, 3f, 4f, 5f)
        "Pop" -> listOf(-1f, 1f, 3f, 4f, 3f, 1f, -1f, -1f, 1f, 2f)
        "Jazz" -> listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f)
        "Electronic" -> listOf(5f, 4f, 2f, 0f, -2f, 2f, 1f, 2f, 4f, 5f)
        "Classical" -> listOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 3f)
        "Bass Boost" -> listOf(7f, 6f, 5f, 3f, 1f, 0f, 0f, 0f, 0f, 0f)
        "Vocal" -> listOf(-2f, -1f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, -1f)
        else -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    }
    settingsManager.eqBands = gains
    settingsManager.saveSettings()
}
