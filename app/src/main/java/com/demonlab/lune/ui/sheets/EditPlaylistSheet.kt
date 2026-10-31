package com.demonlab.lune.ui.sheets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.demonlab.lune.R
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.PlaylistCoverManager
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.components.rememberBlurSheetColors
import com.demonlab.lune.ui.playlist.PlaylistPreviewCovers
import com.demonlab.lune.ui.utils.bounceClick
import com.demonlab.lune.ui.viewmodels.MusicViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistSheet(
    playlist: Playlist,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
    onSaveSuccess: ((newName: String, updatedCovers: List<String>) -> Unit)? = null
) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong
    val blurColors = rememberBlurSheetColors(currentSong)
    val coroutineScope = rememberCoroutineScope()

    var name by remember(playlist.id, playlist.name) { mutableStateOf(playlist.name) }
    val initialCovers = remember(playlist.id, playlist.customCovers) {
        PlaylistCoverManager.parseCovers(playlist.customCovers)
    }
    var currentCovers by remember(playlist.id, initialCovers) {
        mutableStateOf(initialCovers)
    }

    var isSaving by remember { mutableStateOf(false) }

    // Pick visual media (Photo Picker / Gallery)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                val newUris = uris.map { it.toString() }
                currentCovers = (currentCovers + newUris).distinct()
            }
        }
    )

    // Pick files (Document / File manager)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                val newUris = uris.map { it.toString() }
                currentCovers = (currentCovers + newUris).distinct()
            }
        }
    )

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = blurColors.textColor,
        unfocusedTextColor = blurColors.textColor,
        focusedBorderColor = blurColors.primaryTint,
        unfocusedBorderColor = if (blurColors.hasBlur) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline,
        focusedLabelColor = blurColors.primaryTint,
        unfocusedLabelColor = blurColors.textSecondaryColor
    )

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isSmallHeight = screenHeight < 680.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = blurColors.containerColor,
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
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetDefaults.DragHandle(
                        color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.edit_playlist),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = blurColors.textColor
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = blurColors.textColor
                        )
                    }
                }

                // Playlist Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = textFieldColors,
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            IconButton(onClick = { name = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    tint = blurColors.textSecondaryColor
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Covers Section Title + Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.custom_covers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = blurColors.textColor
                    )

                    if (currentCovers.isNotEmpty()) {
                        TextButton(
                            onClick = { currentCovers = emptyList() },
                            modifier = Modifier.bounceClick()
                        ) {
                            Text(
                                text = stringResource(R.string.restore_default_covers),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (blurColors.hasBlur) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Buttons: Gallery and Files
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = blurColors.itemContainerColor,
                        border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = blurColors.primaryTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.add_from_gallery),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = blurColors.textColor
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            filePickerLauncher.launch("image/*")
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = blurColors.itemContainerColor,
                        border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = blurColors.primaryTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.add_from_files),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = blurColors.textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Covers Preview / List
                AnimatedContent(
                    targetState = currentCovers.isNotEmpty(),
                    label = "coversPreviewContent"
                ) { hasCustomCovers ->
                    if (hasCustomCovers) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                itemsIndexed(currentCovers) { index, coverItem ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSmallHeight) 84.dp else 100.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(blurColors.itemContainerColor)
                                    ) {
                                        AsyncImage(
                                            model = coverItem,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Badge index
                                        Surface(
                                            shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 0.dp, topEnd = 0.dp, bottomStart = 8.dp),
                                            color = Color.Black.copy(alpha = 0.60f),
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Delete button
                                        Surface(
                                            onClick = {
                                                currentCovers = currentCovers.toMutableList().also {
                                                    it.removeAt(index)
                                                }
                                            },
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .bounceClick()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (currentCovers.size > 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = blurColors.primaryTint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.covers_rotating_notice),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = blurColors.textSecondaryColor
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty state: using songs covers
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = blurColors.itemContainerColor,
                            border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlaylistPreviewCovers(
                                    playlistId = playlist.id,
                                    viewModel = viewModel,
                                    size = 54.dp,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = stringResource(R.string.using_song_covers),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = blurColors.textSecondaryColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isSmallHeight) 18.dp else 28.dp))

                // Save button
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        if (trimmedName.isEmpty() || isSaving) return@Button
                        isSaving = true
                        coroutineScope.launch {
                            viewModel.updatePlaylist(
                                playlistId = playlist.id,
                                newName = trimmedName,
                                customCovers = currentCovers
                            ) {
                                if (playbackManager.activePlaylistId == playlist.id) {
                                    playbackManager.activePlaylistName = trimmedName
                                }
                                onSaveSuccess?.invoke(trimmedName, currentCovers)
                                onDismiss()
                            }
                        }
                    },
                    enabled = name.isNotBlank() && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = blurColors.primaryTint,
                        contentColor = if (blurColors.hasBlur && blurColors.isDark) Color.Black else Color.White,
                        disabledContainerColor = blurColors.itemContainerColor,
                        disabledContentColor = blurColors.textSecondaryColor
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = if (blurColors.hasBlur && blurColors.isDark) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = if (blurColors.hasBlur && blurColors.isDark) Color.Black else Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.save),
                            fontWeight = FontWeight.Bold,
                            color = if (blurColors.hasBlur && blurColors.isDark) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}
