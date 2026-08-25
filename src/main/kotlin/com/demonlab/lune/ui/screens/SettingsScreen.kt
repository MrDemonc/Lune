package com.demonlab.lune.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.demonlab.lune.tools.MusicProvider
import com.demonlab.lune.tools.SettingsManager
import kotlinx.coroutines.launch
import javax.swing.JFileChooser
import com.demonlab.lune.ui.components.SettingSegmentedChip

import com.demonlab.lune.data.AppLanguage
import com.demonlab.lune.data.L10n

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onRescanLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var scanResultText by remember { mutableStateOf<String?>(null) }
    var customTitleText by remember { mutableStateOf(settingsManager.customTitle) }

    val paletteNames = listOf("Default Purple", "Sunset Peach", "Sage Green", "Ocean Breeze", "Lavender Mist", "Warm Amber")
    val paletteColors = listOf(
        Color(0xFF6650A4),
        Color(0xFFB04B38),
        Color(0xFF386B52),
        Color(0xFF2E6580),
        Color(0xFF6E568F),
        Color(0xFF7F5700)
    )

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = L10n.settingsTitle,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Language Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = L10n.language,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = L10n.selectLanguage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AppLanguage.entries) { lang ->
                            val isSelected = settingsManager.language.equals(lang.code, ignoreCase = true) ||
                                (settingsManager.language.isBlank() && lang == AppLanguage.SYSTEM)
                            SettingSegmentedChip(
                                selected = isSelected,
                                label = if (lang == AppLanguage.SYSTEM) L10n.get("theme_auto") else "${lang.nativeName} (${lang.displayName})",
                                onClick = {
                                    settingsManager.language = lang.code
                                    settingsManager.saveSettings()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Appearance Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = L10n.appearance,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Theme Mode Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.theme, color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "auto" to L10n.themeAuto,
                                "dark" to L10n.themeDark,
                                "light" to L10n.themeLight
                            ).forEach { (mode, label) ->
                                SettingSegmentedChip(
                                    selected = settingsManager.themeMode == mode,
                                    label = label,
                                    onClick = {
                                        settingsManager.themeMode = mode
                                        settingsManager.saveSettings()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Color Palettes
                    Text(L10n.colorPalette, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(paletteNames.indices.toList()) { index ->
                            val isSelected = settingsManager.customColorPalette == index
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        settingsManager.customColorPalette = index
                                        settingsManager.useCustomColors = index != 0
                                        settingsManager.saveSettings()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(paletteColors[index])
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = paletteNames[index],
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // AMOLED Pitch Black
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(L10n.amoledPitchBlack, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                L10n.amoledPitchBlackDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settingsManager.useAmoledPitchBlack,
                            onCheckedChange = {
                                settingsManager.useAmoledPitchBlack = it
                                settingsManager.saveSettings()
                            }
                        )
                    }
                }
            }
        }

        // Custom Title Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = L10n.customTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customTitleText,
                            onValueChange = { customTitleText = it },
                            label = { Text(L10n.appTitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                settingsManager.customTitle = customTitleText.ifBlank { "Lune" }
                                settingsManager.saveSettings()
                            }
                        ) {
                            Text(L10n.save)
                        }
                    }
                }
            }
        }

        // Cover Customizer Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = L10n.coverCustomization,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Cover Shape
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.coverShape, color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                L10n.shapeRounded to 0,
                                L10n.shapeSquare to 1,
                                L10n.shapeCircular to 2
                            ).forEach { (label, value) ->
                                SettingSegmentedChip(
                                    selected = settingsManager.coverShape == value,
                                    label = label,
                                    onClick = {
                                        settingsManager.coverShape = value
                                        settingsManager.saveSettings()
                                    }
                                )
                            }
                        }
                    }

                    if (settingsManager.coverShape == 2) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(L10n.vinylTexture, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = settingsManager.coverVinyl,
                                onCheckedChange = {
                                    settingsManager.coverVinyl = it
                                    settingsManager.saveSettings()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(L10n.spinPlaying, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = settingsManager.coverSpin,
                                onCheckedChange = {
                                    settingsManager.coverSpin = it
                                    settingsManager.saveSettings()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Visualizer toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.audioVisualizer, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = settingsManager.isVisualizerEnabled,
                            onCheckedChange = {
                                settingsManager.isVisualizerEnabled = it
                                settingsManager.saveSettings()
                            }
                        )
                    }
                }
            }
        }

        // Library & Music Folders Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = L10n.musicDirectories,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    settingsManager.musicDirectories.forEach { dir ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dir, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }

                            if (settingsManager.musicDirectories.size > 1) {
                                IconButton(
                                    onClick = {
                                        val mutable = settingsManager.musicDirectories.toMutableList()
                                        mutable.remove(dir)
                                        settingsManager.musicDirectories = mutable
                                        settingsManager.saveSettings()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove folder", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val chooser = JFileChooser().apply {
                                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                    dialogTitle = L10n.addFolder
                                }
                                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    val selected = chooser.selectedFile.absolutePath
                                    if (!settingsManager.musicDirectories.contains(selected)) {
                                        val mutable = settingsManager.musicDirectories.toMutableList()
                                        mutable.add(selected)
                                        settingsManager.musicDirectories = mutable
                                        settingsManager.saveSettings()
                                        onRescanLibrary()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(L10n.addFolder)
                        }

                        FilledTonalButton(
                            onClick = {
                                isScanning = true
                                scope.launch {
                                    val songs = MusicProvider.instance.scanLibrary(settingsManager.musicDirectories)
                                    onRescanLibrary()
                                    isScanning = false
                                    scanResultText = String.format(L10n.songsCount, songs.size)
                                }
                            }
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isScanning) "..." else L10n.rescanLibrary)
                        }
                    }

                    scanResultText?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
