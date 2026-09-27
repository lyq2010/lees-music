package com.lyq2010.leesmusic.data.library

import com.lyq2010.leesmusic.data.api.Song
import kotlinx.coroutines.CancellationException

internal suspend fun refreshDailySongs(
    fetch: suspend () -> List<Song>,
    save: suspend (List<Song>) -> Unit,
    cached: suspend () -> List<Song>,
): List<Song> = try {
    val songs = fetch()
    save(songs)
    songs
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    cached()
}
