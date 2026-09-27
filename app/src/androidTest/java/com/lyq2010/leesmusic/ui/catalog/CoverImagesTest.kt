package com.lyq2010.leesmusic.ui.catalog

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.io.IOException

class CoverImagesTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun png(edge: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        return output.toByteArray()
    }

    @Test fun limitsDownloadedBytesAndDecodedDimensions() = runBlocking {
        MockWebServer().use { server ->
            val small = png(2)
            val oversized = small + ByteArray(5 * 1024 * 1024)
            server.enqueue(MockResponse().setBody(Buffer().write(oversized)))
            server.enqueue(MockResponse().setBody(Buffer().write(png(2048))))
            val client = OkHttpClient()
            val run = System.nanoTime()
            assertNull(CoverImages.load(context, "oversized-$run", server.url("/large").toString(), client))
            val cover = CoverImages.load(context, "dimension-$run", server.url("/wide").toString(), client)
            assertNotNull(cover)
            assertTrue(cover!!.width <= 600)
            assertTrue(cover.height <= 600)
        }
    }

    @Test fun concurrentRequestsForSameCoverFetchOnce() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(Buffer().write(png(4))).setBodyDelay(300, TimeUnit.MILLISECONDS))
            val client = OkHttpClient()
            val pool = Executors.newFixedThreadPool(2)
            val start = CountDownLatch(1)
            val id = "coalesced-${System.nanoTime()}"
            try {
                val tasks = (1..2).map { pool.submit<Bitmap?> {
                    start.await()
                    runBlocking { CoverImages.load(context, id, server.url("/cover").toString(), client) }
                } }
                start.countDown()
                assertTrue(tasks.all { it.get(5, TimeUnit.SECONDS) != null })
                assertEquals(1, server.requestCount)
            } finally { pool.shutdownNow() }
        }
    }

    @Test fun cancelledCoverRequestClosesItsHttpCall() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setHeadersDelay(20, TimeUnit.SECONDS).setBody(Buffer().write(png(4))))
            val closed = CountDownLatch(1)
            val client = OkHttpClient.Builder().eventListener(object : EventListener() {
                override fun callFailed(call: okhttp3.Call, ioe: IOException) { closed.countDown() }
            }).build()
            val job = launch(Dispatchers.IO) {
                CoverImages.load(context, "cancelled-${System.nanoTime()}", server.url("/slow").toString(), client)
            }
            assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
            job.cancelAndJoin()
            assertTrue("cancelled cover call stayed open", closed.await(3, TimeUnit.SECONDS))
        }
    }
}
