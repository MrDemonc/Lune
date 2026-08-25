package com.demonlab.lune.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Default Purple Scheme
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    surfaceVariant = Color(0xFF2B2930),
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFF332D41),
    onTertiary = Color(0xFF492532),
    onSurface = Color(0xFFE6E1E5),
    onBackground = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFFEF7FF),
    surfaceVariant = Color(0xFFE7E0EC),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onSurface = Color(0xFF1D1B20),
    onBackground = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454F),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B)
)

// 1. Sunset Peach
private val SunsetPeachLight = lightColorScheme(
    primary = Color(0xFFB04B38),
    secondary = Color(0xFF775651),
    tertiary = Color(0xFF705C2E),
    background = Color(0xFFFFF8F6),
    surface = Color(0xFFFFF8F6),
    surfaceVariant = Color(0xFFF5DED9),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color(0xFF231917),
    onBackground = Color(0xFF231917),
    onSurfaceVariant = Color(0xFF534341),
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF3B0903)
)
private val SunsetPeachDark = darkColorScheme(
    primary = Color(0xFFFFB4AA),
    secondary = Color(0xFFE7BDB7),
    tertiary = Color(0xFFDEC48C),
    background = Color(0xFF1A1110),
    surface = Color(0xFF1A1110),
    surfaceVariant = Color(0xFF332321),
    onPrimary = Color(0xFF601408),
    onSecondary = Color(0xFF442926),
    onTertiary = Color(0xFF3D2E05),
    onSurface = Color(0xFFEDE0DE),
    onBackground = Color(0xFFEDE0DE),
    onSurfaceVariant = Color(0xFFD8C2BF),
    primaryContainer = Color(0xFF8F3423),
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondaryContainer = Color(0xFF5D3F3B),
    onSecondaryContainer = Color(0xFFE7BDB7)
)

// 2. Sage Green
private val SageGreenLight = lightColorScheme(
    primary = Color(0xFF386B52),
    secondary = Color(0xFF4F6354),
    tertiary = Color(0xFF3C6472),
    background = Color(0xFFF6FBF5),
    surface = Color(0xFFF6FBF5),
    surfaceVariant = Color(0xFFDCE5DC),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color(0xFF171D19),
    onBackground = Color(0xFF171D19),
    onSurfaceVariant = Color(0xFF414942),
    primaryContainer = Color(0xFFBAF0CD),
    onPrimaryContainer = Color(0xFF002113)
)
private val SageGreenDark = darkColorScheme(
    primary = Color(0xFF9FD3B1),
    secondary = Color(0xFFB7CCBA),
    tertiary = Color(0xFFA3CDDC),
    background = Color(0xFF0F1713),
    surface = Color(0xFF0F1713),
    surfaceVariant = Color(0xFF1E2E25),
    onPrimary = Color(0xFF003822),
    onSecondary = Color(0xFF243528),
    onTertiary = Color(0xFF053544),
    onSurface = Color(0xFFE1E4DF),
    onBackground = Color(0xFFE1E4DF),
    onSurfaceVariant = Color(0xFFBFC9C2),
    primaryContainer = Color(0xFF1E523B),
    onPrimaryContainer = Color(0xFFBAF0CD),
    secondaryContainer = Color(0xFF384B3D),
    onSecondaryContainer = Color(0xFFB7CCBA)
)

// 3. Ocean Breeze
private val OceanBreezeLight = lightColorScheme(
    primary = Color(0xFF2E6580),
    secondary = Color(0xFF4F626E),
    tertiary = Color(0xFF64597C),
    background = Color(0xFFF7FAFC),
    surface = Color(0xFFF7FAFC),
    surfaceVariant = Color(0xFFDCE4E9),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color(0xFF171D20),
    onBackground = Color(0xFF171D20),
    onSurfaceVariant = Color(0xFF40484D),
    primaryContainer = Color(0xFFBBE9FF),
    onPrimaryContainer = Color(0xFF001F2B)
)
private val OceanBreezeDark = darkColorScheme(
    primary = Color(0xFF99CCEA),
    secondary = Color(0xFFB7CAD6),
    tertiary = Color(0xFFCEC2EC),
    background = Color(0xFF0D151A),
    surface = Color(0xFF0D151A),
    surfaceVariant = Color(0xFF1A2B35),
    onPrimary = Color(0xFF003549),
    onSecondary = Color(0xFF233540),
    onTertiary = Color(0xFF342B4C),
    onSurface = Color(0xFFDEE3E8),
    onBackground = Color(0xFFDEE3E8),
    onSurfaceVariant = Color(0xFFBFC8D0),
    primaryContainer = Color(0xFF124D67),
    onPrimaryContainer = Color(0xFFBBE9FF),
    secondaryContainer = Color(0xFF374A55),
    onSecondaryContainer = Color(0xFFB7CAD6)
)

// 4. Lavender Mist
private val LavenderMistLight = lightColorScheme(
    primary = Color(0xFF6E568F),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5262),
    background = Color(0xFFFCF7FF),
    surface = Color(0xFFFCF7FF),
    surfaceVariant = Color(0xFFE7E0EB),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color(0xFF1D1B20),
    onBackground = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454F),
    primaryContainer = Color(0xFFEBDCFF),
    onPrimaryContainer = Color(0xFF281146)
)
private val LavenderMistDark = darkColorScheme(
    primary = Color(0xFFD6BAFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C9),
    background = Color(0xFF15111B),
    surface = Color(0xFF15111B),
    surfaceVariant = Color(0xFF2B2137),
    onPrimary = Color(0xFF3E2160),
    onSecondary = Color(0xFF332B41),
    onTertiary = Color(0xFF4A2534),
    onSurface = Color(0xFFE6E0E9),
    onBackground = Color(0xFFE6E0E9),
    onSurfaceVariant = Color(0xFFCBC4CF),
    primaryContainer = Color(0xFF553B76),
    onPrimaryContainer = Color(0xFFEBDCFF),
    secondaryContainer = Color(0xFF4B4358),
    onSecondaryContainer = Color(0xFFCCC2DC)
)

// 5. Warm Amber
private val WarmAmberLight = lightColorScheme(
    primary = Color(0xFF7F5700),
    secondary = Color(0xFF6C5D47),
    tertiary = Color(0xFF4F6548),
    background = Color(0xFFFFF8F2),
    surface = Color(0xFFFFF8F2),
    surfaceVariant = Color(0xFFEEE1CF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color(0xFF201B13),
    onBackground = Color(0xFF201B13),
    onSurfaceVariant = Color(0xFF4E4539),
    primaryContainer = Color(0xFFFFDF9E),
    onPrimaryContainer = Color(0xFF281900)
)
private val WarmAmberDark = darkColorScheme(
    primary = Color(0xFFFCBC43),
    secondary = Color(0xFFD7C4A7),
    tertiary = Color(0xFFB7CDA6),
    background = Color(0xFF1A140B),
    surface = Color(0xFF1A140B),
    surfaceVariant = Color(0xFF332916),
    onPrimary = Color(0xFF432C00),
    onSecondary = Color(0xFF3B2F1C),
    onTertiary = Color(0xFF23351C),
    onSurface = Color(0xFFEAE1D8),
    onBackground = Color(0xFFEAE1D8),
    onSurfaceVariant = Color(0xFFD0C5B7),
    primaryContainer = Color(0xFF614300),
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondaryContainer = Color(0xFF534531),
    onSecondaryContainer = Color(0xFFD7C4A7)
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
    customColorPalette: Int = 0,
    useAmoledPitchBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when (customColorPalette) {
        1 -> if (darkTheme) SunsetPeachDark else SunsetPeachLight
        2 -> if (darkTheme) SageGreenDark else SageGreenLight
        3 -> if (darkTheme) OceanBreezeDark else OceanBreezeLight
        4 -> if (darkTheme) LavenderMistDark else LavenderMistLight
        5 -> if (darkTheme) WarmAmberDark else WarmAmberLight
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    val colorScheme = if (darkTheme && useAmoledPitchBlack) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF141414),
            secondaryContainer = Color(0xFF1A1A1A),
            onSurface = Color(0xFFEDEDED),
            onBackground = Color(0xFFEDEDED),
            onSurfaceVariant = Color(0xFFB8B8B8)
        )
    } else {
        baseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
