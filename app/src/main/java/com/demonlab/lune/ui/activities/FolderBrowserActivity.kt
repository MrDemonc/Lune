package com.demonlab.lune.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.storage.StorageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.MusicProvider
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.BouncySwitch
import com.demonlab.lune.ui.theme.LuneTheme
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.utils.triggerLightVibration
import java.io.File

class FolderBrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsManager = SettingsManager.getInstance(this)
        val playbackManager = PlaybackManager.getInstance(this)
        enableEdgeToEdge()
        setContent {
            val themeMode = settingsManager.themeMode
            val systemInDarkTheme = isSystemInDarkTheme()
            val targetDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> systemInDarkTheme
            }

            LuneTheme(
                darkTheme = targetDarkTheme,
                useCustomColors = settingsManager.useCustomColors,
                customColorPalette = settingsManager.customColorPalette,
                useAmoledPitchBlack = settingsManager.useAmoledPitchBlack
            ) {
                FolderBrowserScreen(
                    onBack = { finish() },
                    playbackManager = playbackManager
                )
            }
        }
    }
}

data class StorageInfo(val name: String, val path: String, val isPrimary: Boolean)

fun getStorageVolumes(context: android.content.Context): List<StorageInfo> {
    val volumes = mutableListOf<StorageInfo>()
    val storageManager = context.getSystemService(android.content.Context.STORAGE_SERVICE) as? StorageManager
    if (storageManager != null) {
        for (volume in storageManager.storageVolumes) {
            val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                volume.directory
            } else {
                @Suppress("DEPRECATION")
                if (volume.isPrimary) {
                    android.os.Environment.getExternalStorageDirectory()
                } else {
                    try {
                        val method = volume.javaClass.getMethod("getPath")
                        File(method.invoke(volume) as String)
                    } catch (_: Exception) { null }
                }
            }
            dir?.let {
                val name = volume.getDescription(context) ?: if (volume.isPrimary) "Internal Storage" else "SD Card"
                volumes.add(StorageInfo(name, it.absolutePath, volume.isPrimary))
            }
        }
    }
    if (volumes.isEmpty()) {
        val primaryDir = android.os.Environment.getExternalStorageDirectory()
        if (primaryDir != null && primaryDir.exists()) {
            volumes.add(StorageInfo("Internal Storage", primaryDir.absolutePath, true))
        }
    }
    return volumes
}

fun isFolderExcluded(folderName: String, folderPath: String?, hiddenFolders: Set<String>): Boolean {
    if (hiddenFolders.contains(folderName)) return true
    if (folderPath != null && hiddenFolders.contains(folderPath)) return true
    if (folderPath != null && hiddenFolders.any { hidden -> folderPath.startsWith(hidden) || folderPath.equals(hidden, ignoreCase = true) }) return true
    return false
}

fun toggleFolder(folderName: String, folderPath: String?, isIncluded: Boolean, settingsManager: SettingsManager) {
    val newHidden = settingsManager.hiddenFolders.toMutableSet()
    if (isIncluded) {
        newHidden.remove(folderName)
        if (folderPath != null) {
            newHidden.remove(folderPath)
            newHidden.removeAll { it == folderName || (folderPath.isNotEmpty() && (it == folderPath || folderPath.startsWith(it))) }
        }
    } else {
        newHidden.add(folderName)
        if (folderPath != null) {
            newHidden.add(folderPath)
        }
    }
    settingsManager.hiddenFolders = newHidden
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    onBack: () -> Unit,
    playbackManager: PlaybackManager
) {
    val context = LocalContext.current
    val vibrator = remember(context) { context.getSystemService(Vibrator::class.java) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val musicProvider = remember { MusicProvider(context) }
    val allSongs = remember { musicProvider.getCachedSongs() }
    val storageVolumes = remember { getStorageVolumes(context) }
    var currentDir by remember { mutableStateOf<String?>(null) }
    val hiddenFolders = settingsManager.hiddenFolders

    // Agrupación de canciones por nombre de carpeta
    val detectedFolders = remember(allSongs) {
        allSongs.groupBy { it.folderName }
            .map { (name, songs) ->
                val samplePath = songs.firstOrNull()?.path?.substringBeforeLast("/")
                Triple(name, songs.size, samplePath)
            }
            .sortedByDescending { it.second }
    }

    // Contenido del directorio actual
    val dirContents = remember(currentDir) {
        if (currentDir == null) {
            null
        } else {
            val file = File(currentDir!!)
            if (!file.isDirectory) {
                null
            } else {
                val subdirs = file.listFiles { f -> f.isDirectory && !f.name.startsWith(".") }
                    ?.sortedBy { it.name.lowercase() }
                    ?.map { it.name to it.absolutePath } ?: emptyList()
                val songs = file.listFiles { f ->
                    f.isFile && f.extension.lowercase() in setOf("mp3", "flac", "wav", "aac", "ogg", "m4a", "wma", "opus", "alac")
                }?.sortedBy { it.name.lowercase() }?.map { it.name to it.absolutePath } ?: emptyList()
                subdirs to songs
            }
        }
    }

    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                settingsManager.musicFolderUri = uri.toString()
                settingsManager.isInitialFolderScanPending = true
                vibrator?.triggerLightVibration()
                val folderName = uri.lastPathSegment?.substringAfterLast(":") ?: uri.lastPathSegment ?: ""
                Toast.makeText(
                    context,
                    context.getString(R.string.folder_selected_toast, folderName),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val navigateUp: () -> Unit = {
        if (currentDir != null) {
            val isAtRootVolume = storageVolumes.any { it.path == currentDir }
            if (isAtRootVolume) {
                currentDir = null
            } else {
                val parent = File(currentDir!!).parent
                if (parent == null || !storageVolumes.any { currentDir!!.startsWith(it.path) && parent.startsWith(it.path) }) {
                    currentDir = null
                } else {
                    currentDir = parent
                }
            }
        } else {
            onBack()
        }
    }

    BackHandler(enabled = currentDir != null) {
        navigateUp()
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (currentDir == null) stringResource(R.string.manage_audio_folders)
                            else currentDir!!.substringAfterLast("/"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentDir != null) {
                            Text(
                                text = currentDir!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { safLauncher.launch(null) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = stringResource(R.string.system_folder_picker),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (currentDir != null) {
                val currentFile = File(currentDir!!)
                val currentName = currentFile.name
                ExtendedFloatingActionButton(
                    onClick = {
                        toggleFolder(currentName, currentDir, true, settingsManager)
                        settingsManager.musicFolderUri = currentDir
                        settingsManager.isInitialFolderScanPending = true
                        vibrator?.triggerLightVibration()
                        Toast.makeText(
                            context,
                            context.getString(R.string.folder_selected_toast, currentName),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    text = { Text(stringResource(R.string.select_this_folder), fontWeight = FontWeight.SemiBold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.bounceClick()
                )
            }
        }
    ) { padding ->
        if (currentDir == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tarjeta explicativa
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    stringResource(R.string.manage_audio_folders),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    stringResource(R.string.manage_audio_folders_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Sección 1: Carpetas de música detectadas
                if (detectedFolders.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.detected_folders),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 4.dp)
                        )
                    }

                    items(detectedFolders) { (name, count, path) ->
                        val isExcluded = isFolderExcluded(name, path, hiddenFolders)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (path != null && File(path).exists()) {
                                            currentDir = path
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isExcluded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (isExcluded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.songs_count_format, count),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isExcluded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                BouncySwitch(
                                    checked = !isExcluded,
                                    onCheckedChange = { isIncluded ->
                                        vibrator?.triggerLightVibration()
                                        toggleFolder(name, path, isIncluded, settingsManager)
                                    }
                                )
                            }
                        }
                    }
                }

                // Sección 2: Explorador de almacenamiento
                item {
                    Text(
                        text = stringResource(R.string.storage_explorer),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 4.dp)
                    )
                }

                items(storageVolumes) { storage ->
                    Surface(
                        onClick = { currentDir = storage.path },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (storage.isPrimary) Icons.Default.PhoneAndroid else Icons.Default.SdCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = storage.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = storage.path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Sección 3: Selector de carpetas del sistema (SAF)
                item {
                    Surface(
                        onClick = { safLauncher.launch(null) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.system_folder_picker),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.onboarding_perm_manage_files_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val (subdirs, songs) = dirContents ?: (emptyList<Pair<String, String>>() to emptyList())
            val currentFile = remember(currentDir) { File(currentDir!!) }
            val currentName = remember(currentFile) { currentFile.name }
            val isCurrentExcluded = isFolderExcluded(currentName, currentDir, hiddenFolders)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tarjeta de estado de la carpeta actual
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isCurrentExcluded) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isCurrentExcluded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isCurrentExcluded) stringResource(R.string.folder_excluded) else stringResource(R.string.folder_included),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCurrentExcluded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                            BouncySwitch(
                                checked = !isCurrentExcluded,
                                onCheckedChange = { isIncluded ->
                                    vibrator?.triggerLightVibration()
                                    toggleFolder(currentName, currentDir, isIncluded, settingsManager)
                                }
                            )
                        }
                    }
                }

                if (subdirs.isEmpty() && songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.no_songs_folder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Subdirectorios
                items(subdirs) { (name, fullPath) ->
                    val isExcluded = isFolderExcluded(name, fullPath, hiddenFolders)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentDir = fullPath }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isExcluded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (isExcluded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isExcluded) stringResource(R.string.folder_excluded) else stringResource(R.string.folder_included),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isExcluded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BouncySwitch(
                                checked = !isExcluded,
                                onCheckedChange = { isIncluded ->
                                    vibrator?.triggerLightVibration()
                                    toggleFolder(name, fullPath, isIncluded, settingsManager)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Archivos de audio en el directorio actual
                items(songs) { (name, fullPath) ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val song = allSongs.find { it.path == fullPath }
                                    if (song != null) {
                                        playbackManager.play(song, listOf(song), fullPath.hashCode().toLong(), category = "FOLDERS")
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
