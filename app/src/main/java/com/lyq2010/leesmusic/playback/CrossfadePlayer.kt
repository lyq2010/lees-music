package com.lyq2010.leesmusic.playback

import android.os.Handler
import android.os.Looper
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import com.google.common.util.concurrent.Futures
import com.lyq2010.leesmusic.data.settings.PlaybackPreferences

/** The session follows the incoming deck; the outgoing deck only supplies its fading tail. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class CrossfadePlayer(
    first: ExoPlayer,
    private val second: ExoPlayer,
    private val preferences: PlaybackPreferences,
) : ForwardingSimpleBasePlayer(first) {
    private var active = first
    private var spare = second
    private val handler = Handler(Looper.getMainLooper())
    private var targetVolume = first.volume
    private var preparedIndex = C.INDEX_UNSET
    private var fading = false
    private var fadeLength = 3000L
    private var changing = false
    private var closed = false
    private val equalizers = mapOf(first to SessionEqualizer(first) { effectsChanged() }, second to SessionEqualizer(second) { effectsChanged() })
    private val audio = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()
    private val listener = object : Player.Listener {
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            if (!changing) cancelTransition()
        }
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (!changing && reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) cancelTransition()
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Focus loss, unplugging headphones, buffering and timer pauses stop the tail too.
            if (!changing && !isPlaying && fading) cancelTransition()
        }
    }
    private val tick = object : Runnable {
        override fun run() {
            if (closed) return
            updateTransition()
            handler.postDelayed(this, 40)
        }
    }
    init { active.addListener(listener); applyPreferences(); handler.post(tick) }

    override fun getState(): State = super.getState().buildUpon().setVolume(targetVolume).build()
    override fun handleSetVolume(volume: Float, flags: Int): com.google.common.util.concurrent.ListenableFuture<*> {
        targetVolume = volume.coerceIn(0f, 1f)
        applyGains()
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    fun applyPreferences() {
        if (!preferences.fadeAudio) cancelTransition()
        active.setAudioAttributes(audio, !preferences.mixAudio)
        spare.setAudioAttributes(audio, false)
        active.setHandleAudioBecomingNoisy(preferences.pauseOnDisconnect)
        spare.setHandleAudioBecomingNoisy(false)
        active.setPlaybackSpeed(preferences.speed)
        spare.setPlaybackSpeed(preferences.speed)
        equalizers.values.forEach { it.apply(preferences.equalizer) }
        effectsChanged()
    }

    private fun effectsChanged() {
        EqualizerStatus.unavailable.value = equalizers.values.any { it.unavailable }
        applyGains()
    }

    private fun updateTransition() {
        if (!preferences.fadeAudio || !active.isPlaying) return
        if (fading) {
            applyGains()
            if (active.currentPosition >= fadeLength || spare.playbackState == Player.STATE_ENDED) cancelTransition()
            return
        }
        val remaining = active.duration - active.currentPosition
        val next = active.nextMediaItemIndex
        if (active.duration <= 6000 || active.isCurrentMediaItemLive || !active.isCurrentMediaItemSeekable ||
            active.repeatMode == Player.REPEAT_MODE_ONE || next == C.INDEX_UNSET || next == active.currentMediaItemIndex) return
        if (remaining > 8000 || remaining <= 0) return
        if (preparedIndex != next) {
            spare.pauseAtEndOfMediaItems = false
            spare.setMediaItems((0 until active.mediaItemCount).map(active::getMediaItemAt), next, 0)
            spare.repeatMode = active.repeatMode
            // Preserve the actual shuffle traversal, including duplicate song IDs.
            val timeline = active.currentTimeline
            val order = mutableListOf<Int>()
            var index = timeline.getFirstWindowIndex(true)
            while (index != C.INDEX_UNSET && order.size < active.mediaItemCount) {
                order += index
                index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
            }
            spare.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(order.toIntArray(), 0L))
            spare.shuffleModeEnabled = active.shuffleModeEnabled
            spare.volume = 0f
            spare.prepare()
            preparedIndex = next
        }
        // A late or failed preload falls back to normal playback, never silent early switching.
        if (remaining > 3000 || remaining < 500 || spare.playbackState != Player.STATE_READY || spare.playerError != null) return
        changing = true
        try {
            fadeLength = remaining
            val outgoing = active
            outgoing.removeListener(listener)
            outgoing.pauseAtEndOfMediaItems = true
            outgoing.setAudioAttributes(audio, false)
            active = spare
            spare = outgoing
            fading = true
            active.setAudioAttributes(audio, !preferences.mixAudio)
            active.setHandleAudioBecomingNoisy(preferences.pauseOnDisconnect)
            spare.setHandleAudioBecomingNoisy(false)
            active.play()
            setPlayer(active)
            active.addListener(listener)
        } finally { changing = false }
    }

    private fun applyGains() {
        val progress = if (fading) (active.currentPosition.toFloat() / fadeLength).coerceIn(0f, 1f) else 1f
        active.volume = targetVolume * progress * (equalizers[active]?.headroom ?: 1f)
        spare.volume = if (fading) targetVolume * (1f - progress) * (equalizers[spare]?.headroom ?: 1f) else 0f
    }

    private fun cancelTransition() {
        fading = false
        preparedIndex = C.INDEX_UNSET
        spare.pause()
        spare.stop()
        spare.clearMediaItems()
        spare.volume = 0f
        applyGains()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): com.google.common.util.concurrent.ListenableFuture<*> {
        if (!playWhenReady) cancelTransition()
        return super.handleSetPlayWhenReady(playWhenReady)
    }
    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): com.google.common.util.concurrent.ListenableFuture<*> {
        cancelTransition()
        return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
    }
    override fun handleSetRepeatMode(repeatMode: Int): com.google.common.util.concurrent.ListenableFuture<*> {
        cancelTransition(); return super.handleSetRepeatMode(repeatMode)
    }
    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): com.google.common.util.concurrent.ListenableFuture<*> {
        cancelTransition(); return super.handleSetShuffleModeEnabled(shuffleModeEnabled)
    }
    override fun handleStop(): com.google.common.util.concurrent.ListenableFuture<*> {
        cancelTransition(); return super.handleStop()
    }
    override fun handleRelease(): com.google.common.util.concurrent.ListenableFuture<*> {
        closed = true
        handler.removeCallbacksAndMessages(null)
        active.removeListener(listener)
        equalizers.values.forEach { it.close() }
        spare.release()
        return super.handleRelease()
    }
}
