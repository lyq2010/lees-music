package com.lyq2010.leesmusic.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import com.lyq2010.leesmusic.ui.catalog.toLibrarySong
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
    const val Servers = "servers"
    const val Login = "login"
    const val AddServer = "add-server"
    const val Welcome = "welcome"
    const val Daily = "daily"
    const val Album = "album"
}

@Composable
fun LeesApp() {
    val context = LocalContext.current
    val store = remember { ServerSettingsStore(context) }
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
    val cacheNamespace = settings?.let { com.lyq2010.leesmusic.data.library.serverCacheIdentity(it.url, it.username, it.kind.name) } ?: "unconfigured"
    val browseCache = remember(cacheNamespace, settings?.password) {
        com.lyq2010.leesmusic.ui.library.LibraryBrowseCache(settings?.takeIf { it.kind == ServerKind.Navidrome }?.let {
            com.lyq2010.leesmusic.data.library.LibraryDiskCache(
                java.io.File(context.filesDir, "libraries/$cacheNamespace/browse"), SubsonicServer(it.url, it.username, it.password))
        })
    }
    val downloads = remember(cacheNamespace) { com.lyq2010.leesmusic.data.library.LibraryDownloads(context, cacheNamespace) }
    var shelfRevision by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var libraryRevision by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var actionSong by remember(cacheNamespace) { mutableStateOf<com.lyq2010.leesmusic.ui.catalog.LibrarySong?>(null) }
    var queueOrderNotice by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    val mixStore = remember(cacheNamespace) { DailyMixStore(context, cacheNamespace) }
    val shelfStore = remember(cacheNamespace) { LibraryShelfStore(context, cacheNamespace) }
    var savedServers by remember { mutableStateOf<List<ServerSettings>>(emptyList()) }
    var editingServer by remember { mutableStateOf<ServerSettings?>(null) }
    var pendingKind by remember { mutableStateOf(ServerKind.Navidrome) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val libraryHttp = remember { OkHttpClient() }
    val lyricsRepository = remember { com.lyq2010.leesmusic.data.library.LyricsRepository(SubsonicClient(libraryHttp)) }
    var newest by remember { mutableStateOf<List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>>(emptyList()) }
    var recent by remember { mutableStateOf<List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>>(emptyList()) }
    var frequent by remember { mutableStateOf<List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>>(emptyList()) }
    var randomAlbums by remember { mutableStateOf<List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>>(emptyList()) }
    var libraryMessage by remember { mutableStateOf("") }
    val searchState = remember(cacheNamespace, settings?.password) { com.lyq2010.leesmusic.ui.search.SearchState() }
    var daily by remember { mutableStateOf<List<com.lyq2010.leesmusic.ui.catalog.LibrarySong>>(emptyList()) }
    var openedTitle by remember { mutableStateOf("") }
    var openedArtist by remember { mutableStateOf("") }
    var openedYear by remember { mutableStateOf(0) }
    var openedCoverId by remember { mutableStateOf<String?>(null) }
    var openedCoverUrl by remember { mutableStateOf<String?>(null) }
    var openedSongs by remember { mutableStateOf<List<com.lyq2010.leesmusic.ui.catalog.LibrarySong>>(emptyList()) }
    var refreshingDaily by remember { mutableStateOf(false) }
    val appPlayer = remember { AppPlayer(context) }
    var playRequest by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    androidx.compose.runtime.DisposableEffect(appPlayer) {
        onDispose { appPlayer.release() }
    }
    var nowPlayingTitle by remember { mutableStateOf("") }
    var currentSong by remember { mutableStateOf<com.lyq2010.leesmusic.ui.catalog.LibrarySong?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var volume by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    var shuffle by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    var playRequested by remember { mutableStateOf(false) }
    var canPrevious by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var sleepRemainingMs by remember { mutableStateOf(0L) }
    var canNext by remember { mutableStateOf(false) }
    var repeatMode by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(appPlayer) {
        while (appPlayer.playerOrNull() == null) {
            kotlinx.coroutines.delay(50)
        }
        val player = appPlayer.playerOrNull() ?: return@LaunchedEffect
        currentSong = appPlayer.currentSong()
        nowPlayingTitle = player.currentSongTitle()
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (com.lyq2010.leesmusic.playback.StreamRetry.isTransient(error)) return
                val blocked = com.lyq2010.leesmusic.playback.PlaybackNetworkPolicy(context).blocked()
                android.widget.Toast.makeText(context, if (blocked) "已关闭计费网络播放，请连接 Wi-Fi 或修改设置" else "播放失败，请检查连接及服务器音质设置后重试", android.widget.Toast.LENGTH_LONG).show()
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                nowPlayingTitle = player.currentSongTitle()
                currentSong = appPlayer.currentSong()
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.takeIf { it > 0 } ?: ((appPlayer.currentSong()?.duration ?: 0) * 1000L)
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                nowPlayingTitle = player.currentSongTitle()
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.takeIf { it > 0 } ?: ((appPlayer.currentSong()?.duration ?: 0) * 1000L)
            }
        }
        player.addListener(listener)
        try {
            while (true) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.takeIf { it > 0 } ?: ((appPlayer.currentSong()?.duration ?: 0) * 1000L)
                isPlaying = player.isPlaying
                volume = player.volume
                repeatMode = player.repeatMode
                shuffle = player.shuffleModeEnabled
                buffering = player.playbackState == androidx.media3.common.Player.STATE_BUFFERING
                playRequested = player.playWhenReady && player.playbackState != androidx.media3.common.Player.STATE_ENDED
                canPrevious = player.hasPreviousMediaItem() || player.currentPosition > 3000
                canNext = player.hasNextMediaItem()
                sleepRemainingMs = appPlayer.sleepRemainingMs()
                kotlinx.coroutines.delay(500)
            }
        } finally {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        settings = store.load()
        savedServers = store.savedServers()
        ready = true
    }
    LaunchedEffect(settings, shelfRevision) {
        val current = settings
        newest = emptyList()
        recent = emptyList()
        frequent = emptyList()
        randomAlbums = emptyList()
        daily = emptyList()
        openedSongs = emptyList()
        if (current == null || current.kind != ServerKind.Navidrome) return@LaunchedEffect
        val cached = withContext(Dispatchers.IO) { shelfStore.load() }
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
            withContext(Dispatchers.IO) { shelfStore.save(shelf) }
            val shown = mapShelf(current, shelf)
            newest = shown.newest
            recent = shown.recent
            frequent = shown.frequent
            randomAlbums = shown.random
            libraryMessage = ""
        }.onFailure {
            if (cached == null || cached.isEmpty()) libraryMessage = it.message ?: "曲库读取失败"
        }
        val saved = withContext(Dispatchers.IO) { mixStore.load() }
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

    fun playSongs(
        songs: List<com.lyq2010.leesmusic.ui.catalog.LibrarySong>,
        index: Int,
        start: Boolean = false,
        openPlayer: Boolean = true,
        shuffle: Boolean = false,
    ) {
        val current = settings ?: return
        if (songs.isEmpty()) return
        if (appPlayer.playerOrNull() == null) {
            android.widget.Toast.makeText(context, "播放器正在连接，请稍后重试", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val safeIndex = index.coerceIn(0, songs.lastIndex)
        val request = ++playRequest
        scope.launch {
            val local = withContext(Dispatchers.IO) {
                try { downloads.list().filter { it.playable }.associate { it.song.id to it.uri!! } }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { emptyMap() }
            }
            if (request != playRequest || settings != current) return@launch
            val playable = com.lyq2010.leesmusic.data.library.withDownloadedSongs(songs, local)
            appPlayer.play(SubsonicServer(current.url, current.username, current.password), playable, safeIndex, start, shuffle)
            currentSong = playable[safeIndex]
            nowPlayingTitle = playable[safeIndex].title
            isPlaying = start
            if (openPlayer) nav.navigate(Routes.Player)
        }
    }

    fun openAlbum(album: com.lyq2010.leesmusic.ui.catalog.LibraryAlbum) {
        val current = settings ?: return
        scope.launch {
            try {
                val client = SubsonicClient(libraryHttp)
                val server = SubsonicServer(current.url, current.username, current.password)
                val loaded = browseCache.album(album.id) { withContext(Dispatchers.IO) { client.album(server, album.id) } }
                if (settings != current) return@launch
                openedTitle = loaded.name.ifBlank { album.name }
                openedArtist = loaded.artist.ifBlank { album.artist }
                openedYear = loaded.year
                openedCoverId = loaded.coverArt ?: album.coverArtId
                openedCoverUrl = openedCoverId?.let { client.coverArtUrl(server, it) }
                openedSongs = loaded.song.map { song -> song.copy(
                    coverArt = song.coverArt ?: openedCoverId, albumId = song.albumId ?: album.id,
                    album = song.album.ifBlank { openedTitle }).toLibrarySong(client, server) }
                nav.navigate(Routes.Album)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: Exception) {
                if (settings == current) android.widget.Toast.makeText(context, "专辑读取失败，请重试", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun refreshDaily() {
        val current = settings ?: return
        scope.launch {
            refreshingDaily = true
            try {
                val songs = loadDaily(current, mixStore, libraryHttp)
                if (settings == current) daily = songs
            } finally {
                refreshingDaily = false
            }
        }
    }
    if (!ready) return

    fun connect(next: ServerSettings) {
        scope.launch {
            busy = true
            status = withContext(Dispatchers.IO) {
                runCatching {
                    if (next.kind != ServerKind.Navidrome) {
                        return@runCatching "${next.kind.name} 的接口尚未接入，暂时无法连接。"
                    }
                    val resolved = ServerAddressResolver { baseUrl, username, password ->
                        runCatching {
                            SubsonicClient(probeClient).ping(SubsonicServer(baseUrl, username, password))
                        }.isSuccess
                    }.resolve(next.url, next.username, next.password)
                    store.save(next)
                    "已连接 $resolved"
                }.getOrElse { it.message ?: "连接失败" }
            }
            if (!status.startsWith("已连接")) { busy = false; return@launch }
            if (settings?.let { com.lyq2010.leesmusic.data.library.serverCacheIdentity(it.url, it.username, it.kind.name) } !=
                com.lyq2010.leesmusic.data.library.serverCacheIdentity(next.url, next.username, next.kind.name)) {
                appPlayer.playerOrNull()?.apply { stop(); clearMediaItems() }
                currentSong = null
                nowPlayingTitle = ""
            }
            savedServers = store.savedServers()
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
        modifier = Modifier.fillMaxSize().then(Modifier.windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)),
    ) {
        composable(Routes.Home) {
            AppShell(
                serverLabel = settings?.kind?.name ?: "未连接",
                libraryContent = {
                    androidx.compose.runtime.key(cacheNamespace) {
                        com.lyq2010.leesmusic.ui.library.LibraryFeature(
                            server = settings?.takeIf { it.kind == ServerKind.Navidrome }?.let { SubsonicServer(it.url, it.username, it.password) },
                            label = settings?.kind?.name ?: "未连接", newest = newest, http = libraryHttp,
                            revision = libraryRevision, downloads = downloads, cache = browseCache, onAlbum = ::openAlbum, onRefreshShelf = { browseCache.invalidate(); shelfRevision++ },
                            onPlay = { songs, index, start -> playSongs(songs, index, start) },
                            onRandomPlay = { songs -> playSongs(songs, songs.indices.random(), start = true, shuffle = true) },
                            onMore = { actionSong = it }, onServer = { status = ""; nav.navigate(Routes.Servers) })
                    }
                },
                onOpenQueue = { showQueue = true },
                newest = newest,
                recent = recent,
                frequent = frequent,
                randomAlbums = randomAlbums,
                searchContent = {
                    com.lyq2010.leesmusic.ui.search.SearchScreen(searchState,
                        settings?.takeIf { it.kind == ServerKind.Navidrome }?.let { SubsonicServer(it.url, it.username, it.password) },
                        libraryHttp, browseCache, downloads, ::openAlbum,
                        { songs, index, start -> playSongs(songs, index, start) }, { actionSong = it },
                        { status = ""; nav.navigate(Routes.Servers) })
                },
                settingsContent = {
                    com.lyq2010.leesmusic.ui.settings.SettingsFeature(
                        settings?.takeIf { it.kind == ServerKind.Navidrome }?.let { SubsonicServer(it.url, it.username, it.password) },
                        libraryHttp, downloads, browseCache,
                        { status = ""; nav.navigate(Routes.Servers) },
                        { songs, index, start -> playSongs(songs, index, start) }, { actionSong = it })
                },
                libraryMessage = libraryMessage,
                http = libraryHttp,
                daily = daily,
                refreshingDaily = refreshingDaily,
                onOpenDaily = { nav.navigate(Routes.Daily) },
                onPlayDaily = { if (daily.isNotEmpty()) playSongs(daily, 0, start = true, openPlayer = true) },
                onRefreshDaily = { refreshDaily() },
                showPlayerBar = currentSong != null,
                nowPlayingTitle = nowPlayingTitle,
                nowPlayingArtist = currentSong?.artist.orEmpty(),
                nowPlayingCoverId = currentSong?.coverArtId,
                nowPlayingCoverUrl = currentSong?.coverUrl,
                isPlaying = isPlaying,
                onOpenPlayer = { nav.navigate(Routes.Player) },
                onOpenAlbum = { album -> openAlbum(album) },
                onTogglePlay = {
                    val player = appPlayer.playerOrNull()
                    if (player == null || player.mediaItemCount == 0) {
                        playSongs(daily, 0)
                    } else com.lyq2010.leesmusic.playback.togglePlayback(player)
                },
                onOpenServer = {
                    status = ""
                    nav.navigate(Routes.Servers)
                },
                canPrevious = appPlayer.playerOrNull()?.hasPreviousMediaItem() == true,
                canNext = canNext,
                onPrevious = { appPlayer.playerOrNull()?.seekToPreviousMediaItem() },
                onNext = { appPlayer.playerOrNull()?.seekToNextMediaItem() },
            )
        }
        composable(Routes.Daily) {
            DailyPlaylistScreen(
                songs = daily,
                onMore = { actionSong = it },
                http = libraryHttp,
                onBack = { nav.popBackStack() },
                onPlay = { index -> playSongs(daily, index) },
                onPlayInOrder = { playSongs(daily, 0, start = true, openPlayer = false) },
                onShuffle = { playSongs(daily, daily.indices.randomOrNull() ?: 0, start = true, openPlayer = false, shuffle = true) },
            )
        }
        composable(Routes.Album) {
            com.lyq2010.leesmusic.ui.album.AlbumScreen(
                title = openedTitle,
                artist = openedArtist,
                year = openedYear,
                coverArtId = openedCoverId,
                coverUrl = openedCoverUrl,
                songs = openedSongs,
                onMore = { actionSong = it },
                http = libraryHttp,
                onBack = { nav.popBackStack() },
                onPlayInOrder = { playSongs(openedSongs, 0, start = true, openPlayer = false) },
                onShuffle = { playSongs(openedSongs, openedSongs.indices.randomOrNull() ?: 0, start = true, openPlayer = false, shuffle = true) },
                onPlay = { index -> playSongs(openedSongs, index) },
            )
        }
        composable(Routes.Player) {
            PlayerScreen(
                song = currentSong,
                http = libraryHttp,
                isPlaying = playRequested,
                positionMs = positionMs,
                durationMs = durationMs,
                volume = volume,
                repeatMode = repeatMode,
                onRepeat = { appPlayer.playerOrNull()?.let { it.repeatMode = com.lyq2010.leesmusic.playback.nextRepeatMode(it.repeatMode) } },
                onSeek = { appPlayer.playerOrNull()?.seekTo(it) },
                onPlayPause = {
                    val player = appPlayer.playerOrNull()
                    if (player != null) {
                        com.lyq2010.leesmusic.playback.togglePlayback(player)
                    }
                },
                onPrevious = { appPlayer.playerOrNull()?.seekToPrevious() },
                onNext = { appPlayer.playerOrNull()?.seekToNextMediaItem() },
                onVolume = { level -> appPlayer.playerOrNull()?.let { it.volume = level } },
                onBack = { nav.popBackStack() },
                onOpenLyrics = { nav.navigate(Routes.Lyrics) },
                shuffle = shuffle, buffering = buffering, canPrevious = canPrevious, canNext = canNext,
                onShuffle = { appPlayer.playerOrNull()?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } },
                onQueue = { showQueue = true }, onMore = { actionSong = currentSong },
                sleepRemainingMs = sleepRemainingMs, onSleepTimer = { showSleepTimer = true },
            )
        }
        composable(Routes.Lyrics) {
            LyricsScreen(
                song = currentSong,
                server = settings?.takeIf { it.kind == ServerKind.Navidrome }?.let {
                    SubsonicServer(it.url, it.username, it.password)
                },
                repository = lyricsRepository,
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = playRequested,
                volume = volume,
                repeatMode = repeatMode,
                onSeek = { appPlayer.playerOrNull()?.seekTo(it) },
                onPlayPause = { appPlayer.playerOrNull()?.let { com.lyq2010.leesmusic.playback.togglePlayback(it) } },
                onVolume = { level -> appPlayer.playerOrNull()?.let { it.volume = level } },
                onPrevious = { appPlayer.playerOrNull()?.seekToPrevious() },
                onNext = { appPlayer.playerOrNull()?.seekToNextMediaItem() },
                onRepeat = { appPlayer.playerOrNull()?.let { it.repeatMode = com.lyq2010.leesmusic.playback.nextRepeatMode(it.repeatMode) } },
                onBack = { nav.popBackStack() },
                shuffle = shuffle, buffering = buffering, canPrevious = canPrevious, canNext = canNext,
                onShuffle = { appPlayer.playerOrNull()?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } },
                onQueue = { showQueue = true }, onMore = { actionSong = currentSong },
                sleepRemainingMs = sleepRemainingMs, onSleepTimer = { showSleepTimer = true },
            )
        }
        composable(Routes.Welcome) {
            WelcomeScreen(onAddServer = { nav.navigate(Routes.AddServer) })
        }
        composable(Routes.AddServer) {
            AddServerScreen(
                onBack = { nav.popBackStack() },
                onPick = { kind ->
                    editingServer = null
                    pendingKind = kind
                    nav.navigate(Routes.Login)
                },
            )
        }
        composable(Routes.Servers) {
            com.lyq2010.leesmusic.ui.login.SavedServersScreen(savedServers, settings, busy, status,
                { nav.popBackStack() }, { editingServer = null; status = ""; nav.navigate(Routes.AddServer) },
                { editingServer = it; pendingKind = it.kind; status = ""; nav.navigate(Routes.Login) }, ::connect)
        }
        composable(Routes.Login) {
            LoginScreen(
                kind = pendingKind,
                initial = editingServer,
                status = status,
                busy = busy,
                onBack = { nav.popBackStack() },
                onSave = ::connect,
            )
        }
    }
    actionSong?.let { selected ->
        settings?.takeIf { it.kind == ServerKind.Navidrome }?.let { current ->
            val server = SubsonicServer(current.url, current.username, current.password)
            androidx.compose.runtime.key(cacheNamespace, selected.id) {
                com.lyq2010.leesmusic.ui.catalog.SongActions(selected, server, SubsonicClient(libraryHttp), downloads,
                    onDismiss = { actionSong = null }, onPlay = { playSongs(listOf(selected), 0, start = true) },
                    onEnqueue = { playable, next ->
                        val shuffled = appPlayer.playerOrNull()?.shuffleModeEnabled == true
                        val added = appPlayer.enqueue(server, playable, next)
                        queueOrderNotice = added && shuffled
                        added
                    },
                    onChanged = { libraryRevision++ },
                    http = libraryHttp,
                    onAlbum = selected.albumId?.takeIf { it.isNotBlank() }?.let { id -> {
                        openAlbum(com.lyq2010.leesmusic.ui.catalog.LibraryAlbum(id, selected.album, selected.artist, selected.coverArtId, selected.coverUrl))
                    } },
                    onMessage = {
                        val message = it + if (queueOrderNotice) "；已固定当前顺序并关闭随机播放" else ""
                        queueOrderNotice = false
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    })
            }
        }
    }
    if (showSleepTimer) com.lyq2010.leesmusic.ui.player.SleepTimerSheet(sleepRemainingMs, appPlayer::setSleepTimer) { showSleepTimer = false }
    if (showQueue) com.lyq2010.leesmusic.ui.player.QueueSheet(appPlayer.playerOrNull(), libraryHttp) { showQueue = false }

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
            id = album.id,
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
    val songs = com.lyq2010.leesmusic.data.library.refreshDailySongs(
        fetch = { client.randomSongs(server, 50) },
        save = { store.save(it) },
        cached = { store.load()?.songs.orEmpty() },
    )
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
    return songs.map { it.toLibrarySong(client, server) }
}
