package com.demonlab.lune.ui.activities

import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.MusicProvider
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.theme.LuneTheme
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val storageManager = context.getSystemService(android.content.Context.STORAGE_SERVICE) as StorageManager
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
                val name = volume.getDescription(context)
                volumes.add(StorageInfo(name, it.absolutePath, volume.isPrimary))
            }
        }
    } else {
        @Suppress("DEPRECATION")
        val primary = android.os.Environment.getExternalStorageDirectory()
        volumes.add(StorageInfo(
            context.getString(if (primary.exists()) R.string.internal_storage else R.string.sd_card),
            primary.absolutePath,
            true
        ))
    }
    return volumes
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    onBack: () -> Unit,
    playbackManager: PlaybackManager
) {
    val context = LocalContext.current
    val musicProvider = remember { MusicProvider(context) }
    val allSongs = remember { musicProvider.getCachedSongs() }
    val storageVolumes = remember { getStorageVolumes(context) }
    var currentDir by remember { mutableStateOf<String?>(null) }

    // Current directory contents
    val dirContents = remember(currentDir) {
        if (currentDir == null) {
            // Show storage volumes as roots
            null
        } else {
            val file = File(currentDir!!)
            if (!file.isDirectory) {
                null
            } else {
                val subdirs = file.listFiles { f -> f.isDirectory }?.sortedBy { it.name }?.map {
                    it.name to it.absolutePath
                } ?: emptyList()
                val songs = file.listFiles { f ->
                    f.isFile && f.extension.lowercase() in setOf("mp3", "flac", "wav", "aac", "ogg", "m4a", "wma")
                }?.sortedBy { it.name }?.map { it.name to it.absolutePath } ?: emptyList()
                subdirs to songs
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (currentDir == null) stringResource(R.string.folder_browser_title)
                        else currentDir!!.substringAfterLast("/")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentDir != null) {
                            val parent = File(currentDir!!).parent
                            currentDir = parent
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (currentDir == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(storageVolumes) { storage ->
                    Surface(
                        onClick = { currentDir = storage.path },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(48.dp)
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
                            Column {
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
                        }
                    }
                }
            }
        } else {
            val (subdirs, songs) = dirContents ?: (emptyList<Pair<String, String>>() to emptyList())

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (subdirs.isEmpty() && songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.no_songs_folder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                subdirs.forEach { (name, fullPath) ->
                    item {
                        ListItem(
                            headlineContent = { Text(name, fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                                    }
                                }
                            },
                            modifier = Modifier.clickable { currentDir = fullPath }
                        )
                    }
                }

                songs.forEach { (name, fullPath) ->
                    item {
                        ListItem(
                            headlineContent = { Text(name, fontWeight = FontWeight.Normal) },
                            leadingContent = {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(48.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(24.dp))
                                    }
                                }
                            },
                            modifier = Modifier.clickable {
                                                            val song = allSongs.find { it.path == fullPath }
                                                            if (song != null) {
                                                                playbackManager.play(song, listOf(song), fullPath.hashCode().toLong(), category = "FOLDERS")
                                                            }
                            }
                        )
                    }
                }
            }
        }
    }
}
