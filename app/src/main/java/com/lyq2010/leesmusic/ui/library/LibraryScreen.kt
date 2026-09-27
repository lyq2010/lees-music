package com.lyq2010.leesmusic.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.catalog.*
import com.lyq2010.leesmusic.ui.shell.*
import okhttp3.OkHttpClient

@Composable
fun LibraryScreen(label: String, username: String, newest: List<LibraryAlbum>, overview: LibraryOverview?,
    loading: Boolean, error: String?, http: OkHttpClient, onRetry: () -> Unit,
    onOpen: (LibraryDestination) -> Unit, onAlbum: (LibraryAlbum) -> Unit,
    onPlayFavorites: () -> Unit, onCreate: () -> Unit, onServer: () -> Unit,
    onDeletePlaylist: (com.lyq2010.leesmusic.data.api.MusicPlaylist) -> Unit = {},
    onSharePlaylist: (com.lyq2010.leesmusic.data.api.MusicPlaylist) -> Unit = {}, cachedAt: Long = 0L) {
    var shared by rememberSaveable { mutableStateOf(false) }
    val playlists = overview?.playlists.orEmpty().filter { if (shared) it.owner != username else it.owner == username }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("音乐库", color = ShellText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(label, color = ShellMuted, fontSize = 14.sp)
                }
                IconButton(onClick = onRetry, enabled = !loading) { Icon(Icons.Default.Refresh, "刷新音乐库", tint = ShellMuted) }
                IconButton(onClick = onServer) { Icon(Icons.Default.Dns, "服务器设置", tint = ShellAccent) }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            CacheNotice(cachedAt)
            error?.let { Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(it, color = ShellMuted, modifier = Modifier.weight(1f))
                TextButton(onClick = onRetry) { Text("重试") }
            } }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibraryTile(Icons.Default.MusicNote, "歌曲", Modifier.weight(1f)) { onOpen(LibraryDestination("songs", "歌曲")) }
                    LibraryTile(Icons.Default.Album, "专辑", Modifier.weight(1f)) { onOpen(LibraryDestination("albums", "专辑")) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibraryTile(Icons.Default.Person, "艺术家", Modifier.weight(1f)) { onOpen(LibraryDestination("artists", "艺术家")) }
                    LibraryTile(Icons.Default.Download, "下载", Modifier.weight(1f)) { onOpen(LibraryDestination("downloads", "下载")) }
                }
                Surface(color = ShellCard, shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(LibraryDestination("favorites", "我喜欢的")) }) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, null, tint = ShellAccent, modifier = Modifier.size(36.dp))
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            Text("我喜欢的", color = ShellText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text(overview?.let { "${it.favorites.size} 首歌曲" } ?: "点击查看收藏", color = ShellMuted)
                        }
                        IconButton(onClick = onPlayFavorites, enabled = overview?.favorites?.isNotEmpty() == true) {
                            Icon(Icons.Default.PlayArrow, "播放我喜欢的", tint = if (overview?.favorites?.isNotEmpty() == true) ShellAccent else ShellMuted)
                        }
                    }
                }
            }
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { shared = false }) { Text("我的歌单", color = if (!shared) ShellText else ShellMuted, fontSize = 18.sp) }
                TextButton(onClick = { shared = true }) { Text("共享歌单", color = if (shared) ShellText else ShellMuted, fontSize = 18.sp) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onCreate) { Icon(Icons.Default.Add, "新建歌单", tint = ShellAccent) }
            }
        }
        if (overview == null && error == null) item {
            LibraryEmpty("正在读取歌单", "请稍候")
        }
        if (overview != null && playlists.isEmpty()) item {
            LibraryEmpty(if (shared) "暂无共享歌单" else "还没有歌单", if (shared) "这里显示其他用户共享给你的歌单" else "点右上方加号，新建自己的歌单")
        }
        items(playlists, key = { it.id }) { playlist ->
            ListItem(headlineContent = { Text(playlist.name) }, supportingContent = { Text("${playlist.songCount} 首歌曲 · ${playlist.owner}") },
                leadingContent = { Icon(Icons.Default.QueueMusic, null, tint = ShellAccent) },
                trailingContent = { Row {
                    if (!shared) {
                        IconButton(onClick = { onSharePlaylist(playlist) }) { Icon(Icons.Default.Share, "分享歌单${playlist.name}", tint = ShellAccent) }
                        IconButton(onClick = { onDeletePlaylist(playlist) }) { Icon(Icons.Default.DeleteOutline, "删除歌单${playlist.name}", tint = ShellMuted) }
                    }
                } },
                colors = ListItemDefaults.colors(containerColor = ShellBg, headlineColor = ShellText, supportingColor = ShellMuted),
                modifier = Modifier.clickable { onOpen(LibraryDestination("playlist", playlist.name, playlist.id)) })
        }
        if (newest.isNotEmpty()) {
            item { Text("最近添加", color = ShellText, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, modifier = Modifier.padding(20.dp)) }
            items(newest.take(8), key = { "recent-${it.id}" }) { album -> LibraryAlbumRow(album, http) { onAlbum(album) } }
        }
    }
}

@Composable
private fun LibraryTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), color = ShellCard, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(vertical = 24.dp, horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = ShellAccent)
            Text(label, color = ShellText, modifier = Modifier.padding(start = 14.dp), fontSize = 17.sp)
        }
    }
}
