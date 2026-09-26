package com.lyq2010.leesmusic.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.lyq2010.leesmusic.data.api.SubsonicClient
import com.lyq2010.leesmusic.data.api.SubsonicServer
import com.lyq2010.leesmusic.ui.catalog.LibrarySong

class AppPlayer(context: Context) {
    private val preferences = com.lyq2010.leesmusic.data.settings.PlaybackPreferences(context)
    private val future = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java)),
    ).buildAsync()

    @Volatile
    private var controller: MediaController? = null
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    fun sleepRemainingMs(): Long = (controller?.sessionExtras?.getLong(SLEEP_TIMER_DEADLINE)
        ?.minus(android.os.SystemClock.elapsedRealtime()) ?: 0L).coerceAtLeast(0L)

    fun setSleepTimer(durationMs: Long, onResult: (Boolean) -> Unit) {
        val player = controller ?: return onResult(false)
        val result = player.sendCustomCommand(androidx.media3.session.SessionCommand(SLEEP_TIMER_ACTION, Bundle.EMPTY),
            Bundle().apply { putLong(SLEEP_TIMER_DURATION, durationMs) })
        result.addListener({ onResult(runCatching {
            result.get().resultCode == androidx.media3.session.SessionResult.RESULT_SUCCESS
        }.getOrDefault(false)) }, mainExecutor)
    }

    fun currentSong(): LibrarySong? = controller?.currentMediaItem?.let(::mediaItemSong)

    init {
        future.addListener(
            { controller = runCatching { future.get() }.getOrNull() },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun playerOrNull(): Player? = controller

    fun play(server: SubsonicServer, songs: List<LibrarySong>, index: Int, start: Boolean = false, shuffle: Boolean = false) {
        val player = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { playbackMediaItem(server, it, preferences.quality.bitRate) }
        player.playWhenReady = start
        player.setMediaItems(items, index.coerceIn(0, items.lastIndex), 0)
        player.shuffleModeEnabled = shuffle
        player.prepare()
    }

    fun enqueue(server: SubsonicServer, song: LibrarySong, next: Boolean): Boolean {
        val player = controller ?: return false
        if (player.mediaItemCount == 0) {
            play(server, listOf(song), 0, start = false)
        } else {
            freezeShuffleOrder(player)
            val index = if (next) (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount) else player.mediaItemCount
            player.addMediaItem(index, playbackMediaItem(server, song, preferences.quality.bitRate))
        }
        return true
    }

    fun release() {
        MediaController.releaseFuture(future)
    }
}

fun Player.currentSongTitle(): String = currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
