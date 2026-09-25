package com.lyq2010.leesmusic.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.shell.ShellAccent
import com.lyq2010.leesmusic.ui.shell.ShellAccentText
import com.lyq2010.leesmusic.ui.shell.ShellCard
import com.lyq2010.leesmusic.ui.shell.ShellMuted
import com.lyq2010.leesmusic.ui.shell.ShellText

private val kinds = listOf("歌曲", "专辑", "艺术家")

@Composable
fun SearchScreen(results: List<String>, onQuery: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) { onQuery(query) }
    var kind by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("搜索", color = ShellText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索歌曲、专辑、艺术家") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            shape = RoundedCornerShape(28.dp),
        )
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            kinds.forEachIndexed { index, label ->
                Surface(
                    onClick = { kind = index },
                    color = if (index == kind) ShellAccent else ShellCard,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        label,
                        color = if (index == kind) ShellAccentText else ShellText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
        results.forEach { line ->
            Text(line, color = ShellText, modifier = Modifier.padding(top = 10.dp))
        }
        if (query.isBlank()) {
            Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = ShellMuted, modifier = Modifier.padding(bottom = 12.dp))
                Text("搜索歌曲、专辑、艺术家", color = ShellMuted)
            }
        } else {
            Text("还没有连上曲库，搜不到「$query」", color = ShellMuted, modifier = Modifier.padding(top = 24.dp))
        }
    }
}
