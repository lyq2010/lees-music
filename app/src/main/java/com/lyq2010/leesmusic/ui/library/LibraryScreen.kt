package com.lyq2010.leesmusic.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.shell.ShellCard
import com.lyq2010.leesmusic.ui.shell.ShellMuted
import com.lyq2010.leesmusic.ui.shell.ShellText

@Composable
fun LibraryScreen(serverLabel: String) {
    var playlistTab by rememberSaveable { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("音乐库", color = ShellText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text(serverLabel, color = ShellMuted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LibraryTile(Icons.Filled.MusicNote, "歌曲", Modifier.weight(1f))
            LibraryTile(Icons.Filled.Album, "专辑", Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LibraryTile(Icons.Filled.Person, "艺术家", Modifier.weight(1f))
            LibraryTile(Icons.Filled.Download, "下载", Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("我的歌单", "共享歌单").forEachIndexed { index, label ->
                Text(
                    label,
                    color = if (index == playlistTab) ShellText else ShellMuted,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .clickable { playlistTab = index },
                )
            }
        }
        Text("暂无歌单", color = ShellMuted, modifier = Modifier.padding(top = 36.dp).align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun LibraryTile(icon: ImageVector, label: String, modifier: Modifier) {
    Surface(modifier, color = ShellCard, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = ShellText)
            Text(label, color = ShellText, modifier = Modifier.padding(start = 12.dp), fontSize = 16.sp)
        }
    }
}
