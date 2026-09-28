package com.lyq2010.leesmusic.ui.library

import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LibraryOverviewStateTest {
    @Test fun restoredOverviewIsShownThenNetworkRefresh() = runBlocking {
        val cached = LibraryOverview(listOf(LibrarySong("old", "旧收藏", "", null, null)), emptyList())
        val fresh = LibraryOverview(listOf(LibrarySong("new", "新收藏", "", null, null)), emptyList())
        var fetched = 0
        var saved: LibraryOverview? = null
        val state = LibraryOverviewState({ cached }, { saved = it })
        state.load(0, 0) { fetched++; fresh }
        assertEquals(1, fetched)
        assertSame(fresh, state.data)
        assertSame(fresh, saved)
        assertNull(state.error)
    }

    @Test fun restoreThenFailedRefreshKeepsCache() = runBlocking {
        val cached = LibraryOverview(listOf(LibrarySong("old", "旧收藏", "", null, null)), emptyList())
        val state = LibraryOverviewState({ cached }, {})
        state.load(0, 0) { error("offline") }
        assertSame(cached, state.data)
        assertNotNull(state.error)
        assertFalse(state.loading)
    }

    @Test fun emptyOverviewIsCachedAndOnlyInvalidationOrMutationReloads() = runBlocking {
        val cache = LibraryBrowseCache()
        var calls = 0
        suspend fun load() = cache.overview.load(cache.generation, 0) { calls++; LibraryOverview(emptyList(), emptyList()) }
        load(); load()
        assertEquals(1, calls)
        assertNotNull(cache.overview.data)
        cache.invalidate(); load()
        assertEquals(2, calls)
        cache.overview.load(cache.generation, 1) { calls++; LibraryOverview(emptyList(), emptyList()) }
        assertEquals(3, calls)
        assertNull(LibraryBrowseCache().overview.data)
    }
    @Test fun failedRefreshRetainsKnownEmptyResultAndCanRetry() = runBlocking {
        val state = LibraryOverviewState()
        val empty = LibraryOverview(emptyList(), emptyList())
        state.load(0, 0) { empty }
        state.load(0, 0, true) { error("offline") }
        assertSame(empty, state.data)
        assertNotNull(state.error)
        assertFalse(state.loading)
        state.load(0, 0, true) { empty }
        assertNull(state.error)
    }
}
