package com.lyq2010.leesmusic.ui.library

import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LibraryBrowseCacheTest {
    @Test fun restoredPageIsShownThenNetworkRefresh() = runBlocking {
        val cached = LibraryPage(songs = listOf(LibrarySong("cached", "旧歌曲", "", null, null)))
        val fresh = LibraryPage(songs = listOf(LibrarySong("fresh", "新歌曲", "", null, null)))
        var fetched = 0
        val state = LibraryPageState({ cached }, {})
        state.load(0, 0) { fetched++; fresh }
        assertEquals(1, fetched)
        assertSame(fresh, state.data)
        state.load(0, 0) { fetched++; error("不应请求") }
        assertEquals(1, fetched)
    }

    @Test fun restoreThenFailedRefreshKeepsCachedPage() = runBlocking {
        val cached = LibraryPage(songs = listOf(LibrarySong("cached", "旧歌曲", "", null, null)))
        val state = LibraryPageState({ cached }, {})
        state.load(0, 0) { error("已命中缓存后刷新失败") }
        assertSame(cached, state.data)
        assertNotNull(state.error)
    }
    @Test fun loadedAndEmptyPagesAreReusedButRefreshReplacesThem() = runBlocking {
        for (kind in listOf("songs", "albums", "artists")) {
            val cache = LibraryBrowseCache()
            val state = cache.page(LibraryDestination(kind, kind))
            var calls = 0
            state.load(0, 0) { calls++; LibraryPage() }
            assertSame(state, cache.page(LibraryDestination(kind, "改名")))
            state.load(0, 0) { calls++; error("不应请求") }
            assertEquals(1, calls)
            state.load(0, 0, force = true) { calls++; LibraryPage() }
            assertEquals(2, calls)
            cache.invalidate()
            state.load(cache.generation, 0) { calls++; LibraryPage() }
            assertEquals(3, calls)
        }
    }

    @Test fun failedRefreshKeepsOldDataAndCanRetry() = runBlocking {
        val state = LibraryPageState()
        val data = LibraryPage(songs = listOf(LibrarySong("a", "歌曲", "", null, null)))
        state.load(0, 0) { data }
        state.filter = "歌曲"
        val scrollState = state.list
        state.load(0, 0, force = true) { error("断网") }
        assertSame(data, state.data)
        assertNotNull(state.error)
        assertEquals("歌曲", state.filter)
        assertSame(scrollState, state.list)
        state.load(0, 0, force = true) { LibraryPage() }
        assertNull(state.error)
        assertTrue(state.data!!.songs.isEmpty())
    }

    @Test fun accountCachesAndArtistPagesAreIsolated() {
        val first = LibraryBrowseCache()
        val second = LibraryBrowseCache()
        val target = LibraryDestination("artists", "艺术家")
        assertNotSame(first.page(target), second.page(target))
        assertNotSame(first.page(LibraryDestination("artist", "同名", "a")), first.page(LibraryDestination("artist", "同名", "b")))
    }

    @Test fun cancelledLoadIsNotCachedAndMutationRevisionReloads() = runBlocking {
        val state = LibraryPageState()
        try { state.load(0, 0) { throw CancellationException() } }
        catch (_: CancellationException) { }
        assertNull(state.data)
        assertFalse(state.loading)
        var calls = 0
        state.load(0, 0) { calls++; LibraryPage() }
        state.load(0, 1) { calls++; LibraryPage() }
        assertEquals(2, calls)
    }
}
