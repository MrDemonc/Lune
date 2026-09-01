package com.demonlab.lune.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.SettingsManager
import androidx.compose.ui.graphics.Color
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.theme.LuneTheme

class PermissionsActivity : ComponentActivity() {
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
                PermissionsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = com.demonlab.lune.tools.SettingsManager.getInstance(context)
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                            stringResource(R.string.permissions),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
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
        val permissions = listOf(
            PermissionItem(
                title = stringResource(R.string.perm_record_audio_title),
                description = stringResource(R.string.perm_record_audio_desc),
                icon = Icons.Default.Mic,
                permission = android.Manifest.permission.RECORD_AUDIO
            ),
            PermissionItem(
                title = stringResource(R.string.perm_bluetooth_title),
                description = stringResource(R.string.perm_bluetooth_desc),
                icon = Icons.Default.Bluetooth,
                permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) 
                    android.Manifest.permission.BLUETOOTH_CONNECT else null
            ),
            PermissionItem(
                title = stringResource(R.string.perm_notifications_title),
                description = stringResource(R.string.perm_notifications_desc),
                icon = Icons.Default.Notifications,
                permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) 
                    android.Manifest.permission.POST_NOTIFICATIONS else null
            ),
            PermissionItem(
                title = stringResource(R.string.perm_storage_title),
                description = stringResource(R.string.perm_storage_desc),
                icon = Icons.Default.Folder,
                permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                    android.Manifest.permission.READ_MEDIA_AUDIO else android.Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            PermissionItem(
                title = stringResource(R.string.onboarding_perm_manage_files_title),
                description = stringResource(R.string.onboarding_perm_manage_files_desc),
                icon = Icons.Default.Folder,
                isSaf = true
            )
        )

        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { /* Logic to refresh UI if needed */ }

        val safLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    settingsManager.musicFolderUri = uri.toString()
                    settingsManager.isInitialFolderScanPending = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val initialSafUri = remember {
            try {
                val musicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
                if (musicDir.exists()) {
                    android.provider.DocumentsContract.buildTreeDocumentUri(
                        "com.android.externalstorage.documents",
                        "primary:${musicDir.name}"
                    )
                } else null
            } catch (_: Exception) { null }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(permissions.size) { index ->
                val permission = permissions[index]
                val position = when {
                    permissions.size == 1 -> PermissionSectionPosition.SINGLE
                    index == 0 -> PermissionSectionPosition.FIRST
                    index == permissions.size - 1 -> PermissionSectionPosition.LAST
                    else -> PermissionSectionPosition.MIDDLE
                }

                PermissionsPreferenceItem(
                    headlineText = permission.title,
                    supportingText = permission.description,
                    icon = permission.icon,
                    position = position,
                    onClick = {
                        if (permission.isSaf) {
                            safLauncher.launch(initialSafUri)
                        } else {
                            permission.permission?.let { launcher.launch(it) }
                        }
                    }
                )
            }
        }
    }
}
}

enum class PermissionSectionPosition {
    FIRST, MIDDLE, LAST, SINGLE
}

@Composable
fun PermissionsPreferenceItem(
    headlineText: String,
    supportingText: String,
    icon: ImageVector,
    position: PermissionSectionPosition,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = com.demonlab.lune.tools.SettingsManager.getInstance(context)
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))

    val shape = when (position) {
        PermissionSectionPosition.FIRST -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        PermissionSectionPosition.MIDDLE -> RoundedCornerShape(4.dp)
        PermissionSectionPosition.LAST -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
        PermissionSectionPosition.SINGLE -> RoundedCornerShape(28.dp)
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clickable(onClick = onClick),
        shape = shape,
        color = cardBg,
        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
    ) {
        ListItem(
            supportingContent = { 
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor
                ) 
            },
            leadingContent = {
                Surface(
                    shape = CircleShape,
                    color = iconBg,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        ) {
            Text(
                headlineText, 
                fontWeight = FontWeight.SemiBold,
                color = headlineColor
            ) 
        }
    }
}

data class PermissionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permission: String? = null,
    val isSaf: Boolean = false
)
