package com.lyq2010.leesmusic.ui.settings

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyq2010.leesmusic.data.settings.PlaybackPreferences
import com.lyq2010.leesmusic.ui.shell.*

@Composable
internal fun PersonalizationScreen(preferences: PlaybackPreferences, onBack: () -> Unit) {
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(preferences) {
        val changed = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        preferences.prefs.registerOnSharedPreferenceChangeListener(changed)
        onDispose { preferences.prefs.unregisterOnSharedPreferenceChangeListener(changed) }
    }
    val mode = remember(revision) { preferences.appearance }
    val accent = remember(revision) { preferences.accent }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SettingsHeading("个性化", onBack)
        Text("深浅模式", color = ShellAccent, modifier = Modifier.padding(vertical = 16.dp))
        listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (value, label) ->
            Row(Modifier.fillMaxWidth().clickable { preferences.appearance = value }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(mode == value, { preferences.appearance = value }); Text(label)
            }
        }
        Text("主题色", color = ShellAccent, modifier = Modifier.padding(vertical = 16.dp))
        listOf("blue" to "雾蓝", "purple" to "鸢尾紫", "green" to "松绿", "rose" to "玫瑰").forEach { (value, label) ->
            Row(Modifier.fillMaxWidth().clickable { preferences.accent = value }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(accent == value, { preferences.accent = value }); Text(label)
            }
        }
        Text("播放页始终使用深色外观。", color = ShellMuted, modifier = Modifier.padding(vertical = 16.dp))
    }
}

@Composable
internal fun SettingsHeading(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回设置") }
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}
