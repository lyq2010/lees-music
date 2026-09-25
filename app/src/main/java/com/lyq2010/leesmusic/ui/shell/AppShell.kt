package com.lyq2010.leesmusic.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.catalog.AlbumCover
import com.lyq2010.leesmusic.ui.catalog.Track
import com.lyq2010.leesmusic.ui.discover.DiscoverScreen
import com.lyq2010.leesmusic.ui.library.LibraryScreen
import com.lyq2010.leesmusic.ui.search.SearchScreen
import com.lyq2010.leesmusic.ui.settings.SettingsScreen

private val tabs = listOf("首页", "发现", "搜索", "设置")

@Composable
fun AppShell(
    serverLabel: String,
    newest: List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>,
    recent: List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>,
    frequent: List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>,
    randomAlbums: List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>,
    searchResults: List<String>,
    libraryMessage: String,
    http: okhttp3.OkHttpClient,
    daily: List<com.lyq2010.leesmusic.ui.catalog.LibrarySong>,
    refreshingDaily: Boolean,
    onOpenDaily: () -> Unit,
    onRefreshDaily: () -> Unit,
    onSearch: (String) -> Unit,
    nowPlayingTitle: String,
    onTogglePlay: () -> Unit,
    onOpenServer: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(ShellBg)) {
        Column(Modifier.weight(1f)) {
            when (tab) {
                0 -> LibraryScreen(serverLabel, newest, libraryMessage, http)
                1 -> DiscoverScreen(daily, refreshingDaily, onOpenDaily, onRefreshDaily, newest, recent, frequent, randomAlbums, http, libraryMessage)
                2 -> SearchScreen(searchResults, onSearch)
                else -> SettingsScreen(onOpenServer)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onTogglePlay)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(nowPlayingTitle.ifBlank { "还没有在播放" }, color = ShellText, fontSize = 14.sp)
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            tabs.forEachIndexed { index, label ->
                val selected = index == tab
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { tab = index }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        when (index) {
                            0 -> Icons.Filled.Home
                            1 -> Icons.Outlined.Explore
                            2 -> Icons.Filled.Search
                            else -> Icons.Filled.Settings
                        },
                        contentDescription = label,
                        tint = if (selected) ShellAccent else ShellMuted,
                    )
                    Text(label, color = if (selected) ShellAccent else ShellMuted, fontSize = 11.sp)
                }
            }
        }
    }
}
