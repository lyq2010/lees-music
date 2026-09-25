package com.lyq2010.leesmusic.ui.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.catalog.Track
import com.lyq2010.leesmusic.ui.catalog.playerBackground
import com.lyq2010.leesmusic.ui.theme.Peach

@Composable
fun LyricsScreen(track: Track, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(track.cover.playerBackground())
            .padding(horizontal = 24.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Peach)
        }
        Text(track.title, color = Peach, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
        Text(track.artist, color = Peach.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 28.dp))
        track.lyrics.forEachIndexed { index, line ->
            Text(
                line,
                color = Peach.copy(alpha = if (index == 0) 1f else 0.45f),
                fontSize = if (index == 0) 22.sp else 18.sp,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
    }
}
