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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import kotlinx.coroutines.launch
import com.demonlab.lune.tools.PlaylistBackupManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.theme.LuneTheme
import androidx.compose.ui.graphics.vector.ImageVector
import com.demonlab.lune.BuildConfig

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
            
            LuneTheme(darkTheme = targetDarkTheme) {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var showWhatsapp by remember { mutableStateOf(settingsManager.showWhatsappAudio) }
    var showHiFi by remember { mutableStateOf(settingsManager.enableHiFi) }
    var showDownloadCovers by remember { mutableStateOf(settingsManager.downloadCovers) }
    var isCinematicEnabled by remember { mutableStateOf(settingsManager.isCinematicPlayerEnabled) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCustomTitleDialog by remember { mutableStateOf(false) }
    val currentLanguage = settingsManager.language
    var customTitle by remember { mutableStateOf(settingsManager.customTitle) }
    val scope = rememberCoroutineScope()
    val backupManager = remember { PlaylistBackupManager(context) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            confirmButton = {},
            title = { Text(stringResource(R.string.select_language)) },
            text = {
                Column {
                    val languages = listOf(
                        "system" to stringResource(R.string.lang_system),
                        "en" to stringResource(R.string.lang_english),
                        "es" to stringResource(R.string.lang_spanish),
                        "pt-BR" to stringResource(R.string.lang_portuguese)
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
                                onClick = null 
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showCustomTitleDialog) {
        var tempTitle by remember { mutableStateOf(customTitle) }
        AlertDialog(
            onDismissRequest = { showCustomTitleDialog = false },
            title = { Text(stringResource(R.string.custom_title)) },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("Título") },
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
                        stringResource(R.string.settings),
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
                            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
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
                colors = TopAppBarDefaults.largeTopAppBarColors(
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
            // General Section
            SettingsSection(title = stringResource(R.string.general)) {
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.hifi_audio),
                    supportingText = stringResource(R.string.hifi_desc),
                    icon = Icons.Default.MusicNote,
                    position = SectionPosition.FIRST,
                    trailingContent = {
                        Switch(
                            checked = showHiFi,
                            onCheckedChange = {
                                showHiFi = it
                                settingsManager.enableHiFi = it
                            }
                        )
                    }
                )
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.download_covers),
                    supportingText = stringResource(R.string.download_covers_desc),
                    icon = Icons.Default.CloudDownload,
                    position = SectionPosition.MIDDLE,
                    trailingContent = {
                        Switch(
                            checked = showDownloadCovers,
                            onCheckedChange = {
                                showDownloadCovers = it
                                settingsManager.downloadCovers = it
                            }
                        )
                    }
                )
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.cinematic_player),
                    supportingText = stringResource(R.string.cinematic_player_desc),
                    icon = Icons.Default.AutoAwesome,
                    position = SectionPosition.MIDDLE,
                    trailingContent = {
                        Switch(
                            checked = isCinematicEnabled,
                            onCheckedChange = {
                                isCinematicEnabled = it
                                settingsManager.isCinematicPlayerEnabled = it
                            }
                        )
                    }
                )
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.custom_title),
                    supportingText = if (customTitle.isEmpty()) "Lune" else customTitle,
                    icon = Icons.Default.Edit,
                    position = SectionPosition.MIDDLE,
                    onClick = { showCustomTitleDialog = true }
                )
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.language),
                    supportingText = when(currentLanguage) {
                        "en" -> stringResource(R.string.lang_english)
                        "es" -> stringResource(R.string.lang_spanish)
                        "pt-BR" -> stringResource(R.string.lang_portuguese)
                        else -> stringResource(R.string.lang_system)
                    },
                    icon = Icons.Default.Language,
                    position = SectionPosition.LAST,
                    onClick = { showLanguageDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Backup Section
            SettingsSection(title = stringResource(R.string.backup)) {
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
                    position = SectionPosition.LAST,
                    onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream")) }
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
                    headlineText = stringResource(R.string.about),
                    icon = Icons.Default.Info,
                    position = SectionPosition.SINGLE,
                    onClick = { context.startActivity(Intent(context, AboutActivity::class.java)) }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

enum class SectionPosition {
    FIRST, MIDDLE, LAST, SINGLE
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
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
    val shape = when (position) {
        SectionPosition.FIRST -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        SectionPosition.MIDDLE -> RoundedCornerShape(4.dp)
        SectionPosition.LAST -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
        SectionPosition.SINGLE -> RoundedCornerShape(28.dp)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        ListItem(
            headlineContent = { Text(headlineText, fontWeight = FontWeight.Bold) },
            supportingContent = supportingText?.let { { Text(it) } },
            leadingContent = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            trailingContent = trailingContent,
            colors = ListItemDefaults.colors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )
    }
}
