package com.lyq2010.leesmusic.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun UpdateHistory(releaseNotes: String? = null) {
    val context = LocalContext.current
    val history by produceState<String?>(null) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open("APP_CHANGELOG.md").bufferedReader().use { it.readText() } }
                .getOrDefault("暂时无法读取更新记录")
        }
    }
    Text("本次更新", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 28.dp, bottom = 12.dp))
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val text = releaseNotes ?: history
            if (text == null) CircularProgressIndicator(Modifier.size(24.dp))
            else text.lineSequence().filter { it.isNotBlank() && !it.startsWith("# ") }.forEach { line ->
                Text(line.removePrefix("## ").replaceFirst(Regex("^- "), "• "),
                    style = if (line.startsWith("## ")) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
