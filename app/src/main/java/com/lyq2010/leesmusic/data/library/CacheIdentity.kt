package com.lyq2010.leesmusic.data.library

import okhttp3.HttpUrl.Companion.toHttpUrl
import java.security.MessageDigest

fun serverCacheIdentity(url: String, username: String, kind: String = "Navidrome"): String =
    digest(listOf(kind, url.toHttpUrl().toString().trimEnd('/'), username))

// Authentication salts change on every request; resource identity must not.
fun resourceCacheIdentity(url: String): String {
    val parsed = url.toHttpUrl()
    val base = parsed.newBuilder().query(null).fragment(null).build().toString()
    return digest(listOf(base, parsed.queryParameter("u").orEmpty(),
        parsed.queryParameter("id").orEmpty(), parsed.queryParameter("format").orEmpty(),
        parsed.queryParameter("size").orEmpty()))
}

private fun digest(parts: List<String>): String = MessageDigest.getInstance("SHA-256")
    .digest(parts.joinToString("") { "${it.length}:$it" }.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
