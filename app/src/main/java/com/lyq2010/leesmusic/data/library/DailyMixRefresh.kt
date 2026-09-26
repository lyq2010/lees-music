package com.lyq2010.leesmusic.data.library

import com.lyq2010.leesmusic.data.api.Song
import kotlinx.coroutines.CancellationException

internal fun refreshDailySongs(
    fetch: () -> List<Song>,
    save: (List<Song>) -> Unit,
    cached: () -> List<Song>,
): List<Song> = try {
    fetch().also(save)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    cached()
}
