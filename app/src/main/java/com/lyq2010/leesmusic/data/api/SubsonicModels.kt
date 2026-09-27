package com.lyq2010.leesmusic.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubsonicEnvelope(
    @SerialName("subsonic-response") val response: SubsonicBody,
)

@Serializable
data class SubsonicBody(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,
    val albumList2: AlbumList? = null,
    val searchResult3: SearchResult? = null,
    val randomSongs: SongList? = null,
    val album: Album? = null,
    val lyricsList: LyricsList? = null,
    val lyrics: PlainLyrics? = null,
    val artists: ArtistIndex? = null,
    val artist: Artist? = null,
    val starred2: SearchResult? = null,
    val playlists: Playlists? = null,
    val playlist: MusicPlaylist? = null,
    val shares: Shares? = null,
)

@Serializable
data class SubsonicError(
    val code: Int,
    val message: String,
)

@Serializable
data class AlbumList(
    val album: List<Album> = emptyList(),
)

@Serializable
data class Album(
    val id: String,
    val name: String,
    val artist: String = "",
    val coverArt: String? = null,
    val year: Int = 0,
    val songCount: Int = 0,
    val song: List<Song> = emptyList(),
)

@Serializable
data class SearchResult(
    val song: List<Song> = emptyList(),
    val album: List<Album> = emptyList(),
    val artist: List<Artist> = emptyList(),
)

@Serializable
data class SongList(
    val song: List<Song> = emptyList(),
)

@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val coverArt: String? = null,
    val duration: Int = 0,
    val track: Int = 0,
    val suffix: String = "",
    val bitRate: Int = 0,
    val albumId: String? = null,
    val artistId: String? = null,
    val starred: String? = null,
)

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val album: List<Album> = emptyList(),
    val coverArt: String? = null,
    val artistImageUrl: String? = null,
    val starred: String? = null,
    val songCount: Int? = null,
)

@Serializable data class ArtistIndex(val index: List<ArtistGroup> = emptyList())
@Serializable data class ArtistGroup(val name: String = "", val artist: List<Artist> = emptyList())
@Serializable data class Playlists(val playlist: List<MusicPlaylist> = emptyList())
@Serializable data class Shares(val share: List<MusicShare> = emptyList())
@Serializable data class MusicShare(val id: String, val url: String)
@Serializable data class MusicPlaylist(
    val id: String, val name: String, val owner: String = "",
    val public: Boolean = false, val songCount: Int = 0, val coverArt: String? = null,
    val entry: List<Song> = emptyList(),
)

class SubsonicException(val code: Int, message: String) : Exception(message)
