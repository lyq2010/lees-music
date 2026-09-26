package com.lyq2010.leesmusic.data.api

import kotlinx.serialization.Serializable

@Serializable
data class LyricsList(val structuredLyrics: List<StructuredLyrics> = emptyList())

@Serializable
data class StructuredLyrics(
    val synced: Boolean = false,
    val offset: Long = 0,
    val line: List<LyricLine> = emptyList(),
) {
    fun activeLine(positionMs: Long): Int = if (!synced) -1 else
        line.indexOfLast { it.start != null && it.start <= positionMs + offset }
}

@Serializable
data class LyricLine(val value: String, val start: Long? = null)

@Serializable
data class PlainLyrics(val value: String = "")
