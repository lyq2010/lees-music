package com.lyq2010.leesmusic.ui

import com.lyq2010.leesmusic.playback.streamCacheKey
import org.junit.Assert.*
import org.junit.Test

class StreamCacheKeyTest {
    @Test fun authenticationRefreshReusesCacheWithoutMixingAccountsOrQualities() {
        val url = "https://music.example/rest/stream?id=1&u=user&format=raw&t=old&s=old"
        assertEquals(streamCacheKey(url), streamCacheKey(url.replace("old", "new")))
        assertNotEquals(streamCacheKey(url), streamCacheKey(url.replace("u=user", "u=other")))
        assertNotEquals(streamCacheKey(url), streamCacheKey(url.replace("raw", "mp3") + "&maxBitRate=128"))
        assertNotEquals(streamCacheKey(url + "&maxBitRate=128"), streamCacheKey(url + "&maxBitRate=320"))
        assertNotEquals(streamCacheKey(url), streamCacheKey(url.replace("music.example", "other.example")))
        assertEquals(64, streamCacheKey(url).length)
    }
}
