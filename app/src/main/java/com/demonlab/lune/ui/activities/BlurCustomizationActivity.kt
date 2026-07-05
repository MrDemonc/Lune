package com.demonlab.lune.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
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

class BlurCustomizationActivity : ComponentActivity() {
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
                BlurCustomizationScreen(
                    onBack = { finish() },
                    settingsManager = settingsManager
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurCustomizationScreen(
    onBack: () -> Unit,
    settingsManager: SettingsManager
) {
    var isBlurEnabled by remember { mutableStateOf(settingsManager.isBlurEnabled) }
    var isBlurDarkMode by remember { mutableStateOf(settingsManager.isBlurDarkMode) }
    var isBlurCinematicMode by remember { mutableStateOf(settingsManager.isBlurCinematicMode) }
    var isBlurLightMode by remember { mutableStateOf(settingsManager.isBlurLightMode) }
    var isBlurControlsEnabled by remember { mutableStateOf(settingsManager.isBlurControlsEnabled) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.blur),
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
                val masterPosition = if (isBlurEnabled) SectionPosition.FIRST else SectionPosition.SINGLE

                SettingsPreferenceItem(
                    headlineText = stringResource(R.string.enable_blur),
                    supportingText = stringResource(R.string.enable_blur_desc),
                    icon = Icons.Default.BlurOn,
                    position = masterPosition,
                    trailingContent = {
                        BouncySwitch(
                            checked = isBlurEnabled,
                            onCheckedChange = {
                                isBlurEnabled = it
                                settingsManager.isBlurEnabled = it
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isBlurEnabled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                )

                AnimatedVisibility(
                    visible = isBlurEnabled,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.blur_dark_mode),
                            supportingText = stringResource(R.string.blur_dark_mode_desc),
                            icon = Icons.Default.DarkMode,
                            position = SectionPosition.MIDDLE,
                            trailingContent = {
                                BouncySwitch(
                                    checked = isBlurDarkMode,
                                    onCheckedChange = {
                                        isBlurDarkMode = it
                                        settingsManager.isBlurDarkMode = it
                                        if (!it && !isBlurLightMode) {
                                            isBlurEnabled = false
                                            settingsManager.isBlurEnabled = false
                                        }
                                    },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (isBlurDarkMode) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            }
                        )

                        SettingsPreferenceItem(
                            headlineText = stringResource(R.string.blur_light_mode),
                            supportingText = stringResource(R.string.blur_light_mode_desc),
                            icon = Icons.Default.LightMode,
                            position = SectionPosition.LAST,
                            trailingContent = {
                                BouncySwitch(
                                    checked = isBlurLightMode,
                                    onCheckedChange = {
                                        isBlurLightMode = it
                                        settingsManager.isBlurLightMode = it
                                        if (!it && !isBlurDarkMode) {
                                            isBlurEnabled = false
                                            settingsManager.isBlurEnabled = false
                                        }
                                    },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (isBlurLightMode) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = stringResource(R.string.media_player)) {
                Column {
                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.blur_controls),
                        supportingText = stringResource(R.string.blur_controls_desc),
                        icon = Icons.Default.ColorLens,
                        position = SectionPosition.FIRST,
                        trailingContent = {
                            BouncySwitch(
                                checked = isBlurControlsEnabled,
                                enabled = isBlurEnabled,
                                onCheckedChange = {
                                    isBlurControlsEnabled = it
                                    settingsManager.isBlurControlsEnabled = it
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (isBlurControlsEnabled) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    )

                    SettingsPreferenceItem(
                        headlineText = stringResource(R.string.blur_cinematic_mode),
                        supportingText = stringResource(R.string.blur_cinematic_mode_desc),
                        icon = Icons.Default.AutoAwesome,
                        position = SectionPosition.LAST,
                        trailingContent = {
                            BouncySwitch(
                                checked = isBlurCinematicMode,
                                enabled = isBlurEnabled,
                                onCheckedChange = {
                                    isBlurCinematicMode = it
                                    settingsManager.isBlurCinematicMode = it
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (isBlurCinematicMode) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
