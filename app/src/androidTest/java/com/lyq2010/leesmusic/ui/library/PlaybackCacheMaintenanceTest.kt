package com.lyq2010.leesmusic.ui.library

import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.DataSpec
import androidx.media3.common.C
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.playback.AdjustableCacheEvictor
import com.lyq2010.leesmusic.playback.PlaybackCache
import com.lyq2010.leesmusic.playback.PLAYBACK_CACHE_FRAGMENT_BYTES
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackCacheMaintenanceTest {
    private fun run(limit: Long = 300, test: (SimpleCache, AdjustableCacheEvictor, File) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val evictor = AdjustableCacheEvictor(limit)
        val directory = File(context.cacheDir, "reliability-test-${UUID.randomUUID()}")
        val cache = SimpleCache(directory, evictor,
            StandaloneDatabaseProvider(context))
        try { test(cache, evictor, directory) } finally { cache.release() }
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
    @Test fun evictsLeastRecentlyUsedAndShrinksImmediately() = run { cache, evictor, _ ->
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
    @Test fun clearingRetainsInUseSongs() = run { cache, _, _ ->
        write(cache, "current"); write(cache, "writing"); write(cache, "unused")
        assertEquals(200L, PlaybackCache.clearUnused(cache, setOf("current", "writing")))
        assertTrue(cache.isCached("current", 0, 100))
        assertTrue(cache.isCached("writing", 0, 100))
        assertFalse(cache.isCached("unused", 0, 100))
    }
    @Test fun continuousWritesStayWithinBudgetIncludingUncommittedFiles() = run(4L * 1024 * 1024) { cache, _, directory ->
        val chunk = ByteArray(512 * 1024)
        listOf(12L * 1024 * 1024, C.LENGTH_UNSET.toLong()).forEachIndexed { index, length ->
            val key = "stream-$index"
            val hole = cache.startReadWrite(key, 0, C.LENGTH_UNSET.toLong())
            val sink = CacheDataSink(cache, PLAYBACK_CACHE_FRAGMENT_BYTES)
            try {
                sink.open(DataSpec.Builder().setUri("https://example.com/$key").setKey(key)
                    .setLength(length).setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION).build())
                repeat(24) {
                    sink.write(chunk, 0, chunk.size)
                    val diskBytes = directory.walkTopDown().filter { it.isFile && it.name.endsWith(".v3.exo") }.sumOf { it.length() }
                    assertTrue("Disk cache grew to $diskBytes bytes", diskBytes <= 4L * 1024 * 1024)
                    assertTrue(cache.cacheSpace <= 4L * 1024 * 1024)
                }
            } finally { sink.close(); cache.releaseHoleSpan(hole) }
            assertTrue(cache.cacheSpace <= 4L * 1024 * 1024)
        }
    }
    @Test fun existingOversizedCacheIsTrimmedOnOpen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "reliability-test-${UUID.randomUUID()}")
        val initial = SimpleCache(directory, NoOpCacheEvictor(), StandaloneDatabaseProvider(context))
        try { repeat(6) { write(initial, "song-$it") }; assertEquals(600L, initial.cacheSpace) }
        finally { initial.release() }
        val reopened = SimpleCache(directory, AdjustableCacheEvictor(300), StandaloneDatabaseProvider(context))
        try { assertEquals(300L, reopened.cacheSpace) } finally { reopened.release() }
    }
}
