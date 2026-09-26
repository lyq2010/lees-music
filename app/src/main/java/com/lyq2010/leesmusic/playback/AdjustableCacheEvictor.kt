package com.lyq2010.leesmusic.playback

import androidx.media3.datasource.cache.*

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class AdjustableCacheEvictor(bytes: Long) : CacheEvictor {
    private var delegate = LeastRecentlyUsedCacheEvictor(bytes)
    fun resize(cache: Cache, bytes: Long) = synchronized(cache) {
        val spans = cache.keys.flatMap { cache.getCachedSpans(it) }.sortedBy { it.lastTouchTimestamp }
        delegate = LeastRecentlyUsedCacheEvictor(bytes)
        spans.forEach { delegate.onSpanAdded(cache, it) }
    }
    override fun requiresCacheSpanTouches() = true
    override fun onCacheInitialized() = delegate.onCacheInitialized()
    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) = delegate.onStartFile(cache, key, position, length)
    override fun onSpanAdded(cache: Cache, span: CacheSpan) = delegate.onSpanAdded(cache, span)
    override fun onSpanRemoved(cache: Cache, span: CacheSpan) = delegate.onSpanRemoved(cache, span)
    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) = delegate.onSpanTouched(cache, oldSpan, newSpan)
}
