package com.demonlab.lune.ui.activities

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.demonlab.lune.tools.normalizeForSearch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.material3.*
import com.demonlab.lune.ui.components.BouncySwitch
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import kotlinx.coroutines.launch
import com.demonlab.lune.tools.PlaylistBackupManager
import com.demonlab.lune.tools.MusicProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.AppIconManager
import com.demonlab.lune.BuildConfig
import com.demonlab.lune.tools.PlaybackManager
import androidx.compose.ui.graphics.vector.ImageVector
import com.demonlab.lune.ui.theme.LuneTheme
import com.demonlab.lune.ui.utils.bounceClick

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsManager = SettingsManager.getInstance(this)
        enableEdgeToEdge()
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
                SettingsScreen(
                    onBack = { finish() },
                    onThemeModeChanged = { themeMode = it },
                    onCustomColorsChanged = { useCustomColors = it },
                    onPaletteChanged = { customColorPalette = it },
                    onAmoledChanged = { useAmoledPitchBlack = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onThemeModeChanged: ((Int) -> Unit)? = null,
    onCustomColorsChanged: ((Boolean) -> Unit)? = null,
    onPaletteChanged: ((Int) -> Unit)? = null,
    onAmoledChanged: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong

    // ─── Section 1: Tema y Apariencia ───
    var themeMode by remember { mutableIntStateOf(settingsManager.themeMode) }
    var useAmoledPitchBlack by remember { mutableStateOf(settingsManager.useAmoledPitchBlack) }
    var useCustomColors by remember { mutableStateOf(settingsManager.useCustomColors) }
    var customColorPalette by remember { mutableIntStateOf(settingsManager.customColorPalette) }
    var appIconKey by remember { mutableStateOf(AppIconManager.getCurrentIcon(context)) }
    var showAppIconDialog by remember { mutableStateOf(false) }

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))
    val isBlurActive = hasBlurBackground && currentSong != null

    // ─── Section 2: Reproductor (Now Playing) ───
    var isCinematicPlayerEnabled by remember { mutableStateOf(settingsManager.isCinematicPlayerEnabled) }
    var coverShape by remember { mutableIntStateOf(settingsManager.coverShape) }
    var progressBarStyle by remember { mutableIntStateOf(settingsManager.progressBarStyle) }
    var isPlayerGradientBackgroundEnabled by remember { mutableStateOf(settingsManager.isPlayerGradientBackgroundEnabled) }
    var coverSpin by remember { mutableStateOf(settingsManager.coverSpin) }
    var coverVinylEffect by remember { mutableStateOf(settingsManager.coverVinylEffect) }
    var useQuickPlayer by remember { mutableStateOf(settingsManager.useQuickPlayer) }
    var showCoverShapeDialog by remember { mutableStateOf(false) }

    // ─── Section 3: Audio y Reproducción ───
    var enableHiFi by remember { mutableStateOf(settingsManager.enableHiFi) }
    var is32BitFloatEnabled by remember { mutableStateOf(settingsManager.is32BitFloatEnabled) }
    var isExclusiveModeEnabled by remember { mutableStateOf(settingsManager.isExclusiveModeEnabled) }
    var isCrossfade by remember { mutableStateOf(settingsManager.isCrossfade) }
    var crossfadeDurationSeconds by remember { mutableIntStateOf(settingsManager.crossfadeDurationSeconds) }
    var seamlessLooping by remember { mutableStateOf(settingsManager.seamlessLooping) }
    var keepScreenOn by remember { mutableStateOf(settingsManager.keepScreenOn) }
    var stopOnTaskRemoved by remember { mutableStateOf(settingsManager.stopOnTaskRemoved) }
    var showCrossfadeDialog by remember { mutableStateOf(false) }

    // ─── Section 4: Biblioteca y Organización ───
    var isScanningLibrary by remember { mutableStateOf(false) }
    var defaultAlbumSort by remember { mutableStateOf(settingsManager.defaultAlbumSortOption) }
    var defaultArtistSort by remember { mutableStateOf(settingsManager.defaultArtistSortOption) }
    var defaultPlaylistSort by remember { mutableStateOf(settingsManager.defaultPlaylistSortOption) }
    var isSectionCustomizationEnabled by remember { mutableStateOf(settingsManager.isSectionCustomizationEnabled) }
    var hiddenSectionTabs by remember { mutableStateOf(settingsManager.hiddenSectionTabs) }
    var showAlbumSortDialog by remember { mutableStateOf(false) }
    var showArtistSortDialog by remember { mutableStateOf(false) }
    var showPlaylistSortDialog by remember { mutableStateOf(false) }
    var showSectionCustomizationDialog by remember { mutableStateOf(false) }

    // ─── Section 5: Copia de Seguridad y Datos ───
    val backupManager = remember { PlaylistBackupManager(context) }
    val musicProvider = remember { MusicProvider(context) }
    var autoSyncBackupUri by remember { mutableStateOf(settingsManager.autoSyncBackupUri) }
    var isAutoSyncEnabled by remember { mutableStateOf(settingsManager.isAutoSyncBackupEnabled) }
    var showBackupWarning by remember { mutableStateOf(settingsManager.showBackupWarning) }

    // ─── Section 6: Privacidad e Inteligencia Artificial ───
    var showAiSection by remember { mutableStateOf(settingsManager.showAiSection) }
    var showResetAiDialog by remember { mutableStateOf(false) }

    // ─── Section 7: Sistema y Acerca de ───
    var isHapticVibrationEnabled by remember { mutableStateOf(settingsManager.isHapticVibrationEnabled) }
    var isScrollToTopEnabled by remember { mutableStateOf(settingsManager.isScrollToTopEnabled) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }
    val currentLanguage = settingsManager.language

    val scope = rememberCoroutineScope()

    // Sincronización continua de estados al volver de otras pantallas
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                themeMode = settingsManager.themeMode
                useAmoledPitchBlack = settingsManager.useAmoledPitchBlack
                useCustomColors = settingsManager.useCustomColors
                customColorPalette = settingsManager.customColorPalette
                appIconKey = AppIconManager.getCurrentIcon(context)
                isCinematicPlayerEnabled = settingsManager.isCinematicPlayerEnabled
                coverShape = settingsManager.coverShape
                progressBarStyle = settingsManager.progressBarStyle
                isPlayerGradientBackgroundEnabled = settingsManager.isPlayerGradientBackgroundEnabled
                coverSpin = settingsManager.coverSpin
                coverVinylEffect = settingsManager.coverVinylEffect
                useQuickPlayer = settingsManager.useQuickPlayer
                enableHiFi = settingsManager.enableHiFi
                is32BitFloatEnabled = settingsManager.is32BitFloatEnabled
                isExclusiveModeEnabled = settingsManager.isExclusiveModeEnabled
                isCrossfade = settingsManager.isCrossfade
                crossfadeDurationSeconds = settingsManager.crossfadeDurationSeconds
                seamlessLooping = settingsManager.seamlessLooping
                keepScreenOn = settingsManager.keepScreenOn
                stopOnTaskRemoved = settingsManager.stopOnTaskRemoved
                defaultAlbumSort = settingsManager.defaultAlbumSortOption
                defaultArtistSort = settingsManager.defaultArtistSortOption
                defaultPlaylistSort = settingsManager.defaultPlaylistSortOption
                isSectionCustomizationEnabled = settingsManager.isSectionCustomizationEnabled
                hiddenSectionTabs = settingsManager.hiddenSectionTabs
                autoSyncBackupUri = settingsManager.autoSyncBackupUri
                isAutoSyncEnabled = settingsManager.isAutoSyncBackupEnabled
                showAiSection = settingsManager.showAiSection
                isHapticVibrationEnabled = settingsManager.isHapticVibrationEnabled
                isScrollToTopEnabled = settingsManager.isScrollToTopEnabled
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Buscador
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(searchQuery) {
        scrollState.scrollTo(0)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val albumSortOptions = remember {
        listOf(
            "TRACK_NUMBER" to R.string.sort_track_number,
            "ALPHABETICAL" to R.string.sort_alphabetical,
            "DATE_ADDED" to R.string.sort_date_added,
            "DURATION" to R.string.sort_duration
        )
    }
    val artistSortOptions = remember {
        listOf(
            "ALPHABETICAL" to R.string.sort_alphabetical,
            "ALBUM" to R.string.sort_album,
            "DATE_ADDED" to R.string.sort_date_added,
            "DURATION" to R.string.sort_duration
        )
    }
    val playlistSortOptions = remember {
        listOf(
            "CUSTOM" to R.string.sort_custom,
            "ALPHABETICAL" to R.string.sort_alphabetical,
            "DATE_ADDED" to R.string.sort_date_added,
            "TRACK_NUMBER" to R.string.sort_track_number,
            "DURATION" to R.string.sort_duration
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            settingsManager.autoSyncBackupUri = it.toString()
            autoSyncBackupUri = it.toString()
            scope.launch {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val success = backupManager.exportPlaylists(outputStream)
                    Toast.makeText(
                        context,
                        if (success) context.getString(R.string.export_success) else context.getString(R.string.export_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val success = backupManager.importPlaylists(inputStream)
                    Toast.makeText(
                        context,
                        if (success) context.getString(R.string.import_success) else context.getString(R.string.import_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // ─── Diálogos Modales ───

    if (showAppIconDialog) {
        AppIconDialog(
            currentIcon = appIconKey,
            currentSong = currentSong,
            onDismiss = { showAppIconDialog = false },
            onSelectIcon = { newIcon ->
                appIconKey = newIcon
                showAppIconDialog = false
                AppIconManager.applyAppIcon(context, newIcon)
                Toast.makeText(context, context.getString(R.string.app_icon_applied), Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCoverShapeDialog) {
        AlertDialog(
            onDismissRequest = { showCoverShapeDialog = false },
            confirmButton = {},
            containerColor = if (hasBlurBackground) (if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.95f) else Color(0xFFF5F5F5).copy(alpha = 0.95f)) else MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.cover_shape_title),
                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    val shapes = listOf(
                        0 to stringResource(R.string.shape_default),
                        1 to stringResource(R.string.shape_square),
                        2 to stringResource(R.string.shape_circular)
                    )
                    shapes.forEach { (index, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsManager.coverShape = index
                                    coverShape = index
                                    showCoverShapeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = coverShape == index,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                    unselectedColor = if (hasBlurBackground) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                label,
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

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
                        value = tempCrossfade.toFloat(),
                        onValueChange = { tempCrossfade = it.toInt() },
                        valueRange = 0f..12f,
                        steps = 11,
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

    if (showSectionCustomizationDialog) {
        var tempHiddenTabs by remember { mutableStateOf(hiddenSectionTabs) }
        val sectionItems = listOf(
            "ALL" to (stringResource(R.string.tab_all) to Icons.Default.MusicNote),
            "FAVORITES" to (stringResource(R.string.tab_favorites) to Icons.Default.Favorite),
            "PLAYLISTS" to (stringResource(R.string.playlists) to Icons.AutoMirrored.Filled.QueueMusic),
            "ALBUMS" to (stringResource(R.string.tab_albums_real) to Icons.Default.Album),
            "ARTISTS" to (stringResource(R.string.tab_artists) to Icons.Default.Person),
            "GENRES" to (stringResource(R.string.tab_genres) to Icons.Default.Category),
            "FOLDERS" to (stringResource(R.string.tab_folders) to Icons.Default.Folder)
        )

        AlertDialog(
            onDismissRequest = { showSectionCustomizationDialog = false },
            containerColor = if (hasBlurBackground) (if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.95f) else Color(0xFFF5F5F5).copy(alpha = 0.95f)) else MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.section_customization),
                    fontWeight = FontWeight.Bold,
                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    sectionItems.forEach { (key, data) ->
                        val (label, icon) = data
                        val isVisible = key !in tempHiddenTabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = tempHiddenTabs.toMutableSet()
                                    if (isVisible) newSet.add(key) else newSet.remove(key)
                                    tempHiddenTabs = newSet
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isVisible,
                                onCheckedChange = { checked ->
                                    val newSet = tempHiddenTabs.toMutableSet()
                                    if (!checked) newSet.add(key) else newSet.remove(key)
                                    tempHiddenTabs = newSet
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                    checkmarkColor = if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                    uncheckedColor = if (hasBlurBackground) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                label,
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        hiddenSectionTabs = tempHiddenTabs
                        settingsManager.hiddenSectionTabs = tempHiddenTabs
                        settingsManager.isSectionCustomizationEnabled = true
                        showSectionCustomizationDialog = false
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
                    onClick = { showSectionCustomizationDialog = false },
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

    if (showResetAiDialog) {
        AlertDialog(
            onDismissRequest = { showResetAiDialog = false },
            containerColor = if (hasBlurBackground) (if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.95f) else Color(0xFFF5F5F5).copy(alpha = 0.95f)) else MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.reset_ai_habits),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    stringResource(R.string.reset_ai_habits_confirm),
                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        com.demonlab.lune.ai.LuneAiEngine.getInstance(context).resetLearning()
                        showResetAiDialog = false
                        Toast.makeText(context, context.getString(R.string.reset_ai_habits_success), Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.delete_song).substringBefore(" "), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showResetAiDialog = false },
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

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            confirmButton = {},
            containerColor = if (hasBlurBackground) (if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.95f) else Color(0xFFF5F5F5).copy(alpha = 0.95f)) else MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.select_language),
                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    val languages = listOf(
                        "system" to stringResource(R.string.lang_system),
                        "en" to stringResource(R.string.lang_english),
                        "es" to stringResource(R.string.lang_spanish),
                        "pt-BR" to stringResource(R.string.lang_portuguese),
                        "fr" to stringResource(R.string.lang_french),
                        "zh" to stringResource(R.string.lang_chinese),
                        "de" to stringResource(R.string.lang_german),
                        "ru" to stringResource(R.string.lang_russian),
                        "fa" to stringResource(R.string.lang_persian),
                        "ar" to stringResource(R.string.lang_arabic),
                        "pl" to stringResource(R.string.lang_polish)
                    )
                    languages.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsManager.language = code
                                    val appLocales: LocaleListCompat = if (code == "system") {
                                        LocaleListCompat.getEmptyLocaleList()
                                    } else {
                                        LocaleListCompat.forLanguageTags(code)
                                    }
                                    AppCompatDelegate.setApplicationLocales(appLocales)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == code,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                    unselectedColor = if (hasBlurBackground) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                label,
                                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showAlbumSortDialog) {
        SortOptionDialog(
            title = stringResource(R.string.default_album_sorting),
            currentOption = defaultAlbumSort,
            options = albumSortOptions,
            hasBlurBackground = hasBlurBackground,
            isDarkTheme = isDarkTheme,
            onOptionSelected = {
                defaultAlbumSort = it
                settingsManager.defaultAlbumSortOption = it
            },
            onDismiss = { showAlbumSortDialog = false }
        )
    }

    if (showArtistSortDialog) {
        SortOptionDialog(
            title = stringResource(R.string.default_artist_sorting),
            currentOption = defaultArtistSort,
            options = artistSortOptions,
            hasBlurBackground = hasBlurBackground,
            isDarkTheme = isDarkTheme,
            onOptionSelected = {
                defaultArtistSort = it
                settingsManager.defaultArtistSortOption = it
            },
            onDismiss = { showArtistSortDialog = false }
        )
    }

    if (showPlaylistSortDialog) {
        SortOptionDialog(
            title = stringResource(R.string.default_playlist_sorting),
            currentOption = defaultPlaylistSort,
            options = playlistSortOptions,
            hasBlurBackground = hasBlurBackground,
            isDarkTheme = isDarkTheme,
            onOptionSelected = {
                defaultPlaylistSort = it
                settingsManager.defaultPlaylistSortOption = it
            },
            onDismiss = { showPlaylistSortDialog = false }
        )
    }

    if (showDonateDialog) {
        DonateDialog(
            onDismiss = { showDonateDialog = false },
            hasBlurBackground = hasBlurBackground,
            isDarkTheme = isDarkTheme,
            currentSong = currentSong
        )
    }

    // ─── Títulos de Secciones Unificadas ───
    val themeSectionTitle = stringResource(R.string.theme)
    val playerSectionTitle = stringResource(R.string.media_player)
    val audioSettingsSectionTitle = stringResource(R.string.audio_settings)
    val librarySectionTitle = stringResource(R.string.library_organization)
    val backupSectionTitle = stringResource(R.string.backup)
    val privacyAiSectionTitle = stringResource(R.string.privacy_ai_title)
    val systemAboutSectionTitle = stringResource(R.string.system_about)

    // ─── Índice Completo de Búsqueda ───
    val allSearchableSettings = remember(
        themeMode,
        useAmoledPitchBlack,
        useCustomColors,
        customColorPalette,
        appIconKey,
        isCinematicPlayerEnabled,
        coverShape,
        progressBarStyle,
        isPlayerGradientBackgroundEnabled,
        coverSpin,
        coverVinylEffect,
        useQuickPlayer,
        enableHiFi,
        is32BitFloatEnabled,
        isExclusiveModeEnabled,
        isCrossfade,
        crossfadeDurationSeconds,
        seamlessLooping,
        keepScreenOn,
        stopOnTaskRemoved,
        isScanningLibrary,
        defaultAlbumSort,
        defaultArtistSort,
        defaultPlaylistSort,
        isSectionCustomizationEnabled,
        hiddenSectionTabs,
        autoSyncBackupUri,
        isAutoSyncEnabled,
        showAiSection,
        isHapticVibrationEnabled,
        isScrollToTopEnabled,
        currentLanguage,
        themeSectionTitle,
        playerSectionTitle,
        audioSettingsSectionTitle,
        librarySectionTitle,
        backupSectionTitle,
        privacyAiSectionTitle,
        systemAboutSectionTitle
    ) {
        val hasExportDestination = !autoSyncBackupUri.isNullOrBlank()
        val hiddenCount = settingsManager.hiddenSongIds.size

        listOf(
            // ── 1. TEMA Y APARIENCIA ──
            SearchableSetting(
                id = "theme_mode",
                title = context.getString(R.string.theme_mode_title),
                description = when (themeMode) {
                    1 -> context.getString(R.string.theme_light)
                    2 -> context.getString(R.string.theme_dark)
                    else -> context.getString(R.string.theme_auto)
                },
                section = themeSectionTitle,
                icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                keywords = listOf("tema", "theme", "modo oscuro", "modo claro", "dark", "light", "sistema"),
                onClick = { context.startActivity(Intent(context, ThemeSettingsActivity::class.java)) }
            ),
            SearchableSetting(
                id = "amoled_pitch_black",
                title = context.getString(R.string.amoled_pitch_black),
                description = context.getString(R.string.amoled_pitch_black_desc),
                section = themeSectionTitle,
                icon = Icons.Default.PhoneAndroid,
                keywords = listOf("amoled", "pitch black", "negro", "puro", "oscuro", "bateria", "oled"),
                onClick = { context.startActivity(Intent(context, ThemeSettingsActivity::class.java)) }
            ),
            SearchableSetting(
                id = "use_custom_colors",
                title = context.getString(R.string.use_custom_colors),
                description = context.getString(R.string.use_custom_colors_desc),
                section = themeSectionTitle,
                icon = Icons.Default.Palette,
                keywords = listOf("colores", "colors", "paleta", "palette", "acento", "accent", "personalizado"),
                onClick = { context.startActivity(Intent(context, ThemeSettingsActivity::class.java)) }
            ),
            SearchableSetting(
                id = "blur",
                title = context.getString(R.string.blur),
                description = context.getString(R.string.blur_desc),
                section = themeSectionTitle,
                icon = Icons.Default.BlurOn,
                keywords = listOf("blur", "desenfoque", "efecto blur", "vidrio", "glass", "fondo desenfocado"),
                onClick = { context.startActivity(Intent(context, ThemeSettingsActivity::class.java)) }
            ),
            SearchableSetting(
                id = "font_customization",
                title = context.getString(R.string.font_customization),
                description = context.getString(R.string.font_customization_desc),
                section = themeSectionTitle,
                icon = Icons.Default.TextFields,
                keywords = listOf("font", "fuente", "tipografia", "typography", "letras", "tamano"),
                onClick = { context.startActivity(Intent(context, FontCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "app_icon",
                title = context.getString(R.string.app_icon_title),
                description = when (appIconKey) {
                    AppIconManager.ICON_DYNAMIC -> context.getString(R.string.app_icon_dynamic)
                    AppIconManager.ICON_MONO_WHITE -> context.getString(R.string.app_icon_mono_white)
                    AppIconManager.ICON_MONO_BLACK -> context.getString(R.string.app_icon_mono_black)
                    else -> context.getString(R.string.app_icon_default)
                },
                section = themeSectionTitle,
                icon = Icons.Default.Apps,
                keywords = listOf("icon", "icono", "app icon", "logo", "launcher"),
                onClick = { showAppIconDialog = true }
            ),

            // ── 2. REPRODUCTOR (NOW PLAYING) ──
            SearchableSetting(
                id = "player_customization",
                title = context.getString(R.string.player_customization),
                description = context.getString(R.string.player_customization_desc),
                section = playerSectionTitle,
                icon = Icons.Default.PlayCircle,
                keywords = listOf("player", "reproductor", "caratula", "cover", "onda", "waveform", "controles", "personalizar"),
                onClick = { context.startActivity(Intent(context, PlayerCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "cinematic_player",
                title = context.getString(R.string.cinematic_player),
                description = context.getString(R.string.cinematic_player_desc),
                section = playerSectionTitle,
                icon = Icons.Default.AutoAwesome,
                keywords = listOf("cinematic", "cinematico", "pantalla completa", "inmersivo", "capa em tela cheia"),
                onClick = {
                    val newVal = !isCinematicPlayerEnabled
                    isCinematicPlayerEnabled = newVal
                    settingsManager.isCinematicPlayerEnabled = newVal
                }
            ),
            SearchableSetting(
                id = "cover_shape",
                title = context.getString(R.string.cover_shape_title),
                description = when (coverShape) {
                    1 -> context.getString(R.string.shape_square)
                    2 -> context.getString(R.string.shape_circular)
                    else -> context.getString(R.string.shape_default)
                },
                section = playerSectionTitle,
                icon = Icons.Default.Album,
                keywords = listOf("cover shape", "forma caratula", "squircle", "cuadrada", "circulo", "redondeada"),
                onClick = { showCoverShapeDialog = true }
            ),
            SearchableSetting(
                id = "progress_bar_style",
                title = context.getString(R.string.progress_bar_style_title),
                description = if (progressBarStyle == 1) context.getString(R.string.progress_bar_wave) else context.getString(R.string.progress_bar_classic),
                section = playerSectionTitle,
                icon = Icons.Default.Equalizer,
                keywords = listOf("progress bar", "barra de progreso", "onda", "wave", "lineal"),
                onClick = {
                    val newStyle = if (progressBarStyle == 1) 0 else 1
                    progressBarStyle = newStyle
                    settingsManager.progressBarStyle = newStyle
                }
            ),
            SearchableSetting(
                id = "player_gradient_background",
                title = context.getString(R.string.player_gradient_background),
                description = context.getString(R.string.player_gradient_background_desc),
                section = playerSectionTitle,
                icon = Icons.Default.Palette,
                keywords = listOf("degradado", "gradient", "fondo reproductor", "ambient", "ambiental", "desfoque"),
                onClick = {
                    val newVal = !isPlayerGradientBackgroundEnabled
                    isPlayerGradientBackgroundEnabled = newVal
                    settingsManager.isPlayerGradientBackgroundEnabled = newVal
                }
            ),
            SearchableSetting(
                id = "cover_spin",
                title = context.getString(R.string.cover_spin),
                description = context.getString(R.string.cover_spin_desc),
                section = playerSectionTitle,
                icon = Icons.Default.Refresh,
                keywords = listOf("girar", "rotar", "spin", "animacion caratula", "disco giratorio"),
                onClick = {
                    val newVal = !coverSpin
                    coverSpin = newVal
                    settingsManager.coverSpin = newVal
                }
            ),
            SearchableSetting(
                id = "cover_vinyl",
                title = context.getString(R.string.cover_vinyl),
                description = context.getString(R.string.cover_vinyl_desc),
                section = playerSectionTitle,
                icon = Icons.Default.Album,
                keywords = listOf("vinilo", "vinyl", "disco vinilo", "anillo vinilo"),
                onClick = {
                    val newVal = !coverVinylEffect
                    coverVinylEffect = newVal
                    settingsManager.coverVinylEffect = newVal
                }
            ),
            SearchableSetting(
                id = "quick_player",
                title = context.getString(R.string.quick_player),
                description = context.getString(R.string.use_quick_player_desc),
                section = playerSectionTitle,
                icon = Icons.Default.Speed,
                keywords = listOf("quick player", "reproductor rapido", "mini", "modal", "flotante"),
                onClick = {
                    val newVal = !useQuickPlayer
                    useQuickPlayer = newVal
                    settingsManager.useQuickPlayer = newVal
                }
            ),
            SearchableSetting(
                id = "gesture",
                title = context.getString(R.string.gesture),
                description = context.getString(R.string.enable_gestures_desc),
                section = playerSectionTitle,
                icon = Icons.Default.Gesture,
                keywords = listOf("gestos", "gesture", "swipe", "deslizar", "mini reproductor"),
                onClick = { context.startActivity(Intent(context, GestureCustomizationActivity::class.java)) }
            ),

            // ── 3. AUDIO Y REPRODUCCIÓN ──
            SearchableSetting(
                id = "equalizer",
                title = context.getString(R.string.eq_title),
                description = context.getString(R.string.audio_settings_desc),
                section = audioSettingsSectionTitle,
                icon = Icons.Default.Equalizer,
                keywords = listOf("equalizer", "ecualizador", "eq", "bass", "graves", "presets", "sonido", "audio"),
                onClick = { context.startActivity(Intent(context, EqualizerActivity::class.java)) }
            ),
            SearchableSetting(
                id = "advanced_audio_settings",
                title = context.getString(R.string.advanced_audio_settings),
                description = context.getString(R.string.advanced_audio_settings_desc),
                section = audioSettingsSectionTitle,
                icon = Icons.Default.Tune,
                keywords = listOf("audio", "avanzado", "hifi", "32bit", "exclusive", "crossfade", "loop"),
                onClick = { context.startActivity(Intent(context, AudioSettingsActivity::class.java)) }
            ),
            SearchableSetting(
                id = "hifi_badge",
                title = context.getString(R.string.hifi_badge),
                description = context.getString(R.string.hifi_badge_desc),
                section = audioSettingsSectionTitle,
                icon = Icons.Default.HighQuality,
                keywords = listOf("hifi", "hi-res", "badge", "insignia", "etiqueta", "lossless", "flac"),
                onClick = {
                    val newVal = !enableHiFi
                    enableHiFi = newVal
                    settingsManager.enableHiFi = newVal
                }
            ),
            SearchableSetting(
                id = "audio_32bit_float",
                title = context.getString(R.string.audio_32bit_float),
                description = context.getString(R.string.audio_32bit_float_desc),
                section = audioSettingsSectionTitle,
                icon = Icons.Default.Equalizer,
                keywords = listOf("32bit", "32 bits", "float", "flotante", "precision", "dsp"),
                onClick = {
                    val newVal = !is32BitFloatEnabled
                    is32BitFloatEnabled = newVal
                    settingsManager.is32BitFloatEnabled = newVal
                }
            ),
            SearchableSetting(
                id = "audio_exclusive_mode",
                title = context.getString(R.string.audio_exclusive_mode),
                description = context.getString(R.string.audio_exclusive_mode_desc),
                section = audioSettingsSectionTitle,
                icon = Icons.Default.MusicNote,
                keywords = listOf("exclusivo", "exclusive", "focus", "enfoque de audio", "aaudio"),
                onClick = {
                    val newVal = !isExclusiveModeEnabled
                    isExclusiveModeEnabled = newVal
                    settingsManager.isExclusiveModeEnabled = newVal
                }
            ),
            SearchableSetting(
                id = "crossfade_time",
                title = context.getString(R.string.crossfade_time),
                description = if (crossfadeDurationSeconds > 0) "${crossfadeDurationSeconds}s" else context.getString(R.string.disabled),
                section = audioSettingsSectionTitle,
                icon = Icons.Default.Tune,
                keywords = listOf("crossfade", "transicion", "desvanecer", "fundido", "mezcla"),
                onClick = { showCrossfadeDialog = true }
            ),
            SearchableSetting(
                id = "seamless_looping",
                title = context.getString(R.string.seamless_looping),
                description = context.getString(R.string.seamless_looping_desc),
                section = audioSettingsSectionTitle,
                icon = Icons.Default.AllInclusive,
                keywords = listOf("seamless", "loop", "bucle", "continuo", "gapless", "sin pausas"),
                onClick = {
                    val newVal = !seamlessLooping
                    seamlessLooping = newVal
                    settingsManager.seamlessLooping = newVal
                    PlaybackManager.getInstance(context).updateLoopingState()
                }
            ),
            SearchableSetting(
                id = "keep_screen_on",
                title = context.getString(R.string.keep_screen_on),
                description = context.getString(R.string.keep_screen_on_desc),
                section = audioSettingsSectionTitle,
                icon = Icons.Default.LightMode,
                keywords = listOf("screen", "display", "pantalla", "luz", "encendida", "awake"),
                onClick = {
                    val newVal = !keepScreenOn
                    keepScreenOn = newVal
                    settingsManager.keepScreenOn = newVal
                }
            ),
            SearchableSetting(
                id = "stop_on_task_removed",
                title = context.getString(R.string.stop_on_task_removed),
                description = context.getString(R.string.stop_on_task_removed_desc),
                section = audioSettingsSectionTitle,
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                keywords = listOf("recientes", "recent apps", "cerrar app", "stop", "detener", "task removed"),
                onClick = {
                    val newVal = !stopOnTaskRemoved
                    stopOnTaskRemoved = newVal
                    settingsManager.stopOnTaskRemoved = newVal
                }
            ),

            // ── 4. BIBLIOTECA Y ORGANIZACIÓN ──
            SearchableSetting(
                id = "rescan_library",
                title = context.getString(R.string.rescan_library),
                description = context.getString(R.string.rescan_library_desc),
                section = librarySectionTitle,
                icon = Icons.Default.Sync,
                keywords = listOf("scan", "escanear", "sync", "sincronizar", "refresh", "actualizar", "biblioteca"),
                onClick = {
                    if (!isScanningLibrary) {
                        scope.launch {
                            isScanningLibrary = true
                            val songs = musicProvider.refreshLibrary()
                            isScanningLibrary = false
                            Toast.makeText(
                                context,
                                context.getString(R.string.rescan_library_success, songs.size),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            ),
            SearchableSetting(
                id = "manage_audio_folders",
                title = context.getString(R.string.manage_audio_folders),
                description = context.getString(R.string.manage_audio_folders_desc),
                section = librarySectionTitle,
                icon = Icons.Default.Folder,
                keywords = listOf("carpetas", "folders", "archivos", "importar", "directorio"),
                onClick = { context.startActivity(Intent(context, FolderBrowserActivity::class.java)) }
            ),
            SearchableSetting(
                id = "hidden_songs",
                title = context.getString(R.string.hidden_songs),
                description = if (hiddenCount > 0) {
                    context.getString(R.string.hidden_songs_count, hiddenCount)
                } else {
                    context.getString(R.string.hidden_songs_desc)
                },
                section = librarySectionTitle,
                icon = Icons.Default.VisibilityOff,
                keywords = listOf("ocultas", "hidden", "ocultar", "hide", "unhide", "desocultar"),
                onClick = { context.startActivity(Intent(context, HiddenSongsActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sort_album",
                title = context.getString(R.string.default_album_sorting),
                description = when (defaultAlbumSort) {
                    "TRACK_NUMBER" -> context.getString(R.string.sort_track_number)
                    "ALPHABETICAL" -> context.getString(R.string.sort_alphabetical)
                    "ARTIST" -> context.getString(R.string.sort_artist)
                    "ALBUM" -> context.getString(R.string.sort_album)
                    "DURATION" -> context.getString(R.string.sort_duration)
                    "DATE_ADDED" -> context.getString(R.string.sort_date_added)
                    "CUSTOM" -> context.getString(R.string.sort_custom)
                    else -> context.getString(R.string.sort_alphabetical)
                },
                section = librarySectionTitle,
                icon = Icons.Default.Album,
                keywords = listOf("album", "albums", "sort", "ordenar", "albumes"),
                onClick = { showAlbumSortDialog = true }
            ),
            SearchableSetting(
                id = "sort_artist",
                title = context.getString(R.string.default_artist_sorting),
                description = when (defaultArtistSort) {
                    "TRACK_NUMBER" -> context.getString(R.string.sort_track_number)
                    "ALPHABETICAL" -> context.getString(R.string.sort_alphabetical)
                    "ARTIST" -> context.getString(R.string.sort_artist)
                    "ALBUM" -> context.getString(R.string.sort_album)
                    "DURATION" -> context.getString(R.string.sort_duration)
                    "DATE_ADDED" -> context.getString(R.string.sort_date_added)
                    "CUSTOM" -> context.getString(R.string.sort_custom)
                    else -> context.getString(R.string.sort_alphabetical)
                },
                section = librarySectionTitle,
                icon = Icons.Default.Person,
                keywords = listOf("artist", "artistas", "sort", "ordenar"),
                onClick = { showArtistSortDialog = true }
            ),
            SearchableSetting(
                id = "sort_playlist",
                title = context.getString(R.string.default_playlist_sorting),
                description = when (defaultPlaylistSort) {
                    "TRACK_NUMBER" -> context.getString(R.string.sort_track_number)
                    "ALPHABETICAL" -> context.getString(R.string.sort_alphabetical)
                    "ARTIST" -> context.getString(R.string.sort_artist)
                    "ALBUM" -> context.getString(R.string.sort_album)
                    "DURATION" -> context.getString(R.string.sort_duration)
                    "DATE_ADDED" -> context.getString(R.string.sort_date_added)
                    "CUSTOM" -> context.getString(R.string.sort_custom)
                    else -> context.getString(R.string.sort_alphabetical)
                },
                section = librarySectionTitle,
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                keywords = listOf("playlist", "playlists", "listas", "sort", "ordenar"),
                onClick = { showPlaylistSortDialog = true }
            ),
            SearchableSetting(
                id = "section_customization",
                title = context.getString(R.string.section_customization),
                description = context.getString(R.string.section_customization_desc),
                section = librarySectionTitle,
                icon = Icons.Default.ViewAgenda,
                keywords = listOf("secciones", "sections", "tabs", "pestanas", "ocultar pestanas", "inicio"),
                onClick = { showSectionCustomizationDialog = true }
            ),

            // ── 5. COPIA DE SEGURIDAD Y DATOS ──
            SearchableSetting(
                id = "export_playlists",
                title = context.getString(R.string.export_playlists),
                description = context.getString(R.string.export_playlists_desc),
                section = backupSectionTitle,
                icon = Icons.Default.CloudDownload,
                keywords = listOf("export", "exportar", "backup", "copia", "seguridad", "guardar"),
                onClick = { exportLauncher.launch("playlists_backup.json") }
            ),
            SearchableSetting(
                id = "import_playlists",
                title = context.getString(R.string.import_playlists),
                description = context.getString(R.string.import_playlists_desc),
                section = backupSectionTitle,
                icon = Icons.Default.Refresh,
                keywords = listOf("import", "importar", "restore", "restaurar", "backup", "cargar"),
                onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream")) }
            ),
            SearchableSetting(
                id = "auto_sync_backup",
                title = context.getString(R.string.auto_sync_backup),
                description = if (hasExportDestination) {
                    context.getString(R.string.auto_sync_backup_desc)
                } else {
                    context.getString(R.string.auto_sync_requires_export)
                },
                section = backupSectionTitle,
                icon = Icons.Default.Sync,
                keywords = listOf("auto", "sync", "sincronizacion", "automatica", "backup", "respaldo"),
                onClick = if (hasExportDestination) {
                    {
                        val newVal = !isAutoSyncEnabled
                        isAutoSyncEnabled = newVal
                        settingsManager.isAutoSyncBackupEnabled = newVal
                        if (newVal) backupManager.triggerAutoSync()
                    }
                } else null
            ),

            // ── 6. PRIVACIDAD E INTELIGENCIA ARTIFICIAL ──
            SearchableSetting(
                id = "show_ai_section",
                title = context.getString(R.string.show_ai_section),
                description = context.getString(R.string.show_ai_section_desc),
                section = privacyAiSectionTitle,
                icon = Icons.Default.AutoAwesome,
                keywords = listOf("ai", "ia", "inteligencia artificial", "recomendaciones", "mezclas", "mixes"),
                onClick = {
                    val newVal = !showAiSection
                    showAiSection = newVal
                    settingsManager.showAiSection = newVal
                }
            ),
            SearchableSetting(
                id = "reset_ai_habits",
                title = context.getString(R.string.reset_ai_habits),
                description = context.getString(R.string.reset_ai_habits_desc),
                section = privacyAiSectionTitle,
                icon = Icons.Default.DeleteForever,
                keywords = listOf("redefinir", "reset", "restablecer", "habitos", "aprendizaje", "ai", "ia"),
                onClick = { showResetAiDialog = true }
            ),

            // ── 7. SISTEMA Y ACERCA DE ──
            SearchableSetting(
                id = "haptic_vibration",
                title = context.getString(R.string.haptic_vibration),
                description = context.getString(R.string.haptic_vibration_desc),
                section = systemAboutSectionTitle,
                icon = Icons.Default.PhoneAndroid,
                keywords = listOf("vibracion", "vibration", "haptico", "haptic", "tactil"),
                onClick = {
                    val newVal = !isHapticVibrationEnabled
                    isHapticVibrationEnabled = newVal
                    settingsManager.isHapticVibrationEnabled = newVal
                }
            ),
            SearchableSetting(
                id = "scroll_to_top_button",
                title = context.getString(R.string.scroll_to_top_button),
                description = context.getString(R.string.scroll_to_top_button_desc),
                section = systemAboutSectionTitle,
                icon = Icons.Default.KeyboardArrowUp,
                keywords = listOf("scroll", "arriba", "volver arriba", "boton arriba"),
                onClick = {
                    val newVal = !isScrollToTopEnabled
                    isScrollToTopEnabled = newVal
                    settingsManager.isScrollToTopEnabled = newVal
                }
            ),
            SearchableSetting(
                id = "widget_customization",
                title = context.getString(R.string.widget_customization),
                description = context.getString(R.string.widget_customization_desc),
                section = systemAboutSectionTitle,
                icon = Icons.Default.Widgets,
                keywords = listOf("widget", "widgets", "pantalla inicio", "home screen"),
                onClick = { context.startActivity(Intent(context, WidgetCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "language",
                title = context.getString(R.string.language),
                description = when (currentLanguage) {
                    "en" -> context.getString(R.string.lang_english)
                    "es" -> context.getString(R.string.lang_spanish)
                    "pt-BR" -> context.getString(R.string.lang_portuguese)
                    "fr" -> context.getString(R.string.lang_french)
                    "zh" -> context.getString(R.string.lang_chinese)
                    "de" -> context.getString(R.string.lang_german)
                    "ru" -> context.getString(R.string.lang_russian)
                    "fa" -> context.getString(R.string.lang_persian)
                    "ar" -> context.getString(R.string.lang_arabic)
                    "pl" -> context.getString(R.string.lang_polish)
                    else -> context.getString(R.string.lang_system)
                },
                section = systemAboutSectionTitle,
                icon = Icons.Default.Language,
                keywords = listOf("idioma", "language", "lenguaje", "traduccion"),
                onClick = { showLanguageDialog = true }
            ),
            SearchableSetting(
                id = "permissions",
                title = context.getString(R.string.permissions),
                description = context.getString(R.string.permissions_desc),
                section = systemAboutSectionTitle,
                icon = Icons.Default.Security,
                keywords = listOf("permissions", "permisos", "storage", "notifications", "security"),
                onClick = { context.startActivity(Intent(context, PermissionsActivity::class.java)) }
            ),
            SearchableSetting(
                id = "donate",
                title = context.getString(R.string.donate_title),
                description = context.getString(R.string.donate_desc),
                section = systemAboutSectionTitle,
                icon = Icons.Default.Favorite,
                keywords = listOf("donate", "donar", "donacion", "support", "patreon"),
                onClick = { showDonateDialog = true }
            ),
            SearchableSetting(
                id = "about",
                title = context.getString(R.string.about),
                description = "Lune v${BuildConfig.VERSION_NAME}",
                section = systemAboutSectionTitle,
                icon = Icons.Default.Info,
                keywords = listOf("about", "acerca", "info", "version", "changelog", "github"),
                onClick = { context.startActivity(Intent(context, AboutActivity::class.java)) }
            )
        )
    }

    val isFiltering = isSearchActive
    val normalizedQuery = remember(searchQuery) { searchQuery.normalizeForSearch() }
    val searchTerms = remember(normalizedQuery) {
        normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    val matchedSettings = remember(allSearchableSettings, searchTerms, isFiltering) {
        if (!isFiltering) emptyList()
        else if (searchTerms.isEmpty()) allSearchableSettings
        else {
            allSearchableSettings.filter { item ->
                val titleNorm = item.title.normalizeForSearch()
                val descNorm = item.description?.normalizeForSearch() ?: ""
                val sectionNorm = item.section.normalizeForSearch()
                val parentNorm = item.parentTitle?.normalizeForSearch() ?: ""
                val combined = "$titleNorm $descNorm $sectionNorm $parentNorm ${item.keywords.joinToString(" ") { it.normalizeForSearch() }}"
                searchTerms.all { term -> combined.contains(term) }
            }
        }
    }

    com.demonlab.lune.ui.components.AppBlurBackdrop(
        hasBlurBackground = hasBlurBackground,
        isDarkTheme = isDarkTheme,
        currentSong = currentSong
    ) {
        Scaffold(
            modifier = if (!isSearchActive) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier,
            containerColor = if (isBlurActive) Color.Transparent else MaterialTheme.colorScheme.surface,
            topBar = {
                AnimatedContent(
                    targetState = isSearchActive,
                    transitionSpec = {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(180))
                    },
                    label = "SettingsTopBarTransition"
                ) { active ->
                    if (active) {
                        TopAppBar(
                            title = {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 8.dp)
                                        .focusRequester(searchFocusRequester),
                                    placeholder = {
                                        Text(
                                            stringResource(R.string.search_settings),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isBlurActive) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = if (isBlurActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = if (isBlurActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                        cursorColor = if (isBlurActive) Color.White else MaterialTheme.colorScheme.primary,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    )
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        isSearchActive = false
                                        searchQuery = ""
                                    },
                                    modifier = Modifier.bounceClick()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isBlurActive) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = stringResource(R.string.cd_back),
                                                tint = if (isBlurActive) Color.White else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.bounceClick()
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isBlurActive) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Clear",
                                                    tint = if (isBlurActive) Color.White else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = if (isBlurActive) Color.Transparent else MaterialTheme.colorScheme.surface,
                                titleContentColor = if (isBlurActive) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    } else {
                        LargeTopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.settings),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = if (isBlurActive) Color.White else MaterialTheme.colorScheme.primary
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.bounceClick()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isBlurActive) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = stringResource(R.string.cd_back),
                                                tint = if (isBlurActive) Color.White else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { isSearchActive = true },
                                    modifier = Modifier.bounceClick()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isBlurActive) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = stringResource(R.string.search_settings),
                                                tint = if (isBlurActive) Color.White else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = if (isBlurActive) Color.Transparent else MaterialTheme.colorScheme.surface,
                                titleContentColor = if (isBlurActive) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isFiltering) {
                    if (matchedSettings.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp, bottom = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isBlurActive) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = if (isBlurActive) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_settings_found),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isBlurActive) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.no_settings_found_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isBlurActive) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val grouped = matchedSettings.groupBy { it.section }
                        grouped.forEach { (sectionTitle, items) ->
                            SettingsSection(title = sectionTitle) {
                                items.forEachIndexed { index, item ->
                                    val pos = when {
                                        items.size == 1 -> SectionPosition.SINGLE
                                        index == 0 -> SectionPosition.FIRST
                                        index == items.size - 1 -> SectionPosition.LAST
                                        else -> SectionPosition.MIDDLE
                                    }
                                    val displayText = if (item.parentTitle != null && item.parentTitle != sectionTitle) {
                                        if (!item.description.isNullOrBlank()) "${item.parentTitle} • ${item.description}" else item.parentTitle
                                    } else {
                                        item.description
                                    }
                                    SettingsPreferenceItem(
                                        headlineText = item.title,
                                        supportingText = displayText,
                                        icon = item.icon,
                                        position = pos,
                                        onClick = item.onClick,
                                        trailingContent = item.trailingContent
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                } else {
                    // ──────────────────────────────────────────────
                    // 1. TEMA Y APARIENCIA
                    // ──────────────────────────────────────────────
                    SettingsSection(title = themeSectionTitle) {
                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.theme_mode_title),
                            supportingText = when (themeMode) {
                                1 -> stringResource(R.string.theme_light)
                                2 -> stringResource(R.string.theme_dark)
                                else -> stringResource(R.string.theme_auto)
                            },
                            icon = when (themeMode) {
                                1 -> Icons.Default.LightMode
                                2 -> Icons.Default.DarkMode
                                else -> Icons.Outlined.BrightnessAuto
                            },
                            position = SectionPosition.FIRST,
                            onClick = { context.startActivity(Intent(context, ThemeSettingsActivity::class.java)) }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.font_customization),
                            supportingText = stringResource(R.string.font_customization_desc),
                            icon = Icons.Default.TextFields,
                            position = SectionPosition.MIDDLE,
                            onClick = { context.startActivity(Intent(context, FontCustomizationActivity::class.java)) }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.app_icon_title),
                            supportingText = when (appIconKey) {
                                AppIconManager.ICON_DYNAMIC -> stringResource(R.string.app_icon_dynamic)
                                AppIconManager.ICON_MONO_WHITE -> stringResource(R.string.app_icon_mono_white)
                                AppIconManager.ICON_MONO_BLACK -> stringResource(R.string.app_icon_mono_black)
                                else -> stringResource(R.string.app_icon_default)
                            },
                            icon = Icons.Default.Apps,
                            position = SectionPosition.LAST,
                            onClick = { showAppIconDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ──────────────────────────────────────────────
                    // 2. REPRODUCTOR (NOW PLAYING)
                    // ──────────────────────────────────────────────
                    SettingsSection(title = playerSectionTitle) {
                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.player_customization),
                            supportingText = stringResource(R.string.player_customization_desc),
                            icon = Icons.Default.PlayCircle,
                            position = SectionPosition.FIRST,
                            onClick = { context.startActivity(Intent(context, PlayerCustomizationActivity::class.java)) }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.gesture),
                            supportingText = stringResource(R.string.enable_gestures_desc),
                            icon = Icons.Default.Gesture,
                            position = SectionPosition.LAST,
                            onClick = { context.startActivity(Intent(context, GestureCustomizationActivity::class.java)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ──────────────────────────────────────────────
                    // 3. AUDIO Y REPRODUCCIÓN
                    // ──────────────────────────────────────────────
                    SettingsSection(title = audioSettingsSectionTitle) {
                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.eq_title),
                            supportingText = stringResource(R.string.audio_settings_desc),
                            icon = Icons.Default.Equalizer,
                            position = SectionPosition.FIRST,
                            onClick = { context.startActivity(Intent(context, EqualizerActivity::class.java)) }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.advanced_audio_settings),
                            supportingText = stringResource(R.string.advanced_audio_settings_desc),
                            icon = Icons.Default.Tune,
                            position = SectionPosition.MIDDLE,
                            onClick = { context.startActivity(Intent(context, AudioSettingsActivity::class.java)) }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.keep_screen_on),
                            supportingText = stringResource(R.string.keep_screen_on_desc),
                            icon = Icons.Default.LightMode,
                            position = SectionPosition.LAST,
                            trailingContent = {
                                BouncySwitch(
                                    checked = keepScreenOn,
                                    onCheckedChange = {
                                        keepScreenOn = it
                                        settingsManager.keepScreenOn = it
                                    },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (keepScreenOn) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ──────────────────────────────────────────────
                    // 4. BIBLIOTECA Y ORGANIZACIÓN
                    // ──────────────────────────────────────────────
                    SettingsSection(title = librarySectionTitle) {
                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.rescan_library),
                            supportingText = stringResource(R.string.rescan_library_desc),
                            icon = Icons.Default.Sync,
                            position = SectionPosition.FIRST,
                            trailingContent = if (isScanningLibrary) {
                                {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null,
                            onClick = {
                                if (!isScanningLibrary) {
                                    scope.launch {
                                        isScanningLibrary = true
                                        val songs = musicProvider.refreshLibrary()
                                        isScanningLibrary = false
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.rescan_library_success, songs.size),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.manage_audio_folders),
                            supportingText = stringResource(R.string.manage_audio_folders_desc),
                            icon = Icons.Default.Folder,
                            position = SectionPosition.MIDDLE,
                            onClick = { context.startActivity(Intent(context, FolderBrowserActivity::class.java)) }
                        )

                        val hiddenCount = settingsManager.hiddenSongIds.size
                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.hidden_songs),
                            supportingText = if (hiddenCount > 0) {
                                stringResource(R.string.hidden_songs_count, hiddenCount)
                            } else {
                                stringResource(R.string.hidden_songs_desc)
                            },
                            icon = Icons.Default.VisibilityOff,
                            position = SectionPosition.MIDDLE,
                            onClick = { context.startActivity(Intent(context, HiddenSongsActivity::class.java)) }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.default_album_sorting),
                            supportingText = getSortOptionLabel(defaultAlbumSort),
                            icon = Icons.Default.Album,
                            position = SectionPosition.MIDDLE,
                            onClick = { showAlbumSortDialog = true }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.default_artist_sorting),
                            supportingText = getSortOptionLabel(defaultArtistSort),
                            icon = Icons.Default.Person,
                            position = SectionPosition.MIDDLE,
                            onClick = { showArtistSortDialog = true }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.default_playlist_sorting),
                            supportingText = getSortOptionLabel(defaultPlaylistSort),
                            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                            position = SectionPosition.MIDDLE,
                            onClick = { showPlaylistSortDialog = true }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.section_customization),
                            supportingText = stringResource(R.string.section_customization_desc),
                            icon = Icons.Default.ViewAgenda,
                            position = SectionPosition.LAST,
                            onClick = { showSectionCustomizationDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ──────────────────────────────────────────────
                    // 5. COPIA DE SEGURIDAD Y DATOS
                    // ──────────────────────────────────────────────
                    SettingsSection(title = backupSectionTitle) {
                        if (showBackupWarning) {
                            BackupWarningCard(
                                onDismiss = {
                                    showBackupWarning = false
                                    settingsManager.showBackupWarning = false
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.export_playlists),
                            supportingText = stringResource(R.string.export_playlists_desc),
                            icon = Icons.Default.CloudDownload,
                            position = SectionPosition.FIRST,
                            onClick = { exportLauncher.launch("playlists_backup.json") }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.import_playlists),
                            supportingText = stringResource(R.string.import_playlists_desc),
                            icon = Icons.Default.Refresh,
                            position = SectionPosition.MIDDLE,
                            onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream")) }
                        )

                        val hasExportDestination = !autoSyncBackupUri.isNullOrBlank()
                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.auto_sync_backup),
                            supportingText = if (hasExportDestination) {
                                stringResource(R.string.auto_sync_backup_desc)
                            } else {
                                stringResource(R.string.auto_sync_requires_export)
                            },
                            icon = Icons.Default.Sync,
                            position = SectionPosition.LAST,
                            trailingContent = {
                                BouncySwitch(
                                    checked = isAutoSyncEnabled && hasExportDestination,
                                    enabled = hasExportDestination,
                                    onCheckedChange = { enabled ->
                                        isAutoSyncEnabled = enabled
                                        settingsManager.isAutoSyncBackupEnabled = enabled
                                        if (enabled) {
                                            backupManager.triggerAutoSync()
                                        }
                                    },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (isAutoSyncEnabled && hasExportDestination) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ──────────────────────────────────────────────
                    // 6. PRIVACIDAD E INTELIGENCIA ARTIFICIAL
                    // ──────────────────────────────────────────────
                    SettingsSection(title = privacyAiSectionTitle) {
                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.show_ai_section),
                            supportingText = stringResource(R.string.show_ai_section_desc),
                            icon = Icons.Default.AutoAwesome,
                            position = SectionPosition.FIRST,
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

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp)
                                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp, topStart = 4.dp, topEnd = 4.dp))
                                .bounceClick()
                                .clickable { showResetAiDialog = true },
                            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp, topStart = 4.dp, topEnd = 4.dp),
                            color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = stringResource(R.string.reset_ai_habits),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = stringResource(R.string.reset_ai_habits_desc),
                                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingContent = {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteForever,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ──────────────────────────────────────────────
                    // 7. SISTEMA Y ACERCA DE
                    // ──────────────────────────────────────────────
                    SettingsSection(title = systemAboutSectionTitle) {
                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.haptic_vibration),
                            supportingText = stringResource(R.string.haptic_vibration_desc),
                            icon = Icons.Default.PhoneAndroid,
                            position = SectionPosition.FIRST,
                            trailingContent = {
                                BouncySwitch(
                                    checked = isHapticVibrationEnabled,
                                    onCheckedChange = {
                                        isHapticVibrationEnabled = it
                                        settingsManager.isHapticVibrationEnabled = it
                                    },
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

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.scroll_to_top_button),
                            supportingText = stringResource(R.string.scroll_to_top_button_desc),
                            icon = Icons.Default.KeyboardArrowUp,
                            position = SectionPosition.MIDDLE,
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

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.widget_customization),
                            supportingText = stringResource(R.string.widget_customization_desc),
                            icon = Icons.Default.Widgets,
                            position = SectionPosition.MIDDLE,
                            onClick = { context.startActivity(Intent(context, WidgetCustomizationActivity::class.java)) }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.language),
                            supportingText = when (currentLanguage) {
                                "en" -> stringResource(R.string.lang_english)
                                "es" -> stringResource(R.string.lang_spanish)
                                "pt-BR" -> stringResource(R.string.lang_portuguese)
                                "fr" -> stringResource(R.string.lang_french)
                                "zh" -> stringResource(R.string.lang_chinese)
                                "de" -> stringResource(R.string.lang_german)
                                "ru" -> stringResource(R.string.lang_russian)
                                "fa" -> stringResource(R.string.lang_persian)
                                "ar" -> stringResource(R.string.lang_arabic)
                                "pl" -> stringResource(R.string.lang_polish)
                                else -> stringResource(R.string.lang_system)
                            },
                            icon = Icons.Default.Language,
                            position = SectionPosition.MIDDLE,
                            onClick = { showLanguageDialog = true }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.permissions),
                            supportingText = stringResource(R.string.permissions_desc),
                            icon = Icons.Default.Security,
                            position = SectionPosition.MIDDLE,
                            onClick = { context.startActivity(Intent(context, PermissionsActivity::class.java)) }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.donate_title),
                            supportingText = stringResource(R.string.donate_desc),
                            icon = Icons.Default.Favorite,
                            position = SectionPosition.MIDDLE,
                            onClick = { showDonateDialog = true }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.about),
                            supportingText = "Lune v${BuildConfig.VERSION_NAME}",
                            icon = Icons.Default.Info,
                            position = SectionPosition.LAST,
                            onClick = { context.startActivity(Intent(context, AboutActivity::class.java)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

data class SearchableSetting(
    val id: String,
    val title: String,
    val description: String? = null,
    val section: String,
    val parentTitle: String? = null,
    val icon: ImageVector,
    val keywords: List<String> = emptyList(),
    val trailingContent: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

enum class SectionPosition {
    FIRST, MIDDLE, LAST, SINGLE
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
fun SettingsPreferenceItem(
    headlineText: String,
    supportingText: String? = null,
    icon: ImageVector,
    position: SectionPosition,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))

    val shape = when (position) {
        SectionPosition.FIRST -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        SectionPosition.MIDDLE -> RoundedCornerShape(4.dp)
        SectionPosition.LAST -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
        SectionPosition.SINGLE -> RoundedCornerShape(28.dp)
    }

    val cardBg = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val iconBg = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val iconTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
    val headlineColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
    val supportingColor = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

    val clickModifier = if (onClick != null) {
        Modifier
            .bounceClick()
            .clip(shape)
            .clickable(onClick = onClick)
    } else Modifier

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = cardBg,
        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
    ) {
        ListItem(
            supportingContent = supportingText?.let { { Text(it, color = supportingColor) } },
            leadingContent = {
                Surface(
                    shape = CircleShape,
                    color = iconBg,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            trailingContent = trailingContent,
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        ) {
            Text(headlineText, fontWeight = FontWeight.Bold, color = headlineColor)
        }
    }
}

@Composable
fun BackupWarningCard(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))

    val cardBg = if (hasBlurBackground) {
        Color.Black.copy(alpha = 0.35f)
    } else {
        Color(0xFFFDE8E8)
    }
    val borderColor = if (hasBlurBackground) Color.White.copy(alpha = 0.20f) else Color(0xFFF8B4B4)
    val titleColor = if (hasBlurBackground) Color.White else Color(0xFF9B1C1C)
    val descColor = if (hasBlurBackground) Color.White.copy(alpha = 0.80f) else Color(0xFF9B1C1C).copy(alpha = 0.85f)
    val iconTint = if (hasBlurBackground) Color(0xFFFF8A80) else Color(0xFF9B1C1C)
    val btnBg = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else Color(0xFFFBD5D5)
    val btnTint = if (hasBlurBackground) Color.White else Color(0xFF9B1C1C)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(com.demonlab.lune.R.string.backup_warning_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(com.demonlab.lune.R.string.backup_warning_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = descColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .bounceClick()
                    .background(btnBg)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = btnTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SortOptionDialog(
    title: String,
    currentOption: String,
    options: List<Pair<String, Int>>,
    hasBlurBackground: Boolean,
    isDarkTheme: Boolean,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dialogContainerColor = if (hasBlurBackground) {
        if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.95f) else Color(0xFFF5F5F5).copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val dialogTitleColor = if (hasBlurBackground) {
        if (isDarkTheme) Color.White else Color(0xFF141316)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val textColor = if (hasBlurBackground) {
        if (isDarkTheme) Color.White else Color(0xFF141316)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = dialogContainerColor,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                title,
                color = dialogTitleColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEach { (key, stringResId) ->
                    val isSelected = currentOption == key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(key)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = if (hasBlurBackground) (if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.primary,
                                unselectedColor = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f)) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(stringResId),
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun getSortOptionLabel(option: String): String {
    return when (option) {
        "TRACK_NUMBER" -> stringResource(R.string.sort_track_number)
        "ALPHABETICAL" -> stringResource(R.string.sort_alphabetical)
        "ARTIST" -> stringResource(R.string.sort_artist)
        "ALBUM" -> stringResource(R.string.sort_album)
        "DURATION" -> stringResource(R.string.sort_duration)
        "DATE_ADDED" -> stringResource(R.string.sort_date_added)
        "CUSTOM" -> stringResource(R.string.sort_custom)
        else -> stringResource(R.string.sort_alphabetical)
    }
}
