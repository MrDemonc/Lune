package com.demonlab.lune.tools

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object CoverCache {
    private const val MAX_ENTRIES = 400
    private val memoryCache = object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > MAX_ENTRIES
        }
    }

    private val lock = Any()
    private val pendingLoads = ConcurrentHashMap<String, Boolean>()

    fun getFromMemory(path: String?): ImageBitmap? {
        if (path.isNullOrBlank()) return null
        synchronized(lock) {
            return memoryCache[path]
        }
    }

    fun putInMemory(path: String, bitmap: ImageBitmap) {
        synchronized(lock) {
            memoryCache[path] = bitmap
        }
    }

    suspend fun loadCover(path: String?): ImageBitmap? {
        if (path.isNullOrBlank()) return null

        // 1. Instant RAM cache hit
        synchronized(lock) {
            memoryCache[path]?.let { return it }
        }

        // 2. Decode in background IO thread
        return withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists() || !file.canRead()) return@withContext null

                val bytes = file.readBytes()
                val skiaImage = Image.makeFromEncoded(bytes)
                val composeBitmap = skiaImage.toComposeImageBitmap()

                synchronized(lock) {
                    memoryCache[path] = composeBitmap
                }
                composeBitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            memoryCache.clear()
        }
    }
}
