package com.demonlab.lune.ui.lyrics

import java.util.regex.Pattern

data class LyricWord(val timeMs: Long, val text: String)

data class LyricsLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null,
    val words: List<LyricWord> = emptyList()
)

val TIME_TAG_PATTERN: Pattern = Pattern.compile("[\\[<](\\d{1,2}):(\\d{2})[.:](\\d{1,3})[\\]>]")
val METADATA_TAG_PATTERN: Pattern = Pattern.compile("^\\[[a-zA-Z]+:.*?\\]$")

fun parseTime(minStr: String, secStr: String, msStr: String): Long {
    val m = minStr.toLongOrNull() ?: 0L
    val s = secStr.toLongOrNull() ?: 0L
    val ms = when (msStr.length) {
        1 -> (msStr.toLongOrNull() ?: 0L) * 100
        2 -> (msStr.toLongOrNull() ?: 0L) * 10
        else -> msStr.take(3).toLongOrNull() ?: 0L
    }
    return (m * 60 * 1000) + (s * 1000) + ms
}

fun parseLyrics(raw: String?, offsetAdjustmentMs: Long = 0L): List<LyricsLine> {
    if (raw.isNullOrBlank()) return emptyList()

    val offsetMatcher = Pattern.compile("\\[offset:\\s*([+-]?\\d+)\\]", Pattern.CASE_INSENSITIVE).matcher(raw)
    val fileOffset = if (offsetMatcher.find()) offsetMatcher.group(1)?.toLongOrNull() ?: 0L else 0L
    val totalOffset = fileOffset + offsetAdjustmentMs

    val lines = mutableListOf<LyricsLine>()
    var lastTimeMs = 0L

    raw.lines().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || METADATA_TAG_PATTERN.matcher(line).matches()) {
            return@forEach
        }

        val matcher = TIME_TAG_PATTERN.matcher(line)
        val matches = mutableListOf<Triple<Int, Int, Long>>() // startIdx, endIdx, timeMs
        while (matcher.find()) {
            val t = parseTime(matcher.group(1) ?: "0", matcher.group(2) ?: "0", matcher.group(3) ?: "0")
            val adjusted = (t + totalOffset).coerceAtLeast(0L)
            matches.add(Triple(matcher.start(), matcher.end(), adjusted))
        }

        if (matches.isEmpty()) {
            if (!line.startsWith("[")) {
                lines.add(LyricsLine(lastTimeMs, line))
            }
            return@forEach
        }

        val cleanText = TIME_TAG_PATTERN.matcher(line).replaceAll("").trim()

        var allTagsAtStart = true
        var curPos = 0
        val startTimes = mutableListOf<Long>()
        for (m in matches) {
            val prefix = line.substring(curPos, m.first).trim()
            if (prefix.isEmpty()) {
                startTimes.add(m.third)
                curPos = m.second
            } else {
                allTagsAtStart = false
                break
            }
        }

        val rest = line.substring(curPos).trim()
        val hasTagsInRest = TIME_TAG_PATTERN.matcher(rest).find()

        if (allTagsAtStart && !hasTagsInRest && cleanText.isNotEmpty()) {
            for (st in startTimes) {
                lines.add(LyricsLine(st, cleanText))
                lastTimeMs = st
            }
            return@forEach
        }

        if (cleanText.isEmpty()) {
            val t = matches.first().third
            lines.add(LyricsLine(t, ""))
            lastTimeMs = t
            return@forEach
        }

        val words = mutableListOf<LyricWord>()
        var lastEnd = 0
        var currentTime: Long? = null

        for (m in matches) {
            val textBefore = line.substring(lastEnd, m.first)
            val matchTime = m.third
            if (textBefore.isNotEmpty()) {
                val assignedTime = currentTime ?: matchTime
                words.add(LyricWord(assignedTime, textBefore))
            }
            currentTime = matchTime
            lastEnd = m.second
        }

        val textAfter = line.substring(lastEnd)
        if (textAfter.isNotEmpty() && currentTime != null) {
            words.add(LyricWord(currentTime, textAfter))
        }

        val lineStart = words.firstOrNull()?.timeMs ?: matches.first().third
        lines.add(LyricsLine(lineStart, cleanText, words = words))
        lastTimeMs = lineStart
    }

    val sorted = lines.sortedBy { it.timeMs }.toMutableList()

    val merged = mutableListOf<LyricsLine>()
    for (line in sorted) {
        val last = merged.lastOrNull()
        if (last != null && Math.abs(last.timeMs - line.timeMs) <= 300 && line.text.isNotBlank() && last.text.isNotBlank()) {
            if (last.translation == null) {
                merged[merged.size - 1] = last.copy(translation = line.text)
            } else {
                merged[merged.size - 1] = last.copy(translation = "${last.translation}\n${line.text}")
            }
        } else {
            merged.add(line)
        }
    }

    if (merged.isNotEmpty() && merged[0].timeMs > 2000) {
        merged.add(0, LyricsLine(0, ""))
    }

    return merged
}
