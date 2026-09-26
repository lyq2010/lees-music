package com.lyq2010.leesmusic.data.library

import com.lyq2010.leesmusic.data.api.Song
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class DailyMixRefreshTest {
    @Test fun offlineRefreshKeepsPreviousSongsWithoutSavingNewDate() {
        val old = listOf(Song("old", "旧歌"))
        val result = refreshDailySongs({ throw IOException("offline") }, { fail("must not save") }, { old })
        assertEquals(old, result)
    }

    @Test fun firstLaunchOfflineReturnsEmpty() {
        assertTrue(refreshDailySongs({ throw IOException() }, {}, { emptyList() }).isEmpty())
    }

    @Test fun cancellationIsNotSwallowed() {
        assertThrows(CancellationException::class.java) {
            refreshDailySongs({ throw CancellationException() }, {}, { emptyList() })
        }
    }

    @Test fun successfulRefreshSavesNewSongs() {
        val fresh = listOf(Song("new", "新歌"))
        var saved = emptyList<Song>()
        assertEquals(fresh, refreshDailySongs({ fresh }, { saved = it }, { emptyList() }))
        assertEquals(fresh, saved)
    }
}
