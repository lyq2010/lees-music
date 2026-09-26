package com.lyq2010.leesmusic.ui.player

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.lyq2010.leesmusic.data.settings.PlaybackPreferences

@Composable
internal fun PlaybackSpeedButton() {
    val context = LocalContext.current
    val preferences = remember { PlaybackPreferences(context) }
    var speed by remember { mutableFloatStateOf(preferences.speed) }
    var expanded by remember { mutableStateOf(false) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> speed = preferences.speed }
        preferences.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    TextButton(onClick = { expanded = true }, modifier = Modifier.semantics { contentDescription = "播放速度" }) {
        Text(if (speed == 1f) "1×" else "${speed}×", color = PlaybackWhite)
    }
    if (expanded) AlertDialog(onDismissRequest = { expanded = false }, title = { Text("播放速度") },
        text = { Column {
            listOf(.75f, 1f, 1.25f, 1.5f, 2f).forEach { option ->
                val select = { preferences.speed = option; expanded = false }
                Row(Modifier.fillMaxWidth().clickable(onClick = select), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(speed == option, onClick = select)
                    Text(if (option == 1f) "正常" else "${option}×")
                }
            }
        } }, confirmButton = { TextButton(onClick = { expanded = false }) { Text("取消") } })
}
