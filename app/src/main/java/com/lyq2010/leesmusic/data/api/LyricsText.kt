package com.lyq2010.leesmusic.data.api

private val timestamp = Regex("^\\[(\\d{1,6}):([0-5]\\d)(?:[.:](\\d{1,3}))?]")
// Seen in Navidrome plain lyrics. -1 is not a valid LRC timestamp; never invent timing for it.
private val invalidTimestamp = Regex("^\\[\\d{1,6}:[0-5]\\d(?:[.:]\\d{1,3})?-1]\\s*")
private val metadata = Regex("^\\[(ar|ti|al|by|re|ve|length):.*]$", RegexOption.IGNORE_CASE)
private val offsetTag = Regex("^\\[offset:([+-]?\\d+)]$", RegexOption.IGNORE_CASE)

internal fun parseLyricsText(text: String, defaultOffset: Long = 0): StructuredLyrics? {
    val plain = mutableListOf<LyricLine>()
    val timed = mutableListOf<LyricLine>()
    var offset = defaultOffset
    var allTimed = true
    for (raw in text.lines()) {
        var value = raw.trim().removePrefix("\uFEFF")
        val offsetMatch = offsetTag.matchEntire(value)
        if (offsetMatch != null) {
            offset = offsetMatch.groupValues[1].toLongOrNull() ?: offset
            continue
        }
        if (metadata.matches(value)) continue
        val starts = mutableListOf<Long>()
        while (true) {
            val match = timestamp.find(value) ?: break
            val fraction = match.groupValues[3].padEnd(3, '0').toLong()
            starts += match.groupValues[1].toLong() * 60_000 + match.groupValues[2].toLong() * 1000 + fraction
            value = value.substring(match.value.length).trimStart()
        }
        value = invalidTimestamp.replaceFirst(value, "")
        plain += LyricLine(value)
        if (starts.isEmpty() && value.isNotBlank()) allTimed = false
        starts.forEach { timed += LyricLine(value, it) }
    }
    if (plain.none { it.value.isNotBlank() }) return null
    return if (allTimed && timed.isNotEmpty()) StructuredLyrics(true, offset, timed.sortedBy { it.start })
    else StructuredLyrics(line = plain)
}
