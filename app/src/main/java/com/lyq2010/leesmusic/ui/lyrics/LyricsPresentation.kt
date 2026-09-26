package com.lyq2010.leesmusic.ui.lyrics

import com.lyq2010.leesmusic.data.api.StructuredLyrics

internal data class DisplayLyric(val text: String, val secondary: List<String> = emptyList(), val timeMs: Long?)

internal fun StructuredLyrics.displayLines(): List<DisplayLyric> {
    val nonEmpty = line.filter { it.value.isNotBlank() }
    if (!synced) return nonEmpty.map { DisplayLyric(it.value.trim(), timeMs = null) }
    return nonEmpty.groupBy { it.start }.map { (start, group) ->
        DisplayLyric(group.first().value.trim(), group.drop(1).map { it.value.trim() },
            start?.let { (it - offset).coerceAtLeast(0) })
    }
}

internal fun List<DisplayLyric>.currentLine(positionMs: Long): Int =
    indexOfLast { it.timeMs != null && it.timeMs <= positionMs }

internal fun StructuredLyrics.playbackHint(positionMs: Long): String = when {
    !synced -> "纯文本歌词"
    activeLine(positionMs) < 0 -> "前奏"
    line[activeLine(positionMs)].value.isBlank() -> "间奏"
    else -> ""
}

sealed interface LyricsUiState {
    data object Loading : LyricsUiState
    data object Empty : LyricsUiState
    data object NoSong : LyricsUiState
    data object Failed : LyricsUiState
    data class Ready(val lyrics: StructuredLyrics) : LyricsUiState
}
