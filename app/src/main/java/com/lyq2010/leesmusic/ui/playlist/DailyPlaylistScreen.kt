package com.lyq2010.leesmusic.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import com.lyq2010.leesmusic.ui.catalog.RemoteCover
import com.lyq2010.leesmusic.ui.shell.ShellBg
import com.lyq2010.leesmusic.ui.shell.ShellMuted
import com.lyq2010.leesmusic.ui.shell.ShellText
import okhttp3.OkHttpClient

@Composable
fun DailyPlaylistScreen(
    songs: List<LibrarySong>,
    http: OkHttpClient,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ShellBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = ShellText)
        }
        Text("每日推荐", color = ShellText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text("${songs.size} 首歌曲", color = ShellMuted, modifier = Modifier.padding(bottom = 12.dp))
        songs.forEachIndexed { index, song ->
            Row(
                Modifier.clickable { onPlay(index) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}", color = ShellMuted, modifier = Modifier.padding(end = 10.dp))
                RemoteCover(song.coverArtId, song.coverUrl, Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)), http)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(song.title, color = ShellText, maxLines = 1)
                    Text(song.artist, color = ShellMuted, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}
