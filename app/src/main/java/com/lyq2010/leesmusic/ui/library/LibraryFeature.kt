package com.lyq2010.leesmusic.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.data.library.LibraryDownloads
import com.lyq2010.leesmusic.ui.catalog.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

@Composable
fun LibraryFeature(server: SubsonicServer?, label: String, newest: List<LibraryAlbum>, http: OkHttpClient,
    revision: Int, downloads: LibraryDownloads, cache: LibraryBrowseCache, onRefreshShelf: () -> Unit, onAlbum: (LibraryAlbum) -> Unit,
    onPlay: (List<LibrarySong>, Int, Boolean) -> Unit, onMore: (LibrarySong) -> Unit, onServer: () -> Unit,
    homeRequest: Int = 0, onPlaylistSongMore: (LibrarySong, String, Int) -> Unit = { song, _, _ -> onMore(song) },
    onRandomPlay: (List<LibrarySong>) -> Unit = { onPlay(it.shuffled(), 0, true) }) {
    var destinationKind by rememberSaveable { mutableStateOf("") }
    var destinationTitle by rememberSaveable { mutableStateOf("") }
    var destinationId by rememberSaveable { mutableStateOf("") }
    fun navigate(target: LibraryDestination) {
        destinationKind = target.kind; destinationTitle = target.title; destinationId = target.id
    }
    fun back() {
        if (destinationKind == "artist") navigate(LibraryDestination("artists", "艺术家"))
        else destinationKind = ""
    }
    val state = cache.overview
    val overview = state.data
    var refresh by remember { mutableIntStateOf(0) }
    var create by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<MusicPlaylist?>(null) }
    var sharing by remember { mutableStateOf<MusicPlaylist?>(null) }
    var operationError by remember { mutableStateOf<String?>(null) }
    val client = remember(http) { SubsonicClient(http) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(homeRequest) { if (homeRequest > 0) destinationKind = "" }
    fun share(playlist: MusicPlaylist) { sharing = playlist; operationError = null }
    LaunchedEffect(server, revision, refresh, cache.generation) {
        if (server == null) return@LaunchedEffect
        state.load(cache.generation, revision, force = refresh > 0) {
            withContext(Dispatchers.IO) {
                LibraryOverview(client.favorites(server).map { it.toLibrarySong(client, server) }, client.playlists(server))
            }
        }
    }
    val target = destinationKind.takeIf { it.isNotEmpty() }?.let { LibraryDestination(it, destinationTitle, destinationId) }
    BackHandler(target != null) { back() }
    if (target == null) {
        LibraryScreen(label, server?.username.orEmpty(), newest, overview, state.loading, state.error, http,
            onRetry = { refresh++; onRefreshShelf() }, onOpen = ::navigate, onAlbum = onAlbum,
            onPlayFavorites = { overview?.favorites?.takeIf { it.isNotEmpty() }?.let { onPlay(it, 0, true) } },
            onCreate = { name = ""; createError = null; create = true }, onServer = onServer,
            onDeletePlaylist = { deleting = it; operationError = null }, onSharePlaylist = ::share,
            cachedAt = state.cacheSavedAt)
    } else {
        key(target) {
            LibraryBrowser(target, server, client, http, revision, downloads, cache, onBack = ::back,
                onAlbum = onAlbum, onArtist = { navigate(LibraryDestination("artist", it.name, it.id)) },
                onPlay = onPlay, onMore = onMore, onPlaylistSongMore = { song, id, index ->
                    if (overview?.playlists?.any { it.id == id && it.owner == server?.username } == true)
                        onPlaylistSongMore(song, id, index) else onMore(song)
                },
                canSharePlaylist = overview?.playlists?.any { it.id == target.id && it.owner == server?.username } == true,
                onSharePlaylist = { id -> share(overview?.playlists?.firstOrNull { it.id == id }
                    ?: MusicPlaylist(id, target.title)) },
                onRandomPlay = onRandomPlay)
        }
    }
    if (create) AlertDialog(
        onDismissRequest = { if (!saving) create = false },
        title = { Text("新建歌单") },
        text = { Column {
            OutlinedTextField(name, { name = it }, label = { Text("歌单名称") }, singleLine = true, enabled = !saving)
            createError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank() && !saving && server != null, onClick = {
            scope.launch {
                saving = true
                createError = null
                try {
                    withContext(Dispatchers.IO) { client.createPlaylist(requireNotNull(server), name) }
                    create = false
                    refresh++
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) { createError = "创建失败，请重试" }
                finally { saving = false }
            }
        }) { Text(if (saving) "正在创建" else "创建") } },
        dismissButton = { TextButton(enabled = !saving, onClick = { create = false }) { Text("取消") } })
    deleting?.let { playlist -> AlertDialog(
        onDismissRequest = { if (!saving) deleting = null }, title = { Text("删除歌单") },
        text = { Column { Text("确定删除“${playlist.name}”？此操作会删除服务器上的歌单，不会删除歌曲文件。")
            operationError?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(enabled = !saving && server != null, onClick = {
            scope.launch {
                saving = true; operationError = null
                try {
                    withContext(Dispatchers.IO) { client.deletePlaylist(requireNotNull(server), playlist.id) }
                    deleting = null; refresh++
                    if (destinationKind == "playlist" && destinationId == playlist.id) destinationKind = ""
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) { operationError = "删除失败，请重试" }
                finally { saving = false }
            }
        }) { Text(if (saving) "正在删除" else "删除") } },
        dismissButton = { TextButton(enabled = !saving, onClick = { deleting = null }) { Text("取消") } }) }
    sharing?.let { playlist -> AlertDialog(
        onDismissRequest = { if (!saving) sharing = null }, title = { Text("分享歌单") },
        text = { Column { Text("将为“${playlist.name}”生成无需登录的公开链接；有效期由服务器设置。")
            operationError?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(enabled = !saving && server != null, onClick = {
            scope.launch {
                saving = true; operationError = null
                try {
                    val url = withContext(Dispatchers.IO) { client.sharePlaylist(requireNotNull(server), playlist.id) }
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, "${playlist.name}\n$url")
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "分享歌单"))
                    sharing = null
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) { operationError = "无法分享歌单，请检查服务器是否支持分享并重试" }
                finally { saving = false }
            }
        }) { Text(if (saving) "正在生成" else "生成并分享") } },
        dismissButton = { TextButton(enabled = !saving, onClick = { sharing = null }) { Text("取消") } }) }
}
