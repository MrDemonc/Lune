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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.demonlab.lune.R
import com.demonlab.lune.ui.components.OptionButton
import com.demonlab.lune.ui.components.SongItem
import com.demonlab.lune.ui.viewmodels.MusicViewModel
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.activities.EqualizerActivity
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.coverUrl ?: song.uri ?: R.drawable.ic_launcher_foreground,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${song.artist} • ${song.album}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 1.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.add_to_playlist)) },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        onAddToPlaylistClick()
                    }
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 1.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.edit_information)) },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        onEditMetadataClick()
                    }
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 1.dp),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.delete_song), color = MaterialTheme.colorScheme.error) },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        onDeleteClick()
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    sortOption: String,
    isSortAscending: Boolean,
    onSortSettingsChange: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.sort_options_title),
                style = MaterialTheme.typography.titleLarge,
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
                        onSortSettingsChange("ALPHABETICAL", true)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.restore_defaults),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Pill Ascending/Descending Toggle
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    onClick = {
                        onSortSettingsChange(sortOption, !isSortAscending)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSortAscending) stringResource(R.string.sort_ascending) else stringResource(R.string.sort_descending),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        BouncySwitch(
                            checked = isSortAscending,
                            onCheckedChange = {
                                onSortSettingsChange(sortOption, it)
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
            }
            
            // Options cards
            val options = listOf(
                "ALPHABETICAL" to R.string.sort_alphabetical,
                "ARTIST" to R.string.sort_artist,
                "DURATION" to R.string.sort_duration,
                "DATE_ADDED" to R.string.sort_date_added
            )
            
            options.forEachIndexed { index, (option, stringResId) ->
                val isSelected = sortOption == option
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    options.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    else -> RoundedCornerShape(4.dp)
                }
                
                Surface(
                    onClick = {
                        onSortSettingsChange(option, isSortAscending)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 2.dp),
                    shape = shape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ) {
                    ListItem(
                        headlineContent = { 
                            Text(
                                text = stringResource(stringResId),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ) 
                        },
                        trailingContent = {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.eq_title), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { playbackManager.toggleEq() }) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = stringResource(R.string.cd_activate_eq),
                        tint = if (playbackManager.isEqEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                            Text(displayLabel, style = MaterialTheme.typography.labelSmall, color = if(isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(modifier = Modifier.height(170.dp).width(60.dp), contentAlignment = Alignment.Center) {
                                Slider(
                                    value = value,
                                    onValueChange = { newVal ->
                                        val newLevel = (minLevel + newVal * (maxLevel - minLevel)).toInt().toShort()
                                        playbackManager.setEqBandLevel(i.toShort(), newLevel)
                                    },
                                    enabled = isEnabled,
                                    modifier = Modifier
                                        .requiredWidth(170.dp)
                                        .rotate(-90f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            val dbLabel = if (level > 0) "+${level/100}" else "${level/100}"
                            Text(dbLabel, style = MaterialTheme.typography.labelSmall, color = if(isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
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
                            val isFirst = index == 0
                            val isLast = index == presets.lastIndex
                            val isActive = presets[index] == playbackManager.activeEqPresetName
                            FilterChip(
                                selected = isActive,
                                onClick = { playbackManager.applyEqPreset(index.toShort()) },
                                label = { Text(presets[index]) },
                                enabled = isEnabled,
                                border = null
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.height(170.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.eq_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.bass_label), style = MaterialTheme.typography.titleMedium)
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
                Text(stringResource(R.string.spatial_audio_label), style = MaterialTheme.typography.titleMedium)
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
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.restore_defaults))
            }
            Spacer(modifier = Modifier.height(16.dp))
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
    var optionsSong by remember { mutableStateOf<Song?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

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
            onSave = { updatedTitle, updatedArtist, updatedAlbum, updatedCoverUri ->
                musicViewModel.updateMetadata(
                    song = optionsSong!!,
                    title = updatedTitle,
                    artist = updatedArtist,
                    album = updatedAlbum,
                    genre = optionsSong!!.genre,
                    coverUri = updatedCoverUri,
                    onSuccess = {
                        val updatedSong = optionsSong!!.copy(
                            title = updatedTitle,
                            artist = updatedArtist,
                            album = updatedAlbum,
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
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val currentSong = playbackManager.currentSong
        val activePlaylist = playbackManager.activePlaylist
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            Text(
                stringResource(R.string.player_queue),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
            )
            
            val fullQueue = playbackManager.getCurrentQueue()
            val currentIdx = fullQueue.indexOfFirst { it.id == currentSong?.id }
            val queueSections = playbackManager.queueSections
            val displayItems = remember(queueSections, fullQueue, currentIdx) {
                if (queueSections.isNotEmpty() && currentIdx != -1) {
                    buildList<QueueItem> {
                        for (section in queueSections) {
                            val sectionStart = maxOf(section.startIndex, currentIdx + 1).coerceAtMost(fullQueue.size)
                            val sectionEnd = minOf(section.startIndex + section.count, fullQueue.size)
                            if (sectionStart >= sectionEnd) continue
                            val songsInSection = fullQueue.subList(sectionStart, sectionEnd)
                            add(QueueItem.Header(section.title))
                            songsInSection.forEachIndexed { idx, song ->
                                add(QueueItem.Song(song, idx, idx == 0, idx == songsInSection.lastIndex))
                            }
                        }
                    }
                } else {
                    val nextSongs = if (currentIdx != -1 && currentIdx < fullQueue.size - 1) {
                        fullQueue.subList(currentIdx + 1, fullQueue.size)
                    } else {
                        emptyList()
                    }
                    nextSongs.mapIndexed { idx, song ->
                        QueueItem.Song(song, idx, idx == 0, idx == nextSongs.lastIndex)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentSong != null) {
                    item {
                        Text(
                            stringResource(R.string.queue_now_playing),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
                        )
                        SongItem(
                            isFirst = true,
                            isLast = true,
                            song = currentSong,
                            currentlyPlaying = true,
                            isPlaying = playbackManager.isPlaying,
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
                
                item {
                    Text(
                        stringResource(R.string.queue_up_next),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                }


                items(displayItems, key = { item ->
                    when (item) {
                        is QueueItem.Header -> "header_${item.title}"
                        is QueueItem.Song -> "song_${item.song.id}"
                    }
                }) { item ->
                    when (item) {
                        is QueueItem.Header -> {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }
                        is QueueItem.Song -> {
                            val song = item.song
                            val isFirst = item.isFirstInSection
                            val isLast = item.isLastInSection
                            var rawOffset by remember { mutableFloatStateOf(0f) }
                            var isDragging by remember { mutableStateOf(false) }
                            val displayOffset by animateFloatAsState(
                                targetValue = if (isDragging) rawOffset else 0f,
                                animationSpec = if (isDragging) snap() else tween(durationMillis = 250),
                                label = "swipe"
                            )
                            val threshold = with(LocalDensity.current) { 80.dp.toPx() }
                            val maxOffset = with(LocalDensity.current) { 150.dp.toPx() }
                            Box {
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
                                            .background(MaterialTheme.colorScheme.errorContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
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
                                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.SkipNext,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
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
                                                    isDragging = true
                                                },
                                                onDragEnd = {
                                                    isDragging = false
                                                    if (rawOffset < -threshold) {
                                                        playbackManager.moveToNextInQueue(song.id)
                                                    } else if (rawOffset > threshold) {
                                                        playbackManager.removeFromQueue(song.id)
                                                    }
                                                    rawOffset = 0f
                                                },
                                                onDragCancel = {
                                                    isDragging = false
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val isFavorite = playbackManager.currentSong?.isFavorite == true

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
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
                        onClick = { playbackManager.toggleSleepTimer() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerSettingsBottomSheet(
    playbackManager: PlaybackManager,
    onClose: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
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
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                stringResource(R.string.option_visualizer),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.visualizer_full_player)) },
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
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.visualizer_mini_player)) },
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
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSongBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onSave: (title: String, artist: String, album: String, coverUri: Uri?) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedCoverUri = uri }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.imePadding() // Fix keyboard compression
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()), // Fix keyboard compression
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Editar Información",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = {
                    onRestore()
                }) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = stringResource(R.string.cd_restore),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Cover with Circular Edit Button
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                        .size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_change_cover),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text(stringResource(R.string.edit_artist)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text(stringResource(R.string.edit_album)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onSave(title, artist, album, selectedCoverUri) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.save_changes))
            }
        }
    }
}
