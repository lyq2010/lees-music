package com.lyq2010.leesmusic.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.shell.ShellMuted
import com.lyq2010.leesmusic.ui.shell.ShellText

@Composable
fun SettingsScreen(onOpenServer: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("设置", color = ShellText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Section("播放")
        ToggleRow("启动后自动播放")
        ToggleRow("音量淡入淡出")
        ToggleRow("与其他应用同时播放")
        LinkRow("在线播放音质", "尚未设置")
        Section("网络")
        ToggleRow("允许移动网络播放", initial = true)
        LinkRow("服务器", "Navidrome、Emby、Plex", onClick = onOpenServer)
        Section("存储")
        LinkRow("下载", "第一版还没有")
        Section("更多")
        LinkRow("关于", "Lee's Music")
    }
}

@Composable
private fun Section(title: String) {
    Text(title, color = ShellMuted, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
}

@Composable
private fun ToggleRow(label: String, initial: Boolean = false) {
    var on by rememberSaveable { mutableStateOf(initial) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ShellText, modifier = Modifier.weight(1f))
        Switch(checked = on, onCheckedChange = { on = it })
    }
}

@Composable
private fun LinkRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = ShellText, modifier = Modifier.weight(1f))
        Text(value, color = ShellMuted, fontSize = 13.sp)
    }
}
