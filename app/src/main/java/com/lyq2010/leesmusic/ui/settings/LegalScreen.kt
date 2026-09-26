package com.lyq2010.leesmusic.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LegalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var file by remember { mutableStateOf("LICENSE") }
    var blocks by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(file) {
        blocks = withContext(Dispatchers.IO) {
            context.assets.open(file).bufferedReader().use { it.readText().split("\n\n") }
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SettingsHeading("开源许可", onBack)
        Row {
            TextButton(onClick = { file = "LICENSE" }) { Text("GPL-3.0") }
            TextButton(onClick = { file = "THIRD_PARTY_NOTICES.md" }) { Text("第三方声明") }
        }
        Text("Copyright © 2026 lyq2010", style = MaterialTheme.typography.bodySmall)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(blocks) { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
