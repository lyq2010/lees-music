package com.lyq2010.leesmusic.playback

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

const val SLEEP_TIMER_ACTION = "com.lyq2010.leesmusic.SLEEP_TIMER"
const val SLEEP_TIMER_DURATION = "durationMs"
const val SLEEP_TIMER_DEADLINE = "sleepDeadlineMs"

/** Owned by the service: navigation and controller disconnection must not cancel it. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class SleepTimer(private val packageName: String) : MediaSession.Callback {
    private val handler = Handler(Looper.getMainLooper())
    private var deadline = 0L
    private var tick: Runnable? = null

    override fun onConnectAsync(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<MediaSession.ConnectionResult> {
        val builder = MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
        if (controller.packageName == packageName && controller.uid == android.os.Process.myUid()) {
            builder.setAvailableSessionCommands(builder.build().availableSessionCommands.buildUpon()
                .add(SessionCommand(SLEEP_TIMER_ACTION, Bundle.EMPTY)).build())
        }
        return Futures.immediateFuture(builder.build())
    }

    override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
        val duration = args.getLong(SLEEP_TIMER_DURATION, -1)
        if (controller.packageName != packageName || customCommand.customAction != SLEEP_TIMER_ACTION || duration !in 0L..7_200_000L) {
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
        }
        tick?.let(handler::removeCallbacks)
        deadline = if (duration == 0L) 0L else SystemClock.elapsedRealtime() + duration
        session.setSessionExtras(Bundle(session.sessionExtras).apply { putLong(SLEEP_TIMER_DEADLINE, deadline) })
        if (deadline != 0L) {
            tick = object : Runnable {
                override fun run() {
                    val remaining = deadline - SystemClock.elapsedRealtime()
                    if (remaining <= 0) {
                        deadline = 0
                        session.player.pause()
                        session.setSessionExtras(Bundle(session.sessionExtras).apply { putLong(SLEEP_TIMER_DEADLINE, 0) })
                        tick = null
                    } else handler.postDelayed(this, minOf(remaining, 1000L))
                }
            }.also { handler.post(it) }
        } else tick = null
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    fun close() { handler.removeCallbacksAndMessages(null) }
}
