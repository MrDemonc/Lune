package com.demonlab.lune.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import com.demonlab.lune.ui.components.BouncySwitch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.demonlab.lune.R
import com.demonlab.lune.tools.SettingsManager
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    onStartClick: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    
    Crossfade(targetState = currentStep, label = "OnboardingCrossfade") { step ->
        when (step) {
            0 -> WelcomeStep(onStartClick = { currentStep = 1 })
            1 -> PermissionStep(onNext = { currentStep = 2 })
            2 -> BluetoothPermissionStep(onNext = { currentStep = 3 })
            3 -> NotificationPermissionStep(onNext = { currentStep = 4 })
            4 -> MusicPermissionStep(onNext = { currentStep = 5 })
            5 -> ManageFilesPermissionStep(onNext = { currentStep = 6 })
            6 -> FolderVisibilityStep(onNext = { currentStep = 7 })
            7 -> PermissionsReminderStep(onNext = { currentStep = 8 })
            8 -> SupportStep(onNext = { currentStep = 9 })
            9 -> FeaturesStep(onFinish = onStartClick)
        }
    }
}

@Composable
fun WelcomeStep(onStartClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val diamondsColor = if (isDark) Color.White else Color.Black
    val noteColor = if (isDark) Color.Black else Color.White

    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteLogoRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_diamonds),
                    contentDescription = null,
                    tint = diamondsColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotation)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_note),
                    contentDescription = null,
                    tint = noteColor,
                    modifier = Modifier.fillMaxSize(0.9f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.onboarding_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onStartClick,
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.onboarding_start_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val diamondsColor = if (isDark) Color.White else Color.Black
    val iconColor = if (isDark) Color.Black else Color.White

    var isPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPermissionGranted = granted
    }

    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteLogoRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_diamonds),
                    contentDescription = null,
                    tint = diamondsColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotation)
                )
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "${stringResource(R.string.onboarding_perm_audio_title)} - ${stringResource(R.string.onboarding_perm_required)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.onboarding_perm_audio_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (isPermissionGranted) {
                        onNext()
                    } else {
                        launcher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isPermissionGranted) stringResource(R.string.onboarding_next_button) else stringResource(R.string.onboarding_grant_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPermissionGranted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!isPermissionGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onNext) {
                    Text(
                        text = stringResource(R.string.onboarding_skip_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun BluetoothPermissionStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val diamondsColor = if (isDark) Color.White else Color.Black
    val iconColor = if (isDark) Color.Black else Color.White

    val bluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Manifest.permission.BLUETOOTH_CONNECT
    } else {
        null
    }

    var isPermissionGranted by remember {
        mutableStateOf(
            bluetoothPermission == null || ContextCompat.checkSelfPermission(context, bluetoothPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPermissionGranted = granted
    }

    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteLogoRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_diamonds),
                    contentDescription = null,
                    tint = diamondsColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotation)
                )
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "${stringResource(R.string.onboarding_perm_bluetooth_title)} - ${stringResource(R.string.onboarding_perm_required)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.onboarding_perm_bluetooth_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (isPermissionGranted) {
                        onNext()
                    } else {
                        bluetoothPermission?.let { launcher.launch(it) }
                    }
                },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isPermissionGranted) stringResource(R.string.onboarding_next_button) else stringResource(R.string.onboarding_grant_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPermissionGranted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!isPermissionGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onNext) {
                    Text(
                        text = stringResource(R.string.onboarding_skip_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationPermissionStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val diamondsColor = if (isDark) Color.White else Color.Black
    val iconColor = if (isDark) Color.Black else Color.White

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

    var isPermissionGranted by remember {
        mutableStateOf(
            notificationPermission == null || ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPermissionGranted = granted
    }

    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteLogoRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_diamonds),
                    contentDescription = null,
                    tint = diamondsColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotation)
                )
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "${stringResource(R.string.onboarding_perm_notifications_title)} - ${stringResource(R.string.onboarding_perm_required)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.onboarding_perm_notifications_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (isPermissionGranted) {
                        onNext()
                    } else {
                        notificationPermission?.let { launcher.launch(it) }
                    }
                },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isPermissionGranted) stringResource(R.string.onboarding_next_button) else stringResource(R.string.onboarding_grant_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPermissionGranted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MusicPermissionStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val diamondsColor = if (isDark) Color.White else Color.Black
    val iconColor = if (isDark) Color.Black else Color.White

    val musicPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var isPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, musicPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPermissionGranted = granted
    }

    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteLogoRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_diamonds),
                    contentDescription = null,
                    tint = diamondsColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotation)
                )
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "${stringResource(R.string.onboarding_perm_music_title)} - ${stringResource(R.string.onboarding_perm_required)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.onboarding_perm_music_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (isPermissionGranted) {
                        onNext()
                    } else {
                        launcher.launch(musicPermission)
                    }
                },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isPermissionGranted) stringResource(R.string.onboarding_next_button) else stringResource(R.string.onboarding_grant_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPermissionGranted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ManageFilesPermissionStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = SettingsManager.getInstance(context)
    val isDark = isSystemInDarkTheme()
    val diamondsColor = if (isDark) Color.White else Color.Black
    val iconColor = if (isDark) Color.Black else Color.White

    var isPermissionGranted by remember {
        mutableStateOf(settingsManager.musicFolderUri != null)
    }

    val launcher = rememberLauncherForActivityResult(
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
                isPermissionGranted = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteLogoRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_diamonds),
                    contentDescription = null,
                    tint = diamondsColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotation)
                )
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "${stringResource(R.string.onboarding_perm_manage_files_title)}${if (isPermissionGranted) "" else " - " + stringResource(R.string.onboarding_perm_required)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.onboarding_perm_manage_files_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (isPermissionGranted) {
                        onNext()
                    } else {
                        launcher.launch(null)
                    }
                },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isPermissionGranted) stringResource(R.string.onboarding_next_button) else stringResource(R.string.onboarding_grant_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPermissionGranted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FolderVisibilityStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = SettingsManager.getInstance(context)
    val isDark = isSystemInDarkTheme()
    val diamondsColor = if (isDark) Color.White else Color.Black
    val iconColor = if (isDark) Color.Black else Color.White

    var showAll by remember {
        mutableStateOf(settingsManager.showAllFoldersOnStart)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteLogoRotationVisibility")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_diamonds),
                    contentDescription = null,
                    tint = diamondsColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotation)
                )
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboarding_folder_visibility_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_folder_visibility_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                onClick = { showAll = !showAll },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_folder_visibility_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    BouncySwitch(
                        checked = showAll,
                        onCheckedChange = { showAll = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    settingsManager.showAllFoldersOnStart = showAll
                    onNext()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    stringResource(R.string.onboarding_next_button),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FeaturesStep(onFinish: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val diamondsColor = if (isDark) Color.White else Color.Black
    val noteColor = if (isDark) Color.Black else Color.White
    val colorPrimary = MaterialTheme.colorScheme.primary

    var isExploding by remember { mutableStateOf(false) }
    
    val explosionProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    val fadeProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    val features = listOf(
        FeatureItem(stringResource(R.string.onboarding_feature_hifi), Icons.Default.GraphicEq),

        FeatureItem(stringResource(R.string.onboarding_feature_title), Icons.Default.Title),
        FeatureItem(stringResource(R.string.onboarding_feature_lyrics), Icons.Default.Lyrics),
        FeatureItem(stringResource(R.string.onboarding_feature_mix), Icons.Default.AutoAwesome),
        FeatureItem(stringResource(R.string.onboarding_feature_more), Icons.Default.Add)
    )

    val pagerState = rememberPagerState(pageCount = { features.size })

    // Particle state
    val particles = remember {
        List(40) {
            val angle = Math.random() * 2 * Math.PI
            val speed = 120f + (Math.random() * 480f).toFloat()
            val vx = (Math.cos(angle) * speed).toFloat()
            val vy = (Math.sin(angle) * speed).toFloat()
            Particle(vx, vy)
        }
    }

    LaunchedEffect(isExploding) {
        if (isExploding) {
            explosionProgress.animateTo(
                1f,
                animationSpec = tween(durationMillis = 850, easing = LinearOutSlowInEasing)
            )
            delay(100)
            fadeProgress.animateTo(
                1f,
                animationSpec = tween(durationMillis = 650, easing = LinearEasing)
            )
            onFinish()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteLogoRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - fadeProgress.value
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(150.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logo_diamonds),
                            contentDescription = null,
                            tint = diamondsColor,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(rotationZ = rotation)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logo_note),
                            contentDescription = null,
                            tint = noteColor,
                            modifier = Modifier.fillMaxSize(0.9f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.onboarding_welcome_back),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colorPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.onboarding_listen_style),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Carousel Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentPadding = PaddingValues(horizontal = 64.dp),
                        pageSpacing = 16.dp
                    ) { page ->
                        val item = features[page]
                        val pageOffset = (
                                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                ).coerceIn(-1f, 1f)
                        
                        val scale = 1f - (Math.abs(pageOffset) * 0.15f)
                        val alpha = 1f - (Math.abs(pageOffset) * 0.7f)

                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                },
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, colorPrimary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorPrimary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = colorPrimary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pager Indicators
                    Row(
                        modifier = Modifier.height(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(features.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) colorPrimary else MaterialTheme.colorScheme.outlineVariant
                            val width by animateDpAsState(
                                targetValue = if (pagerState.currentPage == iteration) 24.dp else 8.dp,
                                label = "IndicatorWidth"
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = width, height = 8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Footer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Button(
                        onClick = { isExploding = true },
                        modifier = Modifier
                            .height(56.dp)
                            .width(220.dp)
                            .alpha(if (isExploding) 0f else 1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_finish_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isExploding) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(220.dp, 56.dp)) {
                            particles.forEach { particle ->
                                val progress = explosionProgress.value
                                val x = center.x + (particle.vx * progress)
                                val y = center.y + (particle.vy * progress)
                                val size = 18f * (1f - progress)
                                
                                drawCircle(
                                    color = colorPrimary,
                                    radius = size,
                                    center = androidx.compose.ui.geometry.Offset(x, y),
                                    alpha = 1f - progress
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class Particle(val vx: Float, val vy: Float)
data class FeatureItem(val title: String, val icon: ImageVector)

@Composable
fun PermissionsReminderStep(onNext: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val diamondsColor = MaterialTheme.colorScheme.primary
    val iconColor = MaterialTheme.colorScheme.onSurface

    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteLogoRotationReminder")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_diamonds),
                    contentDescription = null,
                    tint = diamondsColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotation)
                )
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Security,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboarding_reminder_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_reminder_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    stringResource(R.string.onboarding_reminder_button),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SupportStep(onNext: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val diamondsColor = if (isDark) Color.White else Color.Black
    val iconColor = if (isDark) Color.Black else Color.White

    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteLogoRotationSupport")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_diamonds),
                    contentDescription = null,
                    tint = diamondsColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotation)
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboarding_support_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_support_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    stringResource(R.string.onboarding_next_button),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

