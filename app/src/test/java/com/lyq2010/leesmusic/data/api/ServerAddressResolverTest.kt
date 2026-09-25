package com.lyq2010.leesmusic.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAddressResolverTest {
    @Test
    fun usesLanWhenProbeSucceeds() {
        val resolved = resolver(lanOk = true).resolve("http://192.168.1.2:4533", "https://music.example", "lee", "secret")
        assertTrue(resolved.usingLan)
        assertEquals("http://192.168.1.2:4533", resolved.baseUrl)
    }

    @Test
    fun fallsBackToWanWhenLanProbeFails() {
        val resolved = resolver(lanOk = false).resolve("http://192.168.1.2:4533", "https://music.example/", "lee", "secret")
        assertFalse(resolved.usingLan)
        assertEquals("https://music.example", resolved.baseUrl)
    }

    @Test
    fun rejectsWanThatIsNotHttps() {
        assertThrows(IllegalArgumentException::class.java) {
            resolver(lanOk = false).resolve("", "http://music.example", "lee", "secret")
        }
    }

    private fun resolver(lanOk: Boolean) = ServerAddressResolver { _, _, _ -> lanOk }
}
