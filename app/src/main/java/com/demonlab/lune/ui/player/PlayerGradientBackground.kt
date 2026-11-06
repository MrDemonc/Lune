package com.demonlab.lune.ui.player

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.demonlab.lune.tools.Song
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CoverPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color
)

fun extractPaletteFromBitmap(bitmap: Bitmap, isDarkTheme: Boolean): CoverPalette {
    val scaled = if (bitmap.width > 32 || bitmap.height > 32) {
        Bitmap.createScaledBitmap(bitmap, 32, 32, false)
    } else {
        bitmap
    }

    val w = scaled.width
    val h = scaled.height
    val hsl = FloatArray(3)

    fun sampleZone(startX: Int, endX: Int, startY: Int, endY: Int): Color? {
        var bestColor: Color? = null
        var maxScore = -1f
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0

        for (y in startY until endY) {
            for (x in startX until endX) {
                val pixel = scaled.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha < 128) continue

                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF
                sumR += r
                sumG += g
                sumB += b
                count++

                ColorUtils.RGBToHSL(r, g, b, hsl)
                val saturation = hsl[1]
                val lightness = hsl[2]

                val score = saturation * (1f - kotlin.math.abs(lightness - 0.5f) * 1.5f).coerceAtLeast(0.05f)
                if (score > maxScore && saturation > 0.10f && lightness in 0.08f..0.92f) {
                    maxScore = score
                    bestColor = Color(r, g, b)
                }
            }
        }

        return bestColor ?: if (count > 0) {
            Color((sumR / count).toInt(), (sumG / count).toInt(), (sumB / count).toInt())
        } else null
    }

    val cTopLeft = sampleZone(0, w / 2, 0, h / 2)
    val cTopRight = sampleZone(w / 2, w, 0, h / 2)
    val cBottomLeft = sampleZone(0, w / 2, h / 2, h)
    val cBottomRight = sampleZone(w / 2, w, h / 2, h)
    val cCenter = sampleZone(w / 4, 3 * w / 4, h / 4, 3 * h / 4)

    val fallbackBase = cCenter ?: Color(0xFF6750A4)
    val raw1 = cTopLeft ?: fallbackBase
    val raw2 = cTopRight ?: cCenter ?: fallbackBase
    val raw3 = cBottomLeft ?: cCenter ?: fallbackBase
    val raw4 = cBottomRight ?: fallbackBase

    fun adjust(color: Color): Color {
        val tempHsl = FloatArray(3)
        ColorUtils.RGBToHSL((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt(), tempHsl)
        if (isDarkTheme) {
            tempHsl[1] = tempHsl[1].coerceIn(0.40f, 0.90f)
            tempHsl[2] = tempHsl[2].coerceIn(0.28f, 0.52f)
        } else {
            tempHsl[1] = tempHsl[1].coerceIn(0.25f, 0.65f)
            tempHsl[2] = tempHsl[2].coerceIn(0.68f, 0.88f)
        }
        return Color(ColorUtils.HSLToColor(tempHsl))
    }

    val adj1 = adjust(raw1)
    val adj2 = adjust(raw2)
    val adj3 = adjust(raw3)
    val adj4 = adjust(raw4)

    // Ensure chromatic diversity if the artwork is monochrome or uniform
    val hsl0 = FloatArray(3)
    val hsl1 = FloatArray(3)
    ColorUtils.colorToHSL(adj1.toArgb(), hsl0)
    ColorUtils.colorToHSL(adj2.toArgb(), hsl1)
    val hueDiff = kotlin.math.abs(hsl0[0] - hsl1[0])

    if (hueDiff < 25f || hsl0[1] < 0.15f) {
        val baseHue = if (hsl0[1] < 0.15f) (if (isDarkTheme) 220f else 200f) else hsl0[0]
        val sat = if (isDarkTheme) 0.55f else 0.40f
        val lit = if (isDarkTheme) 0.38f else 0.76f

        fun shift(hShift: Float, sMod: Float = 1f, lMod: Float = 1f): Color {
            val h = (baseHue + hShift + 360f) % 360f
            return Color(ColorUtils.HSLToColor(floatArrayOf(h, (sat * sMod).coerceIn(0f, 1f), (lit * lMod).coerceIn(0f, 1f))))
        }

        return CoverPalette(
            primary = adj1,
            secondary = shift(35f),
            tertiary = shift(-35f),
            accent = shift(70f)
        )
    }

    return CoverPalette(adj1, adj2, adj3, adj4)
}

@Composable
fun rememberCoverPalette(song: Song?, isDarkTheme: Boolean): CoverPalette {
    val context = LocalContext.current
    val primaryScheme = MaterialTheme.colorScheme.primary
    val secondaryScheme = MaterialTheme.colorScheme.secondary
    val tertiaryScheme = MaterialTheme.colorScheme.tertiary
    val containerScheme = MaterialTheme.colorScheme.primaryContainer

    var palette by remember(song?.id, isDarkTheme) {
        mutableStateOf(
            CoverPalette(
                primary = primaryScheme,
                secondary = secondaryScheme,
                tertiary = tertiaryScheme,
                accent = containerScheme
            )
        )
    }

    LaunchedEffect(song?.id, song?.coverUrl, isDarkTheme) {
        if (song == null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val model = song.coverUrl ?: song.albumArtUri ?: song.uri
                val request = ImageRequest.Builder(context)
                    .data(model)
                    .size(64, 64)
                    .allowHardware(false)
                    .build()
                val result = (context.imageLoader.execute(request) as? SuccessResult)?.drawable
                if (result != null) {
                    val bitmap = when (result) {
                        is BitmapDrawable -> result.bitmap
                        else -> {
                            val bmp = Bitmap.createBitmap(
                                result.intrinsicWidth.coerceAtLeast(1),
                                result.intrinsicHeight.coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = AndroidCanvas(bmp)
                            result.setBounds(0, 0, canvas.width, canvas.height)
                            result.draw(canvas)
                            bmp
                        }
                    }
                    val extracted = extractPaletteFromBitmap(bitmap, isDarkTheme)
                    palette = extracted
                }
            } catch (_: Throwable) {
                // Keep default fallback
            }
        }
    }

    return palette
}

@Composable
fun PlayerGradientBackground(
    colors: CoverPalette,
    modifier: Modifier = Modifier,
    hasBlurBackground: Boolean = false,
    isDarkTheme: Boolean = true,
    alpha: Float = 1f
) {
    val posX1 = remember { Animatable(0.20f) }
    val posY1 = remember { Animatable(0.20f) }
    val scale1 = remember { Animatable(0.85f) }

    val posX2 = remember { Animatable(0.80f) }
    val posY2 = remember { Animatable(0.30f) }
    val scale2 = remember { Animatable(0.80f) }

    val posX3 = remember { Animatable(0.22f) }
    val posY3 = remember { Animatable(0.72f) }
    val scale3 = remember { Animatable(0.85f) }

    val posX4 = remember { Animatable(0.82f) }
    val posY4 = remember { Animatable(0.80f) }
    val scale4 = remember { Animatable(0.80f) }

    val posX5 = remember { Animatable(0.50f) }
    val posY5 = remember { Animatable(0.50f) }
    val scale5 = remember { Animatable(0.65f) }

    LaunchedEffect(Unit) {
        val smoothEasing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)

        fun animateWander(
            animatable: Animatable<Float, AnimationVector1D>,
            min: Float,
            max: Float,
            minDur: Int,
            maxDur: Int
        ) = launch {
            while (isActive) {
                val target = Random.nextFloat() * (max - min) + min
                val duration = Random.nextInt(minDur, maxDur)
                animatable.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = duration, easing = smoothEasing)
                )
            }
        }

        // Blob 1: Top-Left (Primary hue wanderer)
        animateWander(posX1, 0.06f, 0.38f, 8000, 15000)
        animateWander(posY1, 0.06f, 0.38f, 10000, 18000)
        animateWander(scale1, 0.75f, 0.95f, 9000, 16000)

        // Blob 2: Top-Right (Secondary hue wanderer)
        animateWander(posX2, 0.62f, 0.94f, 9000, 17000)
        animateWander(posY2, 0.12f, 0.44f, 8500, 16000)
        animateWander(scale2, 0.70f, 0.92f, 10000, 18000)

        // Blob 3: Bottom-Left (Tertiary hue wanderer)
        animateWander(posX3, 0.08f, 0.40f, 8500, 16500)
        animateWander(posY3, 0.58f, 0.88f, 9500, 17500)
        animateWander(scale3, 0.75f, 0.95f, 9000, 15500)

        // Blob 4: Bottom-Right (Accent hue wanderer)
        animateWander(posX4, 0.60f, 0.94f, 10000, 18000)
        animateWander(posY4, 0.60f, 0.92f, 9000, 16000)
        animateWander(scale4, 0.70f, 0.92f, 8500, 16500)

        // Blob 5: Center (Atmosphere & depth wanderer)
        animateWander(posX5, 0.35f, 0.65f, 11000, 19000)
        animateWander(posY5, 0.35f, 0.65f, 10500, 18500)
        animateWander(scale5, 0.55f, 0.75f, 9500, 17000)
    }

    val c1 by animateColorAsState(targetValue = colors.primary, animationSpec = tween(1200, easing = LinearOutSlowInEasing), label = "c1")
    val c2 by animateColorAsState(targetValue = colors.secondary, animationSpec = tween(1200, easing = LinearOutSlowInEasing), label = "c2")
    val c3 by animateColorAsState(targetValue = colors.tertiary, animationSpec = tween(1200, easing = LinearOutSlowInEasing), label = "c3")
    val c4 by animateColorAsState(targetValue = colors.accent, animationSpec = tween(1200, easing = LinearOutSlowInEasing), label = "c4")

    val surfaceColor = MaterialTheme.colorScheme.surface

    val baseAlpha = (when {
        isDarkTheme && !hasBlurBackground -> 0.55f
        isDarkTheme && hasBlurBackground -> 0.45f
        !isDarkTheme && !hasBlurBackground -> 0.45f
        else -> 0.35f
    } * alpha).coerceIn(0f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val maxDim = maxOf(w, h)

        // Blob 1: Top-Left (Primary cover hue)
        val center1 = Offset(w * posX1.value, h * posY1.value)
        val r1 = maxDim * scale1.value
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c1.copy(alpha = baseAlpha), Color.Transparent),
                center = center1,
                radius = r1
            ),
            center = center1,
            radius = r1
        )

        // Blob 2: Top-Right (Secondary cover hue)
        val center2 = Offset(w * posX2.value, h * posY2.value)
        val r2 = maxDim * scale2.value
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c2.copy(alpha = baseAlpha * 0.90f), Color.Transparent),
                center = center2,
                radius = r2
            ),
            center = center2,
            radius = r2
        )

        // Blob 3: Bottom-Left (Tertiary cover hue)
        val center3 = Offset(w * posX3.value, h * posY3.value)
        val r3 = maxDim * scale3.value
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c3.copy(alpha = baseAlpha * 0.95f), Color.Transparent),
                center = center3,
                radius = r3
            ),
            center = center3,
            radius = r3
        )

        // Blob 4: Bottom-Right (Accent cover hue)
        val center4 = Offset(w * posX4.value, h * posY4.value)
        val r4 = maxDim * scale4.value
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c4.copy(alpha = baseAlpha * 0.85f), Color.Transparent),
                center = center4,
                radius = r4
            ),
            center = center4,
            radius = r4
        )

        // Blob 5: Center blending blob for smooth cohesion
        val center5 = Offset(w * posX5.value, h * posY5.value)
        val r5 = maxDim * scale5.value
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c1.copy(alpha = baseAlpha * 0.40f), Color.Transparent),
                center = center5,
                radius = r5
            ),
            center = center5,
            radius = r5
        )

        // Legibility overlay in normal mode
        if (!hasBlurBackground) {
            if (isDarkTheme) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.55f to Color.Transparent,
                        0.80f to surfaceColor.copy(alpha = 0.40f * alpha),
                        1.00f to surfaceColor.copy(alpha = 0.70f * alpha)
                    )
                )
            } else {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.00f to surfaceColor.copy(alpha = 0.20f * alpha),
                        0.50f to Color.Transparent,
                        0.80f to surfaceColor.copy(alpha = 0.35f * alpha),
                        1.00f to surfaceColor.copy(alpha = 0.65f * alpha)
                    )
                )
            }
        }
    }
}
