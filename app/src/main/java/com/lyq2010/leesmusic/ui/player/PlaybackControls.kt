package com.lyq2010.leesmusic.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val PlaybackWhite = Color(0xFFF8F4FA)
internal val PlaybackBackground = Color(0xFF171026)

@Composable
internal fun PlaybackControls(
    title: String, artist: String, positionMs: Long, durationMs: Long,
    isPlaying: Boolean, volume: Float, repeatMode: Int, lyricsVisible: Boolean,
    onSeek: (Long) -> Unit, onPlayPause: () -> Unit, onVolume: (Float) -> Unit,
    onPrevious: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit, onToggleLyrics: () -> Unit,
    shuffle: Boolean = false, buffering: Boolean = false, canPrevious: Boolean = true, canNext: Boolean = true,
    onShuffle: () -> Unit = {}, onQueue: () -> Unit = {}, onMore: () -> Unit = {},
    sleepRemainingMs: Long = 0L, onSleepTimer: () -> Unit = {},
) {
    val connectionMessage by com.lyq2010.leesmusic.playback.PlaybackConnection.message.collectAsState()
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title.ifBlank { "还没有选择歌曲" }, color = PlaybackWhite, fontSize = 26.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = onMore, enabled = title.isNotBlank()) { Icon(Icons.Default.MoreVert, "当前歌曲的更多操作", tint = PlaybackWhite) }
        }
        Text(artist, color = PlaybackWhite.copy(alpha = 0.65f), fontSize = 14.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        if (connectionMessage != null || buffering) Text(connectionMessage ?: "正在缓冲", color = PlaybackWhite.copy(alpha = .65f), fontSize = 12.sp)
        PlaybackSlider(
            value = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
            label = "播放进度", modifier = Modifier.padding(top = 8.dp).testTag("playback-seek"),
            onChange = { if (durationMs > 0) onSeek((it * durationMs).toLong()) },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(playbackTime(positionMs), color = PlaybackWhite.copy(alpha = 0.65f), fontSize = 12.sp)
            Text(playbackTime(durationMs), color = PlaybackWhite.copy(alpha = 0.65f), fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconToggleButton(checked = shuffle, onCheckedChange = { onShuffle() }) {
                Icon(Icons.Default.Shuffle, if (shuffle) "关闭随机播放" else "开启随机播放",
                    tint = PlaybackWhite.copy(alpha = if (shuffle) 1f else .45f))
            }
            IconButton(onClick = onPrevious, enabled = canPrevious) { Icon(Icons.Filled.SkipPrevious, "上一首", tint = PlaybackWhite.copy(alpha = if (canPrevious) 1f else .3f), modifier = Modifier.size(30.dp)) }
            FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(60.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = PlaybackWhite, contentColor = PlaybackBackground)) {
                if (buffering) CircularProgressIndicator(Modifier.size(50.dp), color = PlaybackBackground.copy(alpha = .4f), strokeWidth = 2.dp)
                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (isPlaying) "暂停" else "播放", modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = onNext, enabled = canNext) { Icon(Icons.Filled.SkipNext, "下一首", tint = PlaybackWhite.copy(alpha = if (canNext) 1f else .3f), modifier = Modifier.size(30.dp)) }
            IconButton(onClick = onRepeat) {
                Icon(if (repeatMode == 1) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    when (repeatMode) { 1 -> "单曲循环"; 2 -> "列表循环"; else -> "顺序播放" },
                    tint = PlaybackWhite.copy(alpha = if (repeatMode == 0) 0.45f else 1f))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeDown, null, tint = PlaybackWhite.copy(alpha = 0.65f))
            PlaybackSlider(volume, "音量", onVolume, Modifier.weight(1f).padding(horizontal = 10.dp))
            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = PlaybackWhite.copy(alpha = 0.65f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleLyrics, modifier = Modifier.background(
                if (lyricsVisible) PlaybackWhite.copy(alpha = 0.14f) else Color.Transparent, CircleShape)) {
                Icon(if (lyricsVisible) Icons.Filled.Album else Icons.Filled.Lyrics,
                    if (lyricsVisible) "返回封面" else "查看歌词", tint = PlaybackWhite)
            }
            TextButton(onClick = onSleepTimer) {
                Icon(Icons.Default.Bedtime, "睡眠定时", tint = PlaybackWhite.copy(alpha = if (sleepRemainingMs > 0) 1f else .65f))
                if (sleepRemainingMs > 0) Text(sleepTime(sleepRemainingMs), color = PlaybackWhite, modifier = Modifier.padding(start = 6.dp))
            }
            PlaybackSpeedButton()
            IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "查看播放队列", tint = PlaybackWhite) }
        }
    }
}

@Composable
private fun PlaybackSlider(value: Float, label: String, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    var width by remember { mutableIntStateOf(1) }
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val latestOnChange by rememberUpdatedState(onChange)
    val shown = (dragValue ?: value).coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(40.dp).onSizeChanged { width = it.width.coerceAtLeast(1) }
        .progressSemantics(shown).semantics {
            contentDescription = label
            setProgress { latestOnChange(it.coerceIn(0f, 1f)); true }
        }
        .pointerInput(width) { detectTapGestures { latestOnChange((it.x / width).coerceIn(0f, 1f)) } }
        .pointerInput(width) {
            detectHorizontalDragGestures(
                onDragStart = { dragValue = (it.x / width).coerceIn(0f, 1f) },
                onHorizontalDrag = { change, _ -> dragValue = (change.position.x / width).coerceIn(0f, 1f) },
                onDragEnd = { dragValue?.let(latestOnChange); dragValue = null },
                onDragCancel = { dragValue = null },
            )
        }, contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(PlaybackWhite.copy(alpha = 0.22f)))
        Box(Modifier.fillMaxWidth(shown).height(3.dp).clip(CircleShape).background(PlaybackWhite))
    }
}

private fun playbackTime(ms: Long): String = "%d:%02d".format(ms.coerceAtLeast(0) / 60000, ms.coerceAtLeast(0) / 1000 % 60)
