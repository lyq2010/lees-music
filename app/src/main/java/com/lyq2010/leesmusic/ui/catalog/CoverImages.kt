package com.lyq2010.leesmusic.ui.catalog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object CoverImages {
    private val memory = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun peek(id: String): Bitmap? = memory.get(id)

    fun load(context: Context, id: String, url: String, http: OkHttpClient): Bitmap? {
        memory.get(id)?.let { return it }
        val file = file(context, id)
        if (file.exists() && file.length() > 0L) {
            BitmapFactory.decodeFile(file.absolutePath)?.let { cached ->
                memory.put(id, cached)
                return cached
            }
        }
        val bytes = http.newCall(Request.Builder().url(url).build()).execute().body.bytes()
        if (bytes.isEmpty()) return null
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        memory.put(id, bitmap)
        return bitmap
    }

    private fun file(context: Context, id: String): File {
        val safe = id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(context.cacheDir, "covers/$safe")
    }
}
