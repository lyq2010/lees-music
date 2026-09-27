package com.lyq2010.leesmusic.data.api

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking

class SearchAndArtistApiTest {
    @Test fun artistFavoriteUsesArtistIdAndArtworkMetadataIsPreserved() = runBlocking {
        MockWebServer().use { server ->
            val source = SubsonicServer(server.url("/").toString(), "lee", "secret")
            val client = SubsonicClient()
            server.enqueue(MockResponse().setBody("""{"subsonic-response":{"status":"ok","version":"1.16.1","artists":{"index":[{"artist":[{"id":"a","name":"Artist","coverArt":"ar-a","artistImageUrl":"https://example.org/a.jpg","starred":"date","albumCount":2}]}]}}}"""))
            val artist = client.artists(source).single()
            assertEquals("ar-a", artist.coverArt)
            assertEquals("date", artist.starred)
            assertNull(artist.songCount)
            server.takeRequest()
            for (favorite in listOf(true, false)) {
                server.enqueue(MockResponse().setBody("""{"subsonic-response":{"status":"ok","version":"1.16.1"}}"""))
                client.setArtistFavorite(source, "a&b", favorite)
                val url = server.takeRequest().requestUrl!!
                assertEquals("a&b", url.queryParameter("artistId"))
                assertNull(url.queryParameter("id"))
                assertTrue(url.encodedPath.endsWith(if (favorite) "/star.view" else "/unstar.view"))
            }
        }
    }

    @Test fun searchPaginationRequestsOnlySelectedCategoryAndEncodesQuery() = runBlocking {
        MockWebServer().use { server ->
            val source = SubsonicServer(server.url("/").toString(), "lee", "secret")
            for (kind in listOf("song", "album", "artist")) {
                server.enqueue(MockResponse().setBody("""{"subsonic-response":{"status":"ok","version":"1.16.1","searchResult3":{}}}"""))
                SubsonicClient().searchPage(source, "a & 中文", kind, 40)
                val url = server.takeRequest().requestUrl!!
                assertEquals("a & 中文", url.queryParameter("query"))
                assertEquals("40", url.queryParameter("${kind}Offset"))
                for (candidate in listOf("song", "album", "artist")) assertEquals(if (candidate == kind) "40" else "0", url.queryParameter("${candidate}Count"))
            }
        }
    }

    @Test fun originalQualityDisablesTranscodingAndCompressedQualityRequestsBitrate() {
        val source = SubsonicServer("https://example.org/music", "lee", "secret")
        val client = SubsonicClient()
        val raw = client.streamUrl(source, "song").toHttpUrl()
        assertEquals("raw", raw.queryParameter("format"))
        assertNull(raw.queryParameter("maxBitRate"))
        val compressed = client.streamUrl(source, "song", 192).toHttpUrl()
        assertEquals("mp3", compressed.queryParameter("format"))
        assertEquals("192", compressed.queryParameter("maxBitRate"))
    }
}
