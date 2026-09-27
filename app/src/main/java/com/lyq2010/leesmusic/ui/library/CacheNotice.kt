package com.lyq2010.leesmusic.ui.library

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyq2010.leesmusic.ui.shell.ShellMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun CacheNotice(savedAt: Long) {
    if (savedAt <= 0L) return
    val time = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(savedAt))
    Text("本地缓存 · $time · 点刷新获取最新", color = ShellMuted,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
}
