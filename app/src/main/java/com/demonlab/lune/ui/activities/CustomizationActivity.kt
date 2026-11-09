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
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Widgets

import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.AppIconManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.components.rememberBlurSheetColors
import com.demonlab.lune.ui.utils.bounceClick
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween

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
@Suppress("DEPRECATION")
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
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))

    var showAppIconDialog by remember { mutableStateOf(false) }
    var appIconKey by remember { mutableStateOf(AppIconManager.getCurrentIcon(context)) }
    var showCustomTitleDialog by remember { mutableStateOf(false) }
    var customTitle by remember { mutableStateOf(settingsManager.customTitle) }
    var showBitrateSheet by remember { mutableStateOf(false) }
    var isSectionCustomizationEnabled by remember { mutableStateOf(settingsManager.isSectionCustomizationEnabled) }
    var hiddenSectionTabs by remember { mutableStateOf(settingsManager.hiddenSectionTabs) }
    var isCrossfadeCustomDuration by remember { mutableStateOf(settingsManager.isCrossfadeCustomDuration) }
    var crossfadeDurationSeconds by remember { mutableStateOf(settingsManager.crossfadeDurationSeconds) }
    var seamlessLooping by remember { mutableStateOf(settingsManager.seamlessLooping) }
    var isScrollToTopEnabled by remember { mutableStateOf(settingsManager.isScrollToTopEnabled) }
    var useQuickPlayer by remember { mutableStateOf(settingsManager.useQuickPlayer) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (showCustomTitleDialog) {
        var tempTitle by remember { mutableStateOf(customTitle) }
        AlertDialog(
            onDismissRequest = { showCustomTitleDialog = false },
            containerColor = if (hasBlurBackground) (if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.95f) else Color(0xFFF5F5F5).copy(alpha = 0.95f)) else MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.custom_title),
                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("Titulo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (hasBlurBackground) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                        focusedLabelColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    trailingIcon = {
                        IconButton(onClick = { tempTitle = "" }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.restore_default_title),
                                tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                            )
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
                    Text(
                        stringResource(R.string.ok),
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTitleDialog = false }) {
                    Text(
                        stringResource(R.string.cancel),
                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showAppIconDialog) {
        AppIconDialog(
            currentIcon = appIconKey,
            currentSong = currentSong,
            onDismiss = { showAppIconDialog = false },
            onSelectIcon = { newIcon ->
                appIconKey = newIcon
                AppIconManager.applyAppIcon(context, newIcon)
                showAppIconDialog = false
                Toast.makeText(context, context.getString(R.string.app_icon_applied), Toast.LENGTH_SHORT).show()
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
                            stringResource(R.string.customization),
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
            SettingsSection(title = stringResource(R.string.general)) {
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.app_icon_title),
                    supportingText = when (appIconKey) {
                        AppIconManager.ICON_DYNAMIC -> stringResource(R.string.app_icon_dynamic)
                        AppIconManager.ICON_MONO_WHITE -> stringResource(R.string.app_icon_mono_white)
                        AppIconManager.ICON_MONO_BLACK -> stringResource(R.string.app_icon_mono_black)
                        else -> stringResource(R.string.app_icon_default)
                    },
                    icon = Icons.Default.Apps,
                    position = SectionPosition.FIRST,
                    onClick = { showAppIconDialog = true }
                )

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.custom_title),
                    supportingText = if (customTitle.isEmpty()) "Lune" else customTitle,
                    icon = Icons.Default.Edit,
                    position = SectionPosition.MIDDLE,
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
                        color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val sectionItems = listOf(
                                "ALL" to Icons.Default.LibraryMusic,
                                "FAVORITES" to Icons.Default.Favorite,
                                "PLAYLISTS" to Icons.AutoMirrored.Filled.QueueMusic,
                                "ALBUMS" to Icons.Default.Album,
                                "ARTISTS" to Icons.Default.Person,
                                "GENRES" to Icons.Default.Category,
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
                                    color = if (isActive) {
                                        if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                    } else {
                                        if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .bounceClick()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (isActive) {
                                                if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSecondaryContainer
                                            },
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
                        color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.color_palette),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
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
                                            .clickable { onPaletteChanged(index) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = color,
                                            modifier = Modifier.fillMaxSize(),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                                3.dp,
                                                if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                                            ) else if (index == 6) androidx.compose.foundation.BorderStroke(
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

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.amoled_pitch_black),
                    supportingText = stringResource(R.string.amoled_pitch_black_desc),
                    icon = Icons.Default.PhoneAndroid,
                    position = SectionPosition.MIDDLE,
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

                var showAiSection by remember { mutableStateOf(settingsManager.showAiSection) }
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.show_ai_section),
                    supportingText = stringResource(R.string.show_ai_section_desc),
                    icon = Icons.Default.AutoAwesome,
                    position = SectionPosition.LAST,
                    trailingContent = {
                        BouncySwitch(
                            checked = showAiSection,
                            onCheckedChange = { enabled ->
                                showAiSection = enabled
                                settingsManager.showAiSection = enabled
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (showAiSection) Icons.Default.Check else Icons.Default.Close,
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
                    color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.song_info),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(R.string.song_info_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
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
                    headlineText = stringResource(R.string.quick_player),
                    supportingText = stringResource(R.string.use_quick_player_desc),
                    icon = Icons.Default.Speed,
                    position = SectionPosition.MIDDLE,
                    trailingContent = {
                        BouncySwitch(
                            checked = useQuickPlayer,
                            onCheckedChange = {
                                useQuickPlayer = it
                                settingsManager.useQuickPlayer = it
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (useQuickPlayer) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.player_customization),
                    supportingText = stringResource(R.string.player_customization_desc),
                    icon = Icons.Default.Tune,
                    position = SectionPosition.LAST,
                    onClick = {
                        context.startActivity(Intent(context, PlayerCustomizationActivity::class.java))
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSection(title = stringResource(R.string.other)) {
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.widget_customization),
                    supportingText = stringResource(R.string.widget_customization_desc),
                    icon = Icons.Default.Widgets,
                    position = SectionPosition.FIRST,
                    onClick = {
                        context.startActivity(Intent(context, WidgetCustomizationActivity::class.java))
                    }
                )
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.gesture),
                    supportingText = stringResource(R.string.gesture_desc),
                    icon = Icons.Default.Gesture,
                    position = SectionPosition.MIDDLE,
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
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.font_customization),
                    supportingText = stringResource(R.string.font_customization_desc),
                    icon = Icons.Default.TextFields,
                    position = SectionPosition.MIDDLE,
                    onClick = {
                        context.startActivity(Intent(context, FontCustomizationActivity::class.java))
                    }
                )
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.crossfade_time),
                    supportingText = stringResource(R.string.crossfade_time_desc),
                    icon = Icons.Default.Tune,
                    position = SectionPosition.MIDDLE,
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
                        shape = RoundedCornerShape(4.dp),
                        color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
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
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = crossfadeDurationSeconds.toFloat(),
                                onValueChange = {
                                    crossfadeDurationSeconds = it.toInt()
                                    settingsManager.crossfadeDurationSeconds = it.toInt()
                                },
                                valueRange = 1f..12f,
                                steps = 10,
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
                                Text("1s", style = MaterialTheme.typography.bodySmall, color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("12s", style = MaterialTheme.typography.bodySmall, color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
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
                                com.demonlab.lune.tools.PlaybackManager.getInstance(context).updateLoopingState()
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
                    headlineText = stringResource(R.string.scroll_to_top_button),
                    supportingText = stringResource(R.string.scroll_to_top_button_desc),
                    icon = Icons.Default.KeyboardArrowUp,
                    position = SectionPosition.LAST,
                    trailingContent = {
                        BouncySwitch(
                            checked = isScrollToTopEnabled,
                            onCheckedChange = {
                                isScrollToTopEnabled = it
                                settingsManager.isScrollToTopEnabled = it
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isScrollToTopEnabled) Icons.Default.Check else Icons.Default.Close,
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

    if (showBitrateSheet) {
        var tempOnList by remember { mutableStateOf(settingsManager.isBitrateOnList) }
        var tempOnPlayer by remember { mutableStateOf(settingsManager.isBitrateOnPlayer) }
        AlertDialog(
            onDismissRequest = { showBitrateSheet = false },
            containerColor = if (hasBlurBackground) (if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.95f) else Color(0xFFF5F5F5).copy(alpha = 0.95f)) else MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.song_info),
                    fontWeight = FontWeight.Bold,
                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempOnList = !tempOnList }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempOnList,
                            onCheckedChange = { tempOnList = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                checkmarkColor = if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                uncheckedColor = if (hasBlurBackground) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.show_in_song_list),
                            color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempOnPlayer = !tempOnPlayer }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempOnPlayer,
                            onCheckedChange = { tempOnPlayer = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                checkmarkColor = if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                uncheckedColor = if (hasBlurBackground) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.show_in_full_player),
                            color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                        )
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
                    onClick = { showBitrateSheet = false },
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
}
}

@Composable
fun AppIconDialog(
    currentIcon: String,
    currentSong: Song? = null,
    onDismiss: () -> Unit,
    onSelectIcon: (String) -> Unit
) {
    val context = LocalContext.current
    val blurColors = rememberBlurSheetColors(currentSong)
    var selectedIconKey by remember { mutableStateOf(currentIcon) }

    val iconOptions = listOf(
        IconOptionData(
            key = AppIconManager.ICON_DEFAULT,
            nameRes = R.string.app_icon_default,
            msgRes = R.string.app_icon_msg_default,
            bgColor = Color(0xFF554492),
            fgDrawableRes = R.drawable.ic_launcher_foreground,
            accentColor = Color(0xFF8E7CE6)
        ),
        IconOptionData(
            key = AppIconManager.ICON_DYNAMIC,
            nameRes = R.string.app_icon_dynamic,
            msgRes = R.string.app_icon_msg_dynamic,
            bgColor = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary,
            fgDrawableRes = R.drawable.ic_launcher_dynamic_foreground,
            accentColor = blurColors.primaryTint
        ),
        IconOptionData(
            key = AppIconManager.ICON_MONO_WHITE,
            nameRes = R.string.app_icon_mono_white,
            msgRes = R.string.app_icon_msg_mono_white,
            bgColor = Color.White,
            fgDrawableRes = R.drawable.ic_launcher_mono_white_foreground,
            accentColor = Color(0xFF78909C)
        ),
        IconOptionData(
            key = AppIconManager.ICON_MONO_BLACK,
            nameRes = R.string.app_icon_mono_black,
            msgRes = R.string.app_icon_msg_mono_black,
            bgColor = Color(0xFF141416),
            fgDrawableRes = R.drawable.ic_launcher_mono_black_foreground,
            accentColor = Color(0xFFB0BEC5)
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = currentSong,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 420.dp),
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
                        color = if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = blurColors.primaryTint,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.app_icon_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = blurColors.textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.app_icon_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = blurColors.textSecondaryColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2x2 Grid of Icon Cards
                    val chunked = iconOptions.chunked(2)
                    chunked.forEachIndexed { rowIndex, rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            rowItems.forEach { item ->
                                val isSelected = selectedIconKey == item.key
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.08f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "iconScale_${item.key}"
                                )

                                Surface(
                                    onClick = { selectedIconKey = item.key },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) {
                                        if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        blurColors.itemContainerColor
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .bounceClick()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp, horizontal = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.TopEnd
                                        ) {
                                            // The Icon Preview
                                            Surface(
                                                shape = RoundedCornerShape(18.dp),
                                                color = item.bgColor,
                                                shadowElevation = if (isSelected) 6.dp else 2.dp,
                                                border = if (item.key == AppIconManager.ICON_MONO_WHITE) {
                                                    BorderStroke(1.dp, if (blurColors.isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.12f))
                                                } else null,
                                                modifier = Modifier.size(64.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(item.fgDrawableRes),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            // Animated Selection Checkmark
                                            val checkmarkScale by animateFloatAsState(
                                                targetValue = if (isSelected) 1f else 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                ),
                                                label = "checkmarkScale_${item.key}"
                                            )

                                            if (checkmarkScale > 0.01f) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = blurColors.primaryTint,
                                                    modifier = Modifier
                                                        .offset(x = 6.dp, y = (-6).dp)
                                                        .size(22.dp)
                                                        .graphicsLayer {
                                                            scaleX = checkmarkScale
                                                            scaleY = checkmarkScale
                                                            alpha = checkmarkScale.coerceIn(0f, 1f)
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = if (blurColors.isDark) Color.Black else Color.White,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = stringResource(item.nameRes),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) blurColors.primaryTint else blurColors.textColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        if (rowIndex < chunked.size - 1) {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Animated Funny Message Bubble
                    AnimatedContent(
                        targetState = selectedIconKey,
                        transitionSpec = {
                            (slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) { it / 3 } + fadeIn(tween(220)))
                                .togetherWith(slideOutVertically { -it / 3 } + fadeOut(tween(140)))
                        },
                        label = "funnyMessageTransition"
                    ) { key ->
                        val currentOption = iconOptions.firstOrNull { it.key == key } ?: iconOptions.first()
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = blurColors.itemContainerColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = currentOption.accentColor.copy(alpha = 0.16f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = currentOption.accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(currentOption.msgRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = blurColors.textColor,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dialog Actions (Cancel / Apply)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.bounceClick()
                        ) {
                            Text(stringResource(R.string.cancel), color = blurColors.textSecondaryColor)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val confirmBtnColor = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary
                        val confirmTextColor = if (blurColors.hasBlur && blurColors.isDark) Color.Black else Color.White

                        Button(
                            onClick = { onSelectIcon(selectedIconKey) },
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

private data class IconOptionData(
    val key: String,
    val nameRes: Int,
    val msgRes: Int,
    val bgColor: Color,
    val fgDrawableRes: Int,
    val accentColor: Color
)
