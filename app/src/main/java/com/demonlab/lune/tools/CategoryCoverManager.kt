package com.demonlab.lune.tools

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

object CategoryCoverManager {

    private val _updates = MutableStateFlow(0L)
    val updates: StateFlow<Long> = _updates.asStateFlow()

    private const val PREFS_NAME = "category_custom_covers"

    private fun getCoversDir(context: Context): File {
        val dir = File(context.filesDir, "category_covers")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun makeSafeKey(category: String, key: String): String {
        val safeCat = category.trim().uppercase()
        val safeItem = key.trim().lowercase()
        return "${safeCat}_${safeItem}"
    }

    private fun makeFilePrefix(category: String, key: String): String {
        val safeCat = category.trim().lowercase()
        val hash = Math.abs(key.trim().hashCode())
        val sanitized = key.trim().lowercase().replace(Regex("[^a-zA-Z0-9]"), "_").take(16)
        return "cat_${safeCat}_${sanitized}_${hash}_"
    }

    fun parseCovers(customCovers: String?): List<String> {
        if (customCovers.isNullOrBlank()) return emptyList()
        return customCovers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun formatCovers(covers: List<String>): String? {
        val valid = covers.map { it.trim() }.filter { it.isNotEmpty() }
        return if (valid.isEmpty()) null else valid.joinToString(",")
    }

    fun getCustomCovers(context: Context, category: String, key: String): List<String> {
        if (key.isBlank()) return emptyList()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prefKey = makeSafeKey(category, key)
        val raw = prefs.getString(prefKey, null) ?: return emptyList()
        val parsed = parseCovers(raw)
        return parsed.filter { uriStr ->
            if (uriStr.startsWith("file://")) {
                File(uriStr.removePrefix("file://")).exists()
            } else true
        }
    }

    fun saveCovers(context: Context, category: String, key: String, coverUris: List<String>): List<String> {
        val coversDir = getCoversDir(context)
        val baseInternalPrefix = "file://${coversDir.absolutePath}"
        val filePrefix = makeFilePrefix(category, key)
        val result = mutableListOf<String>()

        coverUris.forEach { uriStr ->
            if (uriStr.startsWith(baseInternalPrefix)) {
                val localFile = File(uriStr.removePrefix("file://"))
                if (localFile.exists()) {
                    result.add(uriStr)
                }
            } else {
                try {
                    val uri = Uri.parse(uriStr)
                    val fileName = "${filePrefix}${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
                    val targetFile = File(coversDir, fileName)

                    context.contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (targetFile.exists() && targetFile.length() > 0) {
                        result.add("file://${targetFile.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.e("CategoryCoverManager", "Failed to copy cover image: $uriStr", e)
                }
            }
        }

        // Clean up files for this key that are no longer active
        cleanUnusedCovers(context, category, key, result)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prefKey = makeSafeKey(category, key)
        val formatted = formatCovers(result)
        if (formatted != null) {
            prefs.edit().putString(prefKey, formatted).apply()
        } else {
            prefs.edit().remove(prefKey).apply()
        }

        _updates.value = System.currentTimeMillis()
        return result
    }

    fun cleanUnusedCovers(context: Context, category: String, key: String, activeCovers: List<String>) {
        try {
            val coversDir = getCoversDir(context)
            val filePrefix = makeFilePrefix(category, key)
            val activeFiles = activeCovers.map { it.removePrefix("file://") }.toSet()

            coversDir.listFiles { _, name -> name.startsWith(filePrefix) }?.forEach { file ->
                if (file.absolutePath !in activeFiles) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w("CategoryCoverManager", "Error cleaning unused covers for $category $key", e)
        }
    }

    fun clearCustomCovers(context: Context, category: String, key: String) {
        try {
            cleanUnusedCovers(context, category, key, emptyList())
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val prefKey = makeSafeKey(category, key)
            prefs.edit().remove(prefKey).apply()
            _updates.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w("CategoryCoverManager", "Error clearing custom covers for $category $key", e)
        }
    }
}
