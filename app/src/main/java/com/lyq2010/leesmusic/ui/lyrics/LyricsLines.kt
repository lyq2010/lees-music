package com.lyq2010.leesmusic.ui.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.data.api.StructuredLyrics
import com.lyq2010.leesmusic.ui.player.PlaybackWhite
import kotlinx.coroutines.delay

@Composable
internal fun LyricsLines(lyrics: StructuredLyrics, positionMs: Long, onSeek: (Long) -> Unit) {
    val rows = remember(lyrics) { lyrics.displayLines() }
    val active = rows.currentLine(positionMs)
    val list = rememberLazyListState()
    var following by rememberSaveable { mutableStateOf(true) }
    val dragged by list.interactionSource.collectIsDraggedAsState()
    var touching by remember { mutableStateOf(false) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    val heights = remember { mutableStateMapOf<Int, Int>() }
    val density = LocalDensity.current
    val defaultHeight = with(density) { 80.dp.roundToPx() }
    val activeHeight = heights[active.coerceAtLeast(0)] ?: defaultHeight
    var initialPositioned by remember { mutableStateOf(false) }
    LaunchedEffect(dragged) { if (dragged) following = false }
    LaunchedEffect(following, touching, dragged, list.isScrollInProgress) {
        if (lyrics.synced && !following && !touching && !dragged && !list.isScrollInProgress) {
            delay(5_000)
            following = true
        }
    }
    LaunchedEffect(active, following, viewportHeight, activeHeight) {
        if (!lyrics.synced || !following || viewportHeight == 0 || rows.isEmpty()) return@LaunchedEffect
        val index = active.coerceAtLeast(0)
        if (!initialPositioned) {
            list.scrollToItem(index, activeHeight / 2)
            initialPositioned = true
        } else list.animateScrollToItem(index, activeHeight / 2)
    }
    Box(Modifier.fillMaxSize().onSizeChanged { viewportHeight = it.height }) {
        val verticalPadding = if (lyrics.synced) with(density) { (viewportHeight / 2).toDp() } else 28.dp
        LazyColumn(state = list,
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = verticalPadding),
            modifier = Modifier.fillMaxSize().testTag("lyrics-lines")
                .pointerInput(Unit) {
                    try {
                        awaitPointerEventScope {
                            while (true) touching = awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed }
                        }
                    } finally { touching = false }
                }
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.10f to Color.Black,
                        0.85f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                }) {
            itemsIndexed(rows) { index, row ->
                val focused = active == index
                val color by animateColorAsState(
                    PlaybackWhite.copy(alpha = if (!lyrics.synced || focused) 1f else 0.36f),
                    animationSpec = tween(240), label = "lyricEmphasis")
                Column(Modifier.fillMaxWidth().onSizeChanged { heights[index] = it.height }
                    .testTag("lyric-$index").semantics { selected = focused }
                    .then(if (row.timeMs != null) Modifier.clickable(onClickLabel = "跳转到此句") {
                        onSeek(row.timeMs)
                        following = true
                    } else Modifier)
                    .padding(vertical = 18.dp)) {
                    Text(row.text, color = color, fontSize = 27.sp, lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold)
                    row.secondary.forEach { text ->
                        Text(text, color = color, fontSize = 18.sp, lineHeight = 27.sp,
                            fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        if (!following && lyrics.synced) {
            TextButton(onClick = { following = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 8.dp)
                    .background(Color(0xEE251A32), androidx.compose.foundation.shape.CircleShape)
                    .testTag("lyrics-follow")) { Text("回到当前歌词", color = PlaybackWhite) }
        } else {
            val hint = lyrics.playbackHint(positionMs)
            if (hint.isNotBlank()) Text(hint, color = PlaybackWhite.copy(alpha = 0.55f), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 32.dp, bottom = 16.dp))
        }
    }
}
