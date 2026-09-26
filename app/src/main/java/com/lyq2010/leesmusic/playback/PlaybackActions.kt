package com.lyq2010.leesmusic.playback

import androidx.media3.common.Player

fun togglePlayback(player: Player) {
    when {
        player.mediaItemCount == 0 -> return
        player.playbackState == Player.STATE_ENDED -> { player.seekToDefaultPosition(); player.play() }
        player.playWhenReady -> player.pause()
        else -> { if (player.playbackState == Player.STATE_IDLE) player.prepare(); player.play() }
    }
}
