package com.lyq2010.leesmusic.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.*
import androidx.media3.datasource.cache.*
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class PlaybackPipeline(
    context: Context,
    private val policy: PlaybackNetworkPolicy,
    private val cache: Cache = PlaybackCache.get(context),
    private val canFetch: () -> Boolean = { policy.online() && !policy.blocked() },
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { task -> Thread(task, "music-prefetch").apply { isDaemon = true } },
) {
    private val preferences = com.lyq2010.leesmusic.data.settings.PlaybackPreferences(context)
    private val client = OkHttpClient.Builder().protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true).build()
    private val prefetchClient = client.newBuilder().dispatcher(okhttp3.Dispatcher()).build()
    private val connections = PlaybackConnections(client, prefetchClient)
    private val generation = AtomicLong()
    @Volatile private var writer: CacheWriter? = null
    private var requested = emptyList<String>()
    private val keyFactory = CacheKeyFactory { spec -> spec.key ?: streamCacheKey(spec.uri.toString()) }
    private fun cached(http: OkHttpClient): CacheDataSource.Factory {
        val network = OkHttpDataSource.Factory(http).setUserAgent("LeesMusic/1.0")
        return CacheDataSource.Factory().setCache(cache).setCacheKeyFactory(keyFactory)
            .setUpstreamDataSourceFactory { policy.wrap(RangeFallbackDataSource(network.createDataSource())) }
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache).setFragmentSize(PLAYBACK_CACHE_FRAGMENT_BYTES))
    }
    // Local downloads, content URIs and test data URIs bypass the network cache entirely.
    // Only the prefetch worker writes: a paused/buffer-full playback loader must not hold
    // a cache hole lock that prevents the worker from downloading the rest of the song.
    val factory: DataSource.Factory = DefaultDataSource.Factory(context, cached(client).setCacheWriteDataSinkFactory(null))
    internal val prefetchFactory = cached(prefetchClient).setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE)

    fun fullyCached(item: MediaItem?): Boolean {
        val uri = item?.localConfiguration?.uri ?: return false
        if (uri.scheme !in setOf("http", "https")) return true
        val key = item.localConfiguration?.customCacheKey ?: streamCacheKey(uri.toString())
        val length = ContentMetadata.getContentLength(cache.getContentMetadata(key))
        return length >= 0 && cache.isCached(key, 0, length)
    }
    fun refreshConnections() { connections.refresh() }

    fun prefetch(items: List<MediaItem>, force: Boolean = false) {
        val remote = items.filter { it.localConfiguration?.uri?.scheme in setOf("http", "https") }
        val keys = remote.map { it.localConfiguration!!.customCacheKey ?: streamCacheKey(it.localConfiguration!!.uri.toString()) }
        PlaybackCache.setProtected(cache, keys.toSet())
        if (!force && requested == keys) return
        cancelPrefetch()
        requested = keys
        if (remote.isEmpty() || !canFetch()) return
        val token = generation.get()
        executor.execute {
            for (item in remote.distinctBy { it.localConfiguration!!.uri }) {
                var retries = 0
                while (generation.get() == token && canFetch()) {
                    if (fullyCached(item)) break
                    val spec = DataSpec.Builder().setUri(item.localConfiguration!!.uri)
                        .setKey(item.localConfiguration?.customCacheKey)
                        .setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION).build()
                    val task = CacheWriter(prefetchFactory.createDataSourceForDownloading(), spec, null) { length, _, _ ->
                        if (length > playbackCacheLimitBytes(preferences.playbackCacheMb) || generation.get() != token || !canFetch()) writer?.cancel()
                    }
                    writer = task
                    if (generation.get() != token) { task.cancel(); break }
                    val key = item.localConfiguration!!.customCacheKey ?: streamCacheKey(item.localConfiguration!!.uri.toString())
                    PlaybackCache.writerStarted(cache, key)
                    try { task.cache(); break }
                    catch (error: java.io.IOException) {
                        if (generation.get() != token || !canFetch() || !StreamRetry.isTransient(error) || retries >= StreamRetry.MAX) break
                        refreshConnections()
                        val until = android.os.SystemClock.elapsedRealtime() + StreamRetry.delayMs(retries++)
                        while (generation.get() == token && android.os.SystemClock.elapsedRealtime() < until) Thread.sleep(100)
                    } finally { PlaybackCache.writerFinished(cache, key); if (writer === task) writer = null }
                }
                // Current song takes priority; do not repeatedly refill an unavailable next song offline.
                if (generation.get() != token || !canFetch()) break
            }
        }
    }
    fun cancelPrefetch() {
        generation.incrementAndGet(); requested = emptyList()
        writer?.cancel(); connections.cancelPrefetch()
    }
    fun close() { cancelPrefetch(); PlaybackCache.setProtected(cache, emptySet()); executor.shutdown(); connections.close() }
}
