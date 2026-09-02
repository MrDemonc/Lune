package com.demonlab.lune.ui.activities

import android.R.attr.icon
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demonlab.lune.BuildConfig
import com.demonlab.lune.R
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.theme.LuneTheme
import com.demonlab.lune.ui.utils.bounceClick
import androidx.compose.ui.platform.LocalUriHandler

class AboutActivity : ComponentActivity() {
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
                AboutScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))

    val scrollState = rememberScrollState()
    var showDonateDialog by remember { mutableStateOf(false) }

    AppBlurBackdrop(
        hasBlurBackground = hasBlurBackground,
        isDarkTheme = isDarkTheme,
        currentSong = currentSong
    ) {
        Scaffold(
            containerColor = if (hasBlurBackground && currentSong != null) Color.Transparent else MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = { },
                    actions = {
                        IconButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.bounceClick()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (hasBlurBackground && currentSong != null) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "System Settings",
                                        tint = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Section
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 10000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "diamond_rotation"
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.ic_logo_diamonds),
                        contentDescription = null,
                        tint = if (hasBlurBackground) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotation)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logo_note),
                        contentDescription = null,
                        tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(200.dp)
                    )
                }

                // Version
                Text(
                    text = "${stringResource(R.string.version)} ${BuildConfig.VERSION_NAME}",
                    fontWeight = FontWeight.Bold,
                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${stringResource(R.string.license)}: GPLv3",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Buttons
                val uriHandler = LocalUriHandler.current

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { 
                            uriHandler.openUri("https://github.com/MrDemonc/Lune/tree/main")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = "GitHub",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text="Github"
                        )
                    }
                    Button(
                        onClick = { showDonateDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                            contentColor = if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalCafe,
                            contentDescription = "Donation",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Donate",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showDonateDialog) {
                    DonateDialog(
                        onDismiss = { showDonateDialog = false },
                        hasBlurBackground = hasBlurBackground,
                        isDarkTheme = isDarkTheme
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Developer Info
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${stringResource(R.string.author)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stringResource(R.string.demon),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.creator_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Credits
                    HorizontalDivider(thickness = 0.5.dp, color = if (hasBlurBackground) Color.White.copy(alpha = 0.20f) else MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.credits),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.secondary濃(0.7f)
                    )
                    Text(
                        text = stringResource(R.string.desukia),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.credits_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = if (hasBlurBackground) Color.White.copy(alpha = 0.20f) else MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.open_source_libraries),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.secondary濃(0.7f)
                    )
                    
                    // Coil
                    Text(
                        text = "Coil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.coil_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Gson
                    Text(
                        text = "Gson",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.gson_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Room
                    Text(
                        text = "Room",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.room_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Jaudiotagger
                    Text(
                        text = "Jaudiotagger",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.jaudiotagger_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Jetpack Compose",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.compose_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Quicksand Font
                    Text(
                        text = "Quicksand Font",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.quicksand_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(32.dp))

            }
        }
    }
}

@Composable
fun MaterialTheme.secondary濃(alpha: Float): Color {
    return colorScheme.secondary.copy(alpha = alpha)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateDialog(
    onDismiss: () -> Unit,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var showMonero by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (hasBlurBackground) (if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.95f) else Color(0xFFF5F5F5).copy(alpha = 0.95f)) else MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = stringResource(R.string.donate_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { uriHandler.openUri("https://paypal.me/TommyZambrano") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                        contentColor = if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalCafe,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.paypal), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showMonero = !showMonero },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonetizationOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.monero), fontWeight = FontWeight.SemiBold)
                }

                if (showMonero) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.monero_address),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Monero Address", context.getString(R.string.monero_address))
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.bounceClick()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = stringResource(R.string.copied),
                                    tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onDismiss,
                shape = CircleShape,
                modifier = Modifier.bounceClick(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
            }
        }
    )
}
