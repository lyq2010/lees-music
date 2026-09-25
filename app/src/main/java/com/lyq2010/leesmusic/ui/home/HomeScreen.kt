package com.lyq2010.leesmusic.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.catalog.AlbumCover
import com.lyq2010.leesmusic.ui.catalog.SampleCatalog
import com.lyq2010.leesmusic.ui.catalog.Track
import com.lyq2010.leesmusic.ui.theme.Ink
import com.lyq2010.leesmusic.ui.theme.Paper
import com.lyq2010.leesmusic.ui.theme.Peach

@Composable
fun HomeScreen(
    nowPlaying: Track,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(SampleCatalog.filters.first()) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 20.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Lee's Music",
                color = Ink,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "设置", tint = Ink)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
            SampleCatalog.filters.forEach { name ->
                FilterChip(name, selected = name == filter, onClick = { filter = name })
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(top = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(SampleCatalog.tracks, key = { it.title }) { track ->
                Column(Modifier.clickable(onClick = onOpenPlayer)) {
                    AlbumCover(track.cover, Modifier.clip(RoundedCornerShape(4.dp)))
                    Text(track.title, color = Ink, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
    MiniPlayer(nowPlaying, onOpenPlayer, Modifier)
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) Ink else Color.Transparent,
        border = BorderStroke(1.dp, Ink),
    ) {
        Text(
            label,
            color = if (selected) Peach else Ink,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun MiniPlayer(track: Track, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(18.dp),
        color = Ink,
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumCover(
                track.cover,
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(track.title, color = Peach, fontSize = 16.sp)
                Text(track.artist, color = Peach.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }
    }
}
