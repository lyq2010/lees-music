package com.lyq2010.leesmusic.playback

import okhttp3.HttpUrl.Companion.toHttpUrl
import java.security.MessageDigest

internal fun streamCacheKey(url: String): String {
    val parsed = url.toHttpUrl()
    val normalized = parsed.newBuilder().query(null).fragment(null).apply {
        parsed.queryParameterNames.sorted().filterNot { it in setOf("t", "s", "p", "v", "c", "f") }.forEach { name ->
            parsed.queryParameterValues(name).sortedBy { it.orEmpty() }.forEach { addQueryParameter(name, it) }
        }
    }.build().toString()
    return MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray()).joinToString("") { "%02x".format(it) }
}
