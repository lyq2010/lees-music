package com.lyq2010.leesmusic.ui.catalog

data class LibraryAlbum(
    val id: String,
    val name: String,
    val artist: String,
    val coverArtId: String?,
    val coverUrl: String?,
)

data class LibrarySong(
    val id: String,
    val title: String,
    val artist: String,
    val coverArtId: String?,
    val coverUrl: String?,
)
