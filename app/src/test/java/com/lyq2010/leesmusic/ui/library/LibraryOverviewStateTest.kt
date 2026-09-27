package com.lyq2010.leesmusic.ui.library

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LibraryOverviewStateTest {
    @Test fun restoredOverviewShowsCacheTimeUntilRefresh() = runBlocking {
        val cached = LibraryOverview(emptyList(), emptyList())
        val state = LibraryOverviewState({ cached }, {}, { 1234L })
        state.load(0, 0) { error("已命中缓存") }
        assertEquals(1234L, state.cacheSavedAt)
        state.load(0, 0, force = true) { cached }
        assertEquals(0L, state.cacheSavedAt)
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
