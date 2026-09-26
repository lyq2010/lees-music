package com.lyq2010.leesmusic.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import com.lyq2010.leesmusic.ui.catalog.RemoteCover
import okhttp3.OkHttpClient

internal val PlaybackGradient = Brush.verticalGradient(listOf(Color(0xFF30203F), PlaybackBackground, Color(0xFF0B0712)))

@Composable
fun PlayerScreen(
    song: LibrarySong?, http: OkHttpClient, isPlaying: Boolean,
    positionMs: Long, durationMs: Long, volume: Float, repeatMode: Int,
    onSeek: (Long) -> Unit, onPlayPause: () -> Unit, onVolume: (Float) -> Unit,
    onPrevious: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit,
    onBack: () -> Unit, onOpenLyrics: () -> Unit,
    shuffle: Boolean = false, buffering: Boolean = false, canPrevious: Boolean = true, canNext: Boolean = true,
    onShuffle: () -> Unit = {}, onQueue: () -> Unit = {}, onMore: () -> Unit = {},
    sleepRemainingMs: Long = 0L, onSleepTimer: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(PlaybackGradient).safeDrawingPadding()) {
        PlaybackHandle("收起播放页", onBack)
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp), contentAlignment = Alignment.Center) {
            RemoteCover(song?.coverArtId, song?.coverUrl, Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(16.dp)), http)
        }
        PlaybackControls(song?.title.orEmpty(), song?.artist.orEmpty(), positionMs, durationMs,
            isPlaying, volume, repeatMode, false, onSeek, onPlayPause, onVolume, onPrevious, onNext, onRepeat, onOpenLyrics,
            shuffle, buffering, canPrevious, canNext, onShuffle, onQueue, onMore, sleepRemainingMs, onSleepTimer)
    }
}

@Composable
internal fun PlaybackHandle(description: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(36.dp).semantics { contentDescription = description }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Box(Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(PlaybackWhite.copy(alpha = 0.3f)))
    }
}
