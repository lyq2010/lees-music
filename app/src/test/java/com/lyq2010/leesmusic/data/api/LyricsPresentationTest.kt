package com.lyq2010.leesmusic.data.api

import com.lyq2010.leesmusic.ui.lyrics.currentLine
import com.lyq2010.leesmusic.ui.lyrics.displayLines
import com.lyq2010.leesmusic.ui.lyrics.playbackHint
import org.junit.Assert.*
import org.junit.Test

class LyricsPresentationTest {
    @Test fun simultaneousOriginalAndTranslationStayTogether() {
        val lyrics = StructuredLyrics(true, 100, listOf(
            LyricLine("Original", 1000), LyricLine("译文", 1000), LyricLine("Next", 2000)))
        val rows = lyrics.displayLines()
        assertEquals(2, rows.size)
        assertEquals(listOf("译文"), rows[0].secondary)
        assertEquals(900L, rows[0].timeMs)
        assertEquals(0, rows.currentLine(1000))
        assertEquals(1, rows.currentLine(2000))
        assertEquals(0, rows.currentLine(1000))
    }

    @Test fun instrumentalDoesNotBecomeInvisibleFocusedRow() {
        val lyrics = StructuredLyrics(true, line = listOf(
            LyricLine("First", 1000), LyricLine(" ", 2000), LyricLine("Next", 4000)))
        assertEquals("前奏", lyrics.playbackHint(0))
        assertEquals("间奏", lyrics.playbackHint(3000))
        assertEquals(2, lyrics.displayLines().size)
        assertEquals(0, lyrics.displayLines().currentLine(3000))
    }

    @Test fun plainLyricsRemainSeparateAndCannotSeek() {
        val rows = StructuredLyrics(line = listOf(LyricLine("a"), LyricLine("b"))).displayLines()
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.timeMs == null })
        assertEquals(-1, rows.currentLine(10000))
    }
}
