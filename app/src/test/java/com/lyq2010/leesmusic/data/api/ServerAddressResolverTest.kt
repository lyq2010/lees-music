package com.lyq2010.leesmusic.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlinx.coroutines.runBlocking

class ServerAddressResolverTest {
    @Test
    fun returnsTheAddressWhenProbeSucceeds() = runBlocking {
        val resolved = ServerAddressResolver { _, _, _ -> true }
            .resolve("http://192.168.1.2:4533/", "lee", "secret")
        assertEquals("http://192.168.1.2:4533", resolved)
    }

    @Test
    fun rejectsABlankAddress() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking { ServerAddressResolver { _, _, _ -> true }.resolve("  ", "lee", "secret") }
        }
    }

    @Test
    fun rejectsWhenTheServerDoesNotAnswer() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking { ServerAddressResolver { _, _, _ -> false }.resolve("https://music.example", "lee", "secret") }
        }
    }
}
