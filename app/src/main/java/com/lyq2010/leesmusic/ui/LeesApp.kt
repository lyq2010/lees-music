package com.lyq2010.leesmusic.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lyq2010.leesmusic.data.api.ServerAddressResolver
import com.lyq2010.leesmusic.data.api.SubsonicClient
import com.lyq2010.leesmusic.data.api.Song
import com.lyq2010.leesmusic.data.api.SubsonicServer
import com.lyq2010.leesmusic.data.api.Album
import com.lyq2010.leesmusic.data.library.DailyMixStore
import com.lyq2010.leesmusic.data.library.dailyMixIsCurrent
import com.lyq2010.leesmusic.data.library.millisUntilNextMidnight
import com.lyq2010.leesmusic.data.library.todayStamp
import com.lyq2010.leesmusic.data.library.LibraryShelf
import com.lyq2010.leesmusic.data.library.LibraryShelfStore
import com.lyq2010.leesmusic.data.settings.ServerKind
import com.lyq2010.leesmusic.data.settings.ServerSettings
import com.lyq2010.leesmusic.data.settings.ServerSettingsStore
import com.lyq2010.leesmusic.ui.catalog.SampleCatalog
import com.lyq2010.leesmusic.ui.login.AddServerScreen
import com.lyq2010.leesmusic.ui.login.LoginScreen
import com.lyq2010.leesmusic.ui.lyrics.LyricsScreen
import com.lyq2010.leesmusic.playback.AppPlayer
import com.lyq2010.leesmusic.playback.currentSongTitle
import com.lyq2010.leesmusic.ui.player.PlayerScreen
import com.lyq2010.leesmusic.ui.playlist.DailyPlaylistScreen
import com.lyq2010.leesmusic.ui.shell.AppShell
import com.lyq2010.leesmusic.ui.welcome.WelcomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private object Routes {
    const val Home = "home"
    const val Player = "player"
    const val Lyrics = "lyrics"
    const val Login = "login"
    const val AddServer = "add-server"
    const val Welcome = "welcome"
    const val Daily = "daily"
}

@Composable
fun LeesApp() {
    val context = LocalContext.current
    val store = remember { ServerSettingsStore(context) }
    val mixStore = remember { DailyMixStore(context) }
    val shelfStore = remember { LibraryShelfStore(context) }
    val probeClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(1500, TimeUnit.MILLISECONDS)
            .readTimeout(1500, TimeUnit.MILLISECONDS)
            .callTimeout(2000, TimeUnit.MILLISECONDS)
            .build()
    }
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf<ServerSettings?>(null) }
    var pendingKind by remember { mutableStateOf(ServerKind.Navidrome) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val libraryHttp = remember { OkHttpClient() }
    var newest by remember { mutableStateOf<List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>>(emptyList()) }
    var recent by remember { mutableStateOf<List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>>(emptyList()) }
    var frequent by remember { mutableStateOf<List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>>(emptyList()) }
    var randomAlbums by remember { mutableStateOf<List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>>(emptyList()) }
    var libraryMessage by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var daily by remember { mutableStateOf<List<com.lyq2010.leesmusic.ui.catalog.LibrarySong>>(emptyList()) }
    var refreshingDaily by remember { mutableStateOf(false) }
    val appPlayer = remember { AppPlayer(context) }
    var nowPlayingTitle by remember { mutableStateOf("") }
    androidx.compose.runtime.DisposableEffect(appPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                nowPlayingTitle = appPlayer.player.currentSongTitle()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                nowPlayingTitle = appPlayer.player.currentSongTitle()
            }
        }
        appPlayer.player.addListener(listener)
        onDispose {
            appPlayer.player.removeListener(listener)
            appPlayer.release()
        }
    }

    LaunchedEffect(Unit) {
        settings = store.load()
        ready = true
    }
    LaunchedEffect(settings) {
        val current = settings
        if (current == null || current.kind != ServerKind.Navidrome) return@LaunchedEffect
        val cached = shelfStore.load()
        if (cached != null && !cached.isEmpty()) {
            val shown = mapShelf(current, cached)
            newest = shown.newest
            recent = shown.recent
            frequent = shown.frequent
            randomAlbums = shown.random
            libraryMessage = ""
        } else {
            libraryMessage = "正在读取曲库"
        }
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val client = SubsonicClient(libraryHttp)
                val server = SubsonicServer(current.url, current.username, current.password)
                LibraryShelf(
                    newest = client.albums(server, "newest"),
                    recent = client.albums(server, "recent"),
                    frequent = client.albums(server, "frequent"),
                    random = client.albums(server, "random"),
                )
            }
        }
        loaded.onSuccess { shelf ->
            shelfStore.save(shelf)
            val shown = mapShelf(current, shelf)
            newest = shown.newest
            recent = shown.recent
            frequent = shown.frequent
            randomAlbums = shown.random
            libraryMessage = ""
        }.onFailure {
            if (cached == null || cached.isEmpty()) libraryMessage = it.message ?: "曲库读取失败"
        }
        val saved = mixStore.load()
        val today = todayStamp()
        daily = if (saved != null && dailyMixIsCurrent(saved.day, today) && saved.songs.isNotEmpty()) {
            mapDaily(current, saved.songs)
        } else {
            loadDaily(current, mixStore, libraryHttp)
        }
        refreshingDaily = false
        while (true) {
            kotlinx.coroutines.delay(millisUntilNextMidnight())
            daily = loadDaily(current, mixStore, libraryHttp)
        }
    }

    fun playDaily(index: Int) {
        val current = settings ?: return
        if (daily.isEmpty()) return
        appPlayer.play(SubsonicServer(current.url, current.username, current.password), daily, index)
        nowPlayingTitle = daily[index].title
    }

    fun refreshDaily() {
        val current = settings ?: return
        scope.launch {
            refreshingDaily = true
            daily = loadDaily(current, mixStore, libraryHttp)
            refreshingDaily = false
        }
    }
    if (!ready) return

    fun connect(next: ServerSettings) {
        scope.launch {
            busy = true
            status = withContext(Dispatchers.IO) {
                runCatching {
                    store.save(next)
                    if (next.kind != ServerKind.Navidrome) {
                        return@runCatching "${next.kind.name} 的地址已保存。这个服务器的接口还没接上，现在还不能播放。"
                    }
                    val resolved = ServerAddressResolver { baseUrl, username, password ->
                        runCatching {
                            SubsonicClient(probeClient).ping(SubsonicServer(baseUrl, username, password))
                        }.isSuccess
                    }.resolve(next.url, next.username, next.password)
                    "已连接 $resolved"
                }.getOrElse { it.message ?: "连接失败" }
            }
            settings = next
            busy = false
            if (status.startsWith("已连接")) {
                nav.navigate(Routes.Home) {
                    popUpTo(Routes.Welcome) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = nav,
        startDestination = if (settings == null) Routes.Welcome else Routes.Home,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(Routes.Home) {
            AppShell(
                serverLabel = settings?.kind?.name ?: "未连接",
                newest = newest,
                recent = recent,
                frequent = frequent,
                randomAlbums = randomAlbums,
                searchResults = searchResults,
                libraryMessage = libraryMessage,
                http = libraryHttp,
                daily = daily,
                refreshingDaily = refreshingDaily,
                onOpenDaily = { nav.navigate(Routes.Daily) },
                onRefreshDaily = { refreshDaily() },
                nowPlayingTitle = nowPlayingTitle,
                onTogglePlay = {
                    if (appPlayer.player.mediaItemCount == 0) {
                        playDaily(0)
                    } else if (appPlayer.player.isPlaying) {
                        appPlayer.player.pause()
                    } else {
                        appPlayer.player.play()
                    }
                },
                onSearch = { query ->
                    val current = settings
                    if (current == null || query.isBlank() || current.kind != ServerKind.Navidrome) {
                        searchResults = emptyList()
                    } else scope.launch {
                        searchResults = withContext(Dispatchers.IO) {
                            runCatching {
                                val server = SubsonicServer(current.url, current.username, current.password)
                                val found = SubsonicClient(libraryHttp).search(server, query)
                                found.song.map { it.title + " · " + it.artist } + found.album.map { it.name + " · " + it.artist }
                            }.getOrElse { listOf(it.message ?: "搜索失败") }
                        }
                    }
                },
                onOpenServer = {
                    pendingKind = settings?.kind ?: ServerKind.Navidrome
                    nav.navigate(Routes.Login)
                },
            )
        }
        composable(Routes.Daily) {
            DailyPlaylistScreen(
                songs = daily,
                http = libraryHttp,
                onBack = { nav.popBackStack() },
                onPlay = { index -> playDaily(index) },
            )
        }
        composable(Routes.Player) {
            PlayerScreen(
                track = SampleCatalog.nowPlaying,
                onBack = { nav.popBackStack() },
                onOpenLyrics = { nav.navigate(Routes.Lyrics) },
            )
        }
        composable(Routes.Lyrics) {
            LyricsScreen(track = SampleCatalog.nowPlaying, onBack = { nav.popBackStack() })
        }
        composable(Routes.Welcome) {
            WelcomeScreen(onAddServer = { nav.navigate(Routes.AddServer) })
        }
        composable(Routes.AddServer) {
            AddServerScreen(
                onBack = { nav.popBackStack() },
                onPick = { kind ->
                    pendingKind = kind
                    nav.navigate(Routes.Login)
                },
            )
        }
        composable(Routes.Login) {
            LoginScreen(
                kind = pendingKind,
                initial = settings,
                status = status,
                busy = busy,
                onBack = { nav.popBackStack() },
                onSave = ::connect,
            )
        }
    }
}

private data class ShownShelf(
    val newest: List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>,
    val recent: List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>,
    val frequent: List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>,
    val random: List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>,
)

private fun mapShelf(settings: ServerSettings, shelf: LibraryShelf): ShownShelf {
    val client = SubsonicClient()
    val server = SubsonicServer(settings.url, settings.username, settings.password)
    fun mapAlbums(albums: List<Album>) = albums.map { album ->
        com.lyq2010.leesmusic.ui.catalog.LibraryAlbum(
            name = album.name,
            artist = album.artist,
            coverArtId = album.coverArt,
            coverUrl = album.coverArt?.let { client.coverArtUrl(server, it) },
        )
    }
    return ShownShelf(
        newest = mapAlbums(shelf.newest),
        recent = mapAlbums(shelf.recent),
        frequent = mapAlbums(shelf.frequent),
        random = mapAlbums(shelf.random),
    )
}

private suspend fun loadDaily(
    settings: ServerSettings,
    store: DailyMixStore,
    http: OkHttpClient,
): List<com.lyq2010.leesmusic.ui.catalog.LibrarySong> = withContext(Dispatchers.IO) {
    val client = SubsonicClient(http)
    val server = SubsonicServer(settings.url, settings.username, settings.password)
    val songs = client.randomSongs(server, 50)
    store.save(songs)
    mapDaily(settings, songs, client, server)
}

private fun mapDaily(
    settings: ServerSettings,
    songs: List<Song>,
): List<com.lyq2010.leesmusic.ui.catalog.LibrarySong> {
    val client = SubsonicClient()
    val server = SubsonicServer(settings.url, settings.username, settings.password)
    return mapDaily(settings, songs, client, server)
}

private fun mapDaily(
    settings: ServerSettings,
    songs: List<Song>,
    client: SubsonicClient,
    server: SubsonicServer,
): List<com.lyq2010.leesmusic.ui.catalog.LibrarySong> {
    return songs.map { song ->
        com.lyq2010.leesmusic.ui.catalog.LibrarySong(
            id = song.id,
            title = song.title,
            artist = song.artist,
            coverArtId = song.coverArt,
            coverUrl = song.coverArt?.let { client.coverArtUrl(server, it) },
        )
    }
}
