package com.lyq2010.leesmusic.ui.player

import android.os.Looper
import android.os.NetworkOnMainThreadException
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.playback.PlaybackConnections
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

class PlaybackConnectionsTest {
    @Test fun connectionRefreshAndServiceCleanupCloseSocketsOffMainThread() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        listOf(false, true).forEach { closingService ->
            val closed = CountDownLatch(1)
            var closedOnMain = false
            val factory = object : SocketFactory() {
                override fun createSocket(): Socket = object : Socket() {
                    override fun close() {
                        closedOnMain = Looper.myLooper() == Looper.getMainLooper()
                        // Reproduce the S25 TLS socket's strict-mode contract deterministically.
                        if (closedOnMain) throw NetworkOnMainThreadException()
                        super.close()
                        closed.countDown()
                    }
                }
                override fun createSocket(h: String, p: Int): Socket = throw UnsupportedOperationException()
                override fun createSocket(h: String, p: Int, l: InetAddress, lp: Int): Socket = throw UnsupportedOperationException()
                override fun createSocket(h: InetAddress, p: Int): Socket = throw UnsupportedOperationException()
                override fun createSocket(h: InetAddress, p: Int, l: InetAddress, lp: Int): Socket = throw UnsupportedOperationException()
            }
            val server = MockWebServer()
            server.start()
            val client = OkHttpClient.Builder().socketFactory(factory).build()
            val connections = PlaybackConnections(client, client)
            try {
                server.enqueue(MockResponse().setBody("music"))
                client.newCall(Request.Builder().url(server.url("/audio")).build()).execute().use { it.body.string() }
                assertEquals(1, client.connectionPool.idleConnectionCount())
                instrumentation.runOnMainSync {
                    if (closingService) connections.close() else connections.refresh()
                }
                assertTrue("Idle socket was not released", closed.await(5, TimeUnit.SECONDS))
                assertFalse("Socket closed on main thread", closedOnMain)
                assertEquals(0, client.connectionPool.connectionCount())
                instrumentation.runOnMainSync { connections.close(); connections.close(); connections.refresh() }
            } finally { connections.close(); server.shutdown() }
        }
    }
}
