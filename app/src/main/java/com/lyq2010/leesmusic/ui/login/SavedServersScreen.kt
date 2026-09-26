package com.lyq2010.leesmusic.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyq2010.leesmusic.data.settings.ServerSettings
import com.lyq2010.leesmusic.ui.shell.*

@Composable
fun SavedServersScreen(servers: List<ServerSettings>, current: ServerSettings?, busy: Boolean, status: String,
    onBack: () -> Unit, onAdd: () -> Unit, onEdit: (ServerSettings) -> Unit, onConnect: (ServerSettings) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                Text("服务器", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(enabled = !busy, onClick = onAdd) { Text("添加") }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (status.isNotBlank()) Text(status, color = ShellMuted)
        }
        if (servers.isEmpty()) item { Text("还没有保存的服务器", color = ShellMuted) }
        items(servers, key = { "${it.kind}:${it.url}:${it.username}" }) { server ->
            Card(colors = CardDefaults.cardColors(containerColor = ShellCard)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Dns, null, tint = ShellAccent)
                        Text(server.kind.name, style = MaterialTheme.typography.titleMedium)
                        if (server == current) Text("当前使用", color = ShellAccent)
                    }
                    Text(android.net.Uri.parse(server.url).let { "${it.scheme}://${it.host.orEmpty()}${if (it.port > 0) ":${it.port}" else ""}${it.path.orEmpty()}" }, color = ShellMuted)
                    Text(server.username, color = ShellMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(enabled = !busy, onClick = { onEdit(server) }) { Text("编辑") }
                        TextButton(enabled = !busy, onClick = { onConnect(server) }) { Text(if (server == current) "重新连接" else "连接") }
                    }
                }
            }
        }
    }
}
