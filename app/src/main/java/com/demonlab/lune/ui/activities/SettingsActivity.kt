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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
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
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.SettingsManager
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
            
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
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
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var showWhatsapp by remember { mutableStateOf(settingsManager.showWhatsappAudio) }
    var keepScreenOn by remember { mutableStateOf(settingsManager.keepScreenOn) }

    var isCinematicEnabled by remember { mutableStateOf(settingsManager.isCinematicPlayerEnabled) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showBackupWarning by remember { mutableStateOf(settingsManager.showBackupWarning) }
    var isScanningLibrary by remember { mutableStateOf(false) }
    val currentLanguage = settingsManager.language
    val scope = rememberCoroutineScope()
    val backupManager = remember { PlaylistBackupManager(context) }
    val musicProvider = remember { MusicProvider(context) }
    var autoSyncBackupUri by remember { mutableStateOf(settingsManager.autoSyncBackupUri) }
    var isAutoSyncEnabled by remember { mutableStateOf(settingsManager.isAutoSyncBackupEnabled) }

    var defaultAlbumSort by remember { mutableStateOf(settingsManager.defaultAlbumSortOption) }
    var defaultArtistSort by remember { mutableStateOf(settingsManager.defaultArtistSortOption) }
    var defaultPlaylistSort by remember { mutableStateOf(settingsManager.defaultPlaylistSortOption) }

    var showAlbumSortDialog by remember { mutableStateOf(false) }
    var showArtistSortDialog by remember { mutableStateOf(false) }
    var showPlaylistSortDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }

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
    val isBlurActive = hasBlurBackground && currentSong != null

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
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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

    val generalSectionTitle = stringResource(R.string.general)
    val hiddenSongsSectionTitle = stringResource(R.string.hidden_songs)
    val defaultSortingSectionTitle = stringResource(R.string.default_sorting)
    val backupSectionTitle = stringResource(R.string.backup)
    val securitySectionTitle = stringResource(R.string.security)
    val aboutSectionTitle = stringResource(R.string.about)
    val audioSettingsSectionTitle = stringResource(R.string.audio_settings)
    val customizationSectionTitle = stringResource(R.string.customization)
    val playerSectionTitle = stringResource(R.string.player_customization)
    val blurSectionTitle = stringResource(R.string.blur)
    val fontSectionTitle = stringResource(R.string.font_customization)
    val widgetSectionTitle = stringResource(R.string.widget_customization)
    val gestureSectionTitle = stringResource(R.string.gesture)

    val allSearchableSettings = remember(
        keepScreenOn,
        isScanningLibrary,
        currentLanguage,
        settingsManager.hiddenSongIds.size,
        defaultAlbumSort,
        defaultArtistSort,
        defaultPlaylistSort,
        autoSyncBackupUri,
        isAutoSyncEnabled,
        generalSectionTitle,
        hiddenSongsSectionTitle,
        defaultSortingSectionTitle,
        backupSectionTitle,
        securitySectionTitle,
        aboutSectionTitle,
        audioSettingsSectionTitle,
        customizationSectionTitle,
        playerSectionTitle,
        blurSectionTitle,
        fontSectionTitle,
        widgetSectionTitle,
        gestureSectionTitle
    ) {
        val hasExportDestination = !autoSyncBackupUri.isNullOrBlank()
        val hiddenCount = settingsManager.hiddenSongIds.size

        listOf(
            // --- CONFIGURACIONES GENERALES ---
            SearchableSetting(
                id = "audio_settings",
                title = context.getString(R.string.audio_settings),
                description = context.getString(R.string.audio_settings_desc),
                section = generalSectionTitle,
                icon = Icons.Default.MusicNote,
                keywords = listOf("audio", "sound", "sonido", "equalizer", "ecualizador", "eq", "bass", "graves", "volume", "volumen", "gain", "efectos", "effects"),
                onClick = { context.startActivity(Intent(context, AudioSettingsActivity::class.java)) }
            ),
            SearchableSetting(
                id = "keep_screen_on",
                title = context.getString(R.string.keep_screen_on),
                description = context.getString(R.string.keep_screen_on_desc),
                section = generalSectionTitle,
                icon = Icons.Default.LightMode,
                keywords = listOf("screen", "display", "pantalla", "luz", "encendida", "awake", "lock", "bloqueo", "apagar"),
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
                },
                onClick = {
                    val newVal = !keepScreenOn
                    keepScreenOn = newVal
                    settingsManager.keepScreenOn = newVal
                }
            ),
            SearchableSetting(
                id = "rescan_library",
                title = context.getString(R.string.rescan_library),
                description = context.getString(R.string.rescan_library_desc),
                section = generalSectionTitle,
                icon = Icons.Default.Sync,
                keywords = listOf("scan", "escanear", "sync", "sincronizar", "refresh", "actualizar", "library", "biblioteca", "canciones", "songs", "recargar"),
                trailingContent = if (isScanningLibrary) {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = if (isBlurActive) Color.White else MaterialTheme.colorScheme.primary
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
            ),
            SearchableSetting(
                id = "customization",
                title = context.getString(R.string.customization),
                description = context.getString(R.string.customization_desc),
                section = generalSectionTitle,
                icon = Icons.Default.Palette,
                keywords = listOf("theme", "tema", "dark", "oscuro", "light", "claro", "color", "colores", "palette", "paleta", "amoled", "pitch black", "negro", "blur", "desenfoque", "font", "fuente", "lyrics", "letras", "player", "reproductor", "waveform", "forma de onda", "lockscreen", "bloqueo", "art", "caratula", "personalizacion"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
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
                section = generalSectionTitle,
                icon = Icons.Default.Language,
                keywords = listOf("idioma", "language", "lenguaje", "traduccion", "translation", "locale"),
                onClick = { showLanguageDialog = true }
            ),

            // --- SUBCONFIGURACIONES: AJUSTES DE AUDIO ---
            SearchableSetting(
                id = "sub_equalizer",
                title = context.getString(R.string.eq_title),
                description = context.getString(R.string.audio_settings_desc),
                section = audioSettingsSectionTitle,
                parentTitle = audioSettingsSectionTitle,
                icon = Icons.Default.Equalizer,
                keywords = listOf("equalizer", "ecualizador", "eq", "bass", "graves", "virtualizer", "sonido", "audio", "presets", "frecuencias", "bandas", "ganancia", "volumen", "tuning"),
                onClick = { context.startActivity(Intent(context, EqualizerActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_hifi_badge",
                title = context.getString(R.string.hifi_badge),
                description = context.getString(R.string.hifi_badge_desc),
                section = audioSettingsSectionTitle,
                parentTitle = audioSettingsSectionTitle,
                icon = Icons.Default.HighQuality,
                keywords = listOf("hifi", "hi-res", "badge", "insignia", "etiqueta", "calidad", "audio", "lossless", "flac", "dsf", "dff", "alta resolucion"),
                onClick = { context.startActivity(Intent(context, AudioSettingsActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_32bit_float",
                title = context.getString(R.string.audio_32bit_float),
                description = context.getString(R.string.audio_32bit_float_desc),
                section = audioSettingsSectionTitle,
                parentTitle = audioSettingsSectionTitle,
                icon = Icons.Default.Equalizer,
                keywords = listOf("32bit", "32 bits", "float", "flotante", "precision", "procesamiento", "calidad", "dsp", "audio float"),
                onClick = { context.startActivity(Intent(context, AudioSettingsActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_exclusive_mode",
                title = context.getString(R.string.audio_exclusive_mode),
                description = context.getString(R.string.audio_exclusive_mode_desc),
                section = audioSettingsSectionTitle,
                parentTitle = audioSettingsSectionTitle,
                icon = Icons.Default.MusicNote,
                keywords = listOf("exclusivo", "exclusive", "focus", "enfoque de audio", "pausar", "otras apps", "reproduccion exclusiva"),
                onClick = { context.startActivity(Intent(context, AudioSettingsActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_stop_on_task_removed",
                title = context.getString(R.string.stop_on_task_removed),
                description = context.getString(R.string.stop_on_task_removed_desc),
                section = audioSettingsSectionTitle,
                parentTitle = audioSettingsSectionTitle,
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                keywords = listOf("recientes", "recent apps", "cerrar app", "stop", "detener", "segundo plano", "task removed"),
                onClick = { context.startActivity(Intent(context, AudioSettingsActivity::class.java)) }
            ),

            // --- SUBCONFIGURACIONES: PERSONALIZACIÓN ---
            SearchableSetting(
                id = "sub_app_icon",
                title = context.getString(R.string.app_icon_title),
                description = context.getString(R.string.customization_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Apps,
                keywords = listOf("icon", "icono", "app icon", "logo", "dinamico", "mono", "blanco", "negro", "launcher"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_custom_title",
                title = context.getString(R.string.custom_title),
                description = context.getString(R.string.customization_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Edit,
                keywords = listOf("titulo", "title", "nombre", "texto", "header", "cabecera", "personalizado"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_section_customization",
                title = context.getString(R.string.section_customization),
                description = context.getString(R.string.section_customization_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.ViewAgenda,
                keywords = listOf("secciones", "sections", "tabs", "pestanas", "ocultar pestanas", "mostrar pestanas", "canciones", "listas", "artistas", "albumes"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_use_custom_colors",
                title = context.getString(R.string.use_custom_colors),
                description = context.getString(R.string.use_custom_colors_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Palette,
                keywords = listOf("colores", "colors", "tema", "theme", "acento", "accent", "personalizado", "color dinamico"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_color_palette",
                title = context.getString(R.string.color_palette),
                description = context.getString(R.string.use_custom_colors_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Brush,
                keywords = listOf("paleta", "palette", "preset", "color", "tonos", "morado", "verde", "azul", "rojo", "amarillo"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_amoled_pitch_black",
                title = context.getString(R.string.amoled_pitch_black),
                description = context.getString(R.string.amoled_pitch_black_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.PhoneAndroid,
                keywords = listOf("amoled", "pitch black", "negro", "puro", "oscuro", "bateria", "oled", "pantalla amoled"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_show_ai_section",
                title = context.getString(R.string.show_ai_section),
                description = context.getString(R.string.show_ai_section_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.AutoAwesome,
                keywords = listOf("ai", "ia", "inteligencia artificial", "recomendaciones", "seccion ia", "smart"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_haptic_vibration",
                title = context.getString(R.string.haptic_vibration),
                description = context.getString(R.string.haptic_vibration_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.PhoneAndroid,
                keywords = listOf("vibracion", "vibration", "haptico", "haptic", "tactil", "vibrar", "botones"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_song_info",
                title = context.getString(R.string.song_info),
                description = context.getString(R.string.song_info_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.MusicNote,
                keywords = listOf("bitrate", "info", "informacion pista", "formato", "khz", "kbps", "flac", "calidad", "sample rate"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_cinematic_player",
                title = context.getString(R.string.cinematic_player),
                description = context.getString(R.string.cinematic_player_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.AutoAwesome,
                keywords = listOf("cinematic", "cinematico", "pantalla completa", "inmersivo", "reproductor cinemático"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_quick_player",
                title = context.getString(R.string.quick_player),
                description = context.getString(R.string.use_quick_player_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Speed,
                keywords = listOf("quick player", "reproductor rapido", "mini", "modal", "flotante", "ventana rapida"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_crossfade_time",
                title = context.getString(R.string.crossfade_time),
                description = context.getString(R.string.crossfade_time_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Tune,
                keywords = listOf("crossfade", "transicion", "desvanecer", "fundido", "segundos", "mezcla", "tiempo crossfade"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_seamless_looping",
                title = context.getString(R.string.seamless_looping),
                description = context.getString(R.string.seamless_looping_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.AllInclusive,
                keywords = listOf("seamless", "loop", "bucle", "continuo", "gapless", "repetir cancion", "sin pausas"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_scroll_to_top_button",
                title = context.getString(R.string.scroll_to_top_button),
                description = context.getString(R.string.scroll_to_top_button_desc),
                section = customizationSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.KeyboardArrowUp,
                keywords = listOf("scroll", "arriba", "volver arriba", "boton arriba", "subir", "desplazamiento"),
                onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
            ),

            // --- SUBCONFIGURACIONES: REPRODUCTOR ---
            SearchableSetting(
                id = "sub_player_customization",
                title = context.getString(R.string.player_customization),
                description = context.getString(R.string.player_customization_desc),
                section = playerSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Tune,
                keywords = listOf("player", "reproductor", "diseno", "aspecto", "personalizar reproductor", "interfaz"),
                onClick = { context.startActivity(Intent(context, PlayerCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_cover_shape",
                title = context.getString(R.string.cover_shape_title),
                description = context.getString(R.string.player_customization_desc),
                section = playerSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Album,
                keywords = listOf("cover shape", "forma caratula", "redondeada", "cuadrada", "circulo", "vinilo", "borde"),
                onClick = { context.startActivity(Intent(context, PlayerCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_cover_size",
                title = context.getString(R.string.cover_size),
                description = context.getString(R.string.player_customization_desc),
                section = playerSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Album,
                keywords = listOf("cover size", "tamano caratula", "pequeno", "mediano", "grande", "escala caratula"),
                onClick = { context.startActivity(Intent(context, PlayerCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_cover_spin",
                title = context.getString(R.string.cover_spin),
                description = context.getString(R.string.cover_spin_desc),
                section = playerSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Album,
                keywords = listOf("girar", "rotar", "spin", "animacion", "rotacion caratula", "giratorio"),
                onClick = { context.startActivity(Intent(context, PlayerCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_cover_vinyl",
                title = context.getString(R.string.cover_vinyl),
                description = context.getString(R.string.cover_vinyl_desc),
                section = playerSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Album,
                keywords = listOf("vinilo", "vinyl", "textura vinilo", "disco vinilo", "anillo vinilo"),
                onClick = { context.startActivity(Intent(context, PlayerCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_progress_bar_style",
                title = context.getString(R.string.progress_bar_style_title),
                description = context.getString(R.string.player_customization_desc),
                section = playerSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Equalizer,
                keywords = listOf("progress bar", "barra de progreso", "onda", "wave", "lineal", "wavy", "ondulada"),
                onClick = { context.startActivity(Intent(context, PlayerCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_controls_filled",
                title = context.getString(R.string.controls_filled),
                description = context.getString(R.string.controls_filled_desc),
                section = playerSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Tune,
                keywords = listOf("controls", "controles", "relleno", "filled", "iconos relleno", "solido", "botones solidos"),
                onClick = { context.startActivity(Intent(context, PlayerCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_controls_custom_color",
                title = context.getString(R.string.controls_custom_color),
                description = context.getString(R.string.controls_custom_color_desc),
                section = playerSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Palette,
                keywords = listOf("color controles", "custom controls color", "color botones", "paleta controles"),
                onClick = { context.startActivity(Intent(context, PlayerCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_player_background",
                title = context.getString(R.string.player_background_title),
                description = context.getString(R.string.player_gradient_background_desc),
                section = playerSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Palette,
                keywords = listOf("degradado", "gradient", "fondo reproductor", "ambient", "ambiental", "blur reproductor"),
                onClick = { context.startActivity(Intent(context, PlayerCustomizationActivity::class.java)) }
            ),

            // --- SUBCONFIGURACIONES: EFECTO BLUR ---
            SearchableSetting(
                id = "sub_blur_customization",
                title = context.getString(R.string.blur),
                description = context.getString(R.string.blur_desc),
                section = blurSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.BlurOn,
                keywords = listOf("blur", "desenfoque", "efecto blur", "vidrio", "glass", "fondo desenfocado"),
                onClick = { context.startActivity(Intent(context, BlurCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_enable_blur",
                title = context.getString(R.string.enable_blur),
                description = context.getString(R.string.enable_blur_desc),
                section = blurSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.BlurOn,
                keywords = listOf("activar blur", "enable blur", "desenfoque caratula", "fondo desenfocado", "album art blur"),
                onClick = { context.startActivity(Intent(context, BlurCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_blur_dark_mode",
                title = context.getString(R.string.blur_dark_mode),
                description = context.getString(R.string.blur_dark_mode_desc),
                section = blurSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.PhoneAndroid,
                keywords = listOf("blur oscuro", "blur dark", "modo oscuro blur", "desenfoque oscuro"),
                onClick = { context.startActivity(Intent(context, BlurCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_blur_light_mode",
                title = context.getString(R.string.blur_light_mode),
                description = context.getString(R.string.blur_light_mode_desc),
                section = blurSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.LightMode,
                keywords = listOf("blur claro", "blur light", "modo claro blur", "desenfoque claro"),
                onClick = { context.startActivity(Intent(context, BlurCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_blur_controls",
                title = context.getString(R.string.blur_controls),
                description = context.getString(R.string.blur_controls_desc),
                section = blurSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Palette,
                keywords = listOf("blur controls", "controles blur", "armonizar colores", "botones blur"),
                onClick = { context.startActivity(Intent(context, BlurCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_blur_cinematic_mode",
                title = context.getString(R.string.blur_cinematic_mode),
                description = context.getString(R.string.blur_cinematic_mode_desc),
                section = blurSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.AutoAwesome,
                keywords = listOf("blur cinematico", "cinematic blur", "desenfoque cinematico", "modo cinematico blur"),
                onClick = { context.startActivity(Intent(context, BlurCustomizationActivity::class.java)) }
            ),

            // --- SUBCONFIGURACIONES: FUENTES ---
            SearchableSetting(
                id = "sub_font_customization",
                title = context.getString(R.string.font_customization),
                description = context.getString(R.string.font_customization_desc),
                section = fontSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.TextFields,
                keywords = listOf("font", "fuente", "tipografia", "typography", "letras", "texto"),
                onClick = { context.startActivity(Intent(context, FontCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_font_size",
                title = context.getString(R.string.font_size),
                description = context.getString(R.string.font_customization_desc),
                section = fontSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.TextFields,
                keywords = listOf("tamano fuente", "font size", "escalar texto", "grande", "pequeno", "tamano letra"),
                onClick = { context.startActivity(Intent(context, FontCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_font_import",
                title = context.getString(R.string.font_import_button),
                description = context.getString(R.string.font_import_desc),
                section = fontSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Edit,
                keywords = listOf("importar fuente", "import font", "ttf", "otf", "archivo fuente", "fuente personalizada"),
                onClick = { context.startActivity(Intent(context, FontCustomizationActivity::class.java)) }
            ),

            // --- SUBCONFIGURACIONES: WIDGETS ---
            SearchableSetting(
                id = "sub_widget_customization",
                title = context.getString(R.string.widget_customization),
                description = context.getString(R.string.widget_customization_desc),
                section = widgetSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Widgets,
                keywords = listOf("widget", "widgets", "pantalla inicio", "home screen", "diseno widget"),
                onClick = { context.startActivity(Intent(context, WidgetCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_widget_blur",
                title = context.getString(R.string.widget_blur),
                description = context.getString(R.string.widget_blur_desc),
                section = widgetSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.BlurOn,
                keywords = listOf("widget blur", "desenfoque widget", "intensidad blur widget", "fondo widget"),
                onClick = { context.startActivity(Intent(context, WidgetCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_widget_solid_bg",
                title = context.getString(R.string.widget_solid_bg),
                description = context.getString(R.string.widget_solid_bg_desc),
                section = widgetSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Palette,
                keywords = listOf("fondo solido", "solid background", "color solido widget", "fondo widget"),
                onClick = { context.startActivity(Intent(context, WidgetCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_widget_circular_cover",
                title = context.getString(R.string.widget_circular_cover),
                description = context.getString(R.string.widget_circular_cover_desc),
                section = widgetSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Album,
                keywords = listOf("caratula circular", "circular cover widget", "disco widget", "redonda widget"),
                onClick = { context.startActivity(Intent(context, WidgetCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_widget_vinyl_cover",
                title = context.getString(R.string.widget_vinyl_cover),
                description = context.getString(R.string.widget_vinyl_cover_desc),
                section = widgetSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Album,
                keywords = listOf("efecto vinilo widget", "vinyl effect widget", "vinilo widget", "disco vinilo"),
                onClick = { context.startActivity(Intent(context, WidgetCustomizationActivity::class.java)) }
            ),

            // --- SUBCONFIGURACIONES: GESTOS ---
            SearchableSetting(
                id = "sub_gesture_customization",
                title = context.getString(R.string.gesture),
                description = context.getString(R.string.gesture_desc),
                section = gestureSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Gesture,
                keywords = listOf("gestos", "gesture", "swipe", "deslizar", "mini reproductor", "tocar"),
                onClick = { context.startActivity(Intent(context, GestureCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_enable_gestures",
                title = context.getString(R.string.enable_gestures),
                description = context.getString(R.string.enable_gestures_desc),
                section = gestureSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Gesture,
                keywords = listOf("activar gestos", "enable gestures", "gestos tactiles", "deslizamiento"),
                onClick = { context.startActivity(Intent(context, GestureCustomizationActivity::class.java)) }
            ),
            SearchableSetting(
                id = "sub_change_gesture",
                title = context.getString(R.string.change_gesture),
                description = context.getString(R.string.change_gesture_desc),
                section = gestureSectionTitle,
                parentTitle = customizationSectionTitle,
                icon = Icons.Default.Gesture,
                keywords = listOf("cambiar gesto", "swipe up", "deslizar arriba", "accion gesto", "mini reproductor"),
                onClick = { context.startActivity(Intent(context, GestureCustomizationActivity::class.java)) }
            ),

            // --- CANCIONES OCULTAS ---
            SearchableSetting(
                id = "hidden_songs",
                title = context.getString(R.string.hidden_songs),
                description = if (hiddenCount > 0) {
                    context.getString(R.string.hidden_songs_count, hiddenCount)
                } else {
                    context.getString(R.string.hidden_songs_desc)
                },
                section = hiddenSongsSectionTitle,
                icon = Icons.Default.VisibilityOff,
                keywords = listOf("ocultas", "hidden", "ocultar", "hide", "unhide", "desocultar", "canciones", "songs"),
                onClick = { context.startActivity(Intent(context, HiddenSongsActivity::class.java)) }
            ),

            // --- ORDENAMIENTO PREDETERMINADO ---
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
                section = defaultSortingSectionTitle,
                icon = Icons.Default.Album,
                keywords = listOf("album", "albums", "albumes", "sort", "orden", "ordenar", "clasificar", "default", "predeterminado"),
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
                section = defaultSortingSectionTitle,
                icon = Icons.Default.Person,
                keywords = listOf("artist", "artists", "artistas", "sort", "orden", "ordenar", "clasificar", "default", "predeterminado"),
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
                section = defaultSortingSectionTitle,
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                keywords = listOf("playlist", "playlists", "listas", "reproduccion", "sort", "orden", "ordenar", "default", "predeterminado"),
                onClick = { showPlaylistSortDialog = true }
            ),

            // --- COPIA DE SEGURIDAD ---
            SearchableSetting(
                id = "export_playlists",
                title = context.getString(R.string.export_playlists),
                description = context.getString(R.string.export_playlists_desc),
                section = backupSectionTitle,
                icon = Icons.Default.CloudDownload,
                keywords = listOf("export", "exportar", "backup", "copia", "seguridad", "save", "guardar", "json", "playlists", "respaldo"),
                onClick = { exportLauncher.launch("playlists_backup.json") }
            ),
            SearchableSetting(
                id = "import_playlists",
                title = context.getString(R.string.import_playlists),
                description = context.getString(R.string.import_playlists_desc),
                section = backupSectionTitle,
                icon = Icons.Default.Refresh,
                keywords = listOf("import", "importar", "restore", "restaurar", "backup", "copia", "cargar", "json", "respaldo"),
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
                keywords = listOf("auto", "sync", "sincronizacion", "automatica", "backup", "copia", "automatica", "respaldo"),
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
                },
                onClick = if (hasExportDestination) {
                    {
                        val newVal = !isAutoSyncEnabled
                        isAutoSyncEnabled = newVal
                        settingsManager.isAutoSyncBackupEnabled = newVal
                        if (newVal) {
                            backupManager.triggerAutoSync()
                        }
                    }
                } else null
            ),

            // --- SEGURIDAD ---
            SearchableSetting(
                id = "permissions",
                title = context.getString(R.string.permissions),
                description = context.getString(R.string.permissions_desc),
                section = securitySectionTitle,
                icon = Icons.Default.Security,
                keywords = listOf("permissions", "permisos", "storage", "almacenamiento", "notifications", "notificaciones", "security", "seguridad", "acceso", "archivos"),
                onClick = { context.startActivity(Intent(context, PermissionsActivity::class.java)) }
            ),

            // --- ACERCA DE ---
            SearchableSetting(
                id = "donate",
                title = context.getString(R.string.donate_title),
                description = context.getString(R.string.donate_desc),
                section = aboutSectionTitle,
                icon = Icons.Default.Favorite,
                keywords = listOf("donate", "donar", "donacion", "support", "apoyar", "colaborar", "patreon", "coffee", "dev", "desarrollo"),
                onClick = { showDonateDialog = true }
            ),
            SearchableSetting(
                id = "about",
                title = context.getString(R.string.about),
                description = "Lune v${BuildConfig.VERSION_NAME}",
                section = aboutSectionTitle,
                icon = Icons.Default.Info,
                keywords = listOf("about", "acerca", "info", "informacion", "version", "github", "source", "changelog", "cambios", "creditos"),
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
                    // General Section
                SettingsSection(title = stringResource(R.string.general)) {
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.audio_settings),
                        supportingText = stringResource(R.string.audio_settings_desc),
                        icon = Icons.Default.MusicNote,
                        position = SectionPosition.FIRST,
                        onClick = { context.startActivity(Intent(context, AudioSettingsActivity::class.java)) }
                    )

                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.keep_screen_on),
                        supportingText = stringResource(R.string.keep_screen_on_desc),
                        icon = Icons.Default.LightMode,
                        position = SectionPosition.MIDDLE,
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

                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.rescan_library),
                        supportingText = stringResource(R.string.rescan_library_desc),
                        icon = Icons.Default.Sync,
                        position = SectionPosition.MIDDLE,
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
                        headlineText = stringResource(R.string.customization),
                        supportingText = stringResource(R.string.customization_desc),
                        icon = Icons.Default.Palette,
                        position = SectionPosition.MIDDLE,
                        onClick = { context.startActivity(Intent(context, CustomizationActivity::class.java)) }
                    )
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.language),
                        supportingText = when(currentLanguage) {
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
                        position = SectionPosition.LAST,
                        onClick = { showLanguageDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hidden Songs Section
                SettingsSection(title = stringResource(R.string.hidden_songs)) {
                    val hiddenCount = settingsManager.hiddenSongIds.size
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.hidden_songs),
                        supportingText = if (hiddenCount > 0) {
                            stringResource(R.string.hidden_songs_count, hiddenCount)
                        } else {
                            stringResource(R.string.hidden_songs_desc)
                        },
                        icon = Icons.Default.VisibilityOff,
                        position = SectionPosition.SINGLE,
                        onClick = { context.startActivity(Intent(context, HiddenSongsActivity::class.java)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Default Sorting Section
                SettingsSection(title = stringResource(R.string.default_sorting)) {
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.default_album_sorting),
                        supportingText = getSortOptionLabel(defaultAlbumSort),
                        icon = Icons.Default.Album,
                        position = SectionPosition.FIRST,
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
                        position = SectionPosition.LAST,
                        onClick = { showPlaylistSortDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Backup Section
                SettingsSection(title = stringResource(R.string.backup)) {
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

                // Security Section
                SettingsSection(title = stringResource(R.string.security)) {
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.permissions),
                        supportingText = stringResource(R.string.permissions_desc),
                        icon = Icons.Default.Security,
                        position = SectionPosition.SINGLE,
                        onClick = { context.startActivity(Intent(context, PermissionsActivity::class.java)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // About Section
                SettingsSection(title = stringResource(R.string.about)) {
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.donate_title),
                        supportingText = stringResource(R.string.donate_desc),
                        icon = Icons.Default.Favorite,
                        position = SectionPosition.FIRST,
                        onClick = { showDonateDialog = true }
                    )

                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.about),
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
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
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
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
