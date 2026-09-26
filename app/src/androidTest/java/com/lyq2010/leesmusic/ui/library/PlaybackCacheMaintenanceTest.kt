package com.lyq2010.leesmusic.ui.library

import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.SimpleCache
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.playback.AdjustableCacheEvictor
import com.lyq2010.leesmusic.playback.PlaybackCache
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackCacheMaintenanceTest {
    private fun run(test: (SimpleCache, AdjustableCacheEvictor) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val evictor = AdjustableCacheEvictor(300)
        val cache = SimpleCache(File(context.cacheDir, "reliability-test-${UUID.randomUUID()}"), evictor,
            StandaloneDatabaseProvider(context))
        try { test(cache, evictor) } finally { cache.release() }
    }
    private fun write(cache: SimpleCache, key: String) {
        val hole = cache.startReadWrite(key, 0, 100)
        try {
            val file = cache.startFile(key, 0, 100)
            file.writeBytes(ByteArray(100))
            cache.commitFile(file, 100)
        } finally { cache.releaseHoleSpan(hole) }
        Thread.sleep(5) // Distinct access times for deterministic LRU order.
    }
    @Test fun evictsLeastRecentlyUsedAndShrinksImmediately() = run { cache, evictor ->
        write(cache, "old"); write(cache, "recent"); write(cache, "active")
        cache.startReadWrite("old", 0, 100) // Touch old so recent becomes the oldest.
        Thread.sleep(5)
        write(cache, "new")
        assertFalse(cache.isCached("recent", 0, 100))
        assertTrue(cache.isCached("old", 0, 100))
        assertEquals(300L, cache.cacheSpace)
        evictor.resize(cache, 100)
        assertEquals(100L, cache.cacheSpace)
        assertTrue(cache.isCached("new", 0, 100))
        evictor.resize(cache, 200)
        write(cache, "another")
        assertEquals(200L, cache.cacheSpace)
    }
    @Test fun clearingRetainsInUseSongs() = run { cache, _ ->
        write(cache, "current"); write(cache, "writing"); write(cache, "unused")
        assertEquals(200L, PlaybackCache.clearUnused(cache, setOf("current", "writing")))
        assertTrue(cache.isCached("current", 0, 100))
        assertTrue(cache.isCached("writing", 0, 100))
        assertFalse(cache.isCached("unused", 0, 100))
    }
}
