package com.lyq2010.leesmusic.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.media3.common.Player
import com.lyq2010.leesmusic.data.library.resourceCacheIdentity
import com.lyq2010.leesmusic.ui.catalog.CoverImages
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal class PlaybackWidgetUpdater(private val context: Context, private val player: Player) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val http = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS).build()
    private var job: Job? = null
    private var last: WidgetState? = null
    private var artworkUrl: String? = null
    private var artwork: Bitmap? = null
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { update() }
    }
    init {
        MusicWidget.refresh = { ready ->
            last = null
            update()
            val loading = job
            if (loading?.isActive == true) loading.invokeOnCompletion { ready() } else ready()
        }
        player.addListener(listener)
        update()
    }
    private fun update() {
        if (MusicWidget.ids(context).isEmpty()) return
        val metadata = player.currentMediaItem?.mediaMetadata
        val url = metadata?.artworkUri?.toString()
        if (url != artworkUrl) {
            artworkUrl = url; artwork = null; job?.cancel()
            if (url != null) job = scope.launch {
                val loaded = withContext(Dispatchers.IO) {
                    runCatching { CoverImages.load(context, resourceCacheIdentity(url), url, http)?.let {
                        Bitmap.createScaledBitmap(it, 192, 192, true)
                    } }.getOrNull()
                }
                if (url == artworkUrl) { artwork = loaded; update() }
            }
        }
        val state = WidgetState(metadata?.title?.toString() ?: "Lee’s Music",
            metadata?.artist?.toString() ?: "打开应用，选择音乐", player.mediaItemCount > 0, player.playWhenReady,
            player.hasPreviousMediaItem(), player.hasNextMediaItem(), artwork)
        if (last != state) { last = state; MusicWidget.publish(context, state) }
    }
    fun close() {
        MusicWidget.refresh = null
        player.removeListener(listener)
        scope.cancel()
        // Keep the restored song visible while the idle service is released.
        MusicWidget.publish(context, last?.copy(playing = false) ?: WidgetState())
    }
}
