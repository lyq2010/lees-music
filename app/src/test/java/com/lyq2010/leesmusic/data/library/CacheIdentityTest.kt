package com.lyq2010.leesmusic.data.library

import org.junit.Assert.*
import org.junit.Test

class CacheIdentityTest {
    @Test fun saltsAndTokensDoNotChangeResourceIdentity() {
        assertEquals(resourceCacheIdentity("https://host/music/rest/stream?id=1&u=lee&t=old&s=a&format=raw"),
            resourceCacheIdentity("https://host/music/rest/stream?id=1&u=lee&t=new&s=b&format=raw"))
    }

    @Test fun serverUserPathAndResourceAreIsolated() {
        val original = "https://host/music/rest/stream?id=1&u=lee&format=raw"
        for (other in listOf(original.replace("host", "other"), original.replace("music", "other"),
            original.replace("lee", "other"), original.replace("id=1", "id=2"))) {
            assertNotEquals(resourceCacheIdentity(original), resourceCacheIdentity(other))
        }
    }

    @Test fun shelfIdentityIsCanonicalAndDoesNotExposeAccount() {
        val identity = serverCacheIdentity("https://HOST:443/music/", "lee")
        assertEquals(identity, serverCacheIdentity("https://host/music", "lee"))
        assertTrue(identity.matches(Regex("[a-f0-9]{64}")))
        assertNotEquals(identity, serverCacheIdentity("https://host/music", "other"))
        assertNotEquals(identity, serverCacheIdentity("https://host/music", "lee", "Emby"))
    }
}
