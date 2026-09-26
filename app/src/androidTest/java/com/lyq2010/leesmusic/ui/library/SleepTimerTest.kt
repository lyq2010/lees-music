package com.lyq2010.leesmusic.ui.library

import android.content.ComponentName
import android.os.Bundle
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.common.MediaItem
import androidx.media3.session.*
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.lyq2010.leesmusic.playback.*
import com.lyq2010.leesmusic.ui.player.SleepTimerSheet
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class SleepTimerTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun startSilentPlayback(controller: MediaController) {
        // Ten seconds of PCM silence in memory: no server, speaker output or temporary files.
        val wav = java.nio.ByteBuffer.allocate(80044).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(80036); put("WAVEfmt ".toByteArray()); putInt(16)
            putShort(1); putShort(1); putInt(8000); putInt(8000); putShort(1); putShort(8)
            put("data".toByteArray()); putInt(80000); put(ByteArray(80000) { 128.toByte() })
        }.array()
        val uri = "data:audio/wav;base64," + android.util.Base64.encodeToString(wav, android.util.Base64.NO_WRAP)
        instrumentation.runOnMainSync {
            assertTrue("Controller cannot set media items", controller.isCommandAvailable(androidx.media3.common.Player.COMMAND_CHANGE_MEDIA_ITEMS))
            controller.volume = 0f
            controller.setMediaItem(MediaItem.Builder().setMediaId("timer-test").setUri(uri).build())
            controller.prepare()
            controller.play()
        }
        try { compose.waitUntil(5000) {
            var playing = false
            instrumentation.runOnMainSync { playing = controller.isPlaying }
            playing
        } } catch (failure: Exception) {
            instrumentation.runOnMainSync { fail("state=${controller.playbackState}, requested=${controller.playWhenReady}, count=${controller.mediaItemCount}, error=${controller.playerError}, suppression=${controller.playbackSuppressionReason}") }
            throw failure
        }
    }
    private fun connect(): MediaController {
        lateinit var future: ListenableFuture<MediaController>
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            future = MediaController.Builder(context, SessionToken(context,
                ComponentName(context, PlaybackService::class.java))).buildAsync()
        }
        return future.get(10, TimeUnit.SECONDS)
    }
    private fun set(controller: MediaController, duration: Long): Int {
        lateinit var result: ListenableFuture<SessionResult>
        instrumentation.runOnMainSync {
            result = controller.sendCustomCommand(SessionCommand(SLEEP_TIMER_ACTION, Bundle.EMPTY),
                Bundle().apply { putLong(SLEEP_TIMER_DURATION, duration) })
        }
        return result.get(5, TimeUnit.SECONDS).resultCode
    }

    @Test fun serviceTimerSurvivesControllerDisconnectAndPreservesQueue() {
        var controller = connect()
        try {
            startSilentPlayback(controller)
            assertEquals(SessionResult.RESULT_SUCCESS, set(controller, 3500))
            instrumentation.runOnMainSync { controller.release() }
            controller = connect()
            instrumentation.runOnMainSync { assertTrue(controller.sessionExtras.getLong(SLEEP_TIMER_DEADLINE) > SystemClock.elapsedRealtime()) }
            SystemClock.sleep(3900)
            var pausedPosition = 0L
            instrumentation.runOnMainSync {
                assertFalse(controller.playWhenReady)
                assertEquals(0L, controller.sessionExtras.getLong(SLEEP_TIMER_DEADLINE))
                assertEquals("timer-test", controller.currentMediaItem?.mediaId)
                assertTrue(controller.currentPosition >= 3000)
                pausedPosition = controller.currentPosition
            }
            SystemClock.sleep(200)
            instrumentation.runOnMainSync {
                assertEquals(pausedPosition, controller.currentPosition)
                controller.clearMediaItems()
                controller.volume = 1f
            }
        } finally { set(controller, 0); instrumentation.runOnMainSync { controller.release() } }
    }

    @Test fun cancellationAndReplacementPreventOldDeadlineFromPausing() {
        val controller = connect()
        try {
            startSilentPlayback(controller)
            assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, set(controller, -1))
            assertEquals(SessionResult.RESULT_SUCCESS, set(controller, 300))
            assertEquals(SessionResult.RESULT_SUCCESS, set(controller, 1200))
            SystemClock.sleep(450)
            instrumentation.runOnMainSync {
                assertTrue(controller.sessionExtras.getLong(SLEEP_TIMER_DEADLINE) > SystemClock.elapsedRealtime())
                assertTrue(controller.isPlaying)
            }
            assertEquals(SessionResult.RESULT_SUCCESS, set(controller, 0))
            SystemClock.sleep(900)
            instrumentation.runOnMainSync {
                assertTrue(controller.playWhenReady)
                assertEquals(0L, controller.sessionExtras.getLong(SLEEP_TIMER_DEADLINE))
                controller.pause()
                controller.clearMediaItems()
                controller.volume = 1f
            }
        } finally { set(controller, 0); instrumentation.runOnMainSync { controller.release() } }
    }

    @Test fun sheetWaitsForAcknowledgementAndOffersRetry() {
        var request = -1L
        var dismissed = false
        var reply: ((Boolean) -> Unit)? = null
        compose.setContent { SleepTimerSheet(60000, { duration, callback -> request = duration; reply = callback }, { dismissed = true }) }
        compose.onNodeWithText("15 分钟").performClick()
        compose.runOnIdle { assertEquals(900000L, request); assertFalse(dismissed) }
        compose.onNodeWithText("取消定时").assertIsNotEnabled()
        compose.runOnIdle { reply!!(false) }
        compose.onNodeWithText("设置未成功，请重试").assertIsDisplayed()
        compose.onNodeWithText("取消定时").performClick()
        compose.runOnIdle { assertEquals(0L, request); reply!!(true); assertTrue(dismissed) }
    }
}
