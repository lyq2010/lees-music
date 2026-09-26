package com.lyq2010.leesmusic.ui.search

import com.lyq2010.leesmusic.data.api.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SearchStateTest {
    @Test fun categoriesAndQueriesStayIsolatedAndReentryKeepsResults() = runBlocking {
        val state = SearchState()
        val a = state.page("old", "song")
        val b = state.page("new", "song")
        b.load("song") { SearchResult(song = listOf(Song("b", "new"))) }
        a.load("song") { SearchResult(song = listOf(Song("a", "old"))) }
        assertEquals("b", state.page("new", "song").result.song.single().id)
        assertFalse(state.page("new", "artist").loaded)
        assertSame(a, state.page("old", "song"))
    }

    @Test fun shortPagesContinueFailuresKeepResultsAndRetryUsesSameOffset() = runBlocking {
        val page = SearchPage()
        page.load("song") { assertEquals(0, it); SearchResult(song = listOf(Song("a", "A"))) }
        assertFalse(page.end)
        page.load("song") { assertEquals(1, it); error("offline") }
        assertNotNull(page.error)
        assertEquals(1, page.result.song.size)
        page.load("song") { assertEquals(1, it); SearchResult(song = listOf(Song("b", "B"))) }
        assertNull(page.error)
        page.load("song") { assertEquals(2, it); SearchResult() }
        assertTrue(page.end)
    }

    @Test fun cancellationReleasesLoadingAndRepeatedPagesDoNotGrowForever() = runBlocking {
        val page = SearchPage()
        try { page.load("song") { throw CancellationException() }; fail() } catch (_: CancellationException) { }
        assertFalse(page.loading)
        assertFalse(page.loaded)
        repeat(2) { page.load("song") { SearchResult(song = listOf(Song("a", "A"))) } }
        assertNotNull(page.error)
        assertEquals(1, page.result.song.size)
    }
}
