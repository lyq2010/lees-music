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
import com.lyq2010.leesmusic.data.api.SubsonicServer
import com.lyq2010.leesmusic.data.settings.ServerKind
import com.lyq2010.leesmusic.data.settings.ServerSettings
import com.lyq2010.leesmusic.data.settings.ServerSettingsStore
import com.lyq2010.leesmusic.ui.catalog.SampleCatalog
import com.lyq2010.leesmusic.ui.home.HomeScreen
import com.lyq2010.leesmusic.ui.login.LoginScreen
import com.lyq2010.leesmusic.ui.lyrics.LyricsScreen
import com.lyq2010.leesmusic.ui.player.PlayerScreen
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
    const val Welcome = "welcome"
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
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val track = SampleCatalog.nowPlaying

    LaunchedEffect(Unit) {
        settings = store.load()
        ready = true
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
                    }.resolve(next.lanUrl, next.wanUrl, next.username, next.password)
                    if (resolved.usingLan) "正在使用内网 ${resolved.baseUrl}" else "内网没有响应，正在使用外网 ${resolved.baseUrl}"
                }.getOrElse { it.message ?: "连接失败" }
            }
            settings = next
            busy = false
        }
    }

    NavHost(
        navController = nav,
        startDestination = if (settings == null) Routes.Welcome else Routes.Home,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(Routes.Home) {
            HomeScreen(
                nowPlaying = track,
                onOpenPlayer = { nav.navigate(Routes.Player) },
                onOpenSettings = { nav.navigate(Routes.Login) },
            )
        }
        composable(Routes.Player) {
            PlayerScreen(
                track = track,
                onBack = { nav.popBackStack() },
                onOpenLyrics = { nav.navigate(Routes.Lyrics) },
            )
        }
        composable(Routes.Lyrics) {
            LyricsScreen(track = track, onBack = { nav.popBackStack() })
        }
        composable(Routes.Welcome) {
            WelcomeScreen(onAddServer = { nav.navigate(Routes.Login) })
        }
        composable(Routes.Login) {
            LoginScreen(
                initial = settings,
                status = status,
                busy = busy,
                onSave = ::connect,
            )
        }
    }
}
