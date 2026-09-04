package com.demonlab.lune.ui.sheets

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.DialogProperties
import com.demonlab.lune.ui.components.BouncySwitch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.demonlab.lune.R
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import com.demonlab.lune.ui.components.SongCoverImage
import com.demonlab.lune.ui.components.OptionButton
import com.demonlab.lune.ui.components.SongItem
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.components.rememberBlurSheetColors
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.viewmodels.MusicViewModel
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.activities.EqualizerActivity
import com.demonlab.lune.ui.utils.rememberReorderableState
import com.demonlab.lune.ui.utils.reorderable
import com.demonlab.lune.ui.utils.reorderableItem
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlin.math.abs
import kotlin.math.roundToInt

private sealed interface QueueItem {
    data class Header(val title: String) : QueueItem
    data class Song(val song: com.demonlab.lune.tools.Song, val indexInSection: Int, val isFirstInSection: Boolean, val isLastInSection: Boolean) : QueueItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onEditMetadataClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val isPlayingOrLoaded = playbackManager.currentSong != null
    val targetSong = playbackManager.currentSong ?: song
    val blurColors = rememberBlurSheetColors(targetSong)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = blurColors.containerColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = targetSong,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetDefaults.DragHandle(
                        color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SongCoverImage(
                        coverUrl = song.coverUrl ?: song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = blurColors.textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${song.artist} • ${song.album}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = blurColors.textSecondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isPlayingOrLoaded) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 1.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                        color = blurColors.itemContainerColor,
                        border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) }
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Surface(
                                    shape = CircleShape,
                                    color = if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = null,
                                            tint = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.clickable {
                                onDismiss()
                                playbackManager.playNext(song)
                                Toast.makeText(context, context.getString(R.string.play_next) + ": ${song.title}", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(stringResource(R.string.play_next), color = blurColors.textColor)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 1.dp)
                        .bounceClick(),
                    shape = if (isPlayingOrLoaded) RoundedCornerShape(4.dp) else RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    color = blurColors.itemContainerColor,
                    border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) }
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = null,
                                        tint = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            onDismiss()
                            onAddToPlaylistClick()
                        }
                    ) {
                        Text(stringResource(R.string.add_to_playlist), color = blurColors.textColor)
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 1.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(4.dp),
                    color = blurColors.itemContainerColor,
                    border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) }
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            onDismiss()
                            onEditMetadataClick()
                        }
                    ) {
                        Text(stringResource(R.string.edit_information), color = blurColors.textColor)
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 1.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                    color = blurColors.itemContainerColor,
                    border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) }
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = if (blurColors.hasBlur) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            onDismiss()
                            onDeleteClick()
                        }
                    ) {
                        Text(stringResource(R.string.delete_song), color = if (blurColors.hasBlur) Color.White else MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    sortOption: String,
    isSortAscending: Boolean,
    isCaseSensitive: Boolean,
    allowCustomOrder: Boolean = false,
    onSortSettingsChange: (String, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val blurColors = rememberBlurSheetColors(currentSong)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = blurColors.containerColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = currentSong,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetDefaults.DragHandle(
                        color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.sort_options_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = blurColors.textColor,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                )
            
            // Top row with Pill switch and Restore button (Restore + Switch side by side on the left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Restore defaults circular button
                IconButton(
                    onClick = {
                        onSortSettingsChange("ALPHABETICAL", true, false)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .bounceClick()
                        .background(blurColors.itemContainerColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.restore_defaults),
                        tint = blurColors.primaryTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Pill Ascending/Descending Toggle
                Surface(
                    shape = CircleShape,
                    color = blurColors.itemContainerColor,
                    border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) },
                    onClick = {
                        onSortSettingsChange(sortOption, !isSortAscending, isCaseSensitive)
                    },
                    modifier = Modifier.bounceClick()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSortAscending) stringResource(R.string.sort_ascending) else stringResource(R.string.sort_descending),
                            style = MaterialTheme.typography.labelMedium,
                            color = blurColors.textColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        BouncySwitch(
                            checked = isSortAscending,
                            onCheckedChange = {
                                onSortSettingsChange(sortOption, it, isCaseSensitive)
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Pill Case-Sensitive Toggle
                Surface(
                    shape = CircleShape,
                    color = blurColors.itemContainerColor,
                    border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) },
                    onClick = {
                        onSortSettingsChange(sortOption, isSortAscending, !isCaseSensitive)
                    },
                    modifier = Modifier.bounceClick()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Aa",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isCaseSensitive) FontWeight.Bold else FontWeight.Normal,
                            color = blurColors.textColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        BouncySwitch(
                            checked = isCaseSensitive,
                            onCheckedChange = {
                                onSortSettingsChange(sortOption, isSortAscending, it)
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isCaseSensitive) Icons.Default.TextFields else Icons.Default.TextFormat,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                }
            }

            // Options cards
            val options = buildList {
                if (allowCustomOrder) {
                    add("CUSTOM" to R.string.sort_custom)
                }
                add("TRACK_NUMBER" to R.string.sort_track_number)
                add("ALPHABETICAL" to R.string.sort_alphabetical)
                add("ARTIST" to R.string.sort_artist)
                add("DURATION" to R.string.sort_duration)
                add("DATE_ADDED" to R.string.sort_date_added)
            }
            
            options.forEachIndexed { index, (option, stringResId) ->
                val isSelected = sortOption == option
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    options.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    else -> RoundedCornerShape(4.dp)
                }
                
                Surface(
                    onClick = {
                        onSortSettingsChange(option, isSortAscending, isCaseSensitive)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 2.dp)
                        .bounceClick(),
                    shape = shape,
                    color = if (isSelected && blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.2f) else blurColors.itemContainerColor,
                    border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) }
                ) {
                    ListItem(
                        trailingContent = {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) (if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary) else Color.Transparent,
                                border = BorderStroke(1.dp, if (isSelected) (if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary) else (if (blurColors.hasBlur) blurColors.textSecondaryColor else MaterialTheme.colorScheme.outline)),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (isSelected) (if (blurColors.hasBlur) (if (blurColors.isDark) Color.Black else Color.White) else MaterialTheme.colorScheme.onPrimary) else Color.Transparent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        Text(
                            text = stringResource(stringResId),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) (if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary) else blurColors.textColor
                        )
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqBottomSheet(
    playbackManager: PlaybackManager,
    onDismiss: () -> Unit
) {
    val currentSong = playbackManager.currentSong
    val blurColors = rememberBlurSheetColors(currentSong)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = blurColors.containerColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = currentSong,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetDefaults.DragHandle(
                        color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.eq_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = blurColors.textColor
                    )
                    IconButton(
                        onClick = { playbackManager.toggleEq() },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = stringResource(R.string.cd_activate_eq),
                            tint = if (playbackManager.isEqEnabled) blurColors.primaryTint else blurColors.textSecondaryColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val isEnabled = playbackManager.isEqEnabled
                val numBands = playbackManager.getEqNumberOfBands()
                val bandRange = playbackManager.getEqBandLevelRange()
                val friendlyNames = listOf(
                    stringResource(R.string.band_sub_bass),
                    stringResource(R.string.band_bass),
                    stringResource(R.string.band_mid),
                    stringResource(R.string.band_presence),
                    stringResource(R.string.band_brilliance)
                )
                
                if (numBands > 0 && bandRange != null) {
                    val minLevel = bandRange[0]
                    val maxLevel = bandRange[1]
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (i in 0 until numBands) {
                            val freq = playbackManager.getEqCenterFreq(i.toShort())
                            val freqLabel = if (freq >= 1000000) "${freq / 1000000}k" else "${freq / 1000}"
                            val displayLabel = friendlyNames.getOrElse(i) { freqLabel }
                            val level = playbackManager.eqBandLevels.getOrElse(i) { 0.toShort() }
                            val value = ((level - minLevel).toFloat() / (maxLevel - minLevel).toFloat()).coerceIn(0f, 1f)
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    displayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if(isEnabled) blurColors.textColor else blurColors.textSecondaryColor
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(modifier = Modifier.height(170.dp).width(60.dp), contentAlignment = Alignment.Center) {
                                    Slider(
                                        value = value,
                                        onValueChange = { newVal ->
                                            val newLevel = (minLevel + newVal * (maxLevel - minLevel)).toInt().toShort()
                                            playbackManager.setEqBandLevel(i.toShort(), newLevel)
                                        },
                                        enabled = isEnabled,
                                        colors = SliderDefaults.colors(
                                            thumbColor = if (blurColors.hasBlur) (if (blurColors.isDark) Color.White else blurColors.primaryTint) else MaterialTheme.colorScheme.primary,
                                            activeTrackColor = if (blurColors.hasBlur) (if (blurColors.isDark) Color.White else blurColors.primaryTint) else MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = if (blurColors.hasBlur) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier
                                            .requiredWidth(170.dp)
                                            .rotate(-90f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                val dbLabel = if (level > 0) "+${level/100}" else "${level/100}"
                                Text(
                                    dbLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if(isEnabled) blurColors.textColor else blurColors.textSecondaryColor
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val presets = playbackManager.getEqPresets()
                    if (presets.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(presets.size) { index ->
                                val isActive = presets[index] == playbackManager.activeEqPresetName
                                FilterChip(
                                    selected = isActive,
                                    onClick = { playbackManager.applyEqPreset(index.toShort()) },
                                    label = { Text(presets[index]) },
                                    enabled = isEnabled,
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (blurColors.hasBlur) (if (blurColors.isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.12f)) else MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = blurColors.itemContainerColor,
                                        labelColor = blurColors.textSecondaryColor
                                    ),
                                    modifier = Modifier.bounceClick()
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.height(170.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.eq_empty_hint), color = blurColors.textSecondaryColor)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.bass_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = blurColors.textColor
                    )
                    BouncySwitch(
                        checked = playbackManager.isBassBoostEnabled,
                        onCheckedChange = { playbackManager.toggleBassBoost() },
                        thumbContent = {
                            Icon(
                                imageVector = if (playbackManager.isBassBoostEnabled) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.spatial_audio_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = blurColors.textColor
                    )
                    BouncySwitch(
                        checked = playbackManager.isSpatialAudioEnabled,
                        onCheckedChange = { playbackManager.toggleSpatialAudio() },
                        thumbContent = {
                            Icon(
                                imageVector = if (playbackManager.isSpatialAudioEnabled) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { playbackManager.resetEq() },
                    modifier = Modifier.fillMaxWidth().bounceClick(),
                    enabled = isEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = blurColors.itemContainerColor,
                        contentColor = blurColors.textColor
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = blurColors.primaryTint)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.restore_defaults), color = blurColors.textColor)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    playbackManager: PlaybackManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val musicViewModel: MusicViewModel = viewModel()
    val currentSong = playbackManager.currentSong
    val blurColors = rememberBlurSheetColors(currentSong)
    var optionsSong by remember { mutableStateOf<Song?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    val fullQueue = playbackManager.getCurrentQueue()
    val currentIdxInFull = fullQueue.indexOfFirst { it.id == currentSong?.id }
    
    // Up Next is the part of the queue after the current song
    val upNextSongs = remember(fullQueue, currentIdxInFull) {
        if (currentIdxInFull != -1 && currentIdxInFull < fullQueue.size - 1) {
            fullQueue.subList(currentIdxInFull + 1, fullQueue.size)
        } else {
            emptyList()
        }
    }

    var isRecentlyPlayedExpanded by remember { mutableStateOf(false) }

    val historyHeaderCount = if (playbackManager.recentlyPlayed.isNotEmpty()) 1 else 0
    val historyItemsCount = if (isRecentlyPlayedExpanded && playbackManager.recentlyPlayed.isNotEmpty()) playbackManager.recentlyPlayed.size else 0
    val historySpacerCount = if (isRecentlyPlayedExpanded && playbackManager.recentlyPlayed.isNotEmpty()) 1 else 0
    val nowPlayingItemCount = if (currentSong != null) 1 else 0
    val upNextHeaderCount = 1

    val upNextOffset = historyHeaderCount + historyItemsCount + historySpacerCount + nowPlayingItemCount + upNextHeaderCount

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableState(
        listState = listState,
        canDragOver = { it >= upNextOffset && it < upNextOffset + upNextSongs.size },
        onMove = { from, to ->
            val fromIdx = from - upNextOffset
            val toIdx = to - upNextOffset
            if (fromIdx in upNextSongs.indices && toIdx in upNextSongs.indices) {
                playbackManager.reorderQueue(fromIdx + currentIdxInFull + 1, toIdx + currentIdxInFull + 1)
            }
        }
    )

    if (showOptionsSheet && optionsSong != null) {
        SongOptionsBottomSheet(
            song = optionsSong!!,
            onDismiss = { showOptionsSheet = false },
            onAddToPlaylistClick = { 
                showOptionsSheet = false
                showAddToPlaylist = true 
            },
            onEditMetadataClick = {
                showOptionsSheet = false
                showEditSheet = true
            },
            onDeleteClick = {
                showOptionsSheet = false
                Toast.makeText(context, "Funcionalidad de borrado disponible en la lista principal", Toast.LENGTH_SHORT).show()
            },
        )
    }

    if (showAddToPlaylist && optionsSong != null) {
        AddToPlaylistDialog(
            song = optionsSong!!,
            viewModel = musicViewModel,
            playbackManager = playbackManager,
            onDismiss = { showAddToPlaylist = false }
        )
    }

    if (showEditSheet && optionsSong != null) {
        EditSongBottomSheet(
            song = optionsSong!!,
            onDismiss = { showEditSheet = false },
            onRestore = {
                musicViewModel.restoreOriginalMetadata(
                    song = optionsSong!!,
                    onSuccess = {
                        val updatedSong = musicViewModel.allSongs.find { it.id == optionsSong!!.id }
                        if (updatedSong != null) {
                            playbackManager.updateSongMetadata(updatedSong)
                        }
                        showEditSheet = false
                        Toast.makeText(context, context.getString(R.string.info_restored), Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onSave = { updatedTitle, updatedArtist, updatedAlbum, updatedGenre, updatedCoverUri ->
                musicViewModel.updateMetadata(
                    song = optionsSong!!,
                    title = updatedTitle,
                    artist = updatedArtist,
                    album = updatedAlbum,
                    genre = updatedGenre,
                    coverUri = updatedCoverUri,
                    onSuccess = {
                        val updatedSong = optionsSong!!.copy(
                            title = updatedTitle,
                            artist = updatedArtist,
                            album = updatedAlbum,
                            genre = updatedGenre,
                            coverUrl = updatedCoverUri?.toString() ?: optionsSong!!.coverUrl
                        )
                        playbackManager.updateSongMetadata(updatedSong)
                        Toast.makeText(context, context.getString(R.string.info_updated), Toast.LENGTH_SHORT).show()
                        showEditSheet = false
                    }
                )
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = blurColors.containerColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        val activePlaylist = playbackManager.activePlaylist
        
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = currentSong,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetDefaults.DragHandle(
                        color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    stringResource(R.string.player_queue),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = blurColors.textColor,
                    modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .reorderable(reorderState)
                ) {
                    // 1. Recently Played Section
                    if (playbackManager.recentlyPlayed.isNotEmpty()) {
                        item {
                            val arrowRotation by animateFloatAsState(
                                targetValue = if (isRecentlyPlayedExpanded) 180f else 0f,
                                animationSpec = tween(durationMillis = 250),
                                label = "recently_played_arrow"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isRecentlyPlayedExpanded = !isRecentlyPlayedExpanded }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.recently_played),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = blurColors.textSecondaryColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = blurColors.itemContainerColor
                                    ) {
                                        Text(
                                            text = "${playbackManager.recentlyPlayed.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = blurColors.textSecondaryColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { isRecentlyPlayedExpanded = !isRecentlyPlayedExpanded },
                                    modifier = Modifier.size(32.dp).bounceClick()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = blurColors.textSecondaryColor,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .rotate(arrowRotation)
                                    )
                                }
                            }
                        }
                        if (isRecentlyPlayedExpanded) {
                            itemsIndexed(playbackManager.recentlyPlayed, key = { _, s -> "history_${s.id}" }) { index, song ->
                                SongItem(
                                    isFirst = index == 0,
                                    isLast = index == playbackManager.recentlyPlayed.lastIndex,
                                    song = song,
                                    currentlyPlaying = false,
                                    isPlaying = false,
                                    hasBlurBackground = blurColors.hasBlur,
                                    useCustomControlsColor = blurColors.useCustomControlsColor,
                                    controlsColorPalette = blurColors.controlsColorPalette,
                                    modifier = Modifier.graphicsLayer { alpha = 0.6f },
                                    onClick = {
                                        playbackManager.play(song)
                                    },
                                    onOptionsClick = {
                                        optionsSong = song
                                        showOptionsSheet = true
                                    },
                                    onFavoriteClick = { s ->
                                        playbackManager.toggleFavorite(s)?.let { updated ->
                                            musicViewModel.syncFavoriteStatusInMemory(updated.id, updated.isFavorite)
                                        }
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(12.dp)) }
                        }
                    }

                    // 2. Now Playing Section
                    if (currentSong != null) {
                        item {
                            Text(
                                stringResource(R.string.queue_now_playing),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = blurColors.primaryTint,
                                modifier = Modifier.padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
                            )
                            SongItem(
                                isFirst = true,
                                isLast = true,
                                song = currentSong,
                                currentlyPlaying = true,
                                isPlaying = playbackManager.isPlaying,
                                hasBlurBackground = blurColors.hasBlur,
                                useCustomControlsColor = blurColors.useCustomControlsColor,
                                controlsColorPalette = blurColors.controlsColorPalette,
                                onClick = { onDismiss() },
                                onOptionsClick = {
                                    optionsSong = currentSong
                                    showOptionsSheet = true
                                },
                                onFavoriteClick = { song ->
                                    playbackManager.toggleFavorite(song)?.let { updated ->
                                        musicViewModel.syncFavoriteStatusInMemory(updated.id, updated.isFavorite)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    
                    // 3. Up Next Section
                    item {
                        Text(
                            stringResource(R.string.queue_up_next),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = blurColors.textColor,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }

                    itemsIndexed(upNextSongs, key = { _, s -> "song_${s.id}" }) { index, song ->
                        val isFirst = index == 0
                        val isLast = index == upNextSongs.lastIndex
                        
                        var rawOffset by remember { mutableFloatStateOf(0f) }
                        var isSwiping by remember { mutableStateOf(false) }
                        val displayOffset by animateFloatAsState(
                            targetValue = if (isSwiping) rawOffset else 0f,
                            animationSpec = if (isSwiping) snap() else tween(durationMillis = 250),
                            label = "swipe"
                        )
                        val threshold = with(LocalDensity.current) { 80.dp.toPx() }
                        val maxOffset = with(LocalDensity.current) { 150.dp.toPx() }

                        Box(modifier = Modifier.reorderableItem(reorderState, index + upNextOffset)) {
                            val swipeProgress = (abs(displayOffset) / threshold).coerceAtMost(1f)
                            if (displayOffset > 0f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 24.dp)
                                        .size(40.dp)
                                        .graphicsLayer {
                                            alpha = swipeProgress
                                            scaleX = swipeProgress
                                            scaleY = swipeProgress
                                        }
                                        .clip(CircleShape)
                                        .background(if (blurColors.hasBlur) Color(0xFFE53935).copy(alpha = 0.85f) else MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = if (blurColors.hasBlur) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (displayOffset < 0f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 24.dp)
                                        .size(40.dp)
                                        .graphicsLayer {
                                            alpha = swipeProgress
                                            scaleX = swipeProgress
                                            scaleY = swipeProgress
                                        }
                                        .clip(CircleShape)
                                        .background(if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.85f) else MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.SkipNext,
                                        contentDescription = null,
                                        tint = if (blurColors.hasBlur) (if (blurColors.isDark) Color.Black else Color.White) else MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Box(modifier = Modifier.offset { IntOffset(displayOffset.roundToInt(), 0) }) {
                                SongItem(
                                    isFirst = isFirst,
                                    isLast = isLast,
                                    song = song,
                                    currentlyPlaying = false,
                                    isPlaying = false,
                                    hasBlurBackground = blurColors.hasBlur,
                                    useCustomControlsColor = blurColors.useCustomControlsColor,
                                    controlsColorPalette = blurColors.controlsColorPalette,
                                    onClick = {
                                        playbackManager.play(song, activePlaylist, playbackManager.activePlaylistId, playbackManager.activeCategory, fromQueue = true)
                                    },
                                    onOptionsClick = {
                                        optionsSong = song
                                        showOptionsSheet = true
                                    },
                                    onFavoriteClick = { s ->
                                        playbackManager.toggleFavorite(s)?.let { updated ->
                                            musicViewModel.syncFavoriteStatusInMemory(updated.id, updated.isFavorite)
                                        }
                                    },
                                    modifier = Modifier.pointerInput(song.id) {
                                        detectHorizontalDragGestures(
                                            onDragStart = {
                                                rawOffset = 0f
                                                isSwiping = true
                                            },
                                            onDragEnd = {
                                                isSwiping = false
                                                if (rawOffset < -threshold) {
                                                    playbackManager.moveToNextInQueue(song.id)
                                                } else if (rawOffset > threshold) {
                                                    playbackManager.removeFromQueue(song.id)
                                                }
                                                rawOffset = 0f
                                            },
                                            onDragCancel = {
                                                isSwiping = false
                                                rawOffset = 0f
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                rawOffset = (rawOffset + dragAmount).coerceIn(-maxOffset, maxOffset)
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOptionsBottomSheet(
    playbackManager: PlaybackManager,
    showWaveform: Boolean,
    onToggleWaveform: () -> Unit,
    onRefreshSongs: (() -> Unit)? = null,
    onSyncFavorite: ((Long, Boolean) -> Unit)? = null,
    onDismiss: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onShowVisualizerSettings: () -> Unit,
    onShowLyrics: () -> Unit
) {
    val currentSong = playbackManager.currentSong
    val blurColors = rememberBlurSheetColors(currentSong)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = blurColors.containerColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = currentSong,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val isFavorite = playbackManager.currentSong?.isFavorite == true
            var showCustomTimerDialog by remember { mutableStateOf(false) }

            if (showCustomTimerDialog) {
                CustomSleepTimerDialog(
                    currentMinutes = playbackManager.sleepTimerMinutes,
                    onDismiss = { showCustomTimerDialog = false },
                    onSetTimer = { minutes ->
                        playbackManager.setCustomSleepTimer(minutes)
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetDefaults.DragHandle(
                        color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val context = LocalContext.current
                    val song = playbackManager.currentSong
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        OptionButton(
                            icon = Icons.Default.Share,
                            label = stringResource(R.string.option_share),
                            active = false,
                            onClick = {
                                song?.let {
                                    try {
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "audio/*"
                                            putExtra(android.content.Intent.EXTRA_STREAM, it.uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, context.getString(R.string.option_share)))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        )
                    }

                    // Repeat
                    val repeatIcon = when (playbackManager.repeatMode) {
                        1 -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    val repeatLabel = when (playbackManager.repeatMode) {
                        1 -> stringResource(R.string.option_repeat_one)
                        2 -> stringResource(R.string.option_repeat_all)
                        else -> stringResource(R.string.option_repeat_off)
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        OptionButton(
                            icon = repeatIcon,
                            label = repeatLabel,
                            active = playbackManager.repeatMode > 0,
                            onClick = { playbackManager.toggleRepeatMode() }
                        )
                    }

                    // Crossfade
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        OptionButton(
                            icon = Icons.Default.Tune,
                            label = stringResource(R.string.option_crossfade),
                            active = playbackManager.isCrossfade,
                            onClick = { playbackManager.toggleCrossfade() }
                        )
                    }

                    // Automix
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        OptionButton(
                            icon = Icons.Default.AutoAwesome,
                            label = stringResource(R.string.option_automix),
                            active = playbackManager.isAutomix,
                            onClick = { playbackManager.toggleAutomix() }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val context = LocalContext.current
                    // Timer
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        OptionButton(
                            icon = Icons.Default.Timer,
                            label = if (playbackManager.sleepTimerMinutes > 0) "${playbackManager.sleepTimerMinutes}m" else stringResource(R.string.option_timer),
                            active = playbackManager.sleepTimerMinutes > 0,
                            onClick = { playbackManager.toggleSleepTimer() },
                            onLongClick = { showCustomTimerDialog = true }
                        )
                    }

                    // EQ
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        OptionButton(
                            icon = Icons.Default.GraphicEq,
                            label = stringResource(R.string.eq_title),
                            active = playbackManager.isEqEnabled,
                            onClick = {
                                onDismiss()
                                val intent = android.content.Intent(context, EqualizerActivity::class.java)
                                context.startActivity(intent)
                            }
                        )
                    }

                    // Playlist
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        OptionButton(
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            label = stringResource(R.string.add_to_playlist),
                            active = false,
                            onClick = {
                                onDismiss()
                                onAddToPlaylistClick()
                            }
                        )
                    }

                    // Waveform Visualizer
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        OptionButton(
                            icon = Icons.Default.Audiotrack,
                            label = stringResource(R.string.option_visualizer),
                            active = playbackManager.isFullPlayerVisualizerEnabled || playbackManager.isMiniPlayerVisualizerEnabled,
                            onClick = onShowVisualizerSettings
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerSettingsBottomSheet(
    playbackManager: PlaybackManager,
    onClose: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val currentSong = playbackManager.currentSong
    val blurColors = rememberBlurSheetColors(currentSong)
    fun toggleVisualizer(isFull: Boolean) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            onRequestPermission()
        } else {
            if (isFull) playbackManager.toggleFullPlayerVisualizer()
            else playbackManager.toggleMiniPlayerVisualizer()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = blurColors.containerColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = currentSong,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetDefaults.DragHandle(
                        color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    stringResource(R.string.option_visualizer),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = blurColors.textColor,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    trailingContent = {
                        BouncySwitch(
                            checked = playbackManager.isFullPlayerVisualizerEnabled,
                            onCheckedChange = { toggleVisualizer(true) },
                            thumbContent = {
                                Icon(
                                    imageVector = if (playbackManager.isFullPlayerVisualizerEnabled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    modifier = Modifier.clickable { toggleVisualizer(true) }
                ) {
                    Text(stringResource(R.string.visualizer_full_player), color = blurColors.textColor)
                }

                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    trailingContent = {
                        BouncySwitch(
                            checked = playbackManager.isMiniPlayerVisualizerEnabled,
                            onCheckedChange = { toggleVisualizer(false) },
                            thumbContent = {
                                Icon(
                                    imageVector = if (playbackManager.isMiniPlayerVisualizerEnabled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    modifier = Modifier.clickable { toggleVisualizer(false) }
                ) {
                    Text(stringResource(R.string.visualizer_mini_player), color = blurColors.textColor)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSongBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onSave: (title: String, artist: String, album: String, genre: String, coverUri: Uri?) -> Unit
) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong ?: song
    val blurColors = rememberBlurSheetColors(currentSong)
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var genre by remember { mutableStateOf(song.genre ?: "") }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedCoverUri = uri }
    )

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = blurColors.textColor,
        unfocusedTextColor = blurColors.textColor,
        focusedBorderColor = blurColors.primaryTint,
        unfocusedBorderColor = if (blurColors.hasBlur) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline,
        focusedLabelColor = blurColors.primaryTint,
        unfocusedLabelColor = blurColors.textSecondaryColor
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = blurColors.containerColor,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        modifier = Modifier.imePadding() // Fix keyboard compression
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = currentSong,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()), // Fix keyboard compression
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetDefaults.DragHandle(
                        color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.edit_information),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = blurColors.textColor
                    )
                    
                    IconButton(
                        onClick = { onRestore() },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = stringResource(R.string.cd_restore),
                            tint = blurColors.primaryTint
                        )
                    }
                }

                // Cover with Circular Edit Button
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(blurColors.itemContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = selectedCoverUri ?: song.coverUrl ?: song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    Surface(
                        onClick = { 
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                            .bounceClick(),
                        shape = CircleShape,
                        color = (if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primaryContainer).copy(alpha = 0.85f),
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_change_cover),
                                tint = if (blurColors.hasBlur && blurColors.isDark) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.edit_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(stringResource(R.string.edit_artist)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text(stringResource(R.string.edit_album)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(stringResource(R.string.edit_genre)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onSave(title, artist, album, genre, selectedCoverUri) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = blurColors.primaryTint,
                        contentColor = if (blurColors.hasBlur) Color.Black else Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        tint = if (blurColors.hasBlur) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.save_changes),
                        fontWeight = FontWeight.Bold,
                        color = if (blurColors.hasBlur) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CustomSleepTimerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    val blurColors = rememberBlurSheetColors()
    var selectedMinutes by remember {
        mutableStateOf(if (currentMinutes > 0) currentMinutes else 30)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .widthIn(max = 380.dp),
        shape = RoundedCornerShape(28.dp),
        containerColor = blurColors.containerColor,
        icon = {
            Surface(
                shape = CircleShape,
                color = if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = blurColors.primaryTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.option_timer),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = blurColors.textColor,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Expressive duration badge
                val offLabel = stringResource(R.string.option_repeat_off)
                val minUnit = stringResource(R.string.timer_minutes_unit)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (selectedMinutes > 0) (if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    else blurColors.itemContainerColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = when {
                            selectedMinutes == 0 -> offLabel
                            selectedMinutes >= 60 -> {
                                val h = selectedMinutes / 60
                                val m = selectedMinutes % 60
                                if (m == 0) "${h}h" else "${h}h ${m}${minUnit}"
                            }
                            else -> "$selectedMinutes $minUnit"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMinutes > 0) blurColors.primaryTint
                        else blurColors.textSecondaryColor,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }

                // Quick preset chips row
                val presets = listOf(0, 15, 30, 45, 60, 90, 120)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { preset ->
                        val isSelected = selectedMinutes == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMinutes = preset },
                            label = {
                                Text(
                                    text = if (preset == 0) offLabel else "${preset}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (blurColors.hasBlur) blurColors.primaryTint.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary,
                                selectedLabelColor = if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onPrimary,
                                containerColor = blurColors.itemContainerColor,
                                labelColor = blurColors.textSecondaryColor
                            ),
                            modifier = Modifier.bounceClick()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stepper + Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledTonalIconButton(
                        onClick = { selectedMinutes = (selectedMinutes - 5).coerceAtLeast(0) },
                        enabled = selectedMinutes > 0,
                        modifier = Modifier.size(44.dp).bounceClick(),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_timer_decrease))
                    }

                    Slider(
                        value = selectedMinutes.toFloat(),
                        onValueChange = { selectedMinutes = ((it / 5).roundToInt() * 5) },
                        valueRange = 0f..180f,
                        steps = 35,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = blurColors.primaryTint,
                            activeTrackColor = blurColors.primaryTint,
                            inactiveTrackColor = if (blurColors.hasBlur) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )

                    FilledTonalIconButton(
                        onClick = { selectedMinutes = (selectedMinutes + 5).coerceAtMost(180) },
                        enabled = selectedMinutes < 180,
                        modifier = Modifier.size(44.dp).bounceClick(),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_timer_increase))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSetTimer(selectedMinutes)
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = blurColors.primaryTint,
                    contentColor = if (blurColors.hasBlur) Color.Black else Color.White
                ),
                modifier = Modifier.bounceClick()
            ) {
                Text(
                    text = if (selectedMinutes == 0) stringResource(R.string.timer_turn_off) else stringResource(R.string.timer_set),
                    fontWeight = FontWeight.SemiBold,
                    color = if (blurColors.hasBlur) Color.Black else Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.bounceClick()
            ) {
                Text(stringResource(R.string.cancel), color = blurColors.textSecondaryColor)
            }
        }
    )
}

