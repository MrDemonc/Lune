package com.demonlab.lune.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Soft Predefined Color Palettes
// 1. Sunset Peach
private val SunsetPeachLight = lightColorScheme(
    primary = Color(0xFFB04B38),
    secondary = Color(0xFF775651),
    tertiary = Color(0xFF705C2E)
)
private val SunsetPeachDark = darkColorScheme(
    primary = Color(0xFFFFB4AA),
    secondary = Color(0xFFE7BDB7),
    tertiary = Color(0xFFDEC48C)
)

// 2. Sage Green
private val SageGreenLight = lightColorScheme(
    primary = Color(0xFF386B52),
    secondary = Color(0xFF4F6354),
    tertiary = Color(0xFF3C6472)
)
private val SageGreenDark = darkColorScheme(
    primary = Color(0xFF9FD3B1),
    secondary = Color(0xFFB7CCBA),
    tertiary = Color(0xFFA3CDDC)
)

// 3. Ocean Breeze
private val OceanBreezeLight = lightColorScheme(
    primary = Color(0xFF2E6580),
    secondary = Color(0xFF4F626E),
    tertiary = Color(0xFF64597C)
)
private val OceanBreezeDark = darkColorScheme(
    primary = Color(0xFF99CCEA),
    secondary = Color(0xFFB7CAD6),
    tertiary = Color(0xFFCEC2EC)
)

// 4. Lavender Mist
private val LavenderMistLight = lightColorScheme(
    primary = Color(0xFF6E568F),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5262)
)
private val LavenderMistDark = darkColorScheme(
    primary = Color(0xFFD6BAFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C9)
)

// 5. Warm Amber
private val WarmAmberLight = lightColorScheme(
    primary = Color(0xFF7F5700),
    secondary = Color(0xFF6C5D47),
    tertiary = Color(0xFF4F6548)
)
private val WarmAmberDark = darkColorScheme(
    primary = Color(0xFFFCBC43),
    secondary = Color(0xFFD7C4A7),
    tertiary = Color(0xFFB7CDA6)
)

@Composable
fun getControlsPrimaryColor(
    useCustomControlsColor: Boolean,
    controlsColorPalette: Int,
    darkTheme: Boolean = isSystemInDarkTheme()
): Color {
    if (!useCustomControlsColor) return MaterialTheme.colorScheme.onSurface
    if (controlsColorPalette == 0) return MaterialTheme.colorScheme.primary
    return when (controlsColorPalette) {
        1 -> if (darkTheme) Color(0xFFFFB4AA) else Color(0xFFB04B38)
        2 -> if (darkTheme) Color(0xFF9FD3B1) else Color(0xFF386B52)
        3 -> if (darkTheme) Color(0xFF99CCEA) else Color(0xFF2E6580)
        4 -> if (darkTheme) Color(0xFFD6BAFF) else Color(0xFF6E568F)
        5 -> if (darkTheme) Color(0xFFFCBC43) else Color(0xFF7F5700)
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun LuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    useCustomColors: Boolean = false,
    customColorPalette: Int = 0,
    useAmoledPitchBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        useCustomColors -> {
            if (darkTheme) {
                when (customColorPalette) {
                    1 -> SunsetPeachDark
                    2 -> SageGreenDark
                    3 -> OceanBreezeDark
                    4 -> LavenderMistDark
                    5 -> WarmAmberDark
                    else -> DarkColorScheme
                }
            } else {
                when (customColorPalette) {
                    1 -> SunsetPeachLight
                    2 -> SageGreenLight
                    3 -> OceanBreezeLight
                    4 -> LavenderMistLight
                    5 -> WarmAmberLight
                    else -> LightColorScheme
                }
            }
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = if (darkTheme && useAmoledPitchBlack) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF121212),
            secondaryContainer = Color(0xFF121212)
        )
    } else {
        baseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var parent = view.parent
            var dialogWindow: android.view.Window? = null
            while (parent != null) {
                if (parent is androidx.compose.ui.window.DialogWindowProvider) {
                    dialogWindow = parent.window
                    break
                }
                parent = parent.parent
            }
            var activity: Activity? = null
            var ctx = view.context
            while (ctx is android.content.ContextWrapper) {
                if (ctx is Activity) {
                    activity = ctx
                    break
                }
                ctx = ctx.baseContext
            }
            val window = dialogWindow ?: activity?.window
            window?.let { win ->
                win.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    win.isNavigationBarContrastEnforced = false
                }
                val insets = WindowCompat.getInsetsController(win, win.decorView)
                insets.isAppearanceLightStatusBars = !darkTheme
                insets.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}