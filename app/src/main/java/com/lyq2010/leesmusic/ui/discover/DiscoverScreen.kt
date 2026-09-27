package com.lyq2010.leesmusic.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onPlayDaily: () -> Unit,
    onRefreshDaily: () -> Unit,
    newest: List<LibraryAlbum>,
    recent: List<LibraryAlbum>,
    frequent: List<LibraryAlbum>,
    random: List<LibraryAlbum>,
    http: OkHttpClient,
    message: String,
    onOpenAlbum: (LibraryAlbum) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("发现", color = ShellText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        DailyCard(daily, refreshing, onOpenDaily, onPlayDaily, onRefreshDaily, http)
        if (message.isNotEmpty()) {
            Text(message, color = ShellMuted, modifier = Modifier.padding(top = 8.dp))
        }
        AlbumRow("最近添加", newest, http, onOpenAlbum)
        AlbumRow("最近播放", recent, http, onOpenAlbum)
        AlbumRow("最常播放", frequent, http, onOpenAlbum)
        AlbumRow("随机推荐", random, http, onOpenAlbum)
    }
}

@Composable
private fun AlbumRow(
    title: String,
    albums: List<LibraryAlbum>,
    http: OkHttpClient,
    onOpenAlbum: (LibraryAlbum) -> Unit,
) {
    Text(title, color = ShellText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
    if (albums.isEmpty()) {
        Text("还没有", color = ShellMuted)
        return
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        albums.forEach { album ->
            Column(Modifier.width(110.dp).clickable { onOpenAlbum(album) }) {
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
    onPlay: () -> Unit,
    onRefresh: () -> Unit,
    http: OkHttpClient,
) {
    val cover = songs.firstOrNull()
    Box(
        Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(18.dp)),
    ) {
        RemoteCover(cover?.coverArtId, cover?.coverUrl, Modifier.matchParentSize(), http)
        // Keep cover content readable independently of the app theme and cover brightness.
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.60f)))
        Column(Modifier.align(androidx.compose.ui.Alignment.BottomStart).padding(16.dp)) {
            Text("每日推荐", color = Color.White, fontSize = 13.sp)
            Text(cover?.title ?: "正在准备", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                if (refreshing) "正在更新" else "50 首歌曲    查看全部  >",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp).clickable(onClick = onOpen),
            )
        }
        IconButton(onClick = onRefresh, modifier = Modifier.align(androidx.compose.ui.Alignment.TopStart), enabled = !refreshing) {
            Icon(Icons.Filled.Refresh, contentDescription = "刷新每日推荐", tint = Color.White)
        }
        Box(
            Modifier
                .align(androidx.compose.ui.Alignment.TopEnd)
                .padding(12.dp)
                .size(44.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onPlay),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "顺序播放每日推荐", tint = Color.White)
        }
    }
}
