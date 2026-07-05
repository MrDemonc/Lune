package com.demonlab.lune.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.demonlab.lune.ui.components.BouncySwitch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demonlab.lune.R
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.player.ReusableSkipIcon
import com.demonlab.lune.ui.theme.LuneTheme
import com.demonlab.lune.ui.theme.getControlsPrimaryColor

class ControlsCustomizationActivity : ComponentActivity() {
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
                ControlsCustomizationScreen(
                    onBack = { finish() },
                    settingsManager = settingsManager
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsCustomizationScreen(
    onBack: () -> Unit,
    settingsManager: SettingsManager
) {
    var controlsIconStyle by remember { mutableIntStateOf(settingsManager.controlsIconStyle) }
    var isControlsFilled by remember { mutableStateOf(settingsManager.isControlsFilled) }
    var useCustomControlsColor by remember { mutableStateOf(settingsManager.useCustomControlsColor) }
    var controlsColorPalette by remember { mutableIntStateOf(settingsManager.controlsColorPalette) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val activePrimary = getControlsPrimaryColor(useCustomControlsColor, controlsColorPalette)
    val activeContainerColor = if (useCustomControlsColor) {
        activePrimary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    }

    val activeIconTint = if (useCustomControlsColor) {
        activePrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.controls_player),
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
            // Live Preview Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.controls_preview),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(28.dp))

                    // Mock Player Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = activeContainerColor,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                ReusableSkipIcon(
                                    isNext = false,
                                    controlsIconStyle = controlsIconStyle,
                                    isControlsFilled = isControlsFilled,
                                    tint = activeIconTint,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = activeContainerColor,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                ReusableSkipIcon(
                                    isNext = true,
                                    controlsIconStyle = controlsIconStyle,
                                    isControlsFilled = isControlsFilled,
                                    tint = activeIconTint,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button Style Selection
            SettingsSection(title = stringResource(R.string.controls_style_title)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val styles = listOf(
                            0 to stringResource(R.string.shape_default),
                            1 to "Play 2",
                            2 to "Play 3"
                        )

                        styles.forEach { (index, label) ->
                            val isSelected = controlsIconStyle == index

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    controlsIconStyle = index
                                    settingsManager.controlsIconStyle = index
                                }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    border = BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        ReusableSkipIcon(
                                            isNext = true,
                                            controlsIconStyle = index,
                                            isControlsFilled = isControlsFilled,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Switches
            SettingsSection(title = stringResource(R.string.categories)) {
                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.controls_filled),
                    supportingText = stringResource(R.string.controls_filled_desc),
                    icon = Icons.Default.CheckCircle,
                    position = SectionPosition.FIRST,
                    trailingContent = {
                        BouncySwitch(
                            checked = isControlsFilled,
                            onCheckedChange = {
                                isControlsFilled = it
                                settingsManager.isControlsFilled = it
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isControlsFilled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.controls_custom_color),
                    supportingText = stringResource(R.string.controls_custom_color_desc),
                    icon = Icons.Default.Palette,
                    position = if (useCustomControlsColor) SectionPosition.MIDDLE else SectionPosition.LAST,
                    trailingContent = {
                        BouncySwitch(
                            checked = useCustomControlsColor,
                            onCheckedChange = {
                                useCustomControlsColor = it
                                settingsManager.useCustomControlsColor = it
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (useCustomControlsColor) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                if (useCustomControlsColor) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(R.string.color_palette),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val palettes = listOf(
                                    0 to MaterialTheme.colorScheme.primary, // App default / Material You
                                    1 to Color(0xFFB04B38), // Sunset Peach
                                    2 to Color(0xFF386B52), // Sage Green
                                    3 to Color(0xFF2E6580), // Ocean Breeze
                                    4 to Color(0xFF6E568F), // Lavender Mist
                                    5 to Color(0xFF7F5700)  // Warm Amber
                                )
                                palettes.forEach { (index, color) ->
                                    val isSelected = controlsColorPalette == index
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .clickable {
                                                controlsColorPalette = index
                                                settingsManager.controlsColorPalette = index
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
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
}
