package com.lyq2010.leesmusic.data.api

import org.junit.Assert.*
import org.junit.Test

class LyricsTextTest {
    @Test fun invalidServerTagsRemainPlainWithoutInventedTiming() {
        val lyrics = parseLyricsText("[00:00.00-1] 作词：Anna Müller\n[00:00.00-1] 作曲：Anna Müller")!!
        assertFalse(lyrics.synced)
        assertEquals(listOf("作词：Anna Müller", "作曲：Anna Müller"), lyrics.line.map { it.value })
        assertTrue(lyrics.line.all { it.start == null })
    }
    @Test fun validLrcPreservesOffsetsFractionsMultipleTimestampsAndBlankInterludes() {
        val lyrics = parseLyricsText("[ar:Artist]\n[offset:-250]\n[00:02.25][01:02.250] Chorus\n[00:01.5]\n[00:00] Intro")!!
        assertTrue(lyrics.synced)
        assertEquals(-250L, lyrics.offset)
        assertEquals(listOf(0L, 1500L, 2250L, 62250L), lyrics.line.map { it.start })
        assertEquals("", lyrics.line[1].value)
        assertEquals(0, lyrics.activeLine(250))
    }
    @Test fun mixedTextDoesNotDiscardUntimedLinesOrFakeSync() {
        val lyrics = parseLyricsText("[00:01.00] One\n[Chorus]\n普通文本\n[00:99.00] Unknown")!!
        assertFalse(lyrics.synced)
        assertEquals(listOf("One", "[Chorus]", "普通文本", "[00:99.00] Unknown"), lyrics.line.map { it.value })
    }
    @Test fun metadataOnlyIsEmpty() {
        assertNull(parseLyricsText("[ar:Artist]\n[offset:10]\n[00:00.00]"))
    }
}
