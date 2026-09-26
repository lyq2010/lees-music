package com.lyq2010.leesmusic.ui.library

import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LibraryDiskRestoreTest {
    @Test fun restartedPageUsesPersistedDataWithoutNetworkAndExplicitRefreshReplacesIt() = runBlocking {
        var saved: LibraryPage? = null
        val first = LibraryPageState({ saved }, { saved = it })
        val expected = LibraryPage(songs = listOf(LibrarySong("one", "歌曲", "艺术家", null, null)))
        first.load(0, 0) { expected }
        val restarted = LibraryPageState({ saved }, { saved = it })
        restarted.load(0, 0) { error("重启后不应重新请求") }
        assertEquals(expected, restarted.data)
        restarted.load(0, 0, force = true) { LibraryPage() }
        assertEquals(LibraryPage(), saved)
    }

    @Test fun emptyOverviewSurvivesRestartAndMissingCacheStillFetches() = runBlocking {
        var saved: LibraryOverview? = null
        var requests = 0
        LibraryOverviewState({ saved }, { saved = it }).load(0, 0) {
            requests++; LibraryOverview(emptyList(), emptyList())
        }
        val restarted = LibraryOverviewState({ saved }, { saved = it })
        restarted.load(0, 0) { error("空歌单也是有效缓存") }
        assertNotNull(restarted.data)
        assertEquals(1, requests)
        restarted.load(1, 0) { requests++; LibraryOverview(emptyList(), emptyList()) }
        assertEquals(2, requests)
    }
}
