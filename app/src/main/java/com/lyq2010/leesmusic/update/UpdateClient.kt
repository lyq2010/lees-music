package com.lyq2010.leesmusic.update

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class AvailableUpdate(val manifest: UpdateManifest, val mirrors: List<HttpUrl>)

class UpdateClient(private val bases: List<String>, private val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
    .callTimeout(5, TimeUnit.MINUTES).followSslRedirects(false).build()) {
    private val json = Json { ignoreUnknownKeys = true }

    fun check(installed: Long, sdk: Int): AvailableUpdate? {
        val found = bases.mapNotNull { base ->
            runCatching {
                val root = (base.trimEnd('/') + "/").toHttpUrl()
                require(root.isHttps)
                val url = root.resolve("latest-lees-music.json")!!
                val request = Request.Builder().url(url).header("Cache-Control", "no-cache").build()
                http.newBuilder().callTimeout(15, TimeUnit.SECONDS).build().newCall(request).execute().use { response ->
                    check(response.isSuccessful)
                    val buffer = java.io.ByteArrayOutputStream()
                    response.body.byteStream().use { input ->
                        val bytes = ByteArray(4096)
                        while (true) {
                            val count = input.read(bytes)
                            if (count < 0) break
                            require(buffer.size() + count <= 65536)
                            buffer.write(bytes, 0, count)
                        }
                    }
                    val manifest = json.decodeFromString<UpdateManifest>(buffer.toString("UTF-8"))
                    manifest.validate()
                    root to manifest
                }
            }.getOrNull()
        }
        check(found.isNotEmpty()) { "暂时无法检查更新，请稍后重试" }
        val newest = found.filter { it.second.minSdk <= sdk }.maxByOrNull { it.second.versionCode } ?: return null
        if (newest.second.versionCode <= installed) return null
        return AvailableUpdate(newest.second, found.filter { it.second == newest.second }.map { it.first.resolve(it.second.apk)!! })
    }

    fun download(update: AvailableUpdate, destination: File, progress: (Long, Long) -> Unit) {
        val expected = update.manifest
        for (url in update.mirrors) {
            try {
                http.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    check(response.isSuccessful)
                    val digest = MessageDigest.getInstance("SHA-256")
                    var count = 0L
                    response.body.byteStream().use { input -> destination.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            count += read
                            check(count <= expected.size)
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                            progress(count, expected.size)
                        }
                    } }
                    check(count == expected.size)
                    val hash = digest.digest().joinToString("") { "%02x".format(it) }
                    check(hash.equals(expected.sha256, true))
                }
                return
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: Exception) { /* Try the other independently validated mirror. */ }
        }
        error("下载失败或安装包校验失败，请重试")
    }
}
