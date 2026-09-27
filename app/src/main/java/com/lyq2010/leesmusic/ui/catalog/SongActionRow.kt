package com.lyq2010.leesmusic.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lyq2010.leesmusic.ui.shell.*

@Composable
internal fun ActionItem(label: String, enabled: Boolean, detail: String? = null,
    icon: ImageVector = when (label) {
        "立即播放" -> Icons.Default.PlayArrow
        "下一首播放" -> Icons.Default.SkipNext
        "加入播放队列" -> Icons.Default.QueueMusic
        "收藏歌曲" -> Icons.Default.FavoriteBorder
        "取消收藏" -> Icons.Default.Favorite
        "加入歌单" -> Icons.Default.PlaylistAdd
        "从歌单移除" -> Icons.Default.PlaylistRemove
        "下载原音质", "重新下载" -> Icons.Default.Download
        "已下载" -> Icons.Default.DownloadDone
        "查看专辑" -> Icons.Default.Album
        "分享歌曲信息" -> Icons.Default.Share
        "歌曲信息" -> Icons.Default.Info
        else -> Icons.Default.MusicNote
    }, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = detail?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        leadingContent = { Icon(icon, null, tint = if (enabled) ShellAccent else ShellMuted) },
        trailingContent = if (label in listOf("加入歌单", "查看专辑", "歌曲信息")) ({ Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }) else null,
        colors = ListItemDefaults.colors(containerColor = ShellCard, headlineColor = if (enabled) ShellText else ShellMuted,
            supportingColor = ShellMuted, trailingIconColor = ShellMuted),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(enabled = enabled, onClick = onClick),
    )
}
