package com.lyq2010.leesmusic.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.shell.*
import okhttp3.OkHttpClient

@Composable
fun SongRow(song: LibrarySong, http: OkHttpClient, onPlay: () -> Unit, onMore: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(start = 20.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RemoteCover(song.coverArtId, song.coverUrl, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), http)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(song.title, color = ShellText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOf(song.artist, song.suffix.uppercase()).filter { it.isNotBlank() }.joinToString(" · "),
                color = ShellMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("%d:%02d".format(song.duration / 60, song.duration % 60), color = ShellMuted, fontSize = 12.sp)
        IconButton(onClick = onMore) {
            Icon(Icons.Default.MoreVert, contentDescription = "${song.title}的更多操作", tint = ShellMuted)
        }
    }
}
