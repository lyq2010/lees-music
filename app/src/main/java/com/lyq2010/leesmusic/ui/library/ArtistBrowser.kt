package com.lyq2010.leesmusic.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.ui.catalog.RemoteCover
import com.lyq2010.leesmusic.ui.shell.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

@Composable
internal fun ArtistBrowser(state: LibraryPageState, server: SubsonicServer?, client: SubsonicClient,
    http: OkHttpClient, onBack: () -> Unit, onArtist: (Artist) -> Unit, onRefresh: () -> Unit) {
    val all = state.data?.artists.orEmpty()
    val artists = all.filter { it.name.contains(state.filter, true) && (!state.favoritesOnly || it.starred != null) }
        .let { entries -> when (state.artistSort) {
            1 -> entries.sortedByDescending { it.name.lowercase() }
            2 -> entries.sortedWith(compareByDescending<Artist> { it.albumCount }.thenBy { it.name.lowercase() })
            else -> entries.sortedBy { it.name.lowercase() }
        } }
    var menu by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(setOf<String>()) }
    var mutationError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun favorite(artist: Artist) {
        if (server == null || artist.id in pending) return
        pending = pending + artist.id
        scope.launch {
            try {
                withContext(Dispatchers.IO) { client.setArtistFavorite(server, artist.id, artist.starred == null) }
                state.updateArtist(artist.id, artist.starred == null)
                mutationError = null
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { mutationError = "收藏未能保存，请再次点击重试" }
            finally { pending = pending - artist.id }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Default.ArrowBack, "返回") }
            Column(Modifier.weight(1f)) {
                Text("艺术家", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(if (state.data == null) "正在读取曲库" else "${all.size} 位艺术家", color = ShellMuted, fontSize = 14.sp)
            }
            IconButton(onClick = { state.artistGrid = !state.artistGrid }) {
                Icon(if (state.artistGrid) Icons.Default.ViewList else Icons.Default.GridView,
                    if (state.artistGrid) "切换列表视图" else "切换网格视图")
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, "排序", tint = ShellText)
                }
                DropdownMenu(menu, { menu = false }) {
                    listOf("名称 A–Z", "名称 Z–A", "专辑数量").forEachIndexed { index, label ->
                        DropdownMenuItem(text = { Text(label + if (state.artistSort == index) " ✓" else "") },
                            onClick = { state.artistSort = index; menu = false })
                    }
                    DropdownMenuItem(text = { Text(if (state.favoritesOnly) "显示全部艺术家" else "只看已收藏") },
                        onClick = { state.favoritesOnly = !state.favoritesOnly; menu = false })
                    DropdownMenuItem(text = { Text("刷新列表") }, enabled = !state.loading,
                        onClick = { onRefresh(); menu = false })
                }
            }
        }
        TextField(state.filter, { state.filter = it }, placeholder = { Text("搜索艺术家") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (state.filter.isNotEmpty()) IconButton(onClick = { state.filter = "" }) { Icon(Icons.Default.Close, "清空筛选") } },
            shape = RoundedCornerShape(32.dp), colors = TextFieldDefaults.colors(
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp))
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        (mutationError ?: state.error)?.let { error ->
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(error, color = ShellMuted, modifier = Modifier.weight(1f))
                if (state.error != null) TextButton(onClick = onRefresh) { Text("重试") }
            }
        }
        if (!state.loading && artists.isEmpty()) LibraryEmpty("没有匹配的艺术家", "试试其他关键词或调整收藏筛选")
        if (state.artistGrid) LazyVerticalGrid(GridCells.Fixed(3), state = state.grid,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            items(artists, key = { it.id }) { artist ->
                Column(Modifier.clickable { onArtist(artist) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        ArtistPortrait(artist, server, client, http, Modifier.fillMaxWidth().aspectRatio(1f))
                        IconButton(onClick = { favorite(artist) }, enabled = artist.id !in pending,
                            modifier = Modifier.align(Alignment.TopEnd).size(40.dp).background(ShellBg, CircleShape)) {
                            Icon(if (artist.starred == null) Icons.Outlined.FavoriteBorder else Icons.Default.Favorite,
                                "${if (artist.starred == null) "收藏" else "取消收藏"}${artist.name}", tint = ShellAccent)
                        }
                    }
                    Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp), fontSize = 15.sp)
                    Text(artistCounts(artist), fontSize = 12.sp, color = ShellMuted, maxLines = 1)
                }
            }
        } else LazyColumn(state = state.list) {
            items(artists, key = { it.id }) { artist ->
                ListItem(headlineContent = { Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(artistCounts(artist)) },
                    leadingContent = { ArtistPortrait(artist, server, client, http, Modifier.size(56.dp)) },
                    trailingContent = { IconButton(onClick = { favorite(artist) }, enabled = artist.id !in pending) {
                        Icon(if (artist.starred == null) Icons.Outlined.FavoriteBorder else Icons.Default.Favorite,
                            "${if (artist.starred == null) "收藏" else "取消收藏"}${artist.name}")
                    } }, colors = ListItemDefaults.colors(containerColor = ShellBg), modifier = Modifier.clickable { onArtist(artist) })
            }
        }
    }
}

private fun artistCounts(artist: Artist) = "专辑 ${artist.albumCount}" + (artist.songCount?.let { " · 歌曲 $it" } ?: "")

@Composable
internal fun ArtistPortrait(artist: Artist, server: SubsonicServer?, client: SubsonicClient, http: OkHttpClient, modifier: Modifier) {
    // Prefer server artwork; never send server credentials to an external image host.
    val url = remember(artist.coverArt, artist.artistImageUrl, server) {
        artist.coverArt?.let { id -> server?.let { client.coverArtUrl(it, id) } } ?: artist.artistImageUrl
    }
    Box(modifier.clip(CircleShape).background(ShellCard), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Person, null, tint = ShellMuted, modifier = Modifier.fillMaxSize(.6f))
        if (!url.isNullOrBlank()) RemoteCover(artist.coverArt ?: artist.id, url, Modifier.fillMaxSize(), http) {
            Icon(Icons.Default.Person, null, tint = ShellMuted, modifier = Modifier.fillMaxSize(.6f))
        }
    }
}
