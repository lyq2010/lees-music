package com.lyq2010.leesmusic.ui.discover

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.catalog.LibraryAlbum
import com.lyq2010.leesmusic.ui.catalog.RemoteCover
import com.lyq2010.leesmusic.ui.shell.ShellMuted
import com.lyq2010.leesmusic.ui.shell.ShellText
import okhttp3.OkHttpClient

@Composable
fun DiscoverScreen(
    newest: List<LibraryAlbum>,
    recent: List<LibraryAlbum>,
    frequent: List<LibraryAlbum>,
    random: List<LibraryAlbum>,
    http: OkHttpClient,
    message: String,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("发现", color = ShellText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        if (message.isNotEmpty()) {
            Text(message, color = ShellMuted, modifier = Modifier.padding(top = 8.dp))
        }
        AlbumRow("最近添加", newest, http)
        AlbumRow("最近播放", recent, http)
        AlbumRow("最常播放", frequent, http)
        AlbumRow("随机推荐", random, http)
    }
}

@Composable
private fun AlbumRow(title: String, albums: List<LibraryAlbum>, http: OkHttpClient) {
    Text(title, color = ShellText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
    if (albums.isEmpty()) {
        Text("还没有", color = ShellMuted)
        return
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        albums.forEach { album ->
            Column(Modifier.width(110.dp)) {
                RemoteCover(album.coverUrl, Modifier.size(110.dp).clip(RoundedCornerShape(8.dp)), http)
                Text(album.name, color = ShellText, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
                Text(album.artist, color = ShellMuted, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}
