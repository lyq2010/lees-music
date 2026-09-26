package com.lyq2010.leesmusic.data.library

import com.lyq2010.leesmusic.ui.catalog.LibrarySong

/** Caller supplies verified downloads from the active server/account only. */
fun withDownloadedSongs(songs: List<LibrarySong>, localUris: Map<String, String>): List<LibrarySong> =
    songs.map { song -> localUris[song.id]?.let { song.copy(localUri = it) } ?: song }
