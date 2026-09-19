package com.demonlab.lune.ui.lyrics

import android.content.ClipboardManager
import android.content.Context
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.demonlab.lune.tools.SettingsManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.demonlab.lune.R
import com.demonlab.lune.tools.CharsetUtils
import com.demonlab.lune.tools.LyricsStorageManager
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.utils.triggerLightVibration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern

private data class SyncLine(
    val id: Int,
    var text: String,
    var timeMs: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditorSheet(
    song: Song,
    initialLyrics: String?,
    onDismiss: () -> Unit,
    onLyricsSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember(context) { context.getSystemService(Vibrator::class.java) }
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val lyricsStorage = remember { LyricsStorageManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settingsManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }
    val hasBlurBackground = settingsManager.isBlurEnabled && ((isDarkTheme && settingsManager.isBlurDarkMode) || (!isDarkTheme && settingsManager.isBlurLightMode))

    val sheetTextColor = if (hasBlurBackground) {
        if (isDarkTheme) Color.White else Color(0xFF1C1C1E)
    } else MaterialTheme.colorScheme.onSurface

    val sheetTextSecondaryColor = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.70f) else Color(0xFF1C1C1E).copy(alpha = 0.65f)
    } else MaterialTheme.colorScheme.onSurfaceVariant

    val sheetCardBg = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    val sheetCardActiveBg = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.14f)
    } else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)

    val sheetBorderColor = if (hasBlurBackground) {
        if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f)
    } else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    val tabIndicatorColor = if (hasBlurBackground) {
        if (isDarkTheme) Color.White else Color(0xFF1C1C1E)
    } else MaterialTheme.colorScheme.primary

    var selectedTab by remember { mutableIntStateOf(0) }
    var textContent by remember { mutableStateOf(initialLyrics ?: "") }
    val hasCustomLyrics = remember(song.id) { lyricsStorage.hasCustomLyrics(song) }

    // Synchronizer state
    val syncLines = remember { mutableStateListOf<SyncLine>() }
    var activeSyncIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    // Function to parse raw text into SyncLine items
    fun parseTextToSyncLines(raw: String) {
        syncLines.clear()
        val timeRegex = Pattern.compile("[\\[<](\\d{1,2}):(\\d{2})[.:](\\d{1,3})[\\]>]")
        var counter = 0
        raw.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach
            if (trimmed.startsWith("[ti:") || trimmed.startsWith("[ar:") || trimmed.startsWith("[al:") ||
                trimmed.startsWith("[by:") || trimmed.startsWith("[offset:")
            ) return@forEach

            val matcher = timeRegex.matcher(trimmed)
            var foundTime: Long? = null
            if (matcher.find()) {
                val m = matcher.group(1)?.toLongOrNull() ?: 0L
                val s = matcher.group(2)?.toLongOrNull() ?: 0L
                val msStr = matcher.group(3) ?: "0"
                val ms = when (msStr.length) {
                    1 -> (msStr.toLongOrNull() ?: 0L) * 100
                    2 -> (msStr.toLongOrNull() ?: 0L) * 10
                    else -> msStr.take(3).toLongOrNull() ?: 0L
                }
                foundTime = (m * 60 * 1000) + (s * 1000) + ms
            }
            val cleanText = timeRegex.matcher(trimmed).replaceAll("").trim()
            if (cleanText.isNotEmpty() || foundTime != null) {
                syncLines.add(SyncLine(id = counter++, text = cleanText, timeMs = foundTime))
            }
        }
        activeSyncIndex = syncLines.indexOfFirst { it.timeMs == null }.coerceAtLeast(0)
    }

    // Initialize sync lines from current textContent
    LaunchedEffect(Unit) {
        parseTextToSyncLines(textContent)
    }

    // Convert sync lines back to LRC string
    fun buildLrcFromSyncLines(): String {
        val sb = StringBuilder()
        for (line in syncLines) {
            val t = line.timeMs
            if (t != null) {
                sb.append(LyricsStorageManager.formatLrcTimestamp(t))
                if (line.text.isNotEmpty()) {
                    sb.append(" ").append(line.text)
                }
            } else {
                sb.append(line.text)
            }
            sb.append("\n")
        }
        return sb.toString().trim()
    }

    // Playback progress ticker for live sync
    var currentProgress by remember { mutableFloatStateOf(playbackManager.getProgress()) }
    val isPlaying = playbackManager.isPlaying
    LaunchedEffect(selectedTab) {
        while (selectedTab == 1) {
            currentProgress = playbackManager.getProgress()
            delay(50)
        }
    }

    val currentPositionMs = (song.duration * currentProgress).toLong().coerceAtLeast(0L)

    // Import .lrc launcher
    val importLrcLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    val decoded = CharsetUtils.decodeBytes(bytes)
                    if (decoded.isNotBlank()) {
                        textContent = decoded
                        parseTextToSyncLines(decoded)
                        Toast.makeText(context, context.getString(R.string.import_success), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.export_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Export .lrc launcher
    val exportLrcLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    val contentToExport = if (selectedTab == 1) buildLrcFromSyncLines() else textContent
                    stream.write(contentToExport.toByteArray(Charsets.UTF_8))
                    Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.export_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasBlurBackground) {
                // Blurred Background with album art
                AsyncImage(
                    model = song.coverUrl ?: song.albumArtUri ?: song.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                        .alpha(0.55f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color.Black.copy(alpha = 0.65f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.65f),
                                        Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.bounceClick()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = sheetTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.lyrics_editor),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = sheetTextColor,
                            maxLines = 1
                        )
                        Text(
                            text = "${song.title} • ${song.artist}",
                            style = MaterialTheme.typography.bodySmall,
                            color = sheetTextSecondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (hasCustomLyrics) {
                        IconButton(
                            onClick = {
                                lyricsStorage.deleteCustomLyrics(song)
                                playbackManager.updateLyrics(null)
                                Toast.makeText(context, context.getString(R.string.lyrics_deleted), Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier.bounceClick()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = stringResource(R.string.delete_custom_lyrics),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Button(
                        onClick = {
                            val finalContent = if (selectedTab == 1) buildLrcFromSyncLines() else textContent
                            if (finalContent.isNotBlank()) {
                                lyricsStorage.saveLyrics(song, finalContent)
                                playbackManager.updateLyrics(finalContent)
                                onLyricsSaved(finalContent)
                                Toast.makeText(context, context.getString(R.string.lyrics_saved), Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasBlurBackground) {
                                if (isDarkTheme) Color.White else Color(0xFF1C1C1E)
                            } else MaterialTheme.colorScheme.primary,
                            contentColor = if (hasBlurBackground) {
                                if (isDarkTheme) Color.Black else Color.White
                            } else MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.bounceClick()
                    ) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                    }
                }

                // Tabs: Text / LRC & Live Sync
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = sheetTextColor,
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(selectedTab),
                            color = tabIndicatorColor,
                            width = 48.dp,
                            height = 3.dp,
                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            if (selectedTab == 1) {
                                textContent = buildLrcFromSyncLines()
                            }
                            selectedTab = 0
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == 0) tabIndicatorColor else sheetTextSecondaryColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.tab_text_lrc),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedTab == 0) tabIndicatorColor else sheetTextSecondaryColor
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            parseTextToSyncLines(textContent)
                            selectedTab = 1
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == 1) tabIndicatorColor else sheetTextSecondaryColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.tab_sync),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedTab == 1) tabIndicatorColor else sheetTextSecondaryColor
                                )
                            }
                        }
                    )
                }

                HorizontalDivider(color = sheetBorderColor)

                // Content for Tab 0: Text / LRC
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Action Bar Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val chipColors = AssistChipDefaults.assistChipColors(
                                containerColor = sheetCardBg,
                                labelColor = sheetTextColor,
                                leadingIconContentColor = sheetTextColor
                            )
                            val chipBorder = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = sheetBorderColor
                            )

                            // Paste Chip
                            AssistChip(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = cm?.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!clip.isNullOrBlank()) {
                                        textContent = clip
                                        parseTextToSyncLines(clip)
                                    }
                                },
                                label = { Text(stringResource(R.string.paste_clipboard)) },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = chipColors,
                                border = chipBorder,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Clear Chip
                            AssistChip(
                                onClick = {
                                    textContent = ""
                                    syncLines.clear()
                                },
                                label = { Text(stringResource(R.string.clear_lyrics)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = chipColors,
                                border = chipBorder,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Strip Timestamps Chip
                            AssistChip(
                                onClick = {
                                    val stripped = LyricsStorageManager.stripTimestamps(textContent)
                                    textContent = stripped
                                    parseTextToSyncLines(stripped)
                                },
                                label = { Text(stringResource(R.string.strip_timestamps)) },
                                colors = chipColors,
                                border = chipBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Text Field
                        OutlinedTextField(
                            value = textContent,
                            onValueChange = {
                                textContent = it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            placeholder = {
                                Text(
                                    "Paste lyrics or write lines here...\n\nExample:\n[00:12.30]First line of song\n[00:16.45]Second line of song\n\nOr plain text without timestamps to sync in the Live Sync tab!",
                                    color = sheetTextSecondaryColor.copy(alpha = 0.6f)
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = sheetTextColor,
                                unfocusedTextColor = sheetTextColor,
                                focusedContainerColor = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                unfocusedContainerColor = if (hasBlurBackground) (if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                focusedBorderColor = if (hasBlurBackground) sheetTextColor else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = sheetBorderColor,
                                cursorColor = sheetTextColor
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Actions: Import & Export .lrc
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    importLrcLauncher.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, sheetBorderColor),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = sheetTextColor
                                )
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.import_lrc_file))
                            }

                            OutlinedButton(
                                onClick = {
                                    val safeFileName = LyricsStorageManager.getInstance(context).sanitizeForFileName("${song.title}.lrc")
                                    exportLrcLauncher.launch(safeFileName)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, sheetBorderColor),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = sheetTextColor
                                )
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.export_lrc_file))
                            }
                        }
                    }
                }

                // Content for Tab 1: Live Tap-to-Sync Mode
                if (selectedTab == 1) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Instruction banner
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            color = if (hasBlurBackground) {
                                if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
                            } else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, sheetBorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = sheetTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.sync_instruction),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = sheetTextColor
                                )
                            }
                        }

                        // Verses list
                        if (syncLines.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No lines to sync. Switch to the Text/LRC tab and paste lyrics first!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = sheetTextSecondaryColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(syncLines) { index, line ->
                                    val isCurrent = index == activeSyncIndex
                                    val isStamped = line.timeMs != null

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                activeSyncIndex = index
                                                vibrator?.triggerLightVibration()
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isCurrent) {
                                            sheetCardActiveBg
                                        } else if (isStamped) {
                                            sheetCardBg
                                        } else {
                                            if (hasBlurBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                        },
                                        border = if (isCurrent) {
                                            androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                if (hasBlurBackground) sheetTextColor else MaterialTheme.colorScheme.primary
                                            )
                                        } else if (hasBlurBackground) {
                                            androidx.compose.foundation.BorderStroke(1.dp, sheetBorderColor.copy(alpha = if (isStamped) 0.5f else 0.25f))
                                        } else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Timestamp badge
                                            if (line.timeMs != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (hasBlurBackground) {
                                                        if (isDarkTheme) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
                                                    } else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    modifier = Modifier.clickable {
                                                        playbackManager.seekToTimeMs(line.timeMs ?: 0L)
                                                    }
                                                ) {
                                                    Text(
                                                        text = LyricsStorageManager.formatLrcTimestamp(line.timeMs ?: 0L),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = sheetTextColor,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (hasBlurBackground) {
                                                        if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                                                    } else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                ) {
                                                    Text(
                                                        text = "--:--.--",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = sheetTextSecondaryColor.copy(alpha = 0.5f),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            // Text
                                            Text(
                                                text = if (line.text.isBlank()) "(Instrumental)" else line.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) sheetTextColor else sheetTextSecondaryColor,
                                                modifier = Modifier.weight(1f)
                                            )

                                            // Fine tuning (+/- 100ms) & remove time
                                            if (line.timeMs != null) {
                                                IconButton(
                                                    onClick = {
                                                        line.timeMs = ((line.timeMs ?: 0L) - 100L).coerceAtLeast(0L)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Text("-0.1", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = sheetTextColor)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        line.timeMs = (line.timeMs ?: 0L) + 100L
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Text("+0.1", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = sheetTextColor)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        line.timeMs = null
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = sheetTextSecondaryColor, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Control Panel: Playback Controls & Big Stamp Button
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (hasBlurBackground) {
                                if (isDarkTheme) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.75f)
                            } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, sheetBorderColor),
                            tonalElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // Progress & time row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = LyricsStorageManager.formatLrcTimestamp(currentPositionMs),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = sheetTextColor
                                    )

                                    TextButton(
                                        onClick = {
                                            syncLines.forEach { it.timeMs = null }
                                            activeSyncIndex = 0
                                        }
                                    ) {
                                        Text(
                                            stringResource(R.string.sync_reset),
                                            fontSize = 12.sp,
                                            color = sheetTextSecondaryColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Controls: Rewind 3s, Big Stamp Button, Play/Pause
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rewind 3s
                                    IconButton(
                                        onClick = {
                                            val target = (currentPositionMs - 3000L).coerceAtLeast(0L)
                                            playbackManager.seekToTimeMs(target)
                                            vibrator?.triggerLightVibration()
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Replay,
                                            contentDescription = "-3s",
                                            tint = sheetTextColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    // BIG STAMP BUTTON
                                    Button(
                                        onClick = {
                                            if (syncLines.isNotEmpty() && activeSyncIndex in syncLines.indices) {
                                                syncLines[activeSyncIndex].timeMs = currentPositionMs
                                                vibrator?.triggerLightVibration()
                                                if (activeSyncIndex < syncLines.size - 1) {
                                                    activeSyncIndex++
                                                    scope.launch {
                                                        listState.animateScrollToItem((activeSyncIndex - 2).coerceAtLeast(0))
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .height(56.dp)
                                            .weight(1f)
                                            .padding(horizontal = 12.dp)
                                            .bounceClick(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (hasBlurBackground) {
                                                if (isDarkTheme) Color.White else Color(0xFF1C1C1E)
                                            } else MaterialTheme.colorScheme.primary,
                                            contentColor = if (hasBlurBackground) {
                                                if (isDarkTheme) Color.Black else Color.White
                                            } else MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.sync_tap_button),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Play / Pause
                                    IconButton(
                                        onClick = {
                                            if (isPlaying) playbackManager.pause() else playbackManager.resume()
                                            vibrator?.triggerLightVibration()
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = sheetTextColor,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
