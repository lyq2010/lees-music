package com.lyq2010.leesmusic.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.lyq2010.leesmusic.data.api.SubsonicClient
import com.lyq2010.leesmusic.data.api.SubsonicServer
import com.lyq2010.leesmusic.ui.catalog.LibrarySong

class AppPlayer(context: Context) {
    private val future = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java)),
    ).buildAsync()

    val player: Player get() = future.get()

    var songs: List<LibrarySong> = emptyList()
        private set

    fun play(server: SubsonicServer, songs: List<LibrarySong>, index: Int, start: Boolean = false) {
        val player = this.player
        if (songs.isEmpty()) return
        this.songs = songs
        val client = SubsonicClient()
        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(client.streamUrl(server, song.id))
                .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).build())
                .build()
        }
        player.setMediaItems(items, index.coerceIn(0, items.lastIndex), 0)
        player.prepare()
        player.playWhenReady = start
    }

    fun release() {
        MediaController.releaseFuture(future)
    }
}

fun Player.currentSongTitle(): String = currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
