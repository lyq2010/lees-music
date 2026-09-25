package com.lyq2010.leesmusic.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.catalog.AlbumCover
import com.lyq2010.leesmusic.ui.catalog.Track
import com.lyq2010.leesmusic.ui.catalog.playerBackground
import com.lyq2010.leesmusic.ui.theme.Peach

@Composable
fun PlayerScreen(
    song: com.lyq2010.leesmusic.ui.catalog.LibrarySong?,
    http: okhttp3.OkHttpClient,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onVolume: (Float) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onOpenLyrics: () -> Unit,
) {
    val ink = com.lyq2010.leesmusic.ui.theme.Ink
    var favorite by rememberSaveable { mutableStateOf(false) }
    var volume by rememberSaveable { mutableFloatStateOf(1f) }
    Column(
        Modifier
            .fillMaxSize()
            .background(ink)
            .padding(horizontal = 24.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "返回", tint = Peach)
        }
        com.lyq2010.leesmusic.ui.catalog.RemoteCover(
            song?.coverArtId,
            song?.coverUrl,
            Modifier
                .padding(top = 8.dp, start = 28.dp, end = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Peach.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
            http,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(song?.title ?: "还没有在播放", color = Peach, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp)
                Text(song?.artist.orEmpty(), color = Peach.copy(alpha = 0.8f), fontSize = 15.sp)
            }
            IconButton(onClick = { favorite = !favorite }) {
                Icon(
                    if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (favorite) "取消收藏" else "收藏",
                    tint = Peach,
                )
            }
            IconButton(onClick = {}) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Peach)
            }
        }
        ThinSeek(
            fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
            onChange = { fraction -> if (durationMs > 0) onSeek((fraction * durationMs).toLong()) },
            modifier = Modifier.padding(top = 20.dp),
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime((positionMs / 1000).toInt()), color = Peach, fontSize = 12.sp)
            Text(formatTime(if (durationMs > 0) (durationMs / 1000).toInt() else 0), color = Peach, fontSize = 12.sp)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportSlot { Icon(Icons.Filled.Shuffle, contentDescription = "随机播放", tint = Peach) }
            TransportSlot {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "上一首", tint = Peach)
                }
            }
            TransportSlot {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Peach)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = ink,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            TransportSlot {
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "下一首", tint = Peach)
                }
            }
            TransportSlot {
                IconButton(onClick = onOpenLyrics) {
                    Icon(Icons.Filled.MusicNote, contentDescription = "歌词", tint = Peach)
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null, tint = Peach)
            ThinSeek(
                fraction = volume,
                onChange = { volume = it; onVolume(it) },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "音量", tint = Peach)
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = onOpenLyrics) {
                Icon(Icons.Filled.MusicNote, contentDescription = "歌词", tint = Peach)
            }
            Icon(Icons.Filled.Cast, contentDescription = "投屏", tint = Peach, modifier = Modifier.padding(12.dp))
            Icon(Icons.Filled.Schedule, contentDescription = "睡眠定时", tint = Peach, modifier = Modifier.padding(12.dp))
            Icon(Icons.Filled.Repeat, contentDescription = "循环", tint = Peach, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun RowScope.TransportSlot(content: @Composable () -> Unit) {
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun ThinSeek(fraction: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    var width by remember { mutableStateOf(1) }
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging ?: fraction
    Box(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .onSizeChanged { width = it.width.coerceAtLeast(1) }
            .pointerInput(width) {
                detectHorizontalDragGestures(
                    onDragStart = { start -> dragging = (start.x / width).coerceIn(0f, 1f) },
                    onHorizontalDrag = { change, _ ->
                        dragging = (change.position.x / width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragging?.let(onChange)
                        dragging = null
                    },
                    onDragCancel = { dragging = null },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Peach.copy(alpha = 0.25f)),
        )
        Box(
            Modifier
                .fillMaxWidth(shown.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(CircleShape)
                .background(Peach),
        )
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
