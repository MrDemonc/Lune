package com.demonlab.lune.ui.sheets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.demonlab.lune.R
import com.demonlab.lune.tools.CategoryCoverManager
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.components.rememberBlurSheetColors
import com.demonlab.lune.ui.utils.bounceClick
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryCoversSheet(
    category: String, // "ALBUMS", "ARTISTS", "GENRES", "FOLDERS"
    itemKey: String,  // album name, artist name, genre name, folder name
    title: String,
    defaultCovers: List<Any?> = emptyList(),
    onDismiss: () -> Unit,
    onSaveSuccess: ((updatedCovers: List<String>) -> Unit)? = null
) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val currentSong = playbackManager.currentSong
    val blurColors = rememberBlurSheetColors(currentSong)
    val coroutineScope = rememberCoroutineScope()

    val initialCovers = remember(category, itemKey) {
        CategoryCoverManager.getCustomCovers(context, category, itemKey)
    }
    var currentCovers by remember(category, itemKey, initialCovers) {
        mutableStateOf(initialCovers)
    }

    var isSaving by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                val newUris = uris.map { it.toString() }
                currentCovers = (currentCovers + newUris).distinct()
            }
        }
    )

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

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isSmallHeight = screenHeight < 680.dp

    val categoryIcon = when (category) {
        "ARTISTS" -> Icons.Default.Person
        "GENRES" -> Icons.Default.Category
        "FOLDERS" -> Icons.Default.Folder
        else -> Icons.Default.Album
    }

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
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = stringResource(R.string.edit_covers),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = blurColors.textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = blurColors.primaryTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = blurColors.textSecondaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

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

                Spacer(modifier = Modifier.height(10.dp))

                // Covers Section Title + Restore default action
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

                // Action Buttons: Galería & Archivos
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
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
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
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = blurColors.textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Covers Display
                AnimatedContent(
                    targetState = currentCovers.isNotEmpty(),
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "coversStateAnimation"
                ) { hasCustomCovers ->
                    if (hasCustomCovers) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (currentCovers.size > 1) {
                                Text(
                                    text = stringResource(R.string.covers_rotating_notice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = blurColors.textSecondaryColor,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                itemsIndexed(currentCovers, key = { index, uri -> "cover_${uri}_$index" }) { index, uriStr ->
                                    Box(
                                        modifier = Modifier
                                            .size(88.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                    ) {
                                        AsyncImage(
                                            model = uriStr,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .background(
                                                    Color.Black.copy(alpha = 0.65f),
                                                    CircleShape
                                                )
                                                .clickable {
                                                    val mutable = currentCovers.toMutableList()
                                                    if (index in mutable.indices) {
                                                        mutable.removeAt(index)
                                                        currentCovers = mutable
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        if (currentCovers.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(4.dp)
                                                    .background(
                                                        Color.Black.copy(alpha = 0.65f),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = blurColors.itemContainerColor,
                            border = blurColors.itemBorderColor?.let { BorderStroke(1.dp, it) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val firstDefaultCover = defaultCovers.firstOrNull()
                                Surface(
                                    modifier = Modifier.size(54.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    if (firstDefaultCover != null) {
                                        AsyncImage(
                                            model = firstDefaultCover,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = categoryIcon,
                                                contentDescription = null,
                                                tint = blurColors.primaryTint,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = stringResource(R.string.using_default_covers),
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
                        if (isSaving) return@Button
                        isSaving = true
                        coroutineScope.launch {
                            val saved = if (currentCovers.isEmpty()) {
                                CategoryCoverManager.clearCustomCovers(context, category, itemKey)
                                emptyList()
                            } else {
                                CategoryCoverManager.saveCovers(context, category, itemKey, currentCovers)
                            }
                            onSaveSuccess?.invoke(saved)
                            onDismiss()
                        }
                    },
                    enabled = !isSaving,
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
