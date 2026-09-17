package com.demonlab.lune.tools

import android.content.Context
import android.util.Log
import java.io.File
import java.util.Locale
import java.util.regex.Pattern

data class LyricBackupItem(
    val songTitle: String,
    val songArtist: String,
    val lrcContent: String
)

class LyricsStorageManager private constructor(private val context: Context) {

    private val lyricsDir: File by lazy {
        File(context.filesDir, "lyrics").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun sanitizeForFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(100)
    }

    fun getCustomLyrics(song: Song): String? {
        // 1. By ID
        val idFile = File(lyricsDir, "${song.id}.lrc")
        if (idFile.exists() && idFile.isFile && idFile.length() > 0) {
            try {
                val content = CharsetUtils.readText(idFile)
                if (content.isNotBlank()) return content
            } catch (e: Exception) {
                Log.e("LyricsStorageManager", "Error reading lyrics file: ${idFile.name}", e)
            }
        }

        // 2. By Artist + Title sanitized
        val artistTitleFile = File(lyricsDir, "${sanitizeForFileName("${song.artist}_${song.title}")}.lrc")
        if (artistTitleFile.exists() && artistTitleFile.isFile && artistTitleFile.length() > 0) {
            try {
                val content = CharsetUtils.readText(artistTitleFile)
                if (content.isNotBlank()) return content
            } catch (e: Exception) {
                Log.e("LyricsStorageManager", "Error reading lyrics file: ${artistTitleFile.name}", e)
            }
        }

        return null
    }

    fun saveLyrics(song: Song, content: String): Boolean {
        return try {
            if (!lyricsDir.exists()) lyricsDir.mkdirs()

            // Ensure title/artist tags are included if not present
            val processedContent = ensureMetadataHeaders(content, song.title, song.artist)

            // Save by ID
            val idFile = File(lyricsDir, "${song.id}.lrc")
            idFile.writeText(processedContent, Charsets.UTF_8)

            // Save by Artist_Title
            val artistTitleFile = File(lyricsDir, "${sanitizeForFileName("${song.artist}_${song.title}")}.lrc")
            artistTitleFile.writeText(processedContent, Charsets.UTF_8)

            // Trigger auto-sync backup if active
            PlaylistBackupManager(context).triggerAutoSync()
            true
        } catch (e: Exception) {
            Log.e("LyricsStorageManager", "Error saving lyrics for ${song.title}", e)
            false
        }
    }

    fun deleteCustomLyrics(song: Song): Boolean {
        return try {
            var deleted = false
            val idFile = File(lyricsDir, "${song.id}.lrc")
            if (idFile.exists()) {
                deleted = idFile.delete() || deleted
            }
            val artistTitleFile = File(lyricsDir, "${sanitizeForFileName("${song.artist}_${song.title}")}.lrc")
            if (artistTitleFile.exists()) {
                deleted = artistTitleFile.delete() || deleted
            }

            // Trigger auto-sync backup if active
            PlaylistBackupManager(context).triggerAutoSync()
            deleted
        } catch (e: Exception) {
            Log.e("LyricsStorageManager", "Error deleting lyrics for ${song.title}", e)
            false
        }
    }

    fun hasCustomLyrics(song: Song): Boolean {
        val idFile = File(lyricsDir, "${song.id}.lrc")
        if (idFile.exists() && idFile.length() > 0) return true
        val artistTitleFile = File(lyricsDir, "${sanitizeForFileName("${song.artist}_${song.title}")}.lrc")
        return artistTitleFile.exists() && artistTitleFile.length() > 0
    }

    fun getAllCustomLyrics(): List<LyricBackupItem> {
        val results = mutableListOf<LyricBackupItem>()
        val seenKey = mutableSetOf<String>()

        val files = lyricsDir.listFiles { f -> f.isFile && f.name.endsWith(".lrc", ignoreCase = true) }
            ?: return emptyList()

        for (file in files) {
            try {
                val content = CharsetUtils.readText(file)
                if (content.isBlank()) continue

                val (title, artist) = extractTitleAndArtist(content, file.nameWithoutExtension)
                val key = "${artist.lowercase()}_${title.lowercase()}"
                if (key !in seenKey && title.isNotBlank()) {
                    seenKey.add(key)
                    results.add(LyricBackupItem(songTitle = title, songArtist = artist, lrcContent = content))
                }
            } catch (e: Exception) {
                Log.e("LyricsStorageManager", "Failed to parse lyric file for backup: ${file.name}", e)
            }
        }
        return results
    }

    fun restoreCustomLyrics(items: List<LyricBackupItem>, allSongs: List<Song> = emptyList()) {
        if (!lyricsDir.exists()) lyricsDir.mkdirs()

        val songsLookup = allSongs.associateBy {
            "${it.artist.trim().lowercase()}_${it.title.trim().lowercase()}"
        }

        for (item in items) {
            try {
                val content = ensureMetadataHeaders(item.lrcContent, item.songTitle, item.songArtist)

                // Save by Artist_Title
                val artistTitleFile = File(lyricsDir, "${sanitizeForFileName("${item.songArtist}_${item.songTitle}")}.lrc")
                artistTitleFile.writeText(content, Charsets.UTF_8)

                // If matched to a library song, also save by ID
                val key = "${item.songArtist.trim().lowercase()}_${item.songTitle.trim().lowercase()}"
                songsLookup[key]?.let { matchedSong ->
                    val idFile = File(lyricsDir, "${matchedSong.id}.lrc")
                    idFile.writeText(content, Charsets.UTF_8)
                }
            } catch (e: Exception) {
                Log.e("LyricsStorageManager", "Error restoring lyrics for ${item.songTitle}", e)
            }
        }
    }

    private fun ensureMetadataHeaders(content: String, title: String, artist: String): String {
        var result = content
        val hasTitle = Pattern.compile("^\\[ti:.*?\\]", Pattern.CASE_INSENSITIVE or Pattern.MULTILINE).matcher(result).find()
        val hasArtist = Pattern.compile("^\\[ar:.*?\\]", Pattern.CASE_INSENSITIVE or Pattern.MULTILINE).matcher(result).find()

        val prefixBuilder = java.lang.StringBuilder()
        if (!hasTitle && title.isNotBlank()) {
            prefixBuilder.append("[ti:").append(title).append("]\n")
        }
        if (!hasArtist && artist.isNotBlank()) {
            prefixBuilder.append("[ar:").append(artist).append("]\n")
        }

        return prefixBuilder.toString() + result
    }

    private fun extractTitleAndArtist(content: String, fallbackName: String): Pair<String, String> {
        var title = ""
        var artist = ""

        val tiMatcher = Pattern.compile("\\[ti:\\s*(.*?)\\]", Pattern.CASE_INSENSITIVE).matcher(content)
        if (tiMatcher.find()) {
            title = tiMatcher.group(1)?.trim() ?: ""
        }

        val arMatcher = Pattern.compile("\\[ar:\\s*(.*?)\\]", Pattern.CASE_INSENSITIVE).matcher(content)
        if (arMatcher.find()) {
            artist = arMatcher.group(1)?.trim() ?: ""
        }

        if (title.isBlank()) {
            val parts = fallbackName.split("_")
            if (parts.size >= 2) {
                artist = parts[0]
                title = parts.drop(1).joinToString("_")
            } else {
                title = fallbackName
            }
        }

        return Pair(title, artist)
    }

    companion object {
        @Volatile
        private var instance: LyricsStorageManager? = null

        fun getInstance(context: Context): LyricsStorageManager {
            return instance ?: synchronized(this) {
                instance ?: LyricsStorageManager(context.applicationContext).also { instance = it }
            }
        }

        fun formatLrcTimestamp(timeMs: Long): String {
            val safeMs = timeMs.coerceAtLeast(0L)
            val totalSeconds = safeMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val centis = (safeMs % 1000) / 10
            return String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, centis)
        }

        private val TIME_TAG_REGEX = Regex("[\\[<]\\d{1,2}:\\d{2}[.:]\\d{1,3}[\\]>]")

        fun stripTimestamps(rawLyrics: String): String {
            return rawLyrics.lines()
                .filterNot { it.startsWith("[ti:") || it.startsWith("[ar:") || it.startsWith("[al:") || it.startsWith("[by:") || it.startsWith("[offset:") }
                .map { line -> TIME_TAG_REGEX.replace(line, "").trim() }
                .joinToString("\n")
                .trim()
        }
    }
}
