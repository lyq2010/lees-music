package com.lyq2010.leesmusic.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.lyq2010.leesmusic.playback.queueOrder
import com.lyq2010.leesmusic.playback.moveUpcomingEntry
import com.lyq2010.leesmusic.ui.catalog.RemoteCover
import com.lyq2010.leesmusic.ui.shell.*
import okhttp3.OkHttpClient

internal data class QueueSections(val current: Int, val upcoming: List<Int>, val earlier: List<Int>)
internal fun queueSections(player: Player): QueueSections {
    val order = queueOrder(player)
    val current = player.currentMediaItemIndex
    val position = order.indexOf(current)
    if (position < 0) return QueueSections(-1, emptyList(), emptyList())
    val before = order.take(position)
    return if (player.repeatMode == Player.REPEAT_MODE_ALL)
        QueueSections(current, order.drop(position + 1) + before, emptyList())
    else QueueSections(current, order.drop(position + 1), before)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(player: Player?, http: OkHttpClient = remember { OkHttpClient() }, onDismiss: () -> Unit) {
    var revision by remember { mutableIntStateOf(0) }
    var clear by remember { mutableStateOf(false) }
    var earlierVisible by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) { revision++ }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    val sections = remember(player, revision) { player?.let(::queueSections) ?: QueueSections(-1, emptyList(), emptyList()) }
    val shuffled = player?.shuffleModeEnabled == true
    ShellTheme {
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = ShellCard) {
            Text("播放队列", color = ShellText, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            if (sections.current < 0) Text("还没有歌曲，从歌曲菜单加入队列", color = ShellMuted, modifier = Modifier.padding(20.dp))
            else if (player != null) LazyColumn(Modifier.fillMaxWidth().heightIn(max = 600.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                item { Text("当前歌曲", color = ShellAccent, modifier = Modifier.padding(20.dp, 12.dp)) }
                item { QueueTrack(player, sections.current, http, current = true) }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("接下来 · ${sections.upcoming.size}", color = ShellAccent, modifier = Modifier.weight(1f))
                        TextButton(enabled = sections.upcoming.isNotEmpty(), onClick = { clear = true }) { Text("清空接下来") }
                    }
                    if (shuffled) Text("按随机播放顺序显示；关闭随机播放后可调整顺序", color = ShellMuted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    if (player.repeatMode == Player.REPEAT_MODE_ONE) Text("当前为单曲循环；切换循环模式后继续队列", color = ShellMuted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    if (sections.upcoming.isEmpty()) Text("后面没有歌曲了，可以从歌曲菜单继续添加", color = ShellMuted, modifier = Modifier.padding(20.dp))
                }
                items(sections.upcoming, key = { "next-$it" }) { index ->
                    QueueTrack(player, index, http, current = false,
                        moveUp = if (!shuffled && sections.upcoming.indexOf(index) > 0) ({ moveUpcomingEntry(player, index, sections.upcoming[sections.upcoming.indexOf(index) - 1]) }) else null,
                        moveDown = if (!shuffled && sections.upcoming.indexOf(index) < sections.upcoming.lastIndex) ({ moveUpcomingEntry(player, index, sections.upcoming[sections.upcoming.indexOf(index) + 1]) }) else null,
                        remove = { player.removeMediaItem(index) })
                }
                if (sections.earlier.isNotEmpty()) {
                    item { TextButton(onClick = { earlierVisible = !earlierVisible }, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(if (earlierVisible) "收起之前的队列" else "之前的队列 · ${sections.earlier.size}")
                    } }
                    if (earlierVisible) items(sections.earlier, key = { "earlier-$it" }) { index -> QueueTrack(player, index, http, current = false) }
                }
            }
        }
        if (clear) AlertDialog(onDismissRequest = { clear = false }, title = { Text("清空接下来的歌曲？") },
            text = { Text("保留当前歌曲和播放进度，不删除音乐库中的歌曲或下载文件。") },
            confirmButton = { TextButton(onClick = {
                player?.let { p -> queueSections(p).upcoming.sortedDescending().forEach { p.removeMediaItem(it) } }
                clear = false
            }) { Text("清空队列") } }, dismissButton = { TextButton(onClick = { clear = false }) { Text("取消") } })
    }
}

@Composable
private fun QueueTrack(player: Player, index: Int, http: OkHttpClient, current: Boolean,
    moveUp: (() -> Unit)? = null, moveDown: (() -> Unit)? = null, remove: (() -> Unit)? = null) {
    if (index !in 0 until player.mediaItemCount) return
    val item = player.getMediaItemAt(index)
    val title = item.mediaMetadata.title?.toString().orEmpty()
    val extras = item.mediaMetadata.extras
    Row(Modifier.fillMaxWidth().clickable(enabled = !current) { player.seekTo(index, 0) }.padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RemoteCover(extras?.getString("coverArtId"), extras?.getString("coverUrl"), Modifier.size(44.dp), http)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(title, color = if (current) ShellAccent else ShellText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.mediaMetadata.artist?.toString().orEmpty(), color = ShellMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (remove != null) {
            Box {
                var actions by remember { mutableStateOf(false) }
                IconButton(onClick = { actions = true }) { Icon(Icons.Default.MoreVert, "$title 的队列操作") }
                DropdownMenu(actions, { actions = false }) {
                    DropdownMenuItem(text = { Text("上移") }, enabled = moveUp != null, onClick = { actions = false; moveUp?.invoke() })
                    DropdownMenuItem(text = { Text("下移") }, enabled = moveDown != null, onClick = { actions = false; moveDown?.invoke() })
                    DropdownMenuItem(text = { Text("从队列移除") }, onClick = { actions = false; remove() })
                }
            }
        }
    }
}
