package com.lyq2010.leesmusic.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import com.lyq2010.leesmusic.data.api.SubsonicClient
import com.lyq2010.leesmusic.data.api.SubsonicServer
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.io.IOException

class ServerEndpointTest {
    @Test fun pastedHttpsUrlReplacesSchemePortAndPath() {
        val initial = ServerEndpoint("old.local", "4040", "old", false)
        val result = endpointFromHostInput("https://music.example:8443/navidrome/", initial)
        assertEquals(ServerEndpoint("music.example", "8443", "navidrome", true), result)
        assertEquals("https://music.example:8443/navidrome", result.toUrl())
    }

    @Test fun pastedHttpUrlDoesNotKeepPreviousHttpsSelection() {
        val result = endpointFromHostInput("http://music.example:4533/music", ServerEndpoint(https = true))
        assertEquals("http://music.example:4533/music", result.toUrl())
    }

    @Test fun plainHostKeepsOtherFields() {
        val previous = ServerEndpoint("old.local", "8443", "navidrome", true)
        assertEquals(previous.copy(host = "new.local"), endpointFromHostInput("new.local", previous))
    }

    @Test fun pastedHttpsUrlCreatesHttpsFirstRequest() = runBlocking {
        val endpoint = endpointFromHostInput("https://music.example:8443/navidrome", ServerEndpoint())
        var firstUrl = ""
        val client = SubsonicClient(OkHttpClient.Builder().addInterceptor { chain ->
            firstUrl = chain.request().url.toString()
            throw IOException("request recorded before network")
        }.build())
        assertThrows(IOException::class.java) {
            runBlocking { client.ping(SubsonicServer(endpoint.toUrl(), "lee", "secret")) }
        }
        assertTrue(firstUrl.startsWith("https://music.example:8443/navidrome/rest/ping.view?"))
    }
}
