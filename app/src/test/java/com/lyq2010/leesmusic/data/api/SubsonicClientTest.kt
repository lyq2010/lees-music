package com.lyq2010.leesmusic.data.api

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SubsonicClientTest {
    @Test
    fun tokenIsMd5OfPasswordPlusSalt() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", SubsonicAuth.token("abc", ""))
    }

    @Test
    fun pingReadsOkResponse() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"subsonic-response":{"status":"ok","version":"1.16.1"}}"""))
        server.start()
        try {
            val body = SubsonicClient().ping(server.config())
            val request = server.takeRequest()
            assertEquals("ok", body.status)
            assertEquals("/rest/ping.view", request.requestUrl!!.encodedPath)
            assertEquals("lee", request.requestUrl!!.queryParameter("u"))
            assertEquals("json", request.requestUrl!!.queryParameter("f"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun failedStatusThrowsSubsonicException() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """{"subsonic-response":{"status":"failed","version":"1.16.1","error":{"code":40,"message":"Wrong username or password"}}}""",
            ),
        )
        server.start()
        try {
            val error = assertThrows(SubsonicException::class.java) {
                SubsonicClient().ping(server.config())
            }
            assertEquals(40, error.code)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun newestAlbumsParsesAlbumList() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """{"subsonic-response":{"status":"ok","version":"1.16.1","albumList2":{"album":[{"id":"1","name":"夜航","artist":"林歌"}]}}}""",
            ),
        )
        server.start()
        try {
            val albums = SubsonicClient().newestAlbums(server.config())
            assertEquals("夜航", albums.single().name)
            assertEquals("newest", server.takeRequest().requestUrl!!.queryParameter("type"))
        } finally {
            server.shutdown()
        }
    }

    private fun MockWebServer.config() = SubsonicServer(url("/").toString().trimEnd('/'), "lee", "secret")
}
