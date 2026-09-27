package com.lyq2010.leesmusic.ui.library

import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.ui.catalog.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Keep requesting pages until the server is exhausted, never label the first page as the whole library. */
suspend fun <T> readLibraryPages(id: (T) -> String, fetch: suspend (Int) -> List<T>): List<T> {
    val result = linkedMapOf<String, T>()
    var offset = 0
    while (true) {
        currentCoroutineContext().ensureActive()
        val page = fetch(offset)
        if (page.isEmpty()) return result.values.toList()
        val oldSize = result.size
        page.forEach { result[id(it)] = it }
        check(result.size > oldSize) { "服务器未正确返回下一页，请重试" }
        offset += page.size
    }
}

@kotlinx.serialization.Serializable
data class LibraryOverview(val favorites: List<LibrarySong>, val playlists: List<MusicPlaylist>)
@kotlinx.serialization.Serializable
data class LibraryPage(
    val songs: List<LibrarySong> = emptyList(),
    val albums: List<LibraryAlbum> = emptyList(),
    val artists: List<Artist> = emptyList(),
)
data class LibraryDestination(val kind: String, val title: String, val id: String = "")

suspend fun loadLibraryPage(client: SubsonicClient, server: SubsonicServer, target: LibraryDestination): LibraryPage =
    when (target.kind) {
        "songs" -> LibraryPage(songs = readLibraryPages<Song>({ it.id }) { client.songs(server, it) }.map { it.toLibrarySong(client, server) })
        "albums" -> LibraryPage(albums = readLibraryPages<Album>({ it.id }) { client.albums(server, "alphabeticalByName", it, 200) }.map { it.toLibraryAlbum(client, server) })
        "artists" -> LibraryPage(artists = client.artists(server))
        "artist" -> LibraryPage(albums = client.artist(server, target.id).album.map { it.toLibraryAlbum(client, server) })
        "favorites" -> LibraryPage(songs = client.favorites(server).map { it.toLibrarySong(client, server) })
        "playlist" -> LibraryPage(songs = client.playlist(server, target.id).entry.map { it.toLibrarySong(client, server) })
        else -> LibraryPage()
    }
