package com.lyq2010.leesmusic.data.settings

import java.net.URI

data class ServerEndpoint(
    val host: String = "",
    val port: String = "",
    val path: String = "",
    val https: Boolean = false,
)

fun ServerEndpoint.toUrl(): String {
    if (host.isBlank()) return ""
    val scheme = if (https) "https" else "http"
    val portPart = port.trim().let { if (it.isEmpty()) "" else ":$it" }
    val pathPart = path.trim().let { raw ->
        when {
            raw.isEmpty() -> ""
            raw.startsWith("/") -> raw.trimEnd('/')
            else -> "/${raw.trimEnd('/')}"
        }
    }
    return "$scheme://${host.trim()}$portPart$pathPart"
}

fun parseEndpoint(url: String, httpsDefault: Boolean): ServerEndpoint {
    if (url.isBlank()) return ServerEndpoint(https = httpsDefault)
    val uri = URI(url)
    val port = if (uri.port == -1) "" else uri.port.toString()
    return ServerEndpoint(
        host = uri.host.orEmpty(),
        port = port,
        path = uri.path.orEmpty().trim('/'),
        https = uri.scheme.equals("https", ignoreCase = true),
    )
}
