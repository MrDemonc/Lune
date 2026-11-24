package com.demonlab.lune.ui.sheets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demonlab.lune.R
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.ui.components.AppBlurBackdrop
import com.demonlab.lune.ui.components.rememberBlurSheetColors
import com.demonlab.lune.ui.theme.quicksand
import com.demonlab.lune.ui.utils.bounceClick
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateChangelogSheet(
    previousVersionName: String,
    currentVersionName: String,
    changelogItems: List<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val blurColors = rememberBlurSheetColors(playbackManager.currentSong)

    var isDismissingViaButton by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden) isDismissingViaButton else true
        }
    )
    val coroutineScope = rememberCoroutineScope()

    // Prevent accidental dismiss via system back gesture
    BackHandler(enabled = !isDismissingViaButton) {
        // Deliberately empty: sheet must be dismissed via the action button
    }

    val prevDisplay = if (previousVersionName.isNotBlank() && previousVersionName != currentVersionName) {
        previousVersionName.removePrefix("v")
    } else {
        if (currentVersionName == "1.5.7") "1.5.6" else ""
    }
    val currDisplay = currentVersionName.removePrefix("v")
    val versionBadgeText = if (prevDisplay.isNotBlank()) {
        stringResource(R.string.update_sheet_version_transition, prevDisplay, currDisplay)
    } else {
        "v$currDisplay"
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (isDismissingViaButton) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = blurColors.containerColor,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = null,
        scrimColor = if (blurColors.hasBlur && !blurColors.isDark) Color.Black.copy(alpha = 0.25f) else BottomSheetDefaults.ScrimColor,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        AppBlurBackdrop(
            hasBlurBackground = blurColors.hasBlur,
            isDarkTheme = blurColors.isDark,
            currentSong = playbackManager.currentSong,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            isSheet = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Subtle non-draggable drag handle indicator
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            color = if (blurColors.hasBlur) {
                                if (blurColors.isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            },
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Rocket Icon Container
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = if (blurColors.hasBlur) {
                                blurColors.primaryTint.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = stringResource(R.string.update_sheet_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = quicksand,
                        fontWeight = FontWeight.Bold,
                        color = blurColors.textColor
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Version Transition Pill Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (blurColors.hasBlur) {
                        if (blurColors.isDark) Color.White.copy(alpha = 0.12f) else blurColors.primaryTint.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = versionBadgeText,
                            fontFamily = quicksand,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (blurColors.hasBlur) blurColors.textColor else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = stringResource(R.string.update_sheet_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = quicksand,
                        color = blurColors.textSecondaryColor
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Changelog Items Container (Scrollable)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = if (blurColors.hasBlur) {
                        if (blurColors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val displayItems = if (changelogItems.isNotEmpty()) {
                            changelogItems
                        } else {
                            listOf(
                                "Performance improvements and smoother animations.",
                                "Updated user interface and general bug fixes."
                            )
                        }

                        displayItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = if (blurColors.hasBlur) {
                                                blurColors.primaryTint.copy(alpha = 0.16f)
                                            } else {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = quicksand,
                                        fontWeight = FontWeight.Medium,
                                        color = blurColors.textColor,
                                        lineHeight = 20.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Fun Action Button (Cannot close by accident)
                Button(
                    onClick = {
                        if (!isDismissingViaButton) {
                            isDismissingViaButton = true
                            coroutineScope.launch {
                                try {
                                    sheetState.hide()
                                } finally {
                                    onDismiss()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (blurColors.hasBlur) blurColors.primaryTint else MaterialTheme.colorScheme.primary,
                        contentColor = if (blurColors.hasBlur) {
                            if (blurColors.isDark) Color.Black else Color.White
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        }
                    )
                ) {
                    Text(
                        text = stringResource(R.string.update_sheet_dismiss_btn),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = quicksand,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}
