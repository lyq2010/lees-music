package com.lyq2010.leesmusic.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.data.library.LibraryDownloads
import com.lyq2010.leesmusic.ui.catalog.*
import com.lyq2010.leesmusic.ui.library.*
import com.lyq2010.leesmusic.ui.shell.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

@Composable
fun SearchScreen(state: SearchState, server: SubsonicServer?, http: OkHttpClient,
    cache: LibraryBrowseCache, downloads: LibraryDownloads,
    onAlbum: (LibraryAlbum) -> Unit, onPlay: (List<LibrarySong>, Int, Boolean) -> Unit,
    onMore: (LibrarySong) -> Unit, onServer: () -> Unit) {
    val client = remember(http) { SubsonicClient(http) }
    val artist = state.artist
    if (artist != null) {
        BackHandler { state.artist = null }
        LibraryBrowser(LibraryDestination("artist", artist.name, artist.id), server, client, http, 0, downloads, cache,
            onBack = { state.artist = null }, onAlbum = onAlbum, onArtist = { state.artist = it }, onPlay = onPlay, onMore = onMore)
        return
    }
    val query = state.query.trim()
    val kind = state.kind
    val page = state.page(query, kind)
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var submit by remember { mutableIntStateOf(0) }
    suspend fun load() {
        if (query.isBlank() || server == null) return
        page.load(kind) { offset -> withContext(Dispatchers.IO) { client.searchPage(server, query, kind, offset) } }
    }
    LaunchedEffect(query, kind, submit) {
        if (!page.loaded && query.isNotBlank()) {
            if (submit == 0) delay(350)
            load()
        }
    }
    val songs = remember(page.result, server) { server?.let { source -> page.result.song.map { it.toLibrarySong(client, source) } }.orEmpty() }
    val albums = remember(page.result, server) { server?.let { source -> page.result.album.map { it.toLibraryAlbum(client, source) } }.orEmpty() }
    val count = when (kind) { "album" -> albums.size; "artist" -> page.result.artist.size; else -> songs.size }
    Column(Modifier.fillMaxSize()) {
        Text("搜索", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))
        TextField(state.query, { state.query = it; submit = 0 }, singleLine = true,
            placeholder = { Text("搜索歌曲、专辑、艺术家") }, leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (state.query.isNotEmpty()) IconButton(onClick = { state.query = "" }) { Icon(Icons.Default.Close, "清空搜索") } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit++; keyboard?.hide() }),
            shape = RoundedCornerShape(32.dp), colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
        Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("song" to "歌曲", "album" to "专辑", "artist" to "艺术家").forEach { (key, label) ->
                FilterChip(selected = kind == key, onClick = { state.kind = key }, label = { Text(label) })
            }
        }
        when {
            server == null -> { LibraryEmpty("请先连接音乐服务器", "连接后即可搜索自己的曲库"); TextButton(onClick = onServer, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("配置服务器") } }
            query.isBlank() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, null, Modifier.size(68.dp), tint = ShellAccent)
                    Text("搜索歌曲、专辑、艺术家", color = ShellMuted, modifier = Modifier.padding(top = 24.dp))
                }
            }
            else -> {
                if (page.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(state = page.list, modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
                    if (page.loaded && count == 0) item { LibraryEmpty("没有找到相关结果", "试试其他关键词，或切换搜索分类") }
                    if (kind == "song") itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                        SongRow(song, http, { keyboard?.hide(); onPlay(songs, index, false) }, { keyboard?.hide(); onMore(song) })
                    }
                    if (kind == "album") items(albums, key = { it.id }) { album -> LibraryAlbumRow(album, http) { keyboard?.hide(); onAlbum(album) } }
                    if (kind == "artist") items(page.result.artist, key = { it.id }) { entry ->
                        ListItem(headlineContent = { Text(entry.name) }, supportingContent = { Text("${entry.albumCount} 张专辑") },
                            leadingContent = { ArtistPortrait(entry, server, client, http, Modifier.size(56.dp)) },
                            colors = ListItemDefaults.colors(containerColor = ShellBg),
                            modifier = Modifier.clickable { keyboard?.hide(); state.artist = entry })
                    }
                    page.error?.let { message -> item {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(message, color = ShellMuted)
                            TextButton(onClick = { scope.launch { load() } }, enabled = !page.loading) { Text("重试") }
                        }
                    } }
                    if (page.loaded && !page.end && page.error == null) item {
                        TextButton(onClick = { scope.launch { load() } }, enabled = !page.loading, modifier = Modifier.fillMaxWidth()) {
                            Text(if (page.loading) "正在加载" else "加载更多")
                        }
                    }
                    if (page.end && count > 0) item { Text("共 $count 条结果", color = ShellMuted, modifier = Modifier.padding(20.dp)) }
                }
            }
        }
    }
}
