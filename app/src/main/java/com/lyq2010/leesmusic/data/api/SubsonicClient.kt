package com.lyq2010.leesmusic.data.api

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class SubsonicServer(
    val baseUrl: String,
    val username: String,
    val password: String,
)

class SubsonicClient(
    private val http: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun ping(server: SubsonicServer): SubsonicBody = call(server, "ping")

    fun lyrics(server: SubsonicServer, songId: String, artist: String, title: String): StructuredLyrics? {
        val candidates = try {
            call(server, "getLyricsBySongId", mapOf("id" to songId)).lyricsList?.structuredLyrics.orEmpty()
        } catch (error: SubsonicException) {
            // Unsupported endpoint or missing lyrics may fall back; authentication and network errors must remain visible.
            if (error.code !in setOf(20, 30, 70, 404, 501)) throw error
            emptyList()
        }
        val available = candidates.filter { lyrics -> lyrics.line.any { it.value.isNotBlank() } }
        val selected = available.firstOrNull { it.synced } ?: available.firstOrNull()
        if (selected != null) return if (selected.synced && selected.line.any { it.start != null }) {
            selected.copy(line = selected.line.filter { it.start != null }.sortedBy { it.start })
        } else parseLyricsText(selected.line.joinToString("\n") { it.value }, selected.offset)
        val plain = call(server, "getLyrics", mapOf("artist" to artist, "title" to title)).lyrics?.value
        return plain?.let { parseLyricsText(it) }
    }

    fun album(server: SubsonicServer, id: String): Album =
        call(server, "getAlbum", mapOf("id" to id)).album ?: Album(id = id, name = "")

    fun albums(server: SubsonicServer, type: String, offset: Int = 0, size: Int = 20): List<Album> =
        call(server, "getAlbumList2", mapOf("type" to type, "size" to size.toString(), "offset" to offset.toString())).albumList2?.album.orEmpty()

    fun songs(server: SubsonicServer, offset: Int, size: Int = 200): List<Song> =
        call(server, "search3", mapOf("query" to "", "songCount" to size.toString(), "songOffset" to offset.toString(),
            "albumCount" to "0", "artistCount" to "0")).searchResult3?.song.orEmpty()

    fun artists(server: SubsonicServer): List<Artist> =
        call(server, "getArtists").artists?.index.orEmpty().flatMap { it.artist }

    fun artist(server: SubsonicServer, id: String): Artist =
        call(server, "getArtist", mapOf("id" to id)).artist ?: throw SubsonicException(0, "艺术家不存在")

    fun favorites(server: SubsonicServer): List<Song> = call(server, "getStarred2").starred2?.song.orEmpty()
    fun setArtistFavorite(server: SubsonicServer, id: String, favorite: Boolean) {
        call(server, if (favorite) "star" else "unstar", mapOf("artistId" to id))
    }
    fun setFavorite(server: SubsonicServer, id: String, favorite: Boolean) {
        call(server, if (favorite) "star" else "unstar", mapOf("id" to id))
    }
    fun playlists(server: SubsonicServer): List<MusicPlaylist> = call(server, "getPlaylists").playlists?.playlist.orEmpty()
    fun playlist(server: SubsonicServer, id: String): MusicPlaylist =
        call(server, "getPlaylist", mapOf("id" to id)).playlist ?: throw SubsonicException(0, "歌单不存在")
    fun createPlaylist(server: SubsonicServer, name: String) {
        require(name.isNotBlank())
        call(server, "createPlaylist", mapOf("name" to name.trim()))
    }
    fun addToPlaylist(server: SubsonicServer, playlistId: String, songId: String) {
        call(server, "updatePlaylist", mapOf("playlistId" to playlistId, "songIdToAdd" to songId))
    }
    fun downloadUrl(server: SubsonicServer, id: String): String = authenticatedUrl(server, "download", mapOf("id" to id))

    fun randomSongs(server: SubsonicServer, size: Int = 50): List<Song> =
        call(server, "getRandomSongs", mapOf("size" to size.toString())).randomSongs?.song.orEmpty()

    fun search(server: SubsonicServer, query: String): SearchResult =
        call(server, "search3", mapOf("query" to query, "songCount" to "20", "albumCount" to "12", "artistCount" to "12"))
            .searchResult3 ?: SearchResult()

    fun searchPage(server: SubsonicServer, query: String, kind: String, offset: Int, size: Int = 40): SearchResult {
        require(kind in setOf("song", "album", "artist"))
        return call(server, "search3", mapOf("query" to query,
            "songCount" to if (kind == "song") size.toString() else "0",
            "albumCount" to if (kind == "album") size.toString() else "0",
            "artistCount" to if (kind == "artist") size.toString() else "0",
            "${kind}Offset" to offset.toString())).searchResult3 ?: SearchResult()
    }

    fun streamUrl(server: SubsonicServer, songId: String, maxBitRate: Int = 0): String = authenticatedUrl(server, "stream",
        if (maxBitRate > 0) mapOf("id" to songId, "format" to "mp3", "maxBitRate" to maxBitRate.toString())
        else mapOf("id" to songId, "format" to "raw"))

    fun coverArtUrl(server: SubsonicServer, coverArtId: String): String {
        val salt = SubsonicAuth.salt()
        return server.baseUrl.trimEnd('/').toHttpUrl().newBuilder()
            .addPathSegments("rest/getCoverArt.view")
            .addQueryParameter("id", coverArtId)
            .addQueryParameter("u", server.username)
            .addQueryParameter("t", SubsonicAuth.token(server.password, salt))
            .addQueryParameter("s", salt)
            .addQueryParameter("v", "1.16.1")
            .addQueryParameter("c", "Lee's Music")
            .addQueryParameter("size", "300")
            .also { }
            .build()
            .toString()
    }

    private fun authenticatedUrl(server: SubsonicServer, view: String, extra: Map<String, String>): String {
        val salt = SubsonicAuth.salt()
        return server.baseUrl.trimEnd('/').toHttpUrl().newBuilder()
            .addPathSegments("rest/$view.view")
            .addQueryParameter("u", server.username)
            .addQueryParameter("t", SubsonicAuth.token(server.password, salt))
            .addQueryParameter("s", salt)
            .addQueryParameter("v", "1.16.1")
            .addQueryParameter("c", "Lee's Music")
            .apply { extra.forEach { (key, value) -> addQueryParameter(key, value) } }
            .build()
            .toString()
    }

    private fun call(server: SubsonicServer, view: String, extra: Map<String, String> = emptyMap()): SubsonicBody {
        val salt = SubsonicAuth.salt()
        val url = server.baseUrl.trimEnd('/').toHttpUrl().newBuilder()
            .addPathSegments("rest/$view.view")
            .addQueryParameter("u", server.username)
            .addQueryParameter("t", SubsonicAuth.token(server.password, salt))
            .addQueryParameter("s", salt)
            .addQueryParameter("v", "1.16.1")
            .addQueryParameter("c", "Lee's Music")
            .addQueryParameter("f", "json")
            .apply { extra.forEach { (key, value) -> addQueryParameter(key, value) } }
            .build()
        val response = http.newCall(Request.Builder().url(url).build()).execute()
        val body = response.body.string()
        if (!response.isSuccessful) {
            throw SubsonicException(response.code, "HTTP ${response.code}")
        }
        val parsed = json.decodeFromString(SubsonicEnvelope.serializer(), body).response
        if (parsed.status != "ok") {
            val error = parsed.error
            throw SubsonicException(error?.code ?: 0, error?.message ?: "Subsonic 请求失败")
        }
        return parsed
    }
}
