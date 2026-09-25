package com.lyq2010.leesmusic.data.api

data class ResolvedServer(
    val baseUrl: String,
    val usingLan: Boolean,
)

class ServerAddressResolver(
    private val probe: (baseUrl: String, username: String, password: String) -> Boolean,
) {
    fun resolve(lanUrl: String, wanUrl: String, username: String, password: String): ResolvedServer {
        val lan = lanUrl.trim().trimEnd('/')
        if (lan.isNotEmpty() && probe(lan, username, password)) {
            return ResolvedServer(lan, usingLan = true)
        }
        val wan = wanUrl.trim().trimEnd('/')
        if (wan.isEmpty()) error("内网没有响应，外网地址还没填")
        require(wan.startsWith("https://")) { "外网地址必须是 https" }
        return ResolvedServer(wan, usingLan = false)
    }
}
