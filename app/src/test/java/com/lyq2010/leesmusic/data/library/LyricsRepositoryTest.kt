package com.lyq2010.leesmusic.data.library

import com.lyq2010.leesmusic.data.api.SubsonicClient
import com.lyq2010.leesmusic.data.api.SubsonicServer
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class LyricsRepositoryTest {
    @Test fun reopeningUsesCacheButAnotherAccountDoesNot() = runBlocking {
        MockWebServer().use { http ->
            repeat(2) { http.enqueue(MockResponse().setBody(
                """{"subsonic-response":{"status":"ok","version":"1.16.1","lyricsList":{"structuredLyrics":[{"synced":false,"line":[{"value":"line"}]}]}}}""")) }
            val repository = LyricsRepository(SubsonicClient())
            val server = SubsonicServer(http.url("/").toString(), "user", "secret")
            assertNotNull(repository.load(server, "1", "a", "t"))
            assertNotNull(repository.load(server, "1", "a", "t"))
            assertEquals(1, http.requestCount)
            assertNotNull(repository.load(server.copy(username = "other"), "1", "a", "t"))
            assertEquals(2, http.requestCount)
        }
    }
}
