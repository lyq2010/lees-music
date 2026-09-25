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
)

class SubsonicException(val code: Int, message: String) : Exception(message)
