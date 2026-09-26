package com.lyq2010.leesmusic.ui.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.data.api.SubsonicServer
import com.lyq2010.leesmusic.data.library.LyricsRepository
import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import com.lyq2010.leesmusic.ui.player.*
import kotlinx.coroutines.CancellationException

@Composable
fun LyricsScreen(
    song: LibrarySong?, server: SubsonicServer?, repository: LyricsRepository,
    positionMs: Long, durationMs: Long, isPlaying: Boolean, volume: Float, repeatMode: Int,
    onSeek: (Long) -> Unit, onPlayPause: () -> Unit, onVolume: (Float) -> Unit,
    onPrevious: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit, onBack: () -> Unit,
    shuffle: Boolean = false, buffering: Boolean = false, canPrevious: Boolean = true, canNext: Boolean = true,
    onShuffle: () -> Unit = {}, onQueue: () -> Unit = {}, onMore: () -> Unit = {},
    sleepRemainingMs: Long = 0L, onSleepTimer: () -> Unit = {},
    onDismiss: () -> Unit = onBack,
) {
    var state by remember(song?.id, server) { mutableStateOf<LyricsUiState>(LyricsUiState.Loading) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(song?.id, server, retry) {
        if (song == null || server == null) {
            state = LyricsUiState.NoSong
            return@LaunchedEffect
        }
        state = LyricsUiState.Loading
        try {
            val lyrics = repository.load(server, song.id, song.artist, song.title)
            state = if (lyrics == null) LyricsUiState.Empty else LyricsUiState.Ready(lyrics)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            state = LyricsUiState.Failed
        }
    }
    PlaybackDismiss(onDismiss) {
        Column(Modifier.fillMaxSize().background(PlaybackGradient).safeDrawingPadding().testTag("lyrics-screen")) {
            PlaybackHandle("收起播放页", onDismiss)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val shown = state) {
                    is LyricsUiState.Ready -> key(song?.id, shown.lyrics) {
                        LyricsLines(shown.lyrics, positionMs, onSeek)
                    }
                    else -> LyricsMessage(shown) { retry++ }
                }
            }
            PlaybackControls(song?.title.orEmpty(), song?.artist.orEmpty(), positionMs, durationMs,
                isPlaying, volume, repeatMode, true, onSeek, onPlayPause, onVolume, onPrevious, onNext, onRepeat, onBack,
                shuffle, buffering, canPrevious, canNext, onShuffle, onQueue, onMore, sleepRemainingMs, onSleepTimer)
        }
    }
}

@Composable
internal fun LyricsMessage(state: LyricsUiState, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (state == LyricsUiState.Loading) {
            CircularProgressIndicator(Modifier.size(24.dp), color = PlaybackWhite, strokeWidth = 2.dp)
            Spacer(Modifier.height(20.dp))
        }
        Text(when (state) {
            LyricsUiState.Loading -> "正在读取歌词"
            LyricsUiState.Failed -> "暂时无法读取歌词"
            LyricsUiState.NoSong -> "先选择一首歌曲"
            else -> "这首歌暂无歌词"
        }, color = PlaybackWhite, fontSize = 18.sp)
        if (state == LyricsUiState.Failed) {
            Text("稍后可以重试", color = PlaybackWhite.copy(alpha = 0.65f),
                fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            TextButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("重试", color = PlaybackWhite) }
        }
    }
}
