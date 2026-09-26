package com.lyq2010.leesmusic.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lyq2010.leesmusic.BuildConfig
import com.lyq2010.leesmusic.update.*
import kotlinx.coroutines.*
import java.io.File

@Composable
fun UpdateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { UpdateClient(listOf(BuildConfig.COS_UPDATE_BASE, "https://plt-releases.leenbsl.workers.dev")) }
    var available by remember { mutableStateOf<AvailableUpdate?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloaded by remember { mutableStateOf(false) }
    val file = remember { File(context.cacheDir, "updates/update.apk") }
    fun check() {
        scope.launch {
            busy = true; message = "正在检查更新"; available = null; downloaded = false
            try {
                available = withContext(Dispatchers.IO) { client.check(BuildConfig.VERSION_CODE.toLong(), android.os.Build.VERSION.SDK_INT) }
                message = if (available == null) "已是最新版本" else "发现新版本 ${available!!.manifest.version}"
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) { message = error.message ?: "检查更新失败，请重试"
            } finally { busy = false }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Default.ArrowBack, "返回") }
            Text("应用更新", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(8.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Lee’s Music", style = MaterialTheme.typography.titleLarge)
        Text("当前版本 ${BuildConfig.VERSION_NAME}")
        if (BuildConfig.DEBUG) Text("开发版本无法覆盖安装正式签名版本。", modifier = Modifier.padding(top = 12.dp))
        Text(message, modifier = Modifier.padding(vertical = 20.dp))
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        available?.let { update ->
            Text(update.manifest.notes)
            Text("安装包 %.1f MB".format(update.manifest.size / 1024.0 / 1024), modifier = Modifier.padding(vertical = 12.dp))
            if (busy && progress > 0) Text("已下载 ${(progress * 100).toInt()}%")
            Button(enabled = !busy && !BuildConfig.DEBUG, onClick = {
                if (downloaded) {
                    runCatching { UpdateInstaller.install(context, file, update.manifest) }
                        .onSuccess { if (!it) message = "允许安装此来源的应用后，返回并点击安装" }
                        .onFailure { message = it.message ?: "无法打开安装程序" }
                } else scope.launch {
                    busy = true; progress = 0f; message = "正在下载更新"
                    try {
                        val job = currentCoroutineContext()
                        withContext(Dispatchers.IO) {
                            file.parentFile!!.mkdirs()
                            client.download(update, file) { count, size ->
                                job.ensureActive()
                                progress = count.toFloat() / size
                            }
                            UpdateInstaller.verify(context, file, update.manifest)
                        }
                        downloaded = true; message = "下载完成，请点击安装"
                    } catch (cancelled: CancellationException) { throw cancelled
                    } catch (error: Exception) { message = error.message ?: "下载失败，请重试"
                    } finally { busy = false }
                }
            }) { Text(if (downloaded) "安装更新" else "下载更新") }
        }
        OutlinedButton(enabled = !busy, onClick = { check() }) { Text("检查更新") }
    }
}
