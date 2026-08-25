package com.demonlab.lune.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.platform.Font
import java.io.File

fun loadCustomFontFamily(): FontFamily {
    return try {
        val regularFile = File("src/main/resources/fonts/quicksand_regular.ttf")
        val boldFile = File("src/main/resources/fonts/quicksand_bold.ttf")
        if (regularFile.exists() && boldFile.exists()) {
            FontFamily(
                Font(regularFile, FontWeight.Normal),
                Font(boldFile, FontWeight.Bold)
            )
        } else {
            // Check resource stream in jar
            val regStream = Typography::class.java.getResourceAsStream("/fonts/quicksand_regular.ttf")
            val boldStream = Typography::class.java.getResourceAsStream("/fonts/quicksand_bold.ttf")
            if (regStream != null && boldStream != null) {
                val tempReg = File.createTempFile("quicksand_reg", ".ttf").apply {
                    deleteOnExit()
                    writeBytes(regStream.readBytes())
                }
                val tempBold = File.createTempFile("quicksand_bold", ".ttf").apply {
                    deleteOnExit()
                    writeBytes(boldStream.readBytes())
                }
                FontFamily(
                    Font(tempReg, FontWeight.Normal),
                    Font(tempBold, FontWeight.Bold)
                )
            } else {
                FontFamily.Default
            }
        }
    } catch (e: Exception) {
        FontFamily.Default
    }
}

val AppFontFamily = loadCustomFontFamily()

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)
