package com.demonlab.lune.ui.activities

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demonlab.lune.R
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.theme.LuneTheme
import com.demonlab.lune.ui.theme.quicksand
import com.demonlab.lune.ui.utils.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

class FontCustomizationActivity : ComponentActivity() {
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

            LuneTheme(
                darkTheme = targetDarkTheme,
                dynamicColor = true,
                useCustomColors = settingsManager.useCustomColors,
                customColorPalette = settingsManager.customColorPalette,
                useAmoledPitchBlack = settingsManager.useAmoledPitchBlack
            ) {
                FontCustomizationScreen(
                    onBack = { finish() },
                    settingsManager = settingsManager
                )
            }
        }
    }
}

private data class SystemFontChoice(
    val id: String,
    val nameRes: Int,
    val fontFamily: FontFamily
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontCustomizationScreen(
    onBack: () -> Unit,
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong

    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val isBlurEnabled = settingsManager.isBlurEnabled
    val isBlurDarkMode = settingsManager.isBlurDarkMode
    val isBlurLightMode = settingsManager.isBlurLightMode
    val hasBlurBackground = isBlurEnabled && ((isDarkTheme && isBlurDarkMode) || (!isDarkTheme && isBlurLightMode))

    var importedFonts by remember { mutableStateOf(settingsManager.getImportedFonts()) }
    var fontToDelete by remember { mutableStateOf<File?>(null) }

    val systemFonts = remember {
        listOf(
            SystemFontChoice("quicksand", R.string.font_default_lune, quicksand),
            SystemFontChoice("system", R.string.font_system_default, FontFamily.Default),
            SystemFontChoice("sans_serif", R.string.font_sans_serif, FontFamily.SansSerif),
            SystemFontChoice("serif", R.string.font_serif, FontFamily.Serif),
            SystemFontChoice("monospace", R.string.font_monospace, FontFamily.Monospace),
            SystemFontChoice("cursive", R.string.font_cursive, FontFamily.Cursive)
        )
    }

    val importFontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val rawName = getFileNameFromUri(context, uri) ?: "font_${System.currentTimeMillis()}.ttf"
                    val safeName = File(rawName).name
                    val ext = safeName.substringAfterLast('.', "").lowercase()
                    val targetName = if (ext in listOf("ttf", "otf", "ttc")) safeName else "$safeName.ttf"

                    val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}.${if (ext.isNotEmpty()) ext else "ttf"}")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (isValidFont(tempFile)) {
                        val destinationFile = File(settingsManager.getCustomFontsDir(), targetName)
                        tempFile.copyTo(destinationFile, overwrite = true)
                        tempFile.delete()

                        withContext(Dispatchers.Main) {
                            settingsManager.fontType = "custom"
                            settingsManager.customFontFileName = targetName
                            importedFonts = settingsManager.getImportedFonts()
                            Toast.makeText(context, context.getString(R.string.font_import_success), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        tempFile.delete()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.font_import_invalid), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.font_import_invalid), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

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
                            stringResource(R.string.font_customization),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (hasBlurBackground && currentSong != null) Color.White else MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
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
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Section 1: Font Size & Live Preview
                SettingsSection(title = stringResource(R.string.font_preview)) {
                    FontPreviewCard(
                        hasBlurBackground = hasBlurBackground,
                        isDarkTheme = isDarkTheme,
                        currentFontName = when (settingsManager.fontType) {
                            "quicksand" -> stringResource(R.string.font_default_lune)
                            "system" -> stringResource(R.string.font_system_default)
                            "sans_serif" -> stringResource(R.string.font_sans_serif)
                            "serif" -> stringResource(R.string.font_serif)
                            "monospace" -> stringResource(R.string.font_monospace)
                            "cursive" -> stringResource(R.string.font_cursive)
                            "custom" -> settingsManager.customFontFileName.substringBeforeLast('.')
                            else -> stringResource(R.string.font_default_lune)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsSection(title = stringResource(R.string.font_size)) {
                    FontSizeSliderCard(
                        fontScale = settingsManager.fontScale,
                        hasBlurBackground = hasBlurBackground,
                        isDarkTheme = isDarkTheme,
                        onFontScaleChange = { newScale ->
                            settingsManager.fontScale = newScale
                        },
                        onResetScale = {
                            settingsManager.fontScale = 1.0f
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: System Fonts
                SettingsSection(title = stringResource(R.string.font_system_section)) {
                    Column {
                        systemFonts.forEachIndexed { index, fontChoice ->
                            val position = when {
                                systemFonts.size == 1 -> SectionPosition.SINGLE
                                index == 0 -> SectionPosition.FIRST
                                index == systemFonts.size - 1 -> SectionPosition.LAST
                                else -> SectionPosition.MIDDLE
                            }
                            val isSelected = (settingsManager.fontType == fontChoice.id)
                            FontPreferenceRow(
                                headlineText = stringResource(fontChoice.nameRes),
                                supportingText = "Aa Bb Gg 123 • ${stringResource(fontChoice.nameRes)}",
                                icon = Icons.Default.FontDownload,
                                fontPreviewFamily = fontChoice.fontFamily,
                                position = position,
                                isSelected = isSelected,
                                hasBlurBackground = hasBlurBackground,
                                isDarkTheme = isDarkTheme,
                                onSelect = {
                                    settingsManager.fontType = fontChoice.id
                                    settingsManager.customFontFileName = ""
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Imported Fonts
                SettingsSection(title = stringResource(R.string.font_imported_section)) {
                    if (importedFonts.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = if (hasBlurBackground) {
                                if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.font_no_imported_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column {
                            importedFonts.forEachIndexed { index, file ->
                                val position = when {
                                    importedFonts.size == 1 -> SectionPosition.SINGLE
                                    index == 0 -> SectionPosition.FIRST
                                    index == importedFonts.size - 1 -> SectionPosition.LAST
                                    else -> SectionPosition.MIDDLE
                                }
                                val isSelected = (settingsManager.fontType == "custom" && settingsManager.customFontFileName == file.name)
                                val fontFam = remember(file.name, file.lastModified()) {
                                    settingsManager.getFontFamilyForFile(file)
                                }

                                FontPreferenceRow(
                                    headlineText = file.nameWithoutExtension,
                                    supportingText = "Aa Bb Gg 123 • ${file.name}",
                                    icon = Icons.Default.TextFormat,
                                    fontPreviewFamily = fontFam,
                                    position = position,
                                    isSelected = isSelected,
                                    hasBlurBackground = hasBlurBackground,
                                    isDarkTheme = isDarkTheme,
                                    onSelect = {
                                        settingsManager.fontType = "custom"
                                        settingsManager.customFontFileName = file.name
                                    },
                                    onDelete = {
                                        fontToDelete = file
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 4: Import Font Button
                SettingsSection(title = stringResource(R.string.font_import_button)) {
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.font_import_button),
                        supportingText = stringResource(R.string.font_import_desc),
                        icon = Icons.Default.Add,
                        position = SectionPosition.SINGLE,
                        onClick = {
                            importFontLauncher.launch(
                                arrayOf(
                                    "font/*",
                                    "font/ttf",
                                    "font/otf",
                                    "application/x-font-ttf",
                                    "application/x-font-otf",
                                    "application/font-sfnt",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (fontToDelete != null) {
        val target = fontToDelete!!
        AlertDialog(
            onDismissRequest = { fontToDelete = null },
            title = {
                Text(
                    text = stringResource(R.string.font_delete_confirm_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.font_delete_confirm_message, target.nameWithoutExtension)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val deleted = settingsManager.deleteImportedFont(target.name)
                        if (deleted) {
                            importedFonts = settingsManager.getImportedFonts()
                            Toast.makeText(context, context.getString(R.string.font_deleted), Toast.LENGTH_SHORT).show()
                        }
                        fontToDelete = null
                    }
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { fontToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun FontPreviewCard(
    hasBlurBackground: Boolean,
    isDarkTheme: Boolean,
    currentFontName: String
) {
    val cardBg = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Preview,
                            contentDescription = null,
                            tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.font_preview),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentFontName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasBlurBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lune Music Player",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Bigflo & Oli • La vraie vie",
                style = MaterialTheme.typography.titleMedium,
                color = if (hasBlurBackground) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = if (hasBlurBackground) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.font_preview_sample_text),
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasBlurBackground) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FontSizeSliderCard(
    fontScale: Float,
    hasBlurBackground: Boolean,
    isDarkTheme: Boolean,
    onFontScaleChange: (Float) -> Unit,
    onResetScale: () -> Unit
) {
    val cardBg = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val percentage = (fontScale * 100).roundToInt()

    val sliderState = remember {
        SliderState(
            value = fontScale,
            steps = 9,
            trackRange = 0.80f..1.30f
        )
    }

    LaunchedEffect(fontScale) {
        if (sliderState.value != fontScale) {
            sliderState.value = fontScale
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                )

                if (percentage != 100) {
                    TextButton(
                        onClick = onResetScale,
                        modifier = Modifier.bounceClick(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.font_size_reset),
                            modifier = Modifier.size(16.dp),
                            tint = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.font_size_reset),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                state = sliderState,
                onValueChange = { raw ->
                    val rounded = (raw * 20).roundToInt() / 20f
                    onFontScaleChange(rounded.coerceIn(0.80f, 1.30f))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                    activeTrackColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = if (hasBlurBackground) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                    activeTickColor = if (hasBlurBackground) Color.Black.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onPrimary,
                    inactiveTickColor = if (hasBlurBackground) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "80% (A)",
                    fontSize = 12.sp,
                    color = if (hasBlurBackground) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "100%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (hasBlurBackground) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "130% (A)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasBlurBackground) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FontPreferenceRow(
    headlineText: String,
    supportingText: String,
    icon: ImageVector,
    fontPreviewFamily: FontFamily,
    position: SectionPosition,
    isSelected: Boolean,
    hasBlurBackground: Boolean,
    isDarkTheme: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(shape)
            .bounceClick()
            .clickable(onClick = onSelect),
        shape = shape,
        color = cardBg,
        tonalElevation = if (hasBlurBackground) 0.dp else 1.dp
    ) {
        ListItem(
            supportingContent = {
                Text(
                    text = supportingText,
                    color = supportingColor,
                    fontFamily = fontPreviewFamily,
                    style = MaterialTheme.typography.bodyMedium
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
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = onSelect,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = if (hasBlurBackground) Color.White else MaterialTheme.colorScheme.primary,
                            unselectedColor = if (hasBlurBackground) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        ) {
            Text(
                text = headlineText,
                color = headlineColor,
                fontFamily = fontPreviewFamily,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    result = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
    }
    if (result == null) {
        result = uri.path?.let { File(it).name }
    }
    return result
}

private fun isValidFont(file: File): Boolean {
    if (!file.exists() || file.length() < 12) return false
    return try {
        file.inputStream().use { input ->
            val header = ByteArray(4)
            if (input.read(header) != 4) return false
            val isTrueType = (header[0] == 0.toByte() && header[1] == 1.toByte() && header[2] == 0.toByte() && header[3] == 0.toByte()) ||
                    (header[0] == 't'.code.toByte() && header[1] == 'r'.code.toByte() && header[2] == 'u'.code.toByte() && header[3] == 'e'.code.toByte())
            val isOpenType = header[0] == 'O'.code.toByte() && header[1] == 'T'.code.toByte() && header[2] == 'T'.code.toByte() && header[3] == 'O'.code.toByte()
            val isTtc = header[0] == 't'.code.toByte() && header[1] == 't'.code.toByte() && header[2] == 'c'.code.toByte() && header[3] == 'f'.code.toByte()
            val isWoff = header[0] == 'w'.code.toByte() && header[1] == 'O'.code.toByte() && header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte()

            if (!isTrueType && !isOpenType && !isTtc && !isWoff) return false
        }
        val tf = Typeface.createFromFile(file)
        tf != null
    } catch (_: Exception) {
        false
    }
}
