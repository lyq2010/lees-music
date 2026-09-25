package com.lyq2010.leesmusic.ui.catalog

data class LibraryAlbum(
    val name: String,
    val artist: String,
    val coverUrl: String?,
)

data class LibrarySong(
    val title: String,
    val artist: String,
    val coverUrl: String?,
)
