package com.lyq2010.leesmusic.ui.catalog

data class LibraryAlbum(
    val id: String,
    val name: String,
    val artist: String,
    val coverArtId: String?,
    val coverUrl: String?,
)

@kotlinx.serialization.Serializable
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
    val albumId: String? = null,
    val album: String = "",
    val artistId: String? = null,
    val starred: Boolean = false,
    val localUri: String? = null,
)

fun com.lyq2010.leesmusic.data.api.Song.toLibrarySong(
    client: com.lyq2010.leesmusic.data.api.SubsonicClient,
    server: com.lyq2010.leesmusic.data.api.SubsonicServer,
) = LibrarySong(id, title, artist, coverArt, coverArt?.let { client.coverArtUrl(server, it) },
    duration, track, suffix, bitRate, albumId, album, artistId, starred != null)

fun com.lyq2010.leesmusic.data.api.Album.toLibraryAlbum(
    client: com.lyq2010.leesmusic.data.api.SubsonicClient,
    server: com.lyq2010.leesmusic.data.api.SubsonicServer,
) = LibraryAlbum(id, name, artist, coverArt, coverArt?.let { client.coverArtUrl(server, it) })
