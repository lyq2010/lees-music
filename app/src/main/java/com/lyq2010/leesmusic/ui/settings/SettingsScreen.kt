package com.lyq2010.leesmusic.ui.settings

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.data.settings.*
import com.lyq2010.leesmusic.ui.shell.*

@Composable
fun SettingsScreen(preferences: PlaybackPreferences, onOpenServer: () -> Unit, onDownloads: () -> Unit, onSystemSettings: () -> Unit,
    onStorage: () -> Unit = {}, onPersonalization: () -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(), onEqualizer: () -> Unit = {}, onUpdate: () -> Unit = {}, onLegal: () -> Unit = {}) {
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        preferences.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val values = remember(revision) { listOf(preferences.mixAudio, preferences.pauseOnDisconnect, preferences.allowMetered, preferences.downloadUnmetered) }
    val quality = remember(revision) { preferences.quality }
    val equalizer = remember(revision) { preferences.equalizer }
    val fade = remember(revision) { preferences.fadeAudio }
    val notificationLyrics = remember(revision) { preferences.notificationLyrics }
    var dialog by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp)) {
        Text("设置", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Section("播放") {
            LinkRow(Icons.Default.Equalizer, "均衡器", equalizer.label, onEqualizer)
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            ToggleRow(Icons.Default.GraphicEq, "音量淡入淡出", "歌曲间交叉淡化 · 3 秒", fade) { preferences.fadeAudio = it }
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            ToggleRow(Icons.Default.QueueMusic, "与其他应用同时播放", "", values[0]) { preferences.mixAudio = it }
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            ToggleRow(Icons.Default.Headphones, "耳机断开时暂停", "", values[1]) { preferences.pauseOnDisconnect = it }
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            LinkRow(Icons.Default.HighQuality, "在线播放音质", quality.label) { dialog = "quality" }
        }
        Section("网络") {
            ToggleRow(Icons.Default.CellTower, "允许计费网络播放", "移动数据及计费 Wi-Fi", values[2]) { preferences.allowMetered = it }
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            ToggleRow(Icons.Default.Download, "仅非计费网络下载", "适用于新下载", values[3]) { preferences.downloadUnmetered = it }
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            LinkRow(Icons.Default.Dns, "服务器", "", onOpenServer)
        }
        Section("存储") {
            LinkRow(Icons.Default.Download, "下载", "", onDownloads)
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            LinkRow(Icons.Default.Storage, "存储空间管理", "缓存与下载", onStorage)
        }
        Section("更多") {
            LinkRow(Icons.Default.Widgets, "桌面小组件", "添加播放控制器") {
                dialog = "widget"
            }
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            LinkRow(Icons.Default.Palette, "个性化", "主题与外观", onPersonalization)
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            ToggleRow(Icons.Default.Lyrics, "通知栏歌词", "在播放通知中显示歌词", notificationLyrics) { preferences.notificationLyrics = it }
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            LinkRow(Icons.Default.BatteryChargingFull, "后台播放与通知", "电池与通知权限", onSystemSettings)
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            LinkRow(Icons.Default.SystemUpdate, "应用更新", "", onUpdate)
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            LinkRow(Icons.Default.Description, "开源许可", "GPL-3.0", onLegal)
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ShellBg)
            LinkRow(Icons.Default.Info, "关于", "Lee's Music") { dialog = "about" }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (dialog == "widget") AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("添加桌面小组件") },
        text = { Column {
            com.lyq2010.leesmusic.widget.WidgetSize.entries.forEach { size ->
                TextButton(onClick = {
                    val manager = android.appwidget.AppWidgetManager.getInstance(context)
                    if (manager.isRequestPinAppWidgetSupported) {
                        manager.requestPinAppWidget(android.content.ComponentName(context, size.provider), null, null)
                    } else android.widget.Toast.makeText(context, "请长按桌面，在小组件中选择 Lee’s Music", android.widget.Toast.LENGTH_LONG).show()
                    dialog = ""
                }) { Text(size.label) }
            }
        } }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("取消") } })
    if (dialog == "quality") AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("在线播放音质") },
        text = { Column {
            Text("适用于新加入队列的歌曲。压缩音质需服务器支持。", color = ShellMuted)
            StreamQuality.entries.forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { preferences.quality = option; dialog = "" }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(quality == option, onClick = { preferences.quality = option; dialog = "" })
                    Text(option.label)
                }
            }
        } }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("关闭") } })
    if (dialog == "about") {
        val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
        AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Lee's Music") },
            text = { Text("版本 $version\n\n连接自己的音乐服务器，收听自己的音乐。\n\n当前支持 Navidrome；Emby、Plex 接口尚未接入。\n\n不收集使用统计，不接入广告与第三方崩溃上报。连接信息保存在本机，密码由 Android Keystore 加密。") },
            confirmButton = { TextButton(onClick = { dialog = "" }) { Text("知道了") } })
    }
}

@Composable private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = ShellAccent, fontSize = 13.sp, modifier = Modifier.padding(start = 12.dp, top = 24.dp, bottom = 10.dp))
    Surface(color = ShellCard, shape = RoundedCornerShape(20.dp)) { Column(content = content) }
}

@Composable private fun ToggleRow(icon: ImageVector, label: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = ShellMuted, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(label, fontSize = 15.sp)
            if (detail.isNotBlank()) Text(detail, color = ShellMuted, fontSize = 12.sp)
        }
        Switch(checked, onCheckedChange = onChange)
    }
}

@Composable private fun LinkRow(icon: ImageVector, label: String, detail: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = ShellMuted, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(label, fontSize = 15.sp)
            if (detail.isNotBlank()) Text(detail, color = ShellMuted, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Default.KeyboardArrowRight, null, tint = ShellMuted)
    }
}
