package com.lyq2010.leesmusic.ui.library

import androidx.media3.common.MediaItem
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.*
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.playback.*
import okhttp3.mockwebserver.*
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackCacheTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val audio = ByteArray(3 * 1024 * 1024) { (it % 251).toByte() }
    private fun read(source: DataSource, url: String): ByteArray {
        val output = ByteArrayOutputStream()
        try {
            source.open(DataSpec.Builder().setUri(url).build())
            val buffer = ByteArray(32768)
            while (true) { val size = source.read(buffer, 0, buffer.size); if (size < 0) break; output.write(buffer, 0, size) }
            return output.toByteArray()
        } finally { source.close() }
    }
    private fun withPipeline(test: (PlaybackPipeline, MockWebServer) -> Unit) {
        val server = MockWebServer(); server.start()
        val cache = SimpleCache(File(context.cacheDir, "reliability-test-${UUID.randomUUID()}"), NoOpCacheEvictor(), StandaloneDatabaseProvider(context))
        val pipeline = PlaybackPipeline(context, PlaybackNetworkPolicy(context), cache, { true })
        try { test(pipeline, server) } finally { pipeline.close(); server.shutdown(); cache.release() }
    }
    private fun rangeServer(server: MockWebServer, rejectRange: Boolean = false) {
        val first = AtomicBoolean(true); val reject = AtomicBoolean(rejectRange)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val start = request.getHeader("Range")?.substringAfter("bytes=")?.substringBefore('-')?.toInt() ?: 0
                if (start > 0 && reject.getAndSet(false)) return MockResponse().setResponseCode(416)
                val body = audio.copyOfRange(start, audio.size)
                return MockResponse().setBody(Buffer().write(body)).apply {
                    if (start > 0) setResponseCode(206).setHeader("Content-Range", "bytes $start-${audio.lastIndex}/${audio.size}")
                    if (first.getAndSet(false)) setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
                }
            }
        }
    }
    @Test fun interruptedReadResumesRangeAndCompletedCachePlaysWithoutServer() = withPipeline { pipeline, server ->
        rangeServer(server)
        val url = server.url("/stream?id=1&u=a&format=raw").toString()
        try { read(pipeline.prefetchFactory.createDataSourceForDownloading(), url); fail("Expected interrupted body") } catch (_: java.io.IOException) { }
        assertArrayEquals(audio, read(pipeline.prefetchFactory.createDataSourceForDownloading(), url))
        val initial = server.takeRequest(2, TimeUnit.SECONDS)!!
        val resumed = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertNull(initial.getHeader("Range"))
        assertTrue(resumed.getHeader("Range")!!.substringAfter("bytes=").substringBefore('-').toLong() > 0)
        assertTrue(pipeline.fullyCached(MediaItem.fromUri(url)))
        server.shutdown()
        assertArrayEquals(audio, read(pipeline.factory.createDataSource(), url))
    }
    @Test fun rejectedRangeFallsBackToFullGetWithoutDuplicatingBytes() = withPipeline { pipeline, server ->
        rangeServer(server, true)
        val url = server.url("/stream?id=2&u=a&format=raw").toString()
        try { read(pipeline.prefetchFactory.createDataSourceForDownloading(), url) } catch (_: java.io.IOException) { }
        assertArrayEquals(audio, read(pipeline.prefetchFactory.createDataSourceForDownloading(), url))
        val requests = (0..2).map { server.takeRequest(2, TimeUnit.SECONDS)!! }
        assertNotNull(requests[1].getHeader("Range"))
        assertNull(requests[2].getHeader("Range"))
    }
    @Test fun prefetchFillsCurrentThenNextAndDoesNotReadOtherQueueItems() = withPipeline { pipeline, server ->
        server.dispatcher = object : Dispatcher() { override fun dispatch(request: RecordedRequest) = MockResponse().setBody(Buffer().write(audio)) }
        val items = (1..2).map { MediaItem.fromUri(server.url("/stream?id=$it&u=a&format=raw").toString()) }
        pipeline.prefetch(items)
        val deadline = System.currentTimeMillis() + 10000
        while (!items.all(pipeline::fullyCached) && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertTrue(items.all(pipeline::fullyCached))
        assertTrue(server.takeRequest(2, TimeUnit.SECONDS)!!.path!!.contains("id=1"))
        assertTrue(server.takeRequest(2, TimeUnit.SECONDS)!!.path!!.contains("id=2"))
        assertEquals(2, server.requestCount)
    }
    @Test fun nasWakeDelayOfTwentySecondsDoesNotTimeout() = withPipeline { pipeline, server ->
        server.enqueue(MockResponse().setHeadersDelay(20, TimeUnit.SECONDS).setBody("audio"))
        assertEquals("audio", String(read(pipeline.factory.createDataSource(), server.url("/stream").toString())))
    }
    @Test fun openPlaybackStreamDoesNotBlockWholeSongPrefetch() = withPipeline { pipeline, server ->
        server.dispatcher = object : Dispatcher() { override fun dispatch(request: RecordedRequest) = MockResponse().setBody(Buffer().write(audio)) }
        val url = server.url("/stream?id=playing").toString()
        val item = MediaItem.fromUri(url)
        val source = pipeline.factory.createDataSource()
        try {
            source.open(DataSpec.Builder().setUri(url).build())
            source.read(ByteArray(4096), 0, 4096)
            // Keep playback's connection open but stop consuming, like a full playback buffer.
            pipeline.prefetch(listOf(item))
            val deadline = System.currentTimeMillis() + 10000
            while (!pipeline.fullyCached(item) && System.currentTimeMillis() < deadline) Thread.sleep(50)
            assertTrue("Whole-song prefetch is blocked by playback", pipeline.fullyCached(item))
        } finally { source.close() }
        assertArrayEquals(audio, read(pipeline.factory.createDataSource(), url))
        assertEquals(2, server.requestCount)
    }
}
