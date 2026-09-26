package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.data.settings.PlaybackPreferences
import com.lyq2010.leesmusic.playback.CrossfadePlayer
import com.lyq2010.leesmusic.ui.player.PlaybackSpeedButton
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class CrossfadeTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun song(id: String): MediaItem {
        val wav = java.nio.ByteBuffer.allocate(80044).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(80036); put("WAVEfmt ".toByteArray()); putInt(16)
            putShort(1); putShort(1); putInt(8000); putInt(8000); putShort(1); putShort(8)
            put("data".toByteArray()); putInt(80000); put(ByteArray(80000) { 128.toByte() })
        }.array()
        return MediaItem.Builder().setMediaId(id).setUri("data:audio/wav;base64," +
            android.util.Base64.encodeToString(wav, android.util.Base64.NO_WRAP)).build()
    }
    private fun waitFor(check: () -> Boolean) = compose.waitUntil(7000) {
        var result = false; compose.runOnUiThread { result = check() }; result
    }

    @Test fun automaticTransitionOverlapsTracksAndPauseStopsBoth() = withPlayer { player, first, second, _ ->
        compose.runOnUiThread {
            player.setMediaItems(listOf(song("first"), song("second")))
            player.prepare(); player.volume = .6f; player.play()
        }
        waitFor { first.isPlaying }
        compose.runOnUiThread { player.seekTo(6500) }
        waitFor { player.currentMediaItemIndex == 1 && second.currentPosition > 400 }
        compose.runOnUiThread {
            assertTrue(first.isPlaying && second.isPlaying)
            assertTrue(first.volume in .01f.. .59f)
            assertTrue(second.volume in .01f.. .59f)
            assertEquals(.6f, player.volume, .001f)
            assertEquals(.6f, first.volume + second.volume, .02f)
            player.pause()
            assertFalse(first.playWhenReady)
            assertFalse(second.playWhenReady)
            assertEquals(.6f, second.volume, .001f)
            player.play()
            assertEquals(.6f, second.volume, .001f)
        }
    }

    @Test fun shuffleQueueAndDisablingDuringFadeKeepIncomingTrack() = withPlayer { player, first, second, preferences ->
        compose.runOnUiThread {
            player.setMediaItems(listOf(song("duplicate"), song("other"), song("duplicate")))
            first.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(intArrayOf(0, 2, 1), 0))
            player.shuffleModeEnabled = true
            player.repeatMode = Player.REPEAT_MODE_ALL
            player.prepare(); player.play()
        }
        waitFor { first.isPlaying }
        compose.runOnUiThread { player.seekTo(6500) }
        waitFor { player.currentMediaItemIndex == 2 && second.currentPosition > 200 }
        compose.runOnUiThread {
            assertEquals(1, player.nextMediaItemIndex)
            preferences.fadeAudio = false; player.applyPreferences()
            assertFalse(first.playWhenReady)
            assertEquals(1f, second.volume, .001f)
            assertEquals(2, player.currentMediaItemIndex)
            player.seekToNextMediaItem()
            assertEquals(1, player.currentMediaItemIndex)
        }
    }

    @Test fun repeatOneAndManualSeekDoNotLeaveHiddenPlayback() = withPlayer { player, first, second, _ ->
        compose.runOnUiThread {
            player.setMediaItems(listOf(song("first"), song("second")))
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.prepare(); player.play()
        }
        waitFor { first.isPlaying }
        compose.runOnUiThread { player.seekTo(8500) }
        waitFor { first.currentPosition > 9000 }
        compose.runOnUiThread {
            assertFalse(second.playWhenReady)
            assertEquals(0, second.mediaItemCount)
            player.seekToNextMediaItem()
            assertEquals(1, player.currentMediaItemIndex)
            assertEquals(1f, first.volume, .001f)
            player.stop()
            assertFalse(second.playWhenReady)
        }
    }

    @Test fun completedFadePreservesQueueAndReleasesOutgoingDeckWithAudioFocus() = withPlayer { player, first, second, preferences ->
        compose.runOnUiThread {
            preferences.mixAudio = false; player.applyPreferences()
            player.setMediaItems(listOf(song("first"), song("second"), song("third")))
            player.prepare(); player.play()
        }
        waitFor { first.isPlaying }
        compose.runOnUiThread { player.seekTo(6500) }
        waitFor { second.isPlaying && second.currentPosition > 400 }
        compose.runOnUiThread { assertTrue(first.isPlaying) }
        waitFor { second.isPlaying && second.currentPosition > 3300 }
        compose.runOnUiThread {
            assertFalse(first.playWhenReady)
            // The idle deck may already be preparing the third track, but must remain silent.
            assertEquals(0f, first.volume, .001f)
            assertEquals(1, player.currentMediaItemIndex)
            assertEquals(3, player.mediaItemCount)
            assertEquals(1f, second.volume, .001f)
            assertEquals(2, player.nextMediaItemIndex)
        }
    }

    private fun withPlayer(test: (CrossfadePlayer, ExoPlayer, ExoPlayer, PlaybackPreferences) -> Unit) {
        val preferences = PlaybackPreferences(context)
        val oldFade = preferences.fadeAudio; val oldMix = preferences.mixAudio; val oldSpeed = preferences.speed
        lateinit var first: ExoPlayer; lateinit var second: ExoPlayer; lateinit var player: CrossfadePlayer
        try {
            compose.runOnUiThread {
                preferences.fadeAudio = true; preferences.mixAudio = true; preferences.speed = 1f
                first = ExoPlayer.Builder(context).build(); second = ExoPlayer.Builder(context).build()
                player = CrossfadePlayer(first, second, preferences)
            }
            test(player, first, second, preferences)
        } finally {
            compose.runOnUiThread { player.release() }
            preferences.fadeAudio = oldFade; preferences.mixAudio = oldMix; preferences.speed = oldSpeed
        }
    }

    @Test fun speedControlLivesInPlayerAndPersistsSelection() {
        val preferences = PlaybackPreferences(context)
        val old = preferences.speed
        try {
            compose.setContent { PlaybackSpeedButton() }
            compose.onNodeWithContentDescription("播放速度").performClick()
            compose.onNodeWithText("1.25×").performClick()
            compose.runOnIdle { assertEquals(1.25f, PlaybackPreferences(context).speed) }
            compose.onNodeWithText("1.25×").assertIsDisplayed()
        } finally { preferences.speed = old }
    }
}
