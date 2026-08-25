package com.demonlab.lune

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.demonlab.lune.data.DataManager
import com.demonlab.lune.tools.MprisManager
import com.demonlab.lune.tools.MusicProvider
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.ui.screens.MainScreen
import com.demonlab.lune.ui.theme.LuneTheme
import java.awt.Dimension
import java.awt.Frame
import java.awt.Taskbar
import java.io.File
import javax.imageio.ImageIO

fun main() {
    System.setProperty("sun.awt.X11.appname", "Lune")
    System.setProperty("awt.useSystemAAFontSettings", "on")
    System.setProperty("swing.aatext", "true")

    // Extract app icon to standard user config and icon directory for system notification and dock
    try {
        val configDir = File(System.getProperty("user.home"), ".config/lune").apply { mkdirs() }
        val iconFile = File(configDir, "icon.png")
        if (!iconFile.exists()) {
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("icons/icon.png")
                ?: File("src/main/resources/icons/icon.png").takeIf { it.exists() }?.inputStream()
            stream?.use { input ->
                iconFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        val userIconDir = File(System.getProperty("user.home"), ".local/share/icons/hicolor/512x512/apps").apply { mkdirs() }
        val userIconFile = File(userIconDir, "lune.png")
        if (!userIconFile.exists() && iconFile.exists()) {
            iconFile.copyTo(userIconFile, overwrite = true)
        }
    } catch (e: Exception) {
        // ignore
    }

    application {
        val settingsManager = SettingsManager.instance
        val playbackManager = PlaybackManager.instance
        val musicProvider = MusicProvider.instance
        val dataManager = DataManager.instance

        // Start Linux MPRIS2 media remote control service
        MprisManager.instance.start()

        val isDark = when (settingsManager.themeMode) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }

        val windowState = rememberWindowState(width = 1100.dp, height = 750.dp)

        Window(
            onCloseRequest = {
                playbackManager.setSleepTimer(0)
                settingsManager.saveSettings()
                MprisManager.instance.stop()
                Runtime.getRuntime().halt(0)
            },
            title = settingsManager.customTitle,
            state = windowState,
            icon = painterResource("icons/icon.png")
        ) {
            window.minimumSize = Dimension(800, 600)

            // Set native AWT / Linux Window and Taskbar icon
            LaunchedEffect(Unit) {
                try {
                    val iconFile = File(System.getProperty("user.home"), ".config/lune/icon.png")
                    val awtIcon = if (iconFile.exists()) {
                        ImageIO.read(iconFile)
                    } else {
                        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("icons/icon.png")
                            ?: File("src/main/resources/icons/icon.png").takeIf { it.exists() }?.inputStream()
                        stream?.let { ImageIO.read(it) }
                    }

                    if (awtIcon != null) {
                        window.iconImage = awtIcon
                        try {
                            if (Taskbar.isTaskbarSupported()) {
                                val taskbar = Taskbar.getTaskbar()
                                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                                    taskbar.iconImage = awtIcon
                                }
                            }
                        } catch (e: Exception) {
                            // Taskbar feature not supported on some WMs
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            LuneTheme(
                darkTheme = isDark,
                customColorPalette = settingsManager.customColorPalette,
                useAmoledPitchBlack = settingsManager.useAmoledPitchBlack
            ) {
                MainScreen(
                    playbackManager = playbackManager,
                    settingsManager = settingsManager,
                    musicProvider = musicProvider,
                    dataManager = dataManager,
                    onMinimize = {
                        window.extendedState = Frame.ICONIFIED
                    },
                    onMaximize = {
                        windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                            WindowPlacement.Floating
                        } else {
                            WindowPlacement.Maximized
                        }
                    },
                    onClose = {
                        playbackManager.setSleepTimer(0)
                        settingsManager.saveSettings()
                        MprisManager.instance.stop()
                        Runtime.getRuntime().halt(0)
                    },
                    isMaximized = windowState.placement == WindowPlacement.Maximized
                )
            }
        }
    }
}
