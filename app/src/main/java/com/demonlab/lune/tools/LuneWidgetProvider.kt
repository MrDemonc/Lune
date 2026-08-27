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
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.demonlab.lune.WIDGET_UPDATE") {
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
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
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

        fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Int): Bitmap {
            val maxDim = 320
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetW = if (aspect >= 1f) maxDim else (maxDim * aspect).toInt()
                val targetH = if (aspect <= 1f) maxDim else (maxDim / aspect).toInt()
                bitmap.scale(targetW.coerceAtLeast(1), targetH.coerceAtLeast(1))
            } else bitmap

            val output = createBitmap(scaledBitmap.width, scaledBitmap.height)
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

        fun getBlurredBitmap(context: Context, bitmap: Bitmap, radius: Int, cornerRadius: Int): Bitmap {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val targetW = 360
                    val targetH = 200
                    val scaled = bitmap.scale(targetW, targetH)
                    val output = createBitmap(targetW, targetH)
                    val canvas = Canvas(output)
                    val paint = Paint()
                    paint.isAntiAlias = true
                    try {
                        val setRenderEffectMethod = Paint::class.java.getMethod("setRenderEffect", RenderEffect::class.java)
                        setRenderEffectMethod.invoke(paint, RenderEffect.createBlurEffect(
                            radius.toFloat(),
                            radius.toFloat(),
                            Shader.TileMode.CLAMP
                        ))
                    } catch (_: Exception) {
                        // Fallback to software blur logic if RenderEffect fails or is not found
                    }
                    canvas.drawBitmap(scaled, 0f, 0f, paint)
                    scaled.recycle()
                    return getRoundedCornerBitmap(output, cornerRadius)
                } catch (_: Exception) { }
            }

            return try {
                val targetW = 360
                val targetH = 200
                val tinyWidth = (targetW / 8).coerceAtLeast(8)
                val tinyHeight = (targetH / 8).coerceAtLeast(8)

                val tinyBitmap = bitmap.scale(tinyWidth, tinyHeight)
                val output = createBitmap(targetW, targetH)
                val canvas = Canvas(output)
                val paint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                }

                canvas.drawBitmap(tinyBitmap, null, Rect(0, 0, targetW, targetH), paint)
                tinyBitmap.recycle()

                getRoundedCornerBitmap(output, cornerRadius)
            } catch (_: Exception) {
                getRoundedCornerBitmap(bitmap, cornerRadius)
            }
        }
    }
}