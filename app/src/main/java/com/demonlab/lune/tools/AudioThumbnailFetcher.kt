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

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun fetch(): FetchResult? {
        // 1. Try modern ContentResolver.loadThumbnail (fast, system-cached, per-song correct)
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

        // 2. Fallback to embedded picture via MediaMetadataRetriever (slower, higher quality)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
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
            e.printStackTrace()
        } finally {
            retriever.release()
        }

        // 3. Final fallback to MediaStore album art
        val mediaId = try {
            ContentUris.parseId(uri)
        } catch (_: Exception) { null } ?: return null

        val projection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)
        val albumId = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            MediaStore.Audio.Media._ID + " = ?",
            arrayOf(mediaId.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
            } else null
        } ?: return null

        val albumArtUri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )

        return runCatching {
            context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    DrawableResult(
                        drawable = bitmap.toDrawable(context.resources),
                        isSampled = true,
                        dataSource = DataSource.DISK
                    )
                } else null
            }
        }.getOrNull()
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                data.scheme == "content" &&
                data.toString().contains("audio/media")) {
                return AudioThumbnailFetcher(data, options, context)
            }
            return null
        }
    }
}
