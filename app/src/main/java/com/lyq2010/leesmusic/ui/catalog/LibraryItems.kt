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
    val duration: Int = 0,
    val track: Int = 0,
    val suffix: String = "",
    val bitRate: Int = 0,
)
