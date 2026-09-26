package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.data.settings.PlaybackPreferences
import com.lyq2010.leesmusic.playback.ManagedPlayer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ManagedPlayerTest {
    @get:Rule val compose = createComposeRule()
    @Test fun notificationLyricsEmitMetadataWithoutChangingQueueArtist() {
        compose.runOnUiThread {
            val engine = ExoPlayer.Builder(InstrumentationRegistry.getInstrumentation().targetContext).build()
            val player = ManagedPlayer(engine, PlaybackPreferences(InstrumentationRegistry.getInstrumentation().targetContext))
            try {
                engine.setMediaItem(MediaItem.Builder().setMediaId("song").setUri("https://example.org/a.mp3")
                    .setMediaMetadata(MediaMetadata.Builder().setTitle("Song").setArtist("Artist").build()).build())
                var updates = 0
                player.addListener(object : Player.Listener {
                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) { updates++ }
                })
                player.setNotificationLyric("当前歌词")
                assertEquals("当前歌词", player.mediaMetadata.artist)
                assertEquals("Artist", player.currentMediaItem!!.mediaMetadata.artist)
                player.setNotificationLyric("当前歌词")
                assertEquals(1, updates)
                player.setNotificationLyric(null)
                assertEquals("Artist", player.mediaMetadata.artist)
                assertEquals(2, updates)
            } finally { player.release() }
        }
    }
}
