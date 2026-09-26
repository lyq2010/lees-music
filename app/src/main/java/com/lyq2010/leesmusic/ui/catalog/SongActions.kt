package com.lyq2010.leesmusic.ui.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.data.library.LibraryDownloads
import com.lyq2010.leesmusic.ui.shell.*
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActions(song: LibrarySong, server: SubsonicServer, client: SubsonicClient, downloads: LibraryDownloads,
    onDismiss: () -> Unit, onPlay: () -> Unit, onEnqueue: (LibrarySong, Boolean) -> Boolean,
    onChanged: () -> Unit, onMessage: (String) -> Unit,
    onAlbum: (() -> Unit)? = null, http: okhttp3.OkHttpClient = remember { okhttp3.OkHttpClient() }) {
    val context = LocalContext.current
    var download by remember(song.id) { mutableStateOf<com.lyq2010.leesmusic.data.library.LibraryDownload?>(null) }
    var downloadLoaded by remember(song.id) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var favorite by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf("menu") }
    var playlists by remember { mutableStateOf<List<MusicPlaylist>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val playable = if (download?.playable == true) song.copy(localUri = download?.uri) else song
    LaunchedEffect(song.id) {
        try { download = withContext(Dispatchers.IO) { downloads.list().firstOrNull { it.song.id == song.id } }; downloadLoaded = true }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { downloadLoaded = false }
    }
    LaunchedEffect(song.id, reload) {
        try { favorite = withContext(Dispatchers.IO) { client.favorites(server).any { it.id == song.id } } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "收藏状态读取失败，可重试" }
    }
    fun operation(success: String, action: () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            try {
                withContext(Dispatchers.IO) { action() }
                onChanged()
                onMessage(success)
                onDismiss()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { error = "操作失败，未确认成功，请重试" }
            finally { busy = false }
        }
    }
    ShellTheme {
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = { if (!busy) onDismiss() }, containerColor = ShellCard) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RemoteCover(song.coverArtId, song.coverUrl, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)), http)
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleLarge, color = ShellText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, color = ShellMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (song.album.isNotBlank()) Text(song.album, color = ShellMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
            if (favorite == null && !busy && error != null) TextButton(onClick = { error = null; reload++ }) { Text("重试读取收藏状态") }
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (mode == "info") {
                item {
                    Text(listOf("专辑：${song.album.ifBlank { "未提供" }}", "艺术家：${song.artist}",
                        "时长：%d:%02d".format(song.duration / 60, song.duration % 60),
                        "格式：${song.suffix.uppercase().ifBlank { "未知" }}",
                        "码率：${song.bitRate.takeIf { it > 0 }?.let { "$it kbps" } ?: "未知"}").joinToString("\n"),
                        color = ShellText, modifier = Modifier.padding(12.dp))
                    TextButton(onClick = { mode = "menu" }) { Text("返回操作菜单") }
                }
            } else if (mode == "playlists") {
                item { TextButton(enabled = !busy, onClick = { mode = "menu" }) { Text("返回操作菜单") } }
                if (playlists.isEmpty()) item { Text("还没有自己的歌单，请先在首页新建", color = ShellMuted, modifier = Modifier.padding(12.dp)) }
                items(playlists, key = { it.id }) { playlist ->
                    ActionItem(playlist.name, !busy) { operation("已加入 ${playlist.name}") { client.addToPlaylist(server, playlist.id, song.id) } }
                }
            } else {
                item { Text("播放", color = ShellAccent, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(16.dp, 8.dp)) }
                item { ActionItem("立即播放", !busy) { onPlay(); onDismiss() } }
                item { ActionItem("下一首播放", !busy && downloadLoaded) { if (onEnqueue(playable, true)) { onMessage("已设为下一首"); onDismiss() } else error = "播放器正在连接，请稍后重试" } }
                item { ActionItem("加入播放队列", !busy && downloadLoaded) { if (onEnqueue(playable, false)) { onMessage("已加入播放队列"); onDismiss() } else error = "播放器正在连接，请稍后重试" } }
                item { HorizontalDivider(color = ShellBg, modifier = Modifier.padding(vertical = 8.dp)) }
                item { ActionItem(if (favorite == true) "取消收藏" else "收藏歌曲", !busy && favorite != null) {
                    operation(if (favorite == true) "已取消收藏" else "已收藏") { client.setFavorite(server, song.id, favorite != true) }
                } }
                item { ActionItem("加入歌单", !busy) {
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            playlists = withContext(Dispatchers.IO) { client.playlists(server).filter { it.owner == server.username } }
                            mode = "playlists"
                        } catch (cancelled: CancellationException) { throw cancelled
                        } catch (_: Exception) { error = "歌单读取失败，请重试" }
                        finally { busy = false }
                    }
                } }
                item { ActionItem(if (download?.playable == true) "已下载" else if (download?.status == android.app.DownloadManager.STATUS_FAILED) "重新下载" else "下载原音质",
                    !busy && download?.playable != true && download?.status !in listOf(android.app.DownloadManager.STATUS_PENDING, android.app.DownloadManager.STATUS_RUNNING, android.app.DownloadManager.STATUS_PAUSED),
                    detail = if (downloadLoaded) download?.label ?: "离线播放" else "正在读取下载状态") {
                    scope.launch {
                        busy = true
                        try {
                            val message = withContext(Dispatchers.IO) { downloads.enqueue(server, song) }
                            onChanged(); onMessage(message); onDismiss()
                        } catch (cancelled: CancellationException) { throw cancelled
                        } catch (_: Exception) { error = "无法开始下载，请检查存储与网络后重试" }
                        finally { busy = false }
                    }
                } }
                item { HorizontalDivider(color = ShellBg, modifier = Modifier.padding(vertical = 8.dp)) }
                if (onAlbum != null) item { ActionItem("查看专辑", !busy) { onDismiss(); onAlbum() } }
                item { ActionItem("分享歌曲信息", !busy, "") {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, listOf(song.title, song.artist, song.album).filter { it.isNotBlank() }.joinToString(" · "))
                    }
                    runCatching { context.startActivity(android.content.Intent.createChooser(intent, "分享歌曲信息")) }
                        .onFailure { error = "没有可用的分享应用" }
                } }
                item { ActionItem("歌曲信息", !busy) { mode = "info" } }
            }
        }
    }
    }
}
