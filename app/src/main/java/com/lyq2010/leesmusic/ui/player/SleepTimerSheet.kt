package com.lyq2010.leesmusic.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyq2010.leesmusic.ui.shell.*

internal fun sleepTime(ms: Long): String = "%d:%02d".format((ms + 999) / 60000, (ms + 999) / 1000 % 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SleepTimerSheet(remainingMs: Long, onSet: (Long, (Boolean) -> Unit) -> Unit, onDismiss: () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    fun set(duration: Long) {
        busy = true
        failed = false
        onSet(duration) { success ->
            busy = false
            if (success) onDismiss() else failed = true
        }
    }
    ShellTheme {
        ModalBottomSheet(onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = ShellCard) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                Text("睡眠定时", style = MaterialTheme.typography.titleLarge)
                Text(if (remainingMs > 0) "将在 ${sleepTime(remainingMs)} 后暂停" else "计时结束后暂停播放",
                    color = ShellMuted, modifier = Modifier.padding(vertical = 12.dp))
                if (failed) Text("设置未成功，请重试", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
                listOf(5, 15, 30, 45, 60).forEach { minutes ->
                    TextButton(onClick = { set(minutes * 60_000L) }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("$minutes 分钟")
                    }
                }
                if (remainingMs > 0) OutlinedButton(onClick = { set(0) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("取消定时") }
            }
        }
    }
}
