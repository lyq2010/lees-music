package com.lyq2010.leesmusic.playback

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.*
import kotlinx.coroutines.flow.MutableStateFlow

internal object PlaybackConnection {
    val message = MutableStateFlow<String?>(null)
}

/** Recovery belongs to the service and is cancelled by every explicit pause/queue change. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class RecoveringPlayer(
    private val engine: Player,
    private val online: () -> Boolean,
    private val allowed: () -> Boolean,
    private val refreshConnections: () -> Unit,
    private val prefetch: (List<MediaItem>, Boolean) -> Unit,
    private val cancelPrefetch: () -> Unit,
    private val retryDelay: (Int) -> Long = StreamRetry::delayMs,
    private val longPauseMs: Long = 5 * 60_000L,
) : ForwardingPlayer(engine) {
    private val handler = Handler(Looper.getMainLooper())
    private var pending = false
    private var attempts = 0
    private var pausedAt = SystemClock.elapsedRealtime()
    private var lastNetworkOnline = online()
    private var terminalFailure: String? = null
    private var pendingItem: MediaItem? = null
    private val retry = Runnable { recover() }
    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            if (!engine.playWhenReady || !remote() || !allowed() || !StreamRetry.isTransient(error)) {
                cancelRecovery()
                if (engine.playWhenReady && remote()) {
                    failPlayback("无法播放，请检查服务器连接")
                }
                return
            }
            pending = true; pendingItem = engine.currentMediaItem
            schedule()
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) { cancelRecovery(); attempts = 0; updatePrefetch(false) }
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) {
                pausedAt = SystemClock.elapsedRealtime(); cancelRecovery(clearMessage = terminalFailure == null); cancelPrefetch()
            } else if (playWhenReady) updatePrefetch(false)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            cancelRecovery(); attempts = 0; updatePrefetch(false)
        }
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                // Editing upcoming songs must not strand the current song in STATE_IDLE.
                // Actual current-item transitions cancel recovery in onMediaItemTransition.
                if (!pending || engine.currentMediaItem != pendingItem) { cancelRecovery(); attempts = 0 }
                updatePrefetch(true)
            }
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { updatePrefetch(true) }
        override fun onRepeatModeChanged(repeatMode: Int) { updatePrefetch(true) }
    }
    init { engine.addListener(listener) }

    private fun remote() = engine.currentMediaItem?.localConfiguration?.uri?.scheme in setOf("http", "https")
    private fun updatePrefetch(force: Boolean) {
        if (!engine.playWhenReady || !allowed()) { cancelPrefetch(); return }
        val items = mutableListOf<MediaItem>()
        engine.currentMediaItem?.let(items::add)
        val next = engine.nextMediaItemIndex
        if (next != C.INDEX_UNSET && next != engine.currentMediaItemIndex) items += engine.getMediaItemAt(next)
        prefetch(items, force)
    }
    private fun cancelRecovery(clearMessage: Boolean = true) {
        handler.removeCallbacks(retry); pending = false; pendingItem = null
        if (clearMessage) { terminalFailure = null; PlaybackConnection.message.value = null }
    }
    private fun failPlayback(message: String) {
        cancelRecovery()
        terminalFailure = message
        engine.pause()
        cancelPrefetch()
        PlaybackConnection.message.value = message
    }
    private fun schedule() {
        handler.removeCallbacks(retry)
        if (!online()) { PlaybackConnection.message.value = "等待网络恢复"; return }
        if (attempts >= StreamRetry.MAX) {
            failPlayback("连接失败，点击播放重试")
            return
        }
        PlaybackConnection.message.value = "正在重新连接"
        handler.postDelayed(retry, retryDelay(attempts))
    }
    private fun recover() {
        if (!pending || engine.currentMediaItem != pendingItem || !engine.playWhenReady || !allowed()) { cancelRecovery(); return }
        if (!online()) { schedule(); return }
        attempts++
        refreshConnections()
        // prepare() in STATE_IDLE retains ExoPlayer's current index and position.
        engine.prepare()
        updatePrefetch(true)
    }

    fun networkChanged() {
        val connected = online()
        if (connected != lastNetworkOnline) {
            lastNetworkOnline = connected; refreshConnections()
            updatePrefetch(true)
        }
        if (!allowed()) { pause(); engine.stop(); return }
        if (pending) {
            if (connected) { handler.removeCallbacks(retry); handler.post(retry) }
            else { handler.removeCallbacks(retry); PlaybackConnection.message.value = "等待网络恢复" }
        }
    }
    fun networkRouteChanged() { refreshConnections(); updatePrefetch(true); networkChanged() }
    fun preferencesChanged() { if (!allowed()) { pause(); engine.stop() } else updatePrefetch(true) }

    override fun play() = setPlayWhenReady(true)
    override fun pause() = setPlayWhenReady(false)
    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (!playWhenReady) {
            pausedAt = SystemClock.elapsedRealtime(); cancelRecovery(); cancelPrefetch()
            engine.pause(); return
        }
        if (!allowed()) { PlaybackConnection.message.value = "当前网络不允许在线播放"; return }
        cancelRecovery(); attempts = 0
        if (remote() && SystemClock.elapsedRealtime() - pausedAt >= longPauseMs) {
            engine.stop()
            refreshConnections()
            engine.prepare()
        } else if (engine.playbackState == Player.STATE_IDLE) engine.prepare()
        engine.play(); updatePrefetch(true)
    }
    override fun stop() { cancelRecovery(); cancelPrefetch(); engine.stop() }
    override fun release() { cancelRecovery(); cancelPrefetch(); engine.removeListener(listener); super.release() }
}
