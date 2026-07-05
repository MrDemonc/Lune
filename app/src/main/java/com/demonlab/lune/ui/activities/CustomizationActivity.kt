package com.demonlab.lune.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import android.content.Intent
import androidx.compose.material3.*
import com.demonlab.lune.ui.components.BouncySwitch
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
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.AutoAwesome
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.theme.LuneTheme
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Gesture

class CustomizationActivity : ComponentActivity() {
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
            var isHapticVibrationEnabled by remember { mutableStateOf(settingsManager.isHapticVibrationEnabled) }
            var isSongInfoEnabled by remember { mutableStateOf(settingsManager.isSongInfoEnabled) }
            var isCinematicEnabled by remember { mutableStateOf(settingsManager.isCinematicPlayerEnabled) }
            LuneTheme(
                darkTheme = targetDarkTheme,
                useCustomColors = useCustomColors,
                customColorPalette = customColorPalette,
                useAmoledPitchBlack = useAmoledPitchBlack
            ) {
                CustomizationScreen(
                    onBack = { finish() },
                    settingsManager = settingsManager,
                    useCustomColors = useCustomColors,
                    customColorPalette = customColorPalette,
                    useAmoledPitchBlack = useAmoledPitchBlack,
                    isHapticVibrationEnabled = isHapticVibrationEnabled,
                    isSongInfoEnabled = isSongInfoEnabled,
                    isCinematicEnabled = isCinematicEnabled,
                    onCustomColorsChanged = {
                        useCustomColors = it
                        settingsManager.useCustomColors = it
                    },
                    onPaletteChanged = {
                        customColorPalette = it
                        settingsManager.customColorPalette = it
                    },
                    onAmoledChanged = {
                        useAmoledPitchBlack = it
                        settingsManager.useAmoledPitchBlack = it
                    },
                    onHapticChanged = {
                        isHapticVibrationEnabled = it
                        settingsManager.isHapticVibrationEnabled = it
                    },
                    onSongInfoChanged = {
                        isSongInfoEnabled = it
                        settingsManager.isSongInfoEnabled = it
                    },
                    onCinematicChanged = {
                        isCinematicEnabled = it
                        settingsManager.isCinematicPlayerEnabled = it
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    onBack: () -> Unit,
    settingsManager: SettingsManager,
    useCustomColors: Boolean,
    customColorPalette: Int,
    useAmoledPitchBlack: Boolean,
    isHapticVibrationEnabled: Boolean,
    isSongInfoEnabled: Boolean,
    isCinematicEnabled: Boolean,
    onCustomColorsChanged: (Boolean) -> Unit,
    onPaletteChanged: (Int) -> Unit,
    onAmoledChanged: (Boolean) -> Unit,
    onHapticChanged: (Boolean) -> Unit,
    onSongInfoChanged: (Boolean) -> Unit,
    onCinematicChanged: (Boolean) -> Unit
) {
    var showCustomTitleDialog by remember { mutableStateOf(false) }
    var customTitle by remember { mutableStateOf(settingsManager.customTitle) }
    var showBitrateSheet by remember { mutableStateOf(false) }
    var isSectionCustomizationEnabled by remember { mutableStateOf(settingsManager.isSectionCustomizationEnabled) }
    var hiddenSectionTabs by remember { mutableStateOf(settingsManager.hiddenSectionTabs) }
    var isCrossfadeCustomDuration by remember { mutableStateOf(settingsManager.isCrossfadeCustomDuration) }
    var crossfadeDurationSeconds by remember { mutableStateOf(settingsManager.crossfadeDurationSeconds) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (showCustomTitleDialog) {
        var tempTitle by remember { mutableStateOf(customTitle) }
        AlertDialog(
            onDismissRequest = { showCustomTitleDialog = false },
            title = { Text(stringResource(R.string.custom_title)) },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("Titulo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { tempTitle = "" }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.restore_default_title))
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    customTitle = tempTitle
                    settingsManager.customTitle = tempTitle
                    showCustomTitleDialog = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTitleDialog = false }) {
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
                        stringResource(R.string.customization),
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
            SettingsSection(title = stringResource(R.string.general)) {
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.custom_title),
                    supportingText = if (customTitle.isEmpty()) "Lune" else customTitle,
                    icon = Icons.Default.Edit,
                    position = SectionPosition.FIRST,
                    onClick = { showCustomTitleDialog = true }
                )

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.section_customization),
                    supportingText = stringResource(R.string.section_customization_desc),
                    icon = Icons.Default.ViewAgenda,
                    position = SectionPosition.MIDDLE,
                    trailingContent = {
                        BouncySwitch(
                            checked = isSectionCustomizationEnabled,
                            onCheckedChange = { enabled ->
                                isSectionCustomizationEnabled = enabled
                                settingsManager.isSectionCustomizationEnabled = enabled
                                if (!enabled) {
                                    hiddenSectionTabs = emptySet()
                                    settingsManager.hiddenSectionTabs = emptySet()
                                }
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isSectionCustomizationEnabled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                if (isSectionCustomizationEnabled) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val isAlbumView = settingsManager.albumBrowseMode
                            val sectionItems = listOf(
                                "ALL" to Icons.Default.LibraryMusic,
                                "FAVORITES" to Icons.Default.Favorite,
                                "PLAYLISTS" to Icons.AutoMirrored.Filled.QueueMusic,
                                "ALBUMS" to if (isAlbumView) Icons.Default.Album else Icons.Default.Person,
                                "FOLDERS" to Icons.Default.Folder
                            )

                            sectionItems.forEach { (key, icon) ->
                                val isActive = key !in hiddenSectionTabs
                                Surface(
                                    onClick = {
                                        val newHidden = hiddenSectionTabs.toMutableSet()
                                        if (isActive) newHidden.add(key) else newHidden.remove(key)
                                        hiddenSectionTabs = newHidden
                                        settingsManager.hiddenSectionTabs = newHidden
                                    },
                                    shape = CircleShape,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.use_custom_colors),
                    supportingText = stringResource(R.string.use_custom_colors_desc),
                    icon = Icons.Default.Palette,
                    position = SectionPosition.MIDDLE,
                    trailingContent = {
                        BouncySwitch(
                            checked = useCustomColors,
                            onCheckedChange = onCustomColorsChanged,
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

                if (useCustomColors) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.color_palette),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

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
                                    5 to Color(0xFF7F5700)
                                )
                                palettes.forEach { (index, color) ->
                                    val isSelected = customColorPalette == index
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .clickable { onPaletteChanged(index) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = color,
                                            modifier = Modifier.fillMaxSize(),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                                3.dp,
                                                MaterialTheme.colorScheme.onSurface
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
                                                        modifier = Modifier.size(18.dp)
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

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.amoled_pitch_black),
                    supportingText = stringResource(R.string.amoled_pitch_black_desc),
                    icon = Icons.Default.PhoneAndroid,
                    position = SectionPosition.LAST,
                    trailingContent = {
                        BouncySwitch(
                            checked = useAmoledPitchBlack,
                            onCheckedChange = onAmoledChanged,
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
            val context = LocalContext.current
            SettingsSection(title = stringResource(R.string.media_player)) {
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.haptic_vibration),
                    supportingText = stringResource(R.string.haptic_vibration_desc),
                    icon = androidx.compose.material.icons.Icons.Default.PhoneAndroid,
                    position = SectionPosition.FIRST,
                    trailingContent = {
                        BouncySwitch(
                            checked = isHapticVibrationEnabled,
                            onCheckedChange = onHapticChanged,
                            thumbContent = {
                                Icon(
                                    imageVector = if (isHapticVibrationEnabled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                // Song Info: tap to open bitrate location dialog
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showBitrateSheet = true },
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.song_info),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                stringResource(R.string.song_info_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.cinematic_player),
                    supportingText = stringResource(R.string.cinematic_player_desc),
                    icon = androidx.compose.material.icons.Icons.Default.AutoAwesome,
                    position = SectionPosition.MIDDLE,
                    trailingContent = {
                        BouncySwitch(
                            checked = isCinematicEnabled,
                            onCheckedChange = onCinematicChanged,
                            thumbContent = {
                                Icon(
                                    imageVector = if (isCinematicEnabled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.cover_player),
                    supportingText = stringResource(R.string.cover_player_desc),
                    icon = Icons.Default.Album,
                    position = SectionPosition.MIDDLE,
                    onClick = {
                        context.startActivity(Intent(context, CoverCustomizationActivity::class.java))
                    }
                )
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.controls_player),
                    supportingText = stringResource(R.string.controls_player_desc),
                    icon = Icons.Default.PlayArrow,
                    position = SectionPosition.LAST,
                    onClick = {
                        context.startActivity(Intent(context, ControlsCustomizationActivity::class.java))
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSection(title = stringResource(R.string.other)) {
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.gesture),
                    supportingText = stringResource(R.string.gesture_desc),
                    icon = Icons.Default.Gesture,
                    position = SectionPosition.FIRST,
                    onClick = {
                        context.startActivity(Intent(context, GestureCustomizationActivity::class.java))
                    }
                )
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.blur),
                    supportingText = stringResource(R.string.blur_desc),
                    icon = Icons.Default.BlurOn,
                    position = SectionPosition.MIDDLE,
                    onClick = {
                        context.startActivity(Intent(context, BlurCustomizationActivity::class.java))
                    }
                )
                val crossfadeSwitchPosition = if (isCrossfadeCustomDuration) SectionPosition.MIDDLE else SectionPosition.LAST
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.crossfade_time),
                    supportingText = stringResource(R.string.crossfade_time_desc),
                    icon = Icons.Default.Tune,
                    position = crossfadeSwitchPosition,
                    trailingContent = {
                        BouncySwitch(
                            checked = isCrossfadeCustomDuration,
                            onCheckedChange = {
                                isCrossfadeCustomDuration = it
                                settingsManager.isCrossfadeCustomDuration = it
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isCrossfadeCustomDuration) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )
                AnimatedVisibility(
                    visible = isCrossfadeCustomDuration,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "${crossfadeDurationSeconds}s",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = crossfadeDurationSeconds.toFloat(),
                                onValueChange = {
                                    crossfadeDurationSeconds = it.toInt()
                                    settingsManager.crossfadeDurationSeconds = it.toInt()
                                },
                                valueRange = 1f..12f,
                                steps = 10,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("1s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("12s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBitrateSheet) {
        var tempOnList by remember { mutableStateOf(settingsManager.isBitrateOnList) }
        var tempOnPlayer by remember { mutableStateOf(settingsManager.isBitrateOnPlayer) }
        AlertDialog(
            onDismissRequest = { showBitrateSheet = false },
            title = { Text(stringResource(R.string.song_info), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempOnList = !tempOnList }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = tempOnList, onCheckedChange = { tempOnList = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.show_in_song_list))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempOnPlayer = !tempOnPlayer }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = tempOnPlayer, onCheckedChange = { tempOnPlayer = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.show_in_full_player))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsManager.isBitrateOnList = tempOnList
                        settingsManager.isBitrateOnPlayer = tempOnPlayer
                        showBitrateSheet = false
                    },
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showBitrateSheet = false },
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
