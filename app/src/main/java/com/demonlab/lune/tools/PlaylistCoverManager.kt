package com.demonlab.lune.tools

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.UUID

object PlaylistCoverManager {

    private fun getCoversDir(context: Context): File {
        val dir = File(context.filesDir, "playlist_covers")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun parseCovers(customCovers: String?): List<String> {
        if (customCovers.isNullOrBlank()) return emptyList()
        return customCovers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun formatCovers(covers: List<String>): String? {
        val valid = covers.map { it.trim() }.filter { it.isNotEmpty() }
        return if (valid.isEmpty()) null else valid.joinToString(",")
    }

    fun saveCovers(context: Context, playlistId: Long, coverUris: List<String>): List<String> {
        val coversDir = getCoversDir(context)
        val baseInternalPrefix = "file://${coversDir.absolutePath}"
        val result = mutableListOf<String>()

        coverUris.forEachIndexed { index, uriStr ->
            if (uriStr.startsWith(baseInternalPrefix)) {
                val localFile = File(uriStr.removePrefix("file://"))
                if (localFile.exists()) {
                    result.add(uriStr)
                }
            } else {
                try {
                    val uri = Uri.parse(uriStr)
                    val fileName = "pl_${playlistId}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
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
                    Log.e("PlaylistCoverManager", "Failed to copy cover image: $uriStr", e)
                }
            }
        }

        // Clean up files belonging to this playlist that are no longer in the active list
        cleanUnusedCovers(context, playlistId, result)

        return result
    }

    fun cleanUnusedCovers(context: Context, playlistId: Long, activeCovers: List<String>) {
        try {
            val coversDir = getCoversDir(context)
            val prefix = "pl_${playlistId}_"
            val activeFiles = activeCovers.map { it.removePrefix("file://") }.toSet()

            coversDir.listFiles { _, name -> name.startsWith(prefix) }?.forEach { file ->
                if (file.absolutePath !in activeFiles) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w("PlaylistCoverManager", "Error cleaning unused covers for playlist $playlistId", e)
        }
    }

    fun deleteCoversForPlaylist(context: Context, playlistId: Long) {
        try {
            val coversDir = getCoversDir(context)
            val prefix = "pl_${playlistId}_"
            coversDir.listFiles { _, name -> name.startsWith(prefix) }?.forEach { file ->
                file.delete()
            }
        } catch (e: Exception) {
            Log.w("PlaylistCoverManager", "Error deleting covers for playlist $playlistId", e)
        }
    }
}
