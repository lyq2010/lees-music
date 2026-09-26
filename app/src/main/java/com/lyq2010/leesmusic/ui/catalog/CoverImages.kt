package com.lyq2010.leesmusic.ui.catalog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object CoverImages {
    private val lock = Any()
    private var generation = 0
    data class Usage(val diskBytes: Long, val memoryBytes: Long)

    private fun cacheFiles(context: Context): List<File> {
        val root = File(context.cacheDir, "covers")
        require(root.canonicalFile.parentFile == context.cacheDir.canonicalFile)
        return root.listFiles().orEmpty().filter { it.isFile && it.canonicalFile.parentFile == root.canonicalFile }
    }

    fun usage(context: Context): Usage = synchronized(lock) {
        Usage(cacheFiles(context).sumOf { it.length() }, memory.size().toLong() * 1024)
    }

    /** Call only after the user confirms clearing artwork; downloads/settings are outside this directory. */
    fun clear(context: Context): Usage = synchronized(lock) {
        generation++
        memory.evictAll()
        var failed = false
        cacheFiles(context).forEach { if (!it.delete() && it.exists()) failed = true }
        check(!failed) { "部分封面缓存未能清理" }
        usage(context)
    }
    private val memory = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun peek(id: String): Bitmap? = memory.get(id)

    fun load(context: Context, id: String, url: String, http: OkHttpClient): Bitmap? {
        val started = synchronized(lock) { generation }
        memory.get(id)?.let { return it }
        val file = file(context, id)
        synchronized(lock) {
            if (file.exists() && file.length() > 0L) {
                BitmapFactory.decodeFile(file.absolutePath)?.let { cached ->
                    memory.put(id, cached)
                    return cached
                }
            }
        }
        val bytes = http.newCall(Request.Builder().url(url).build()).execute().body.bytes()
        if (bytes.isEmpty()) return null
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        synchronized(lock) {
            if (started == generation) {
                file.parentFile?.mkdirs()
                file.writeBytes(bytes)
                memory.put(id, bitmap)
            }
        }
        return bitmap
    }

    private fun file(context: Context, id: String): File {
        val safe = id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(context.cacheDir, "covers/$safe")
    }
}
