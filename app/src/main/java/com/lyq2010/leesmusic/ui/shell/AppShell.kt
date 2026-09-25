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
    nowPlaying: Track,
    serverLabel: String,
    onOpenPlayer: () -> Unit,
    onOpenServer: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(ShellBg)) {
        Column(Modifier.weight(1f)) {
            when (tab) {
                0 -> LibraryScreen(serverLabel)
                1 -> DiscoverScreen(onOpenPlayer)
                2 -> SearchScreen()
                else -> SettingsScreen(onOpenServer)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPlayer)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumCover(nowPlaying.cover, Modifier.size(42.dp).clip(RoundedCornerShape(6.dp)))
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(nowPlaying.title, color = ShellText, fontSize = 15.sp)
                Text("${nowPlaying.artist} · ${nowPlaying.album}", color = ShellMuted, fontSize = 12.sp, maxLines = 1)
            }
            Icon(Icons.Filled.MusicNote, contentDescription = "歌词", tint = ShellMuted)
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
