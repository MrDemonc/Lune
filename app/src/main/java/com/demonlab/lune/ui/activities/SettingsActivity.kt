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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LightMode
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
    var showHiFi by remember { mutableStateOf(settingsManager.enableHiFi) }
    var keepScreenOn by remember { mutableStateOf(settingsManager.keepScreenOn) }

    var isCinematicEnabled by remember { mutableStateOf(settingsManager.isCinematicPlayerEnabled) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showBackupWarning by remember { mutableStateOf(settingsManager.showBackupWarning) }
    val currentLanguage = settingsManager.language
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
                        "pt-BR" to stringResource(R.string.lang_portuguese),
                        "fr" to stringResource(R.string.lang_french),
                        "zh" to stringResource(R.string.lang_chinese),
                        "de" to stringResource(R.string.lang_german),
                        "ru" to stringResource(R.string.lang_russian),
                        "fa" to stringResource(R.string.lang_persian)
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
            // General Section
            SettingsSection(title = stringResource(R.string.general)) {
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.hifi_audio),
                    supportingText = stringResource(R.string.hifi_desc),
                    icon = Icons.Default.MusicNote,
                    position = SectionPosition.FIRST,
                    trailingContent = {
                        BouncySwitch(
                            checked = showHiFi,
                            onCheckedChange = {
                                showHiFi = it
                                settingsManager.enableHiFi = it
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (showHiFi) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
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



@Composable
fun BackupWarningCard(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFDE8E8), // Beautiful soft red background (pastel red)
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF8B4B4))
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
                tint = Color(0xFF9B1C1C), // Stronger red for icon
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(com.demonlab.lune.R.string.backup_warning_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF9B1C1C) // Nice contrast text color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(com.demonlab.lune.R.string.backup_warning_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9B1C1C).copy(alpha = 0.85f)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFBD5D5)) // Slightly darker soft red for button background
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF9B1C1C),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
