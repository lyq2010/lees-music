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

    fun albums(server: SubsonicServer, type: String): List<Album> =
        call(server, "getAlbumList2", mapOf("type" to type, "size" to "20")).albumList2?.album.orEmpty()

    fun search(server: SubsonicServer, query: String): SearchResult =
        call(server, "search3", mapOf("query" to query, "songCount" to "20", "albumCount" to "12", "artistCount" to "12"))
            .searchResult3 ?: SearchResult()

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
