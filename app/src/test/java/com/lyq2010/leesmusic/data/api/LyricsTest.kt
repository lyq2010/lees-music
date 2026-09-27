package com.lyq2010.leesmusic.data.api

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking

class LyricsTest {
    @Test fun timingHandlesOffsetsAndBackwardSeek() {
        val lyrics = StructuredLyrics(true, 100, listOf(LyricLine("a", 1000), LyricLine("b", 2000)))
        assertEquals(-1, lyrics.activeLine(899))
        assertEquals(0, lyrics.activeLine(900))
        assertEquals(1, lyrics.activeLine(2500))
        assertEquals(0, lyrics.activeLine(1200))
        assertEquals(-1, lyrics.copy(offset = -100).activeLine(1000))
        assertEquals(0, lyrics.copy(offset = -100).activeLine(1100))
        assertEquals(-1, lyrics.copy(synced = false).activeLine(2500))
    }

    @Test fun prefersSyncedLyricsAndUsesSongId() = runBlocking {
        MockWebServer().use { http ->
            http.enqueue(ok(""""lyricsList":{"structuredLyrics":[{"synced":false,"line":[{"value":"plain"}]},{"synced":true,"offset":-100,"line":[{"start":1000,"value":"同步"}]}]}"""))
            val result = SubsonicClient().lyrics(http.config(), "song/1", "artist", "title")!!
            assertTrue(result.synced)
            assertEquals("同步", result.line.single().value)
            assertEquals(-100L, result.offset)
            assertEquals("song/1", http.takeRequest().requestUrl!!.queryParameter("id"))
            assertEquals(1, http.requestCount)
        }
    }

    @Test fun unsupportedEndpointFallsBackToArtistAndTitle() = runBlocking {
        MockWebServer().use { http ->
            http.enqueue(MockResponse().setResponseCode(404))
            http.enqueue(ok(""""lyrics":{"value":"第一行\n第二行"}"""))
            val result = SubsonicClient().lyrics(http.config(), "1", "歌手", "歌名")!!
            assertFalse(result.synced)
            assertEquals(2, result.line.size)
            http.takeRequest()
            val request = http.takeRequest().requestUrl!!
            assertEquals("/rest/getLyrics.view", request.encodedPath)
            assertEquals("歌手", request.queryParameter("artist"))
            assertEquals("歌名", request.queryParameter("title"))
        }
    }

    @Test fun emptyLyricsReturnNull() = runBlocking {
        MockWebServer().use { http ->
            http.enqueue(ok(""""lyricsList":{"structuredLyrics":[]}"""))
            http.enqueue(ok(""""lyrics":{}"""))
            assertNull(SubsonicClient().lyrics(http.config(), "1", "a", "t"))
        }
    }

    @Test fun authenticationFailureDoesNotFallBack() = runBlocking {
        MockWebServer().use { http ->
            http.enqueue(MockResponse().setBody("""{"subsonic-response":{"status":"failed","version":"1.16.1","error":{"code":40,"message":"Denied"}}}"""))
            assertThrows(SubsonicException::class.java) {
                runBlocking { SubsonicClient().lyrics(http.config(), "1", "a", "t") }
            }
            assertEquals(1, http.requestCount)
        }
    }

    private fun ok(fields: String) = MockResponse().setBody(
        """{"subsonic-response":{"status":"ok","version":"1.16.1",$fields}}""",
    )
    private fun MockWebServer.config() = SubsonicServer(url("/").toString(), "lee", "secret")
}
