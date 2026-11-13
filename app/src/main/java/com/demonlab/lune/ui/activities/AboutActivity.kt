package com.demonlab.lune.ui.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.demonlab.lune.BuildConfig
import com.demonlab.lune.R
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.components.rememberBlurSheetColors
import com.demonlab.lune.ui.theme.LuneTheme
import com.demonlab.lune.ui.theme.quicksand
import com.demonlab.lune.ui.utils.bounceClick
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private data class OpenSourceLibrary(
    val name: String,
    val description: String,
    val license: String,
    val icon: ImageVector,
    val url: String
)

private data class Contributor(
    val login: String = "",
    val avatar_url: String = "",
    val html_url: String = "",
    val contributions: Int = 0
)

private fun loadContributors(context: Context): List<Contributor> {
    return runCatching {
        context.assets.open("contributors.json").use { inputStream ->
            val json = inputStream.bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Contributor>>() {}.type
            val list = Gson().fromJson<List<Contributor>>(json, type) ?: emptyList()
            list.filter {
                !it.login.equals("MrDemonc", ignoreCase = true) &&
                !it.login.equals("desukia", ignoreCase = true)
            }
        }
    }.getOrElse { emptyList() }
}

private fun loadAvatarBitmap(context: Context, login: String): ImageBitmap? {
    return runCatching {
        context.assets.open("avatars/$login.png").use { inputStream ->
            BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
        }
    }.getOrNull()
}

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
    val uriHandler = LocalUriHandler.current
    val currentSong = playbackManager.currentSong
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))

    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showDonateDialog by remember { mutableStateOf(false) }
    val contributors = remember { loadContributors(context) }

    val cardBg = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val iconBg = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val iconTint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
    val headlineColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
    val supportingColor = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

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
                            text = stringResource(R.string.about),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { (context as? android.app.Activity)?.finish() },
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
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Hero Identity Card (Material 3 Expressive)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = cardBg,
                    tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Animated Logo Section
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(130.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 12000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "diamond_rotation"
                            )

                            Icon(
                                painter = painterResource(id = R.drawable.ic_logo_diamonds),
                                contentDescription = null,
                                tint = if (hasBlurBackground) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(rotation)
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_logo_note),
                                contentDescription = null,
                                tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(130.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Lune",
                            fontFamily = quicksand,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium,
                            color = headlineColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Chips Row: Version + License
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.bounceClick()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (hasBlurBackground) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.bounceClick()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "GPL-3.0",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    runCatching { uriHandler.openUri("https://github.com/MrDemonc/Lune/tree/main") }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .bounceClick(),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    contentColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_github),
                                    contentDescription = "GitHub",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GitHub",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    runCatching { uriHandler.openUri("https://mrdemonc.github.io/Lune/") }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .bounceClick(),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.22f)) else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    contentColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_web),
                                    contentDescription = stringResource(R.string.website),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.website),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { showDonateDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .bounceClick(),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                    contentColor = if (hasBlurBackground) Color.Black else MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Donate",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Donate",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Author Section (MrDemonc)
                SettingsSection(title = stringResource(R.string.author)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .bounceClick()
                            .clickable {
                                runCatching { uriHandler.openUri("https://github.com/MrDemonc") }
                            },
                        shape = RoundedCornerShape(24.dp),
                        color = cardBg,
                        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.avatar_mrdemonc),
                                contentDescription = stringResource(R.string.demon),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.demon),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = headlineColor
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = stringResource(R.string.creator_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = supportingColor
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = supportingColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Credits Section (Desukia)
                SettingsSection(title = stringResource(R.string.credits)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .bounceClick()
                            .clickable {
                                runCatching { uriHandler.openUri("https://github.com/desukia") }
                            },
                        shape = RoundedCornerShape(24.dp),
                        color = cardBg,
                        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.avatar_desukia),
                                contentDescription = stringResource(R.string.desukia),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.desukia),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = headlineColor
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = stringResource(R.string.credits_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = supportingColor
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = supportingColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 4. Contributors Section (Auto-updated from GitHub on build)
                if (contributors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsSection(title = stringResource(R.string.contributors)) {
                        contributors.forEachIndexed { index, contributor ->
                            val position = when {
                                contributors.size == 1 -> SectionPosition.SINGLE
                                index == 0 -> SectionPosition.FIRST
                                index == contributors.lastIndex -> SectionPosition.LAST
                                else -> SectionPosition.MIDDLE
                            }
                            val shape = when (position) {
                                SectionPosition.FIRST -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                SectionPosition.MIDDLE -> RoundedCornerShape(4.dp)
                                SectionPosition.LAST -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                                SectionPosition.SINGLE -> RoundedCornerShape(28.dp)
                            }

                            val avatarBitmap = remember(contributor.login) {
                                loadAvatarBitmap(context, contributor.login)
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                                    .clip(shape)
                                    .bounceClick()
                                    .clickable {
                                        runCatching {
                                            uriHandler.openUri(
                                                contributor.html_url.ifEmpty { "https://github.com/${contributor.login}" }
                                            )
                                        }
                                    },
                                shape = shape,
                                color = cardBg,
                                tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (avatarBitmap != null) {
                                        Image(
                                            bitmap = avatarBitmap,
                                            contentDescription = contributor.login,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = iconBg,
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = iconTint,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contributor.login,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = headlineColor
                                        )
                                        Text(
                                            text = stringResource(R.string.contributions_count, contributor.contributions),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = supportingColor
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = supportingColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Open Source Libraries
                SettingsSection(title = stringResource(R.string.open_source_libraries)) {
                    val libraries = listOf(
                        OpenSourceLibrary(
                            name = "Coil",
                            description = stringResource(R.string.coil_desc).substringBefore("\n"),
                            license = "Apache 2.0",
                            icon = Icons.Default.Image,
                            url = "https://github.com/coil-kt/coil"
                        ),
                        OpenSourceLibrary(
                            name = "Gson",
                            description = stringResource(R.string.gson_desc).substringBefore("\n"),
                            license = "Apache 2.0",
                            icon = Icons.Default.DataObject,
                            url = "https://github.com/google/gson"
                        ),
                        OpenSourceLibrary(
                            name = "Room",
                            description = stringResource(R.string.room_desc).substringBefore("\n"),
                            license = "Apache 2.0",
                            icon = Icons.Default.Storage,
                            url = "https://developer.android.com/training/data-storage/room"
                        ),
                        OpenSourceLibrary(
                            name = "Jaudiotagger",
                            description = stringResource(R.string.jaudiotagger_desc).substringBefore("\n"),
                            license = "LGPL 2.1",
                            icon = Icons.Default.Audiotrack,
                            url = "https://www.jthink.net/jaudiotagger/"
                        ),
                        OpenSourceLibrary(
                            name = "Jetpack Compose",
                            description = stringResource(R.string.compose_desc).substringBefore("\n"),
                            license = "Apache 2.0",
                            icon = Icons.Default.Widgets,
                            url = "https://developer.android.com/jetpack/compose"
                        ),
                        OpenSourceLibrary(
                            name = "Quicksand Font",
                            description = stringResource(R.string.quicksand_desc).substringBefore("\n"),
                            license = "OFL 1.1",
                            icon = Icons.Default.FontDownload,
                            url = "https://fonts.google.com/specimen/Quicksand"
                        )
                    )

                    libraries.forEachIndexed { index, lib ->
                        val position = when {
                            index == 0 -> SectionPosition.FIRST
                            index == libraries.lastIndex -> SectionPosition.LAST
                            else -> SectionPosition.MIDDLE
                        }
                        SettingsPreferenceItem(
                            headlineText = lib.name,
                            supportingText = lib.description,
                            icon = lib.icon,
                            position = position,
                            onClick = { runCatching { uriHandler.openUri(lib.url) } },
                            trailingContent = {
                                Surface(
                                    shape = CircleShape,
                                    color = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = lib.license,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showDonateDialog) {
            DonateDialog(
                onDismiss = { showDonateDialog = false },
                hasBlurBackground = hasBlurBackground,
                isDarkTheme = isDarkTheme,
                currentSong = currentSong
            )
        }
    }
}

@Composable
fun DonateDialog(
    onDismiss: () -> Unit,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = false,
    currentSong: Song? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val song = currentSong ?: remember { PlaybackManager.getInstance(context).currentSong }
    val blurColors = rememberBlurSheetColors(song)
    var showMonero by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = song,
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
                    // Header Badge
                    Surface(
                        shape = CircleShape,
                        color = if (blurColors.hasBlur) {
                            blurColors.primaryTint.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.donate_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = quicksand,
                        color = blurColors.textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.donate_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = blurColors.textSecondaryColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // PayPal Option Card
                    Surface(
                        onClick = { uriHandler.openUri("https://paypal.me/TommyZambrano") },
                        shape = RoundedCornerShape(20.dp),
                        color = if (blurColors.hasBlur) {
                            blurColors.itemContainerColor
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (blurColors.hasBlur) {
                                    blurColors.primaryTint.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalCafe,
                                        contentDescription = null,
                                        tint = blurColors.primaryTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.paypal),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = blurColors.textColor
                                )
                                Text(
                                    text = "paypal.me/TommyZambrano",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = blurColors.textSecondaryColor
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = blurColors.textSecondaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Monero Option Card (Expandable)
                    Surface(
                        onClick = { showMonero = !showMonero },
                        shape = RoundedCornerShape(20.dp),
                        color = if (blurColors.hasBlur) {
                            blurColors.itemContainerColor
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (blurColors.hasBlur) {
                                        blurColors.primaryTint.copy(alpha = 0.2f)
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    },
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.MonetizationOn,
                                            contentDescription = null,
                                            tint = blurColors.primaryTint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.monero),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = blurColors.textColor
                                    )
                                    Text(
                                        text = "XMR Crypto",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = blurColors.textSecondaryColor
                                    )
                                }

                                Icon(
                                    imageVector = if (showMonero) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    tint = blurColors.textSecondaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = showMonero,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (blurColors.hasBlur) {
                                            if (blurColors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                                        } else {
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.monero_address),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = blurColors.textColor,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
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
                                                    tint = blurColors.primaryTint
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Cancel / Close button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (blurColors.hasBlur) {
                                blurColors.itemContainerColor
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = blurColors.textColor
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
