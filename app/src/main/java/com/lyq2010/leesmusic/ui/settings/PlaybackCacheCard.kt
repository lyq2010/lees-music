package com.lyq2010.leesmusic.ui.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lyq2010.leesmusic.data.settings.PlaybackPreferences
import com.lyq2010.leesmusic.playback.PlaybackCache
import com.lyq2010.leesmusic.ui.shell.*
import kotlinx.coroutines.*

@Composable
internal fun PlaybackCacheCard(bytes: Long?, onChanged: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PlaybackPreferences(context) }
    var limit by remember { mutableIntStateOf(prefs.playbackCacheMb) }
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Card(colors = CardDefaults.cardColors(containerColor = ShellCard)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("播放缓存", style = MaterialTheme.typography.titleMedium)
            Text(bytes?.let { Formatter.formatFileSize(context, it) } ?: "正在统计", color = ShellMuted)
            Text("达到上限时自动清理旧缓存。", color = ShellMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(512 to "512 MB", 1024 to "1 GB", 2048 to "2 GB").forEach { (value, label) ->
                    FilterChip(selected = limit == value, enabled = !busy, onClick = {
                        scope.launch {
                            busy = true; message = null
                            try {
                                withContext(Dispatchers.IO) { PlaybackCache.resize(context, value) }
                                prefs.playbackCacheMb = value; limit = value; onChanged()
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { message = "设置失败，请重试" }
                            finally { busy = false }
                        }
                    }, label = { Text(label) })
                }
            }
            OutlinedButton(enabled = !busy, onClick = { confirm = true }) { Text("清理播放缓存") }
            message?.let { Text(it, color = ShellMuted) }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { if (!busy) confirm = false }, title = { Text("清理播放缓存？") },
        text = { Text("正在使用的缓存将保留，不影响离线歌曲。") },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            scope.launch {
                busy = true
                try {
                    val remaining = withContext(Dispatchers.IO) { PlaybackCache.clearUnused(context) }
                    message = if (remaining > 0) "已清理，正在使用的缓存已保留" else "播放缓存已清理"
                    confirm = false; onChanged()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { message = "清理失败，请重试"; confirm = false }
                finally { busy = false }
            }
        }) { Text(if (busy) "正在清理" else "清理") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { confirm = false }) { Text("取消") } })
}
