package com.lyq2010.leesmusic.ui.catalog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.lyq2010.leesmusic.data.api.awaitBytes
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.max

object CoverImages {
    private const val MAX_BYTES = 5 * 1024 * 1024
    private const val MAX_EDGE = 600
    private const val MAX_DISK_BYTES = 96L * 1024 * 1024
    private val lock = Any()
    private val keyLocks = Array(32) { Mutex() }
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

    suspend fun load(context: Context, id: String, url: String, http: OkHttpClient): Bitmap? =
        keyLocks[(id.hashCode() and Int.MAX_VALUE) % keyLocks.size].withLock {
            val started = synchronized(lock) { generation }
            memory.get(id)?.let { return@withLock it }
            val file = file(context, id)
            val cached = synchronized(lock) {
                if (file.exists() && file.length() in 1..MAX_BYTES.toLong()) {
                    decode(file.readBytes())?.also { memory.put(id, it) }
                } else null
            }
            if (cached != null) return@withLock cached
            val call = http.newCall(Request.Builder().url(url).build())
            call.timeout().timeout(15, TimeUnit.SECONDS)
            val bytes = call.awaitBytes(MAX_BYTES) ?: return@withLock null
            if (bytes.isEmpty()) return@withLock null
            val bitmap = decode(bytes) ?: return@withLock null
            synchronized(lock) {
                if (started == generation) {
                    val used = cacheFiles(context).sumOf { it.length() }
                    if (used - file.length() + bytes.size <= MAX_DISK_BYTES) {
                        file.parentFile?.mkdirs()
                        file.writeBytes(bytes)
                    }
                    memory.put(id, bitmap)
                }
            }
            bitmap
        }

    private fun decode(bytes: ByteArray): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        var sample = 1
        while (max(options.outWidth, options.outHeight) / sample > MAX_EDGE && sample < 1 shl 30) sample *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sample
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun file(context: Context, id: String): File {
        val safe = id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(context.cacheDir, "covers/$safe")
    }
}
