package com.lyq2010.leesmusic.ui.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lyq2010.leesmusic.ui.catalog.CoverImages
import kotlinx.coroutines.*

@Composable
internal fun CacheDialog(onDismiss: () -> Unit,
    readUsage: suspend () -> CoverImages.Usage,
    clearCache: suspend () -> CoverImages.Usage) {
    val context = LocalContext.current
    var usage by remember { mutableStateOf<CoverImages.Usage?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        try { usage = readUsage() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { message = "无法读取缓存大小，请重试" }
    }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(if (confirmed) "确认清理封面缓存？" else "封面缓存") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            usage?.let { Text("磁盘：${Formatter.formatFileSize(context, it.diskBytes)}\n内存：${Formatter.formatFileSize(context, it.memoryBytes)}") }
                ?: Text("正在统计缓存")
            Text("清理后将重新加载封面，不影响离线歌曲。")
            message?.let { Text(it) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        } },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            if (usage == null) { message = null; reload++ }
            else if (!confirmed) confirmed = true
            else scope.launch {
                busy = true; message = null
                try { usage = clearCache(); message = "封面缓存已清理"; confirmed = false }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { message = "部分缓存未能清理，请重试" }
                finally { busy = false }
            }
        }) { Text(if (usage == null) "重试" else if (confirmed) "确认清理" else "清理缓存") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { if (confirmed) confirmed = false else onDismiss() }) { Text(if (confirmed) "取消" else "关闭") } })
}
