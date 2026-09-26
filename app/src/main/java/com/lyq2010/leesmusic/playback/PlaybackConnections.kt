package com.lyq2010.leesmusic.playback

import okhttp3.OkHttpClient
import java.util.concurrent.Executors

/** TLS socket shutdown can write close_notify; it must never run on the service's main thread. */
internal class PlaybackConnections(private val playback: OkHttpClient, private val prefetch: OkHttpClient) {
    private var closed = false

    @Synchronized fun refresh() {
        if (!closed) worker.execute { playback.connectionPool.evictAll() }
    }

    @Synchronized fun cancelPrefetch() {
        if (closed) return
        // Capture the old calls now so delayed cancellation cannot cancel a new prefetch.
        val calls = prefetch.dispatcher.queuedCalls() + prefetch.dispatcher.runningCalls()
        worker.execute { calls.forEach { it.cancel() } }
    }

    @Synchronized fun close() {
        if (closed) return
        closed = true
        val calls = playback.dispatcher.queuedCalls() + playback.dispatcher.runningCalls() +
            prefetch.dispatcher.queuedCalls() + prefetch.dispatcher.runningCalls()
        worker.execute {
            calls.forEach { it.cancel() }
            playback.connectionPool.evictAll()
            if (prefetch.connectionPool !== playback.connectionPool) prefetch.connectionPool.evictAll()
        }
    }

    companion object {
        private val worker = Executors.newSingleThreadExecutor { task ->
            Thread(task, "music-connection-cleanup").apply { isDaemon = true }
        }
    }
}
