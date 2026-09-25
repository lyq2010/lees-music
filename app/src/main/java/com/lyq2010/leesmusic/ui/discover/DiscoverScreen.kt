package com.lyq2010.leesmusic.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.catalog.LibraryAlbum
import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import com.lyq2010.leesmusic.ui.catalog.RemoteCover
import com.lyq2010.leesmusic.ui.shell.ShellCard
import com.lyq2010.leesmusic.ui.shell.ShellMuted
import com.lyq2010.leesmusic.ui.shell.ShellText
import okhttp3.OkHttpClient

@Composable
fun DiscoverScreen(
    daily: List<LibrarySong>,
    refreshing: Boolean,
    onOpenDaily: () -> Unit,
    onRefreshDaily: () -> Unit,
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
        DailyCard(daily, refreshing, onOpenDaily, onRefreshDaily, http)
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
                RemoteCover(album.coverArtId, album.coverUrl, Modifier.size(110.dp).clip(RoundedCornerShape(8.dp)), http)
                Text(album.name, color = ShellText, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
                Text(album.artist, color = ShellMuted, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DailyCard(
    songs: List<LibrarySong>,
    refreshing: Boolean,
    onOpen: () -> Unit,
    onRefresh: () -> Unit,
    http: OkHttpClient,
) {
    val cover = songs.firstOrNull()
    Surface(
        onClick = onOpen,
        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
        color = ShellCard,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RemoteCover(cover?.coverArtId, cover?.coverUrl, Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)), http)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("每日推荐", color = ShellText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(if (songs.isEmpty()) "点刷新生成 50 首" else "50 首歌曲", color = ShellMuted)
            }
            IconButton(onClick = onRefresh, enabled = !refreshing) {
                Icon(Icons.Filled.Refresh, contentDescription = if (refreshing) "正在更新" else "刷新每日推荐", tint = ShellText)
            }
        }
    }
}
