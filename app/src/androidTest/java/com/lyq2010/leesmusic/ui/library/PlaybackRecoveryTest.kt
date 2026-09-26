package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.common.*
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.playback.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import okio.Buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackRecoveryTest {
    @get:Rule val compose = createComposeRule()
    private fun wav(): ByteArray = java.nio.ByteBuffer.allocate(240044).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray()); putInt(240036); put("WAVEfmt ".toByteArray()); putInt(16)
        putShort(1); putShort(1); putInt(8000); putInt(8000); putShort(1); putShort(8)
        put("data".toByteArray()); putInt(240000); put(ByteArray(240000) { 128.toByte() })
    }.array()
    private fun waitFor(check: () -> Boolean) = compose.waitUntil(6000) {
        var result = false; compose.runOnUiThread { result = check() }; result
    }
    private fun run(withCrossfade: Boolean = false, test: (RecoveringPlayer, MockWebServer, AtomicBoolean, () -> Int) -> Unit) {
        val server = MockWebServer(); server.start()
        val online = AtomicBoolean(true)
        var connections = 0
        lateinit var player: RecoveringPlayer
        compose.runOnUiThread {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            fun createEngine() = ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(OkHttpClient()))
                    .setLoadErrorHandlingPolicy(StreamRetry.loadPolicy)).build()
            val engine = if (withCrossfade) CrossfadePlayer(createEngine(), createEngine(),
                com.lyq2010.leesmusic.data.settings.PlaybackPreferences(context)) else createEngine()
            player = RecoveringPlayer(engine, online::get, { true }, { connections++ }, { _, _ -> }, {}, { 100L }, 100L)
            player.volume = 0f
            player.setMediaItems(listOf(MediaItem.Builder().setMediaId("current").setUri(server.url("/current.wav").toString()).build(),
                MediaItem.Builder().setMediaId("next").setUri(server.url("/next.wav").toString()).build()), 0, 3000)
        }
        try { test(player, server, online) { connections } } catch (error: Throwable) {
            var state = ""
            compose.runOnUiThread { state = "requests=${server.requestCount} state=${player.playbackState} ready=${player.playWhenReady} message=${PlaybackConnection.message.value} error=${player.playerError}" }
            throw AssertionError(state, error)
        } finally { compose.runOnUiThread { player.release() }; server.shutdown() }
    }
    @Test fun transientFailureResumesSameSongAtOriginalPosition() = run { player, server, _, _ ->
        var first = true
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (first) { first = false; return MockResponse().setResponseCode(503) }
                return MockResponse().setBody(Buffer().write(wav()))
            }
        }
        compose.runOnUiThread { player.play() }
        waitFor { player.isPlaying }
        compose.runOnUiThread { assertEquals("current", player.currentMediaItem?.mediaId); assertTrue(player.currentPosition >= 3000) }
        assertTrue(server.requestCount >= 2)
    }
    @Test fun networkRestorationResumesButManualPauseCancelsRecovery() = run { player, server, online, _ ->
        online.set(false)
        var first = true
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (first) { first = false; return MockResponse().setResponseCode(503) }
                return MockResponse().setBody(Buffer().write(wav()))
            }
        }
        compose.runOnUiThread { player.play() }
        waitFor { PlaybackConnection.message.value == "等待网络恢复" }
        compose.runOnUiThread { player.pause(); online.set(true); player.networkChanged() }
        Thread.sleep(300)
        assertEquals(1, server.requestCount)
        compose.runOnUiThread { assertFalse(player.playWhenReady); player.play() }
        waitFor { player.isPlaying }
        compose.runOnUiThread { assertEquals("current", player.currentMediaItem?.mediaId); assertTrue(player.currentPosition >= 3000) }
    }
    @Test fun networkReturnAutomaticallyRetriesWithoutSkipping() = run { player, server, online, _ ->
        online.set(false)
        var first = true
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (first) { first = false; return MockResponse().setResponseCode(503) }
                return MockResponse().setBody(Buffer().write(wav()))
            }
        }
        compose.runOnUiThread { player.play() }
        waitFor { PlaybackConnection.message.value == "等待网络恢复" }
        compose.runOnUiThread { online.set(true); player.networkChanged() }
        waitFor { player.isPlaying }
        compose.runOnUiThread { assertEquals(0, player.currentMediaItemIndex); assertTrue(player.currentPosition >= 3000) }
    }
    @Test fun retryBudgetStopsAtFiveAndAuthorizationErrorsDoNotLoop() = run { player, server, _, _ ->
        repeat(6) { server.enqueue(MockResponse().setResponseCode(503)) }
        compose.runOnUiThread { player.play() }
        waitFor { PlaybackConnection.message.value == "连接失败，点击播放重试" }
        assertEquals(6, server.requestCount)
        compose.runOnUiThread { assertFalse(player.playWhenReady); assertEquals(0, player.currentMediaItemIndex) }
        server.enqueue(MockResponse().setResponseCode(401))
        compose.runOnUiThread { player.play() }
        waitFor { PlaybackConnection.message.value == "无法播放，请检查服务器连接" }
        Thread.sleep(300)
        assertEquals(7, server.requestCount)
    }
    @Test fun longPauseRefreshesConnectionsAndKeepsPosition() = run { player, server, _, connections ->
        server.dispatcher = object : Dispatcher() { override fun dispatch(request: RecordedRequest) = MockResponse().setBody(Buffer().write(wav())) }
        compose.runOnUiThread { player.play() }
        waitFor { player.isPlaying }
        var position = 0L; var before = 0
        compose.runOnUiThread { player.pause(); position = player.currentPosition; before = connections() }
        Thread.sleep(150)
        compose.runOnUiThread { player.play() }
        waitFor { player.isPlaying }
        compose.runOnUiThread { assertTrue(connections() > before); assertTrue(player.currentPosition >= position); assertEquals(0, player.currentMediaItemIndex) }
    }
    @Test fun crossfadeWrapperRecoversAndKeepsExhaustedRetryMessage() = run(withCrossfade = true) { player, server, online, _ ->
        online.set(false)
        server.enqueue(MockResponse().setResponseCode(503))
        compose.runOnUiThread { player.play() }
        waitFor { PlaybackConnection.message.value == "等待网络恢复" }
        server.dispatcher = object : Dispatcher() { override fun dispatch(request: RecordedRequest) = MockResponse().setBody(Buffer().write(wav())) }
        compose.runOnUiThread { online.set(true); player.networkChanged() }
        waitFor { player.isPlaying }
        compose.runOnUiThread { assertEquals("current", player.currentMediaItem?.mediaId); assertTrue(player.currentPosition >= 3000); player.stop() }
        server.dispatcher = object : Dispatcher() { override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(503) }
        compose.runOnUiThread { player.setMediaItem(MediaItem.fromUri(server.url("/failure.wav").toString())); player.play() }
        waitFor { PlaybackConnection.message.value == "连接失败，点击播放重试" }
        Thread.sleep(200)
        compose.runOnUiThread { assertFalse(player.playWhenReady); assertEquals("连接失败，点击播放重试", PlaybackConnection.message.value) }
    }
    @Test fun editingUpcomingQueueWhileOfflineKeepsCurrentRecovery() = run(withCrossfade = true) { player, server, online, _ ->
        online.set(false)
        server.enqueue(MockResponse().setResponseCode(503))
        compose.runOnUiThread { player.play() }
        waitFor { PlaybackConnection.message.value == "等待网络恢复" }
        compose.runOnUiThread {
            player.addMediaItem(MediaItem.fromUri(server.url("/appended.wav").toString()))
            player.moveMediaItem(2, 1)
            player.removeMediaItem(2)
        }
        compose.runOnUiThread { assertEquals("等待网络恢复", PlaybackConnection.message.value) }
        server.dispatcher = object : Dispatcher() { override fun dispatch(request: RecordedRequest) = MockResponse().setBody(Buffer().write(wav())) }
        compose.runOnUiThread { online.set(true); player.networkChanged() }
        waitFor { player.isPlaying }
        compose.runOnUiThread { assertEquals("current", player.currentMediaItem?.mediaId); assertTrue(player.currentPosition >= 3000); assertEquals(2, player.mediaItemCount) }
    }
    @Test fun replacingQueueWhileOfflineCancelsOldRecovery() = run(withCrossfade = true) { player, server, online, _ ->
        online.set(false)
        server.enqueue(MockResponse().setResponseCode(503))
        compose.runOnUiThread { player.play() }
        waitFor { PlaybackConnection.message.value == "等待网络恢复" }
        compose.runOnUiThread {
            player.setMediaItem(MediaItem.Builder().setMediaId("replacement").setUri(server.url("/replacement.wav").toString()).build())
            online.set(true); player.networkChanged()
        }
        Thread.sleep(300)
        assertEquals(1, server.requestCount)
        compose.runOnUiThread { assertEquals("replacement", player.currentMediaItem?.mediaId); assertFalse(player.isPlaying); assertNull(PlaybackConnection.message.value) }
    }
}
