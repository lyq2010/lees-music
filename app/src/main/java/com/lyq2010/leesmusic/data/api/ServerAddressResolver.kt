package com.lyq2010.leesmusic.data.api

class ServerAddressResolver(
    private val probe: suspend (baseUrl: String, username: String, password: String) -> Boolean,
) {
    suspend fun resolve(url: String, username: String, password: String): String {
        val base = url.trim().trimEnd('/')
        if (base.isEmpty()) error("还没填服务器地址")
        if (!probe(base, username, password)) error("连不上 $base")
        return base
    }
}
