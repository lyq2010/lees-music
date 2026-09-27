package com.lyq2010.leesmusic.ui.library

import com.lyq2010.leesmusic.data.library.LibraryDownload
import com.lyq2010.leesmusic.ui.catalog.LibraryAlbum
import com.lyq2010.leesmusic.ui.catalog.LibrarySong

internal fun sortedSongs(songs: List<LibrarySong>, sort: Int): List<IndexedValue<LibrarySong>> {
    val entries = songs.withIndex().toList()
    return when (sort) {
        1 -> entries.sortedBy { it.value.title.lowercase() }
        2 -> entries.sortedByDescending { it.value.title.lowercase() }
        3 -> entries.sortedWith(compareBy<IndexedValue<LibrarySong>> { it.value.artist.lowercase() }.thenBy { it.value.title.lowercase() })
        4 -> entries.sortedBy { it.value.duration }
        else -> entries
    }
}

internal fun sortedAlbums(albums: List<LibraryAlbum>, sort: Int): List<LibraryAlbum> = when (sort) {
    1 -> albums.sortedBy { it.name.lowercase() }
    2 -> albums.sortedByDescending { it.name.lowercase() }
    3 -> albums.sortedWith(compareBy<LibraryAlbum> { it.artist.lowercase() }.thenBy { it.name.lowercase() })
    else -> albums
}

internal fun sortedDownloads(downloads: List<LibraryDownload>, sort: Int): List<LibraryDownload> = when (sort) {
    1 -> downloads.sortedBy { it.song.title.lowercase() }
    2 -> downloads.sortedByDescending { it.song.title.lowercase() }
    3 -> downloads.sortedWith(compareBy<LibraryDownload> { it.song.artist.lowercase() }.thenBy { it.song.title.lowercase() })
    else -> downloads
}
