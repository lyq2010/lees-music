package com.lyq2010.leesmusic.ui.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lyq2010.leesmusic.data.library.LibraryDownloads
import com.lyq2010.leesmusic.ui.catalog.CoverImages
import com.lyq2010.leesmusic.ui.shell.*
import kotlinx.coroutines.*

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun StorageScreen(downloads: LibraryDownloads, onBack: () -> Unit, onDownloads: () -> Unit) {
    val context = LocalContext.current
    var usage by remember { mutableStateOf<CoverImages.Usage?>(null) }
    var downloaded by remember { mutableStateOf<Pair<Int, Long>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var clear by remember { mutableStateOf(false) }
    var playbackBytes by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(refresh) {
        failed = false
        try {
            usage = withContext(Dispatchers.IO) { CoverImages.usage(context) }
            playbackBytes = withContext(Dispatchers.IO) { com.lyq2010.leesmusic.playback.PlaybackCache.get(context).cacheSpace }
            downloaded = withContext(Dispatchers.IO) { downloads.list().let { items -> items.count { it.playable } to items.sumOf { it.bytes.coerceAtLeast(0) } } }
        } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { failed = true }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsHeading("存储空间管理", onBack)
        PlaybackCacheCard(playbackBytes) { refresh++ }
        Card(colors = CardDefaults.cardColors(containerColor = ShellCard)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("封面缓存", style = MaterialTheme.typography.titleMedium)
                Text(usage?.let { "磁盘 ${Formatter.formatFileSize(context, it.diskBytes)} · 内存缓存 ${Formatter.formatFileSize(context, it.memoryBytes)}" } ?: "正在统计", color = ShellMuted)
                Text("清理后将按需重新加载。", color = ShellMuted)
                OutlinedButton(onClick = { clear = true }) { Text("清理封面缓存") }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = ShellCard)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("离线歌曲", style = MaterialTheme.typography.titleMedium)
                Text(downloaded?.let { "${it.first} 首可播放 · 已下载 ${Formatter.formatFileSize(context, it.second)}" } ?: "正在统计", color = ShellMuted)
                Text("当前账号的离线歌曲", color = ShellMuted)
                TextButton(onClick = onDownloads) { Text("管理下载") }
            }
        }
        if (failed) Text("部分数据读取失败，请重试", color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { refresh++ }) { Text("重新统计") }
    }
    if (clear) CacheDialog({ clear = false; refresh++ },
        { withContext(Dispatchers.IO) { CoverImages.usage(context) } },
        { withContext(Dispatchers.IO) { CoverImages.clear(context) } })
}
