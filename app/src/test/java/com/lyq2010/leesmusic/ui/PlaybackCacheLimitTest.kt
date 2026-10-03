package com.lyq2010.leesmusic.ui

import com.lyq2010.leesmusic.playback.playbackCacheLimitBytes
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackCacheLimitTest {
    @Test fun limitsMatchTheDisplayedDecimalUnits() {
        assertEquals(512_000_000L, playbackCacheLimitBytes(512))
        assertEquals(1_000_000_000L, playbackCacheLimitBytes(1024))
        assertEquals(2_000_000_000L, playbackCacheLimitBytes(2048))
    }
}
