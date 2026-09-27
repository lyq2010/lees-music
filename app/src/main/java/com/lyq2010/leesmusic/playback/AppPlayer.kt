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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lyq2010.leesmusic.data.api.SubsonicClient
import com.lyq2010.leesmusic.data.api.SubsonicServer
import com.lyq2010.leesmusic.ui.catalog.LibrarySong

class AppPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = com.lyq2010.leesmusic.data.settings.PlaybackPreferences(context)
    private var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var controller: MediaController? by mutableStateOf(null)
    private var pendingCredentials: SubsonicServer? = null
    private var released = false
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
        connect()
    }

    private fun connect() {
        if (released || future != null) return
        val pending = MediaController.Builder(appContext,
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)))
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(mediaController: MediaController) {
                    if (controller !== mediaController || released) return
                    controller = null
                    future?.let(MediaController::releaseFuture)
                    future = null
                    connect()
                }
            }).buildAsync()
        future = pending
        pending.addListener({
            if (future !== pending) return@addListener
            val connected = runCatching { pending.get() }.getOrNull()
            if (connected == null) {
                future = null
                MediaController.releaseFuture(pending)
            } else {
                controller = connected
                pendingCredentials?.let { refreshCredentials(it) }
            }
        }, mainExecutor)
    }

    fun retryConnection() = connect()

    fun playerOrNull(): Player? = controller

    fun mediaItems(server: SubsonicServer, songs: List<LibrarySong>): List<MediaItem> {
        val bitRate = preferences.quality.bitRate
        val client = SubsonicClient()
        return songs.map { playbackMediaItem(server, it, bitRate, client) }
    }

    fun play(server: SubsonicServer, songs: List<LibrarySong>, index: Int, start: Boolean = false, shuffle: Boolean = false) {
        playPrepared(mediaItems(server, songs), index, start, shuffle)
    }

    fun playPrepared(items: List<MediaItem>, index: Int, start: Boolean = false, shuffle: Boolean = false) {
        val player = controller ?: return
        if (items.isEmpty()) return
        player.setMediaItems(items, index.coerceIn(0, items.lastIndex), 0)
        player.shuffleModeEnabled = shuffle
        player.prepare()
        player.playWhenReady = start
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

    fun refreshCredentials(server: SubsonicServer) {
        val player = controller ?: run {
            pendingCredentials = server
            retryConnection()
            return
        }
        pendingCredentials = null
        if (player.mediaItemCount == 0) return
        val index = player.currentMediaItemIndex
        val position = player.currentPosition.coerceAtLeast(0L)
        val wasPlaying = player.playWhenReady
        val client = SubsonicClient()
        val songs = (0 until player.mediaItemCount).map { mediaItemSong(player.getMediaItemAt(it)) }
        val items = songs.map { song ->
            playbackMediaItem(server, song.copy(coverUrl = song.coverArtId?.let { client.coverArtUrl(server, it) }), preferences.quality.bitRate, client)
        }
        player.playWhenReady = false
        player.setMediaItems(items, index, position)
        player.prepare()
        player.playWhenReady = wasPlaying
    }

    fun release() {
        released = true
        controller = null
        future?.let(MediaController::releaseFuture)
        future = null
    }
}

fun Player.currentSongTitle(): String = currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
