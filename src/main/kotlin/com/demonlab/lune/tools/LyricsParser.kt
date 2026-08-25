package com.demonlab.lune.tools

import com.demonlab.lune.data.LyricLine
import com.demonlab.lune.data.ParsedLyrics
import java.io.File
import java.util.regex.Pattern

object LyricsParser {
    private val lrcPattern = Pattern.compile("\\[(\\d{2}):(\\d{2})(?:[.:](\\d{2,3}))?\\](.*)")

    fun parse(lyricsText: String?): ParsedLyrics {
        if (lyricsText.isNullOrBlank()) return ParsedLyrics()

        val lines = lyricsText.lines()
        val parsedLines = mutableListOf<LyricLine>()
        var hasTimestamps = false

        for (line in lines) {
            val matcher = lrcPattern.matcher(line.trim())
            if (matcher.find()) {
                hasTimestamps = true
                val minutes = matcher.group(1).toLongOrNull() ?: 0L
                val seconds = matcher.group(2).toLongOrNull() ?: 0L
                val millisRaw = matcher.group(3)
                val millis = when {
                    millisRaw == null -> 0L
                    millisRaw.length == 2 -> millisRaw.toLong() * 10
                    else -> millisRaw.take(3).padEnd(3, '0').toLong()
                }
                val timestampMs = (minutes * 60 + seconds) * 1000 + millis
                val text = matcher.group(4).trim()
                if (text.isNotEmpty() || parsedLines.isNotEmpty()) {
                    parsedLines.add(LyricLine(timestampMs, text))
                }
            } else if (!hasTimestamps && line.isNotBlank()) {
                parsedLines.add(LyricLine(0L, line.trim()))
            }
        }

        val sortedLines = if (hasTimestamps) parsedLines.sortedBy { it.timestampMs } else parsedLines
        return ParsedLyrics(
            lines = sortedLines,
            isSynced = hasTimestamps,
            rawText = lyricsText
        )
    }

    fun findLrcForSong(songPath: String): String? {
        val songFile = File(songPath)
        val lrcFile = File(songFile.parentFile, "${songFile.nameWithoutExtension}.lrc")
        return if (lrcFile.exists() && lrcFile.canRead()) {
            try {
                lrcFile.readText()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}
