package com.lyq2010.leesmusic.data.api

import com.lyq2010.leesmusic.ui.library.readLibraryPages
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class LibraryApiTest {
    private fun response(fields: String) = MockResponse().setBody("""{"subsonic-response":{"status":"ok","version":"1.16.1",$fields}}""")
    private fun MockWebServer.config() = SubsonicServer(url("/music").toString(), "lee", "secret")

    @Test fun allSongsContinuesAfterAShortPageAndKeepsMetadata() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(response(""""searchResult3":{"song":[{"id":"a","title":"A","albumId":"album","artistId":"artist","starred":"date"}]}"""))
            server.enqueue(response(""""searchResult3":{"song":[{"id":"b","title":"B"}]}"""))
            server.enqueue(response(""""searchResult3":{}"""))
            val client = SubsonicClient()
            val songs = readLibraryPages<Song>({ it.id }) { client.songs(server.config(), it) }
            assertEquals(listOf("a", "b"), songs.map { it.id })
            assertEquals("album", songs.first().albumId)
            for (offset in 0..2) {
                val url = server.takeRequest().requestUrl!!
                assertEquals("", url.queryParameter("query"))
                assertEquals(offset.toString(), url.queryParameter("songOffset"))
                assertEquals("0", url.queryParameter("albumCount"))
            }
        }
    }

    @Test fun repeatedPageFailsInsteadOfLoopingForever() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking { readLibraryPages<Song>({ it.id }) { listOf(Song("a", "A")) } }
        }
    }

    @Test fun playlistsDistinguishOwnerAndEntriesKeepDuplicates() {
        MockWebServer().use { server ->
            server.enqueue(response(""""playlists":{"playlist":[{"id":"p","name":"我的","owner":"lee"},{"id":"q","name":"共享","owner":"other","public":true}]}"""))
            server.enqueue(response(""""playlist":{"id":"p","name":"我的","entry":[{"id":"a","title":"A"},{"id":"a","title":"A"}]}"""))
            val client = SubsonicClient()
            assertEquals(listOf("lee", "other"), client.playlists(server.config()).map { it.owner })
            assertEquals(2, client.playlist(server.config(), "p").entry.size)
        }
    }

    @Test fun mutationsUseCorrectIdsAndDoNotReplacePlaylist() {
        MockWebServer().use { server ->
            repeat(4) { server.enqueue(MockResponse().setBody("""{"subsonic-response":{"status":"ok","version":"1.16.1"}}""")) }
            val client = SubsonicClient()
            client.setFavorite(server.config(), "song&1", true)
            client.setFavorite(server.config(), "song&1", false)
            client.addToPlaylist(server.config(), "p", "song&1")
            client.createPlaylist(server.config(), "  通勤  ")
            assertEquals("/music/rest/star.view", server.takeRequest().requestUrl!!.encodedPath)
            assertEquals("/music/rest/unstar.view", server.takeRequest().requestUrl!!.encodedPath)
            val update = server.takeRequest().requestUrl!!
            assertEquals("song&1", update.queryParameter("songIdToAdd"))
            assertEquals("p", update.queryParameter("playlistId"))
            assertNull(update.queryParameter("songIndexToRemove"))
            assertEquals("通勤", server.takeRequest().requestUrl!!.queryParameter("name"))
        }
    }

    @Test fun artistAlbumsAndFavoritesParseActualResponses() {
        MockWebServer().use { server ->
            server.enqueue(response(""""artists":{"index":[{"name":"A","artist":[{"id":"ar","name":"艺人","albumCount":2}]}]}"""))
            server.enqueue(response(""""artist":{"id":"ar","name":"艺人","album":[{"id":"al","name":"专辑"}]}"""))
            server.enqueue(response(""""starred2":{"song":[{"id":"s","title":"收藏"}]}"""))
            val client = SubsonicClient()
            assertEquals(2, client.artists(server.config()).single().albumCount)
            assertEquals("al", client.artist(server.config(), "ar").album.single().id)
            assertEquals("s", client.favorites(server.config()).single().id)
        }
    }
}
