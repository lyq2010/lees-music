package com.lyq2010.leesmusic.ui.settings

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyq2010.leesmusic.data.settings.*
import com.lyq2010.leesmusic.playback.EqualizerStatus
import com.lyq2010.leesmusic.ui.shell.*

@Composable
internal fun EqualizerScreen(preferences: PlaybackPreferences, onBack: () -> Unit) {
    var selected by remember { mutableStateOf(preferences.equalizer) }
    val unavailable by EqualizerStatus.unavailable.collectAsState()
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> selected = preferences.equalizer }
        preferences.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SettingsHeading("均衡器", onBack)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = ShellCard)) {
            EqualizerPreset.entries.forEachIndexed { index, preset ->
                Row(Modifier.fillMaxWidth().clickable { preferences.equalizer = preset }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected == preset, onClick = { preferences.equalizer = preset })
                    Column(Modifier.weight(1f).padding(8.dp)) {
                        Text(if (preset == EqualizerPreset.Balanced) "均衡（默认）" else preset.label, style = MaterialTheme.typography.titleMedium)
                        Text(preset.description, color = ShellMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (index < EqualizerPreset.entries.lastIndex) HorizontalDivider(Modifier.padding(start = 64.dp), color = ShellBg)
            }
        }
        if (unavailable) Text("当前设备暂不支持均衡器", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
    }
}
