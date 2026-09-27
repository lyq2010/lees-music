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
    var rawHost = host.trim()
    var useHttps = https
    when {
        rawHost.startsWith("https://") -> {
            useHttps = true
            rawHost = rawHost.removePrefix("https://")
        }
        rawHost.startsWith("http://") -> {
            rawHost = rawHost.removePrefix("http://")
        }
    }
    rawHost = rawHost.substringBefore("/")
    val scheme = if (useHttps) "https" else "http"
    val portPart = port.trim().let { if (it.isEmpty()) "" else ":$it" }
    val pathPart = path.trim().let { raw ->
        when {
            raw.isEmpty() -> ""
            raw.startsWith("/") -> raw.trimEnd('/')
            else -> "/${raw.trimEnd('/')}"
        }
    }
    return "$scheme://$rawHost$portPart$pathPart"
}

fun normalizeServerUrl(url: String): String =
    url.trim().replace(Regex("^(https?)://https?://"), "$1://")

/** A pasted full URL updates every endpoint field, including its scheme. */
fun endpointFromHostInput(input: String, current: ServerEndpoint): ServerEndpoint {
    val value = input.trim()
    if (!value.startsWith("https://", ignoreCase = true) && !value.startsWith("http://", ignoreCase = true)) {
        return current.copy(host = input)
    }
    return runCatching { parseEndpoint(value, current.https) }
        .getOrNull()?.takeIf { it.host.isNotBlank() } ?: current.copy(host = input)
}

fun parseEndpoint(url: String, httpsDefault: Boolean): ServerEndpoint {
    if (url.isBlank()) return ServerEndpoint(https = httpsDefault)
    val uri = URI(normalizeServerUrl(url))
    val port = if (uri.port == -1) "" else uri.port.toString()
    return ServerEndpoint(
        host = uri.host.orEmpty(),
        port = port,
        path = uri.path.orEmpty().trim('/'),
        https = uri.scheme.equals("https", ignoreCase = true),
    )
}
