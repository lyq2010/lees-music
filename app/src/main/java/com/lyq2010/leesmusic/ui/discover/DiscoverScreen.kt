package com.lyq2010.leesmusic.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.catalog.AlbumCover
import com.lyq2010.leesmusic.ui.catalog.SampleCatalog
import com.lyq2010.leesmusic.ui.catalog.Track
import com.lyq2010.leesmusic.ui.shell.ShellMuted
import com.lyq2010.leesmusic.ui.shell.ShellText

@Composable
fun DiscoverScreen(onOpenPlayer: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("发现", color = ShellText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text("每日推荐", color = ShellMuted, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        AlbumCover(
            SampleCatalog.nowPlaying.cover,
            Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Section("最近添加") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SampleCatalog.tracks.forEach { track ->
                    CoverTitle(track, Modifier.width(96.dp))
                }
            }
        }
        Section("最近播放") {
            SampleCatalog.tracks.take(3).forEach { track ->
                SongRow(track, onOpenPlayer)
            }
        }
        Section("最常播放") {
            SampleCatalog.tracks.take(3).forEach { track ->
                SongRow(track, onOpenPlayer)
            }
        }
        Section("随机推荐") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SampleCatalog.tracks.take(2).forEach { track ->
                    CoverTitle(track, Modifier.width(96.dp))
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title, color = ShellText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
    content()
}

@Composable
private fun CoverTitle(track: Track, modifier: Modifier) {
    Column(modifier) {
        AlbumCover(track.cover, Modifier.clip(RoundedCornerShape(8.dp)))
        Text(track.title, color = ShellText, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Text(track.artist, color = ShellMuted, fontSize = 12.sp)
    }
}

@Composable
private fun SongRow(track: Track, onClick: () -> Unit) {
    Row(
        Modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumCover(track.cover, Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)))
        Column(Modifier.padding(start = 10.dp)) {
            Text(track.title, color = ShellText)
            Text(track.artist, color = ShellMuted, fontSize = 12.sp)
        }
    }
}
