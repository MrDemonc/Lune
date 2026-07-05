package com.demonlab.lune.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material3.*
import com.demonlab.lune.ui.components.BouncySwitch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.theme.LuneTheme

class GestureCustomizationActivity : ComponentActivity() {
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
                useCustomColors = settingsManager.useCustomColors,
                customColorPalette = settingsManager.customColorPalette,
                useAmoledPitchBlack = settingsManager.useAmoledPitchBlack
            ) {
                GestureCustomizationScreen(
                    onBack = { finish() },
                    settingsManager = settingsManager
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureCustomizationScreen(
    onBack: () -> Unit,
    settingsManager: SettingsManager
) {
    var isGesturesEnabled by remember { mutableStateOf(settingsManager.isGesturesEnabled) }
    var swipeUpAction by remember { mutableIntStateOf(settingsManager.swipeUpAction) }
    var showSwipeUpOptions by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.gesture),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
            SettingsSection(title = stringResource(R.string.general)) {
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.enable_gestures),
                    supportingText = stringResource(R.string.enable_gestures_desc),
                    icon = Icons.Default.Gesture,
                    position = SectionPosition.FIRST,
                    trailingContent = {
                        BouncySwitch(
                            checked = isGesturesEnabled,
                            onCheckedChange = { 
                                isGesturesEnabled = it 
                                settingsManager.isGesturesEnabled = it
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isGesturesEnabled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.change_gesture),
                    supportingText = stringResource(R.string.change_gesture_desc),
                    icon = Icons.Default.SwipeUp,
                    position = SectionPosition.LAST,
                    onClick = { showSwipeUpOptions = true }
                )
            }
        }
        
        if (showSwipeUpOptions) {
            val swipeUpOptions = listOf(
                stringResource(R.string.disabled),
                stringResource(R.string.open_queue),
                stringResource(R.string.eq_title),
                stringResource(R.string.add_to_playlist),
                stringResource(R.string.option_share)
            )
            ModalBottomSheet(
                onDismissRequest = { showSwipeUpOptions = false },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        text = stringResource(R.string.change_gesture),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    swipeUpOptions.forEachIndexed { index, title ->
                        val isSelected = swipeUpAction == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    swipeUpAction = index
                                    settingsManager.swipeUpAction = index
                                    showSwipeUpOptions = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = title)
                        }
                    }
                }
            }
        }
    }
}
