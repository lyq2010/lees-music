package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.playback.*
import com.lyq2010.leesmusic.ui.player.*
import com.lyq2010.leesmusic.ui.shell.ShellTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PlaybackQueueTest {
    @get:Rule val compose = createComposeRule()
    private fun item(title: String) = MediaItem.Builder().setMediaId("duplicate-id").setUri("https://example.org/$title.mp3")
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist("Artist").build()).build()
    private fun withPlayer(block: (ExoPlayer) -> Unit) {
        lateinit var player: ExoPlayer
        compose.runOnUiThread {
            player = ExoPlayer.Builder(InstrumentationRegistry.getInstrumentation().targetContext).build()
            player.setMediaItems(listOf(item("A"), item("B"), item("C"), item("D")), 1, 12000)
        }
        try { block(player) } finally { compose.runOnUiThread { player.release() } }
    }

    @Test fun shuffledOrderCanBeFrozenWithoutChangingTrackOrPositionEvenWithDuplicateIds() = withPlayer { player ->
        compose.runOnUiThread {
            player.shuffleModeEnabled = true
            val before = queueOrder(player).map { player.getMediaItemAt(it).mediaMetadata.title }
            val current = player.currentMediaItem!!.mediaMetadata.title
            freezeShuffleOrder(player)
            assertFalse(player.shuffleModeEnabled)
            assertEquals(before, queueOrder(player).map { player.getMediaItemAt(it).mediaMetadata.title })
            assertEquals(current, player.currentMediaItem!!.mediaMetadata.title)
            assertEquals(12000L, player.currentPosition)
            assertFalse(player.playWhenReady)
        }
    }

    @Test fun repeatAllReorderingAcrossWrapAndClearingUpcomingPreservesCurrentSong() = withPlayer { player ->
        compose.runOnUiThread {
            player.repeatMode = Player.REPEAT_MODE_ALL
            assertEquals(listOf(2, 3, 0), queueSections(player).upcoming)
            moveUpcomingEntry(player, 0, 3)
            assertEquals(listOf("C", "A", "D"), queueSections(player).upcoming.map { player.getMediaItemAt(it).mediaMetadata.title })
            assertEquals("B", player.currentMediaItem!!.mediaMetadata.title)
            assertEquals(12000L, player.currentPosition)
            queueSections(player).upcoming.sortedDescending().forEach { player.removeMediaItem(it) }
            assertEquals(1, player.mediaItemCount)
            assertEquals("B", player.currentMediaItem!!.mediaMetadata.title)
            assertEquals(12000L, player.currentPosition)
        }
    }

    @Test fun queueClearRequiresConfirmationAndDoesNotRemoveCurrent() = withPlayer { player ->
        compose.setContent { ShellTheme { QueueSheet(player, onDismiss = {}) } }
        compose.onNodeWithText("当前歌曲").assertIsDisplayed()
        compose.onNodeWithText("接下来 · 2").assertIsDisplayed()
        compose.onNodeWithText("清空接下来").performClick()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertEquals(4, player.mediaItemCount) }
        compose.onNodeWithText("清空接下来").performClick()
        compose.onNodeWithText("清空队列", substring = false).performClick()
        compose.runOnIdle {
            assertEquals(2, player.mediaItemCount)
            assertEquals("B", player.currentMediaItem!!.mediaMetadata.title)
            assertEquals(12000L, player.currentPosition)
        }
    }

    @Test fun controlsExposeActionsAndBufferingCanStillBePaused() {
        var pause = 0
        var queue = 0
        var more = 0
        var shuffle = 0
        compose.setContent { ShellTheme {
            PlaybackControls("Song", "Artist", 0, 10000, true, 1f, 0, false,
                {}, { pause++ }, {}, {}, {}, {}, {}, buffering = true, canNext = false,
                onShuffle = { shuffle++ }, onQueue = { queue++ }, onMore = { more++ })
        } }
        compose.onNodeWithText("正在缓冲").assertIsDisplayed()
        compose.onNodeWithContentDescription("暂停").performClick()
        compose.onNodeWithContentDescription("下一首").assertIsNotEnabled()
        compose.onNodeWithContentDescription("当前歌曲的更多操作").performClick()
        compose.onNodeWithContentDescription("查看播放队列").performClick()
        compose.onNodeWithContentDescription("开启随机播放").performClick()
        compose.runOnIdle { assertEquals(listOf(1, 1, 1, 1), listOf(pause, queue, more, shuffle)) }
    }
}
