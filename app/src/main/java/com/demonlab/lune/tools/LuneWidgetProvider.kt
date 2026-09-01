package com.demonlab.lune.tools

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.RenderEffect
import android.graphics.Shader
import android.widget.RemoteViews
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.os.Build
import com.demonlab.lune.R
import com.demonlab.lune.ui.activities.Lune
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class LuneWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val serviceIntent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_UPDATE_WIDGET
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.demonlab.lune.WIDGET_UPDATE" || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, LuneWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val serviceIntent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_UPDATE_WIDGET
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {

        fun applyWidgetStyling(
            context: Context,
            views: RemoteViews,
            settingsManager: SettingsManager
        ) {
            val isNight = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val isSolid = settingsManager.widgetUseSolidBackground
            val solidColor = if (isNight) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            val isLightBg = isSolid && isColorLight(solidColor)

            if (isLightBg) {
                val darkText = android.graphics.Color.parseColor("#151515")
                val darkSubtext = android.graphics.Color.parseColor("#555555")
                views.setTextColor(R.id.widget_title, darkText)
                views.setTextColor(R.id.widget_artist, darkSubtext)
                views.setTextColor(R.id.widget_output_text, darkText)
                views.setInt(R.id.widget_output_icon, "setColorFilter", darkText)
                views.setInt(R.id.widget_prev, "setColorFilter", darkText)
                views.setInt(R.id.widget_play_pause, "setColorFilter", darkText)
                views.setInt(R.id.widget_next, "setColorFilter", darkText)
            } else {
                val white = android.graphics.Color.WHITE
                val subtext = android.graphics.Color.parseColor("#D0D0D0")
                views.setTextColor(R.id.widget_title, white)
                views.setTextColor(R.id.widget_artist, subtext)
                views.setTextColor(R.id.widget_output_text, white)
                views.setInt(R.id.widget_output_icon, "setColorFilter", white)
                views.setInt(R.id.widget_prev, "setColorFilter", white)
                views.setInt(R.id.widget_play_pause, "setColorFilter", white)
                views.setInt(R.id.widget_next, "setColorFilter", white)
            }

            if (settingsManager.widgetCircularCover) {
                views.setInt(R.id.cover_container, "setBackgroundResource", R.drawable.widget_cover_circle_shape)
            } else {
                views.setInt(R.id.cover_container, "setBackgroundResource", R.drawable.widget_cover_shape)
            }

            if (isSolid) {
                views.setImageViewBitmap(R.id.widget_blur_bg, getSolidColorBitmap(solidColor))
                views.setViewVisibility(R.id.widget_blur_bg, android.view.View.VISIBLE)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val playbackManager = PlaybackManager.getInstance(context)
            val currentSong = playbackManager.currentSong
            val isPlaying = playbackManager.isPlaying

            val views = RemoteViews(context.packageName, R.layout.lune_widget_layout)

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            val isCompact = minHeight in 1..125

            val openAppIntent = Intent(context, Lune::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            if (currentSong != null) {
                views.setTextViewText(R.id.widget_title, currentSong.title)
                views.setTextViewText(R.id.widget_artist, currentSong.artist)

                if (isCompact) {
                    views.setViewVisibility(R.id.widget_title, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_artist, android.view.View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_title, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_artist, android.view.View.VISIBLE)
                }

                views.setImageViewResource(R.id.widget_play_pause,
                    if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)

                views.setImageViewResource(R.id.widget_output_icon, getOutputIconRes(context))
                views.setTextViewText(R.id.widget_output_text, getOutputName(context))
            } else {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.no_song_playing))
                views.setTextViewText(R.id.widget_artist, "")

                if (isCompact) {
                    views.setViewVisibility(R.id.widget_title, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_artist, android.view.View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_title, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_artist, android.view.View.VISIBLE)
                }
            }

            views.setOnClickPendingIntent(R.id.widget_play_pause, getServicePendingIntent(context,
                if (isPlaying) MusicService.ACTION_PAUSE else MusicService.ACTION_PLAY))
            views.setOnClickPendingIntent(R.id.widget_prev, getServicePendingIntent(context, MusicService.ACTION_PREVIOUS))
            views.setOnClickPendingIntent(R.id.widget_next, getServicePendingIntent(context, MusicService.ACTION_NEXT))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getServicePendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MusicService::class.java).apply {
                this.action = action
            }
            return PendingIntent.getService(context, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
        }

        private fun getOutputIconRes(context: Context): Int {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> return R.drawable.ic_bluetooth
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> return R.drawable.ic_headphones
                    else -> { /* Catch-all fallback block to suppress compiler warnings on unhandled constants */ }
                }
            }
            return R.drawable.ic_speaker
        }

        private fun getOutputName(context: Context): String {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> return context.getString(R.string.output_bluetooth)
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> return context.getString(R.string.output_headphones)
                    else -> { /* Catch-all fallback block to suppress compiler warnings on unhandled constants */ }
                }
            }
            return context.getString(R.string.output_speaker)
        }


        fun isColorLight(color: Int): Boolean {
            val darkness = 1 - (0.299 * android.graphics.Color.red(color) + 0.587 * android.graphics.Color.green(color) + 0.114 * android.graphics.Color.blue(color)) / 255
            return darkness < 0.45
        }

        fun getSolidColorBitmap(color: Int, width: Int = 360, height: Int = 200, cornerRadius: Int = 28): Bitmap {
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint().apply {
                isAntiAlias = true
                this.color = color
            }
            val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val roundPx = cornerRadius.toFloat()
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
            return output
        }

        fun getCircularBitmap(bitmap: Bitmap): Bitmap {
            val maxDim = 320
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim || bitmap.width != bitmap.height) {
                val size = Math.min(bitmap.width, Math.min(bitmap.height, maxDim))
                val x = (bitmap.width - size) / 2
                val y = (bitmap.height - size) / 2
                val square = Bitmap.createBitmap(bitmap, x, y, size, size)
                if (size != maxDim) Bitmap.createScaledBitmap(square, maxDim, maxDim, true) else square
            } else bitmap

            val size = scaledBitmap.width
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint().apply { isAntiAlias = true }
            val radius = size / 2f
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = android.graphics.Color.BLACK
            canvas.drawCircle(radius, radius, radius, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            return output
        }

        fun getVinylRecordBitmap(bitmap: Bitmap): Bitmap {
            val size = 320
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val center = size / 2f

            // 1. Vinyl disc black base
            val discPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#101010")
            }
            canvas.drawCircle(center, center, center, discPaint)

            // 2. Concentric grooves
            val groovePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            val grooveFractions = floatArrayOf(0.92f, 0.85f, 0.78f, 0.70f, 0.63f)
            for (fraction in grooveFractions) {
                groovePaint.color = android.graphics.Color.argb(
                    if (fraction == 0.85f || fraction == 0.70f) 22 else 14,
                    255, 255, 255
                )
                canvas.drawCircle(center, center, center * fraction, groovePaint)
            }

            // 3. Center circular album art (scaled to 55% diameter)
            val artSize = (size * 0.55f).toInt()
            val circularArt = getCircularBitmap(bitmap)
            val scaledArt = if (circularArt.width != artSize) {
                Bitmap.createScaledBitmap(circularArt, artSize, artSize, true)
            } else circularArt

            val artOffset = (size - artSize) / 2f
            canvas.drawBitmap(scaledArt, artOffset, artOffset, null)
            if (scaledArt != circularArt) {
                scaledArt.recycle()
            }
            circularArt.recycle()

            // 4. Subtle ring around center art
            val ringPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = android.graphics.Color.parseColor("#252525")
            }
            canvas.drawCircle(center, center, artSize / 2f, ringPaint)

            // 5. Center spindle hole
            val holePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#101010")
            }
            val holeBorderPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                color = android.graphics.Color.argb(60, 255, 255, 255)
            }
            canvas.drawCircle(center, center, 10f, holePaint)
            canvas.drawCircle(center, center, 10f, holeBorderPaint)

            return output
        }

        fun getSquareScaledBitmap(bitmap: Bitmap, maxDim: Int = 400): Bitmap {
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim || bitmap.width != bitmap.height) {
                val size = Math.min(bitmap.width, bitmap.height)
                val x = (bitmap.width - size) / 2
                val y = (bitmap.height - size) / 2
                val square = Bitmap.createBitmap(bitmap, x, y, size, size)
                val targetSize = Math.min(size, maxDim)
                if (square.width != targetSize) {
                    val scaled = Bitmap.createScaledBitmap(square, targetSize, targetSize, true)
                    if (square != bitmap) square.recycle()
                    scaled
                } else {
                    square
                }
            } else {
                bitmap
            }
            return scaledBitmap
        }

        fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Int): Bitmap {
            val maxDim = 320
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetW = if (aspect >= 1f) maxDim else (maxDim * aspect).toInt()
                val targetH = if (aspect <= 1f) maxDim else (maxDim / aspect).toInt()
                Bitmap.createScaledBitmap(bitmap, Math.max(1, targetW), Math.max(1, targetH), true)
            } else bitmap

            val output = Bitmap.createBitmap(scaledBitmap.width, scaledBitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint().apply { isAntiAlias = true }
            val rect = Rect(0, 0, scaledBitmap.width, scaledBitmap.height)
            val rectF = RectF(rect)
            val roundPx = pixels.toFloat()
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = -0xbdbdbe
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaledBitmap, rect, rect, paint)

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            return output
        }

        fun fastBlur(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
            var width = Math.round(sentBitmap.width * scale)
            var height = Math.round(sentBitmap.height * scale)
            val bitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false).copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
            
            if (radius < 1) {
                return null
            }
            val w = bitmap.width
            val h = bitmap.height
            val pix = IntArray(w * h)
            bitmap.getPixels(pix, 0, w, 0, 0, w, h)
            val wm = w - 1
            val hm = h - 1
            val wh = w * h
            val div = radius + radius + 1
            val r = IntArray(wh)
            val g = IntArray(wh)
            val b = IntArray(wh)
            var rsum: Int; var gsum: Int; var bsum: Int; var x: Int; var y: Int; var i: Int; var p: Int; var yp: Int; var yi: Int; var yw: Int
            val vmin = IntArray(Math.max(w, h))
            var divsum = div + 1 shr 1
            divsum *= divsum
            val dv = IntArray(256 * divsum)
            i = 0
            while (i < 256 * divsum) {
                dv[i] = i / divsum
                i++
            }
            yi = 0; yw = yi
            val stack = Array(div) { IntArray(3) }
            var stackpointer: Int; var stackstart: Int; var sir: IntArray; var rbs: Int
            val r1 = radius + 1
            var routsum: Int; var goutsum: Int; var boutsum: Int; var rinsum: Int; var ginsum: Int; var binsum: Int
            y = 0
            while (y < h) {
                bsum = 0; gsum = bsum; rsum = gsum; boutsum = rsum; goutsum = boutsum; routsum = goutsum; binsum = routsum; ginsum = binsum; rinsum = ginsum
                i = -radius
                while (i <= radius) {
                    p = pix[yi + Math.min(wm, Math.max(i, 0))]
                    sir = stack[i + radius]
                    sir[0] = p and 0xff0000 shr 16
                    sir[1] = p and 0x00ff00 shr 8
                    sir[2] = p and 0x0000ff
                    rbs = r1 - Math.abs(i)
                    rsum += sir[0] * rbs
                    gsum += sir[1] * rbs
                    bsum += sir[2] * rbs
                    if (i > 0) {
                        rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                    } else {
                        routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                    }
                    i++
                }
                stackpointer = radius
                x = 0
                while (x < w) {
                    r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum]
                    rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                    stackstart = stackpointer - radius + div
                    sir = stack[stackstart % div]
                    routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                    if (y == 0) {
                        vmin[x] = Math.min(x + radius + 1, wm)
                    }
                    p = pix[yw + vmin[x]]
                    sir[0] = p and 0xff0000 shr 16; sir[1] = p and 0x00ff00 shr 8; sir[2] = p and 0x0000ff
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                    rsum += rinsum; gsum += ginsum; bsum += binsum
                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer % div]
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                    rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                    yi++; x++
                }
                yw += w; y++
            }
            x = 0
            while (x < w) {
                bsum = 0; gsum = bsum; rsum = gsum; boutsum = rsum; goutsum = boutsum; routsum = goutsum; binsum = routsum; ginsum = binsum; rinsum = ginsum
                yp = -radius * w
                i = -radius
                while (i <= radius) {
                    yi = Math.max(0, yp) + x
                    sir = stack[i + radius]
                    sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[yi]
                    rbs = r1 - Math.abs(i)
                    rsum += r[yi] * rbs; gsum += g[yi] * rbs; bsum += b[yi] * rbs
                    if (i > 0) {
                        rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                    } else {
                        routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                    }
                    if (i < hm) {
                        yp += w
                    }
                    i++
                }
                yi = x
                stackpointer = radius
                y = 0
                while (y < h) {
                    pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                    rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                    stackstart = stackpointer - radius + div
                    sir = stack[stackstart % div]
                    routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                    if (x == 0) {
                        vmin[y] = Math.min(y + r1, hm) * w
                    }
                    p = x + vmin[y]
                    sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p]
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                    rsum += rinsum; gsum += ginsum; bsum += binsum
                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer]
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                    rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                    yi += w; y++
                }
                x++
            }
            bitmap.setPixels(pix, 0, w, 0, 0, w, h)
            return bitmap
        }

        fun getBlurredBitmap(context: Context, bitmap: Bitmap, radius: Int, cornerRadius: Int, darkness: Float = 0.50f): Bitmap {
            val targetW = 600
            val targetH = 300
            
            return try {
                val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                
                val basePaint = Paint().apply { color = android.graphics.Color.parseColor("#141316") }
                canvas.drawRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), basePaint)
                
                val scale = 150f / Math.max(bitmap.width, bitmap.height)
                val blurred = fastBlur(bitmap, scale, radius.coerceIn(1, 50))
                
                val paint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                    val artAlpha = ((1f - (darkness * 0.7f).coerceIn(0f, 0.9f)) * 255).toInt()
                    alpha = artAlpha
                }
                
                if (blurred != null) {
                    canvas.drawBitmap(blurred, null, Rect(0, 0, targetW, targetH), paint)
                    blurred.recycle()
                } else {
                    canvas.drawBitmap(bitmap, null, Rect(0, 0, targetW, targetH), paint)
                }

                if (darkness > 0.3f) {
                    val scrimPaint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        alpha = (((darkness - 0.3f) / 0.7f) * 130).toInt()
                    }
                    canvas.drawRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), scrimPaint)
                }

                output
            } catch (_: Exception) {
                bitmap
            }
        }
    }
}
