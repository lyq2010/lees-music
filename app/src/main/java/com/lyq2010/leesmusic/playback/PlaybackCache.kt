package com.lyq2010.leesmusic.playback

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal object PlaybackCache {
    @Volatile private var instance: SimpleCache? = null
    private var evictor: AdjustableCacheEvictor? = null
    private var protected = emptySet<String>()
    private val writers = mutableSetOf<String>()
    // One cache owner for the process, shared by both decks and the prefetch worker.
    fun get(context: Context): SimpleCache = instance ?: synchronized(this) {
        instance ?: run {
            val limit = playbackCacheLimitBytes(com.lyq2010.leesmusic.data.settings.PlaybackPreferences(context).playbackCacheMb)
            val policy = AdjustableCacheEvictor(limit).also { evictor = it }
            SimpleCache(File(context.applicationContext.cacheDir, "playback"), policy,
                StandaloneDatabaseProvider(context.applicationContext)).also { instance = it }
        }
    }
    fun setProtected(cache: androidx.media3.datasource.cache.Cache, keys: Set<String>) {
        if (cache === instance) synchronized(cache) { protected = keys }
    }
    fun writerStarted(cache: androidx.media3.datasource.cache.Cache, key: String) {
        if (cache === instance) synchronized(cache) { writers += key }
    }
    fun writerFinished(cache: androidx.media3.datasource.cache.Cache, key: String) {
        if (cache === instance) synchronized(cache) { writers -= key }
    }
    fun resize(context: Context, megabytes: Int) {
        val cache = get(context)
        evictor!!.resize(cache, playbackCacheLimitBytes(megabytes))
    }
    fun clearUnused(context: Context): Long {
        val cache = get(context)
        return synchronized(cache) {
            clearUnused(cache, protected + writers)
        }
    }
    internal fun clearUnused(cache: Cache, retained: Set<String>): Long = synchronized(cache) {
        cache.keys.filterNot { it in retained }.forEach { cache.removeResource(it) }
        cache.cacheSpace
    }
}
