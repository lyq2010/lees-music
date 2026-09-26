package com.lyq2010.leesmusic.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.data.library.LibraryDownloads
import com.lyq2010.leesmusic.data.settings.PlaybackPreferences
import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import com.lyq2010.leesmusic.ui.library.*
import okhttp3.OkHttpClient

@Composable
fun SettingsFeature(server: SubsonicServer?, http: OkHttpClient, downloads: LibraryDownloads, cache: LibraryBrowseCache,
    onServer: () -> Unit, onPlay: (List<LibrarySong>, Int, Boolean) -> Unit, onMore: (LibrarySong) -> Unit) {
    val context = LocalContext.current
    val preferences = remember { PlaybackPreferences(context) }
    var downloadPage by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("") }
    val settingsScroll = androidx.compose.foundation.rememberScrollState()
    BackHandler(page.isNotEmpty() && !downloadPage) { page = "" }
    if (downloadPage) {
        BackHandler { downloadPage = false }
        LibraryBrowser(LibraryDestination("downloads", "下载"), server, SubsonicClient(http), http, 0, downloads, cache,
            { downloadPage = false }, {}, {}, onPlay, onMore)
    } else if (page == "storage") StorageScreen(downloads, { page = "" }, { downloadPage = true })
    else if (page == "personalization") PersonalizationScreen(preferences) { page = "" }
    else if (page == "equalizer") EqualizerScreen(preferences) { page = "" }
    else if (page == "update") UpdateScreen { page = "" }
    else if (page == "legal") LegalScreen { page = "" }
    else SettingsScreen(preferences, onServer, { downloadPage = true }, {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        runCatching { context.startActivity(intent) }.onFailure {
            android.widget.Toast.makeText(context, "无法打开系统设置，请在系统中查找 Lee's Music", android.widget.Toast.LENGTH_LONG).show()
        }
    }, onStorage = { page = "storage" }, onPersonalization = { page = "personalization" }, scrollState = settingsScroll,
        onEqualizer = { page = "equalizer" }, onUpdate = { page = "update" }, onLegal = { page = "legal" })
}
