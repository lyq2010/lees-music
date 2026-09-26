package com.lyq2010.leesmusic.playback

import androidx.media3.common.*
import com.lyq2010.leesmusic.data.settings.PlaybackPreferences

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class ManagedPlayer(private val engine: Player, private val preferences: PlaybackPreferences) : ForwardingPlayer(engine) {
    private val listeners = mutableSetOf<Player.Listener>()
    private var lyric: String? = null
    override fun addListener(listener: Player.Listener) { listeners += listener; super.addListener(listener) }
    override fun removeListener(listener: Player.Listener) { listeners -= listener; super.removeListener(listener) }
    override fun getMediaMetadata(): MediaMetadata = lyric?.let { engine.mediaMetadata.buildUpon().setArtist(it).build() } ?: engine.mediaMetadata
    fun setNotificationLyric(value: String?) {
        if (lyric == value) return
        lyric = value
        val events = Player.Events(FlagSet.Builder().add(Player.EVENT_MEDIA_METADATA_CHANGED).build())
        listeners.toList().forEach { it.onMediaMetadataChanged(mediaMetadata); it.onEvents(this, events) }
    }
    override fun release() { listeners.clear(); super.release() }
}
