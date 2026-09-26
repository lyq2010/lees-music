package com.lyq2010.leesmusic.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import com.lyq2010.leesmusic.ui.player.trackSwipe

private val tabs = listOf("首页", "发现", "搜索", "设置")

@Composable
fun AppShell(
    serverLabel: String,
    libraryContent: @Composable () -> Unit,
    onOpenQueue: () -> Unit,
    newest: List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>,
    recent: List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>,
    frequent: List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>,
    randomAlbums: List<com.lyq2010.leesmusic.ui.catalog.LibraryAlbum>,
    searchContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    libraryMessage: String,
    http: okhttp3.OkHttpClient,
    daily: List<com.lyq2010.leesmusic.ui.catalog.LibrarySong>,
    refreshingDaily: Boolean,
    onOpenDaily: () -> Unit,
    onPlayDaily: () -> Unit,
    onRefreshDaily: () -> Unit,
    showPlayerBar: Boolean,
    nowPlayingTitle: String,
    nowPlayingArtist: String,
    nowPlayingCoverId: String?,
    nowPlayingCoverUrl: String?,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenAlbum: (com.lyq2010.leesmusic.ui.catalog.LibraryAlbum) -> Unit,
    onOpenServer: () -> Unit,
    canPrevious: Boolean = false,
    canNext: Boolean = false,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    ShellTheme {
    Column(Modifier.fillMaxSize().background(ShellBg).safeDrawingPadding().imePadding()) {
        Column(Modifier.weight(1f)) {
            when (tab) {
                0 -> libraryContent()
                1 -> DiscoverScreen(daily, refreshingDaily, onOpenDaily, onPlayDaily, onRefreshDaily, newest, recent, frequent, randomAlbums, http, libraryMessage, onOpenAlbum)
                2 -> searchContent()
                else -> settingsContent()
            }
        }
        if (showPlayerBar) Surface(color = ShellCard, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .trackSwipe(canPrevious, canNext, onPrevious, onNext)
                    .clickable(onClick = onOpenPlayer)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.lyq2010.leesmusic.ui.catalog.RemoteCover(
                    nowPlayingCoverId,
                    nowPlayingCoverUrl,
                    Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                    http,
                )
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(nowPlayingTitle.ifBlank { "还没有在播放" }, color = ShellText, fontSize = 15.sp, maxLines = 1)
                    if (nowPlayingArtist.isNotBlank()) {
                        Text(nowPlayingArtist, color = ShellMuted, fontSize = 12.sp, maxLines = 1)
                    }
                }
                IconButton(onClick = onOpenQueue) { Icon(Icons.Default.QueueMusic, "播放队列", tint = ShellText) }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = ShellText,
                    )
                }
            }
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
}
