package com.gramo.mediaplayer.data

import com.gramo.mediaplayer.model.LyricLine
import java.util.Locale

object LyricsParser {

    private val lrcTagRegex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val srtTimeRegex = Regex("""(\d{2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})[,.](\d{3})""")

    fun parse(rawContent: String, totalDurationMs: Long = 0L): List<LyricLine> {
        val text = rawContent.trim()
        if (text.isEmpty()) return emptyList()

        if (srtTimeRegex.containsMatchIn(text)) {
            return parseSrt(text)
        }

        if (lrcTagRegex.containsMatchIn(text)) {
            return parseLrc(text)
        }

        val plainLines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (plainLines.isEmpty()) return emptyList()

        val effectiveDuration = if (totalDurationMs > 3000L) totalDurationMs else plainLines.size * 4000L
        val stepMs = (effectiveDuration / plainLines.size).coerceAtLeast(1500L)
        return plainLines.mapIndexed { idx, line ->
            LyricLine(startTimeMs = idx * stepMs, text = line)
        }
    }

    private fun parseLrc(text: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        for (rawLine in text.lines()) {
            val matches = lrcTagRegex.findAll(rawLine).toList()
            if (matches.isEmpty()) continue
            val lyricText = rawLine.replace(lrcTagRegex, "").trim()
            for (m in matches) {
                val min = m.groupValues[1].toLongOrNull() ?: 0L
                val sec = m.groupValues[2].toLongOrNull() ?: 0L
                val fracStr = m.groupValues[3]
                val ms = when (fracStr.length) {
                    0 -> 0L
                    1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                    else -> fracStr.take(3).toLongOrNull() ?: 0L
                }
                val startMs = min * 60_000L + sec * 1_000L + ms
                result.add(LyricLine(startTimeMs = startMs, text = lyricText))
            }
        }
        return result.sortedBy { it.startTimeMs }
    }

    private fun parseSrt(text: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        val blocks = text.split(Regex("""\r?\n\s*\r?\n"""))
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size < 2) continue
            val timeLineIndex = lines.indexOfFirst { srtTimeRegex.containsMatchIn(it) }
            if (timeLineIndex == -1) continue

            val match = srtTimeRegex.find(lines[timeLineIndex]) ?: continue
            val startMs = hmsToMs(
                match.groupValues[1].toLong(),
                match.groupValues[2].toLong(),
                match.groupValues[3].toLong(),
                match.groupValues[4].toLong()
            )
            val endMs = hmsToMs(
                match.groupValues[5].toLong(),
                match.groupValues[6].toLong(),
                match.groupValues[7].toLong(),
                match.groupValues[8].toLong()
            )
            val caption = lines.drop(timeLineIndex + 1)
                .joinToString("\n")
                .replace(Regex("<[^>]*>"), "")
                .trim()

            if (caption.isNotEmpty()) {
                result.add(LyricLine(startTimeMs = startMs, text = caption))
                result.add(LyricLine(startTimeMs = endMs, text = ""))
            }
        }
        return result.sortedBy { it.startTimeMs }
    }

    private fun hmsToMs(h: Long, m: Long, s: Long, ms: Long): Long {
        return h * 3_600_000L + m * 60_000L + s * 1_000L + ms
    }

    fun formatTimestampTag(timeMs: Long): String {
        val safeMs = timeMs.coerceAtLeast(0L)
        val minutes = safeMs / 60_000L
        val seconds = (safeMs % 60_000L) / 1_000L
        val centis = (safeMs % 1_000L) / 10L
        return String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, centis)
    }

    fun stripTimestamps(rawContent: String): List<String> {
        return rawContent.lines()
            .map { it.replace(lrcTagRegex, "").trim() }
            .filter { it.isNotEmpty() }
    }

    fun findActiveLyric(lines: List<LyricLine>, positionMs: Long): String? {
        if (lines.isEmpty()) return null
        val target = positionMs + 80L
        var low = 0
        var high = lines.lastIndex
        var bestIndex = -1

        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].startTimeMs <= target) {
                bestIndex = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        if (bestIndex < 0) return null
        return lines[bestIndex].text.takeIf { it.isNotBlank() }
    }
}
