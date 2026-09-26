package com.lyq2010.leesmusic.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import com.lyq2010.leesmusic.ui.catalog.RemoteCover
import com.lyq2010.leesmusic.ui.shell.ShellAccent
import com.lyq2010.leesmusic.ui.shell.ShellAccentText
import com.lyq2010.leesmusic.ui.shell.ShellBg
import com.lyq2010.leesmusic.ui.shell.ShellCard
import com.lyq2010.leesmusic.ui.shell.ShellMuted
import com.lyq2010.leesmusic.ui.shell.ShellText
import okhttp3.OkHttpClient

@Composable
fun AlbumScreen(
    title: String,
    artist: String,
    year: Int,
    coverArtId: String?,
    coverUrl: String?,
    songs: List<LibrarySong>,
    http: OkHttpClient,
    onBack: () -> Unit,
    onPlayInOrder: () -> Unit,
    onShuffle: () -> Unit,
    onPlay: (Int) -> Unit,
    onMore: (LibrarySong) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ShellBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = ShellText)
            }
            Text("专辑", color = ShellText, fontSize = 18.sp, modifier = Modifier.weight(1f))
        }
        RemoteCover(
            coverArtId,
            coverUrl,
            Modifier.padding(top = 8.dp).size(220.dp).clip(RoundedCornerShape(16.dp)),
            http,
        )
        Text(title, color = ShellText, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        Text(artist, color = ShellAccent, fontSize = 16.sp)
        Text(albumMeta(year, songs.size), color = ShellMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPlayInOrder, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("顺序播放", modifier = Modifier.padding(start = 4.dp))
            }
            Button(
                onClick = onShuffle,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ShellCard, contentColor = ShellText),
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null)
                Text("随机播放", modifier = Modifier.padding(start = 4.dp))
            }
        }
        songs.forEachIndexed { index, song ->
            com.lyq2010.leesmusic.ui.catalog.SongRow(song, http, { onPlay(index) }, { onMore(song) })
        }
    }
}

private fun albumMeta(year: Int, count: Int): String {
    val yearText = if (year > 0) "$year · " else ""
    return "$yearText$count 首歌曲"
}
