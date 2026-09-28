package com.lyq2010.leesmusic.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.data.library.*
import com.lyq2010.leesmusic.ui.catalog.*
import com.lyq2010.leesmusic.ui.shell.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

@Composable
fun LibraryBrowser(target: LibraryDestination, server: SubsonicServer?, client: SubsonicClient,
    http: OkHttpClient, revision: Int, downloads: LibraryDownloads, cache: LibraryBrowseCache, onBack: () -> Unit,
    onAlbum: (LibraryAlbum) -> Unit, onArtist: (Artist) -> Unit,
    onPlay: (List<LibrarySong>, Int, Boolean) -> Unit, onMore: (LibrarySong) -> Unit,
    onPlaylistSongMore: (LibrarySong, String, Int) -> Unit = { song, _, _ -> onMore(song) },
    onSharePlaylist: (String) -> Unit = {},
    canSharePlaylist: Boolean = false,
    onRandomPlay: (List<LibrarySong>) -> Unit = { onPlay(it.shuffled(), 0, true) }) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state = remember(cache, target.kind, target.id) { cache.page(target) }
    var transfers by remember { mutableStateOf<List<LibraryDownload>>(emptyList()) }
    var downloadLoading by remember { mutableStateOf(true) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    val filter = state.filter
    val offline = target.kind == "downloads"
    val contentRevision = if (target.kind == "favorites" || target.kind == "playlist") revision else 0
    val loading = if (offline) downloadLoading else state.loading
    val error = if (offline) downloadError else state.error
    LaunchedEffect(state, server, contentRevision, cache.generation, refresh) {
        if (!offline) {
            state.load(cache.generation, contentRevision, force = refresh > 0) {
                withContext(Dispatchers.IO) { loadLibraryPage(client, requireNotNull(server), target) }
            }
            return@LaunchedEffect
        }
        downloadLoading = true
        downloadError = null
        try {
            if (offline) {
                while (true) {
                    transfers = withContext(Dispatchers.IO) {
                        downloads.list().map { download ->
                            download.copy(song = download.song.copy(coverUrl = server?.let { source ->
                                download.song.coverArtId?.let { client.coverArtUrl(source, it) }
                            }))
                        }
                    }
                    downloadLoading = false
                    delay(1500)
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { downloadError = "读取失败，请检查连接后重试" }
        finally { downloadLoading = false }
    }
    val shown = state.data ?: LibraryPage()
    val songEntries = sortedSongs(shown.songs, state.sort).filter { filter.isBlank() || it.value.title.contains(filter, true) || it.value.artist.contains(filter, true) }
    val songs = songEntries.map { it.value }
    val albums = sortedAlbums(shown.albums, state.sort).filter { filter.isBlank() || it.name.contains(filter, true) || it.artist.contains(filter, true) }
    val orderedTransfers = sortedDownloads(transfers, state.sort)
    val artists = shown.artists.filter { filter.isBlank() || it.name.contains(filter, true) }
    if (target.kind == "artists") {
        ArtistBrowser(state, server, client, http, onBack, onArtist, onRefresh = { refresh++ })
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Default.ArrowBack, "返回", tint = ShellText) }
            Text(target.title, color = ShellText, fontSize = 22.sp, modifier = Modifier.weight(1f))
            if (target.kind in setOf("songs", "albums", "playlist", "downloads", "favorites")) Box {
                IconButton(onClick = { sortMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, "排序", tint = ShellText)
                }
                DropdownMenu(sortMenu, { sortMenu = false }) {
                    val choices = if (target.kind == "playlist") listOf("添加时间", "标题 A–Z", "标题 Z–A", "艺术家", "时长")
                        else if (target.kind == "albums") listOf("默认顺序", "名称 A–Z", "名称 Z–A", "艺术家")
                        else if (offline) listOf("默认顺序", "标题 A–Z", "标题 Z–A", "艺术家")
                        else listOf("默认顺序", "标题 A–Z", "标题 Z–A", "艺术家", "时长")
                    choices.forEachIndexed { index, choice ->
                        DropdownMenuItem(text = { Text(choice + if (state.sort == index) " ✓" else "") },
                            onClick = { state.sort = index; sortMenu = false })
                    }
                }
            }
            if (target.kind == "playlist" && canSharePlaylist) TextButton(onClick = { onSharePlaylist(target.id) }) { Text("分享") }
            IconButton(onClick = { refresh++ }, enabled = !loading) { Icon(Icons.Default.Refresh, "刷新列表", tint = ShellText) }
        }
        if (!offline) OutlinedTextField(filter, { state.filter = it }, placeholder = { Text("筛选当前列表") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp))
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { message -> Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = ShellMuted, modifier = Modifier.weight(1f))
            TextButton(onClick = { refresh++ }) { Text("重试") }
        } }
        LazyColumn(Modifier.weight(1f), state = state.list, contentPadding = PaddingValues(bottom = 20.dp)) {
            if (offline) {
                if (!loading && orderedTransfers.isEmpty()) item { LibraryEmpty("暂无下载", "在歌曲的三点菜单中选择下载，完成后可离线播放") }
                items(orderedTransfers, key = { it.song.id }) { download ->
                    val playable = orderedTransfers.filter { it.playable }.map { it.song.copy(localUri = it.uri) }
                    Column {
                        SongRow(download.song, http, onPlay = {
                            if (download.playable) onPlay(playable, playable.indexOfFirst { it.id == download.song.id }, false)
                            else android.widget.Toast.makeText(context, download.label, android.widget.Toast.LENGTH_SHORT).show()
                        }, onMore = { onMore(download.song.copy(localUri = download.uri)) })
                        Text(download.label, color = ShellMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 80.dp, bottom = 8.dp))
                    }
                }
            } else {
                if (!loading && error == null && songs.isEmpty() && albums.isEmpty() && artists.isEmpty())
                    item { LibraryEmpty(if (filter.isBlank()) "这里还没有内容" else "没有匹配的结果",
                        if (target.kind == "favorites") "在歌曲菜单中收藏歌曲，即可在这里找到" else "可以刷新列表，或更换筛选词") }
                if (songs.isNotEmpty()) item {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onPlay(songs, 0, true) }) { Text("播放全部 · ${songs.size}") }
                        OutlinedButton(onClick = { onRandomPlay(songs) }) { Text("随机播放") }
                    }
                }
                itemsIndexed(songEntries) { index, entry -> SongRow(entry.value, http,
                    { onPlay(songs, index, false) },
                    { if (target.kind == "playlist") onPlaylistSongMore(entry.value, target.id, entry.index) else onMore(entry.value) }) }
                items(albums, key = { it.id }) { LibraryAlbumRow(it, http) { onAlbum(it) } }
                items(artists, key = { it.id }) { artist ->
                    ListItem(headlineContent = { Text(artist.name) }, supportingContent = { Text("${artist.albumCount} 张专辑") },
                        colors = ListItemDefaults.colors(containerColor = ShellBg, headlineColor = ShellText, supportingColor = ShellMuted),
                        modifier = Modifier.clickable { onArtist(artist) })
                }
            }
        }
    }
}

@Composable
internal fun LibraryEmpty(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = ShellText, fontSize = 18.sp)
        Text(detail, color = ShellMuted, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
internal fun LibraryAlbumRow(album: LibraryAlbum, http: OkHttpClient, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RemoteCover(album.coverArtId, album.coverUrl, Modifier.size(52.dp), http)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(album.name, color = ShellText, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(album.artist, color = ShellMuted, fontSize = 12.sp, maxLines = 1)
        }
    }
}
