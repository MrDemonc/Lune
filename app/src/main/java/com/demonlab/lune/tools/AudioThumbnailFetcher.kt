package com.demonlab.lune.tools

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.pxOrElse

class AudioThumbnailFetcher(
    private val uri: Uri,
    private val options: Options,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        // 1. Try modern ContentResolver.loadThumbnail (fast, system-cached, per-song correct)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == "content") {
            val thumbnail = runCatching {
                val width = options.size.width.pxOrElse { 512 }
                val height = options.size.height.pxOrElse { 512 }
                context.contentResolver.loadThumbnail(uri, android.util.Size(width, height), null)
            }.getOrNull()

            if (thumbnail != null) {
                return DrawableResult(
                    drawable = thumbnail.toDrawable(context.resources),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            }
        }

        // 2. Fallback to embedded picture via MediaMetadataRetriever (works for content:// and file://)
        val retriever = MediaMetadataRetriever()
        try {
            if (uri.scheme == "file") {
                val path = uri.path
                if (path != null && java.io.File(path).exists()) {
                    retriever.setDataSource(path)
                } else {
                    retriever.setDataSource(context, uri)
                }
            } else {
                retriever.setDataSource(context, uri)
            }
            retriever.embeddedPicture?.let { picture ->
                val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                if (bitmap != null) {
                    return DrawableResult(
                        drawable = bitmap.toDrawable(context.resources),
                        isSampled = false,
                        dataSource = DataSource.DISK
                    )
                }
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            runCatching { retriever.release() }
        }

        // Fallback returns null if no embedded thumbnail or picture exists
        return null
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val scheme = data.scheme
            if (scheme == "content" || scheme == "file") {
                val str = data.toString().lowercase()
                val isAudio = str.contains("audio") ||
                        str.endsWith(".mp3") ||
                        str.endsWith(".flac") ||
                        str.endsWith(".wav") ||
                        str.endsWith(".ogg") ||
                        str.endsWith(".m4a") ||
                        str.endsWith(".opus") ||
                        str.endsWith(".aac") ||
                        runCatching { context.contentResolver.getType(data)?.startsWith("audio/") == true }.getOrDefault(false)
                if (isAudio) {
                    return AudioThumbnailFetcher(data, options, context)
                }
            }
            return null
        }
    }
}
