package com.lyq2010.leesmusic.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    track: Track,
    onBack: () -> Unit,
    onOpenLyrics: () -> Unit,
) {
    val ink = track.cover.playerBackground()
    var favorite by rememberSaveable { mutableStateOf(false) }
    var position by rememberSaveable { mutableFloatStateOf(134f) }
    var volume by rememberSaveable { mutableFloatStateOf(0.7f) }
    Column(
        Modifier
            .fillMaxSize()
            .background(ink)
            .padding(horizontal = 24.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "返回", tint = Peach)
        }
        AlbumCover(
            track.cover,
            Modifier
                .padding(top = 8.dp, start = 28.dp, end = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Peach.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(track.title, color = Peach, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp)
                Text("${track.artist} · ${track.album}", color = Peach.copy(alpha = 0.8f), fontSize = 15.sp)
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
        Slider(
            value = position,
            onValueChange = { position = it },
            valueRange = 0f..track.durationSeconds.toFloat(),
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position.toInt()), color = Peach, fontSize = 12.sp)
            Text(formatTime(track.durationSeconds), color = Peach, fontSize = 12.sp)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Shuffle, contentDescription = "随机播放", tint = Peach)
            Icon(Icons.Filled.SkipPrevious, contentDescription = "上一首", tint = Peach, modifier = Modifier.size(36.dp))
            Icon(Icons.Filled.PlayArrow, contentDescription = "播放", tint = ink, modifier = Modifier.size(64.dp).background(Peach, androidx.compose.foundation.shape.CircleShape).padding(12.dp))
            Icon(Icons.Filled.SkipNext, contentDescription = "下一首", tint = Peach, modifier = Modifier.size(36.dp))
            IconButton(onClick = onOpenLyrics) {
                Icon(Icons.Filled.MusicNote, contentDescription = "歌词", tint = Peach)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null, tint = Peach)
            Slider(value = volume, onValueChange = { volume = it }, modifier = Modifier.weight(1f))
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

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
