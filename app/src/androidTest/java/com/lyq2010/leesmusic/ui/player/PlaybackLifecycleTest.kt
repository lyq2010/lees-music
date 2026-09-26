package com.lyq2010.leesmusic.ui.player

import android.content.ComponentName
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.lyq2010.leesmusic.MainActivity
import com.lyq2010.leesmusic.playback.PlaybackService
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class PlaybackLifecycleTest {
    @Test fun pausedAppCanRemoveTaskAndReconnectFiveTimes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        repeat(5) {
            val activity = ActivityScenario.launch(MainActivity::class.java)
            lateinit var future: ListenableFuture<MediaController>
            instrumentation.runOnMainSync {
                future = MediaController.Builder(context.applicationContext,
                    SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
            }
            val controller = future.get(10, TimeUnit.SECONDS)
            instrumentation.runOnMainSync {
                assertTrue(controller.isConnected)
                controller.pause()
                controller.release()
            }
            activity.moveToState(Lifecycle.State.CREATED)
            activity.onActivity { it.finishAndRemoveTask() }
            activity.close()
            SystemClock.sleep(750)
        }
    }
}
