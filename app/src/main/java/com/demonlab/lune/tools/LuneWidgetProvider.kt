package com.demonlab.lune.tools

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.widget.RemoteViews
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.renderscript.*
import com.demonlab.lune.R
import com.demonlab.lune.ui.activities.Lune

class LuneWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Handle specific updates if needed
        if (intent.action == "com.demonlab.lune.WIDGET_UPDATE") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, LuneWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val playbackManager = PlaybackManager.getInstance(context)
            val currentSong = playbackManager.currentSong
            val isPlaying = playbackManager.isPlaying
            
            val views = RemoteViews(context.packageName, R.layout.lune_widget_layout)

            // Open App on click
            val openAppIntent = Intent(context, Lune::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            if (currentSong != null) {
                views.setTextViewText(R.id.widget_title, currentSong.title)
                views.setTextViewText(R.id.widget_artist, currentSong.artist)
                
                // Update Progress
                val progress = (playbackManager.getProgress() * 100).toInt()
                views.setProgressBar(R.id.widget_progress, 100, progress, false)
                
                // Set Play/Pause Icon
                views.setImageViewResource(R.id.widget_play_pause, 
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
                
                // Audio Output
                views.setImageViewResource(R.id.widget_output_icon, getOutputIconRes(context))
                views.setTextViewText(R.id.widget_output_text, getOutputName(context))
            } else {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.no_song_playing))
                views.setTextViewText(R.id.widget_artist, "")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
                views.setViewVisibility(R.id.widget_blurred_background, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_dark_overlay, android.view.View.GONE)
            }

            // Button Intents
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                for (device in devices) {
                    when (device.type) {
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> return R.drawable.ic_bluetooth
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> return R.drawable.ic_headphones
                    }
                }
            }
            return R.drawable.ic_speaker
        }

        private fun getOutputName(context: Context): String {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                for (device in devices) {
                    when (device.type) {
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> return context.getString(R.string.output_bluetooth)
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> return context.getString(R.string.output_headphones)
                    }
                }
            }
            return context.getString(R.string.output_speaker)
        }

        fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Int): Bitmap {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint()
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val rectF = RectF(rect)
            val roundPx = pixels.toFloat()
            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = -0xbdbdbe
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            return output
        }

        fun getBlurredBitmap(context: Context, bitmap: Bitmap, radius: Int, cornerRadius: Int): Bitmap {
            return try {
                // Create a copy for blurring (RS needs a mutable bitmap or a source)
                val input = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
                
                val rs = RenderScript.create(context)
                val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
                val allIn = Allocation.createFromBitmap(rs, input)
                val allOut = Allocation.createFromBitmap(rs, output)
                
                blurScript.setRadius(25f) // High radius for deep defocus
                blurScript.setInput(allIn)
                blurScript.forEach(allOut)
                allOut.copyTo(output)
                
                rs.destroy()
                getRoundedCornerBitmap(output, cornerRadius)
            } catch (e: Exception) {
                // Fallback to simple rounded bitmap if RS fails
                getRoundedCornerBitmap(bitmap, cornerRadius)
            }
        }
    }
}
