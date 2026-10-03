package com.lyq2010.leesmusic.playback

internal const val PLAYBACK_CACHE_FRAGMENT_BYTES = 2L * 1024 * 1024

// Keep the persisted selection values, but match Android's decimal MB/GB display.
internal fun playbackCacheLimitBytes(selection: Int): Long = when (selection) {
    512 -> 512_000_000L
    1024 -> 1_000_000_000L
    2048 -> 2_000_000_000L
    else -> error("Unsupported playback cache limit: $selection")
}
