package com.lyq2010.leesmusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.lyq2010.leesmusic.MainActivity
import com.lyq2010.leesmusic.R
import com.lyq2010.leesmusic.playback.PlaybackService
import com.lyq2010.leesmusic.playback.togglePlayback

internal data class WidgetState(val title: String = "Lee’s Music", val artist: String = "打开应用，选择音乐",
    val active: Boolean = false, val playing: Boolean = false, val previous: Boolean = false, val next: Boolean = false,
    val artwork: Bitmap? = null)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
open class MusicWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        render(context)
        connect(context, null)
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in actions) connect(context, intent.action)
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        render(context)
        connect(context, null)
    }
    private fun connect(context: Context, action: String?) {
        val pending = goAsync()
        val handler = Handler(Looper.getMainLooper())
        val app = context.applicationContext
        val future = MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java))).buildAsync()
        var finished = false
        fun finish() {
            if (finished) return
            finished = true
            MediaController.releaseFuture(future)
            pending.finish()
        }
        val timeout = Runnable { finish() }
        handler.postDelayed(timeout, 8000)
        future.addListener({
            if (!finished) {
                val player = runCatching { future.get() }.getOrNull()
                if (player == null) { handler.removeCallbacks(timeout); finish() }
                else handler.post(object : Runnable {
                    override fun run() {
                    if (finished) return
                    if (!player.sessionExtras.getBoolean("playbackRestored")) {
                        handler.postDelayed(this, 50)
                        return
                    }
                    runCatching {
                    when (action) {
                        actions[0] -> togglePlayback(player)
                        actions[1] -> if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                        actions[2] -> if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                    }
                    publish(context, WidgetState(
                        player.mediaMetadata.title?.toString() ?: "Lee’s Music",
                        player.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "打开应用，选择音乐",
                        player.mediaItemCount > 0, player.playWhenReady, player.hasPreviousMediaItem(), player.hasNextMediaItem()))
                    }
                    val ready = { handler.post { handler.removeCallbacks(timeout); finish() }; Unit }
                    refresh?.invoke(ready) ?: ready()
                    }
                })
            }
        }, ContextCompat.getMainExecutor(context))
    }
    companion object {
        private val actions = listOf("com.lyq2010.leesmusic.widget.TOGGLE", "com.lyq2010.leesmusic.widget.PREVIOUS", "com.lyq2010.leesmusic.widget.NEXT")
        private var state = WidgetState()
        private var backdrop: Bitmap? = null
        internal var refresh: ((() -> Unit) -> Unit)? = null
        internal fun ids(context: Context) = WidgetSize.entries.flatMap {
            AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, it.provider)).toList()
        }.toIntArray()
        internal fun publish(context: Context, value: WidgetState) {
            if (state.artwork !== value.artwork) backdrop = value.artwork?.let(::widgetBackdrop)
            state = value
            render(context)
        }
        private fun render(context: Context) {
            val ids = ids(context)
            if (ids.isEmpty()) return
            val manager = AppWidgetManager.getInstance(context)
            ids.forEach { id ->
                val provider = manager.getAppWidgetInfo(id)?.provider?.className
                val size = WidgetSize.entries.firstOrNull { it.provider.name == provider } ?: WidgetSize.Medium
                val views = views(context, size.layout)
                manager.updateAppWidget(id, views)
            }
        }
        internal fun views(context: Context, layout: Int): RemoteViews {
            val views = RemoteViews(context.packageName, layout)
            val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            if (layout != R.layout.music_widget_mini) {
                views.setTextViewText(R.id.widget_title, state.title)
                views.setTextViewText(R.id.widget_artist, state.artist)
                views.setOnClickPendingIntent(R.id.widget_info, open)
            }
            views.setContentDescription(R.id.widget_cover, "${state.title} · ${state.artist}")
            views.setOnClickPendingIntent(R.id.widget_cover, open)
            views.setImageViewBitmap(R.id.widget_backdrop,
                if (layout == R.layout.music_widget_mini || layout == R.layout.music_widget_square) null else backdrop)
            if (state.artwork == null) views.setImageViewResource(R.id.widget_cover, R.drawable.widget_placeholder)
            else views.setImageViewBitmap(R.id.widget_cover, state.artwork)
            views.setImageViewResource(R.id.widget_play, if (state.playing) R.drawable.widget_pause else R.drawable.widget_play)
            views.setContentDescription(R.id.widget_play, if (state.playing) "暂停" else "播放")
            listOf(R.id.widget_play, R.id.widget_previous, R.id.widget_next).forEachIndexed { index, id ->
                if (layout == R.layout.music_widget_mini && index > 0) return@forEachIndexed
                val enabled = when (index) { 1 -> state.previous; 2 -> state.next; else -> true }
                views.setBoolean(id, "setEnabled", enabled)
                views.setFloat(id, "setAlpha", if (enabled) 1f else .35f)
                views.setOnClickPendingIntent(id, if (!state.active) open else PendingIntent.getBroadcast(context, index,
                    Intent(context, MusicWidget::class.java).setAction(actions[index]), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            return views
        }
    }
}
