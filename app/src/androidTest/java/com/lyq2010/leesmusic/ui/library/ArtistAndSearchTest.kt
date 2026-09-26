package com.lyq2010.leesmusic.ui.library

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.data.library.LibraryDownloads
import com.lyq2010.leesmusic.ui.search.*
import com.lyq2010.leesmusic.ui.shell.ShellTheme
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ArtistAndSearchTest {
    @get:Rule val compose = createComposeRule()

    @Test fun artistGridSupportsFavoriteWithoutNavigationAndRetainsViewAndFilter() {
        MockWebServer().use { server ->
            val state = LibraryPageState()
            runBlocking { state.load(0, 0) { LibraryPage(artists = listOf(Artist("a", "Alpha", 2), Artist("b", "Beta", 1))) } }
            var opened = 0
            val source = SubsonicServer(server.url("/").toString(), "lee", "secret")
            server.enqueue(MockResponse().setBody("""{"subsonic-response":{"status":"ok","version":"1.16.1"}}"""))
            compose.setContent { ShellTheme { ArtistBrowser(state, source, SubsonicClient(), OkHttpClient(), {}, { opened++ }, {}) } }
            compose.onNodeWithText("2 位艺术家").assertIsDisplayed()
            compose.onNodeWithContentDescription("收藏Alpha").performClick()
            compose.waitUntil(5000) { state.data?.artists?.first()?.starred != null }
            compose.runOnIdle { assertEquals(0, opened) }
            assertEquals("a", server.takeRequest().requestUrl?.queryParameter("artistId"))
            compose.onNodeWithContentDescription("切换列表视图").performClick()
            compose.onNode(hasSetTextAction()).performTextInput("Alpha")
            compose.onNodeWithText("Beta").assertDoesNotExist()
            compose.onNode(hasText("Alpha") and !hasSetTextAction()).performClick()
            compose.runOnIdle { assertEquals(1, opened); assertFalse(state.artistGrid); assertEquals("Alpha", state.filter) }
        }
    }

    @Test fun searchResultsHaveRealActionsAndCategorySwitchDoesNotMixResults() {
        MockWebServer().use { server ->
            val songRequests = AtomicInteger()
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val url = request.requestUrl!!
                    val result = when {
                        url.queryParameter("songCount") == "40" -> { songRequests.incrementAndGet(); """"song":[{"id":"s","title":"测试歌曲","artist":"测试艺人"}]""" }
                        url.queryParameter("albumCount") == "40" -> """"album":[{"id":"al","name":"测试专辑"}]"""
                        else -> """"artist":[{"id":"ar","name":"测试艺人","albumCount":2}]"""
                    }
                    return MockResponse().setBody("""{"subsonic-response":{"status":"ok","version":"1.16.1","searchResult3":{$result}}}""")
                }
            }
            val state = SearchState()
            var played = false
            var menu = false
            var album = false
            val source = SubsonicServer(server.url("/").toString(), "lee", "secret")
            compose.setContent {
                val context = LocalContext.current
                ShellTheme { SearchScreen(state, source, OkHttpClient(), remember { LibraryBrowseCache() }, remember { LibraryDownloads(context, "search-test") },
                    { album = true }, { _, _, start -> played = true; assertFalse(start) }, { menu = true }, {}) }
            }
            compose.onNode(hasSetTextAction()).performTextInput("测试")
            compose.waitUntil(5000) { compose.onAllNodesWithText("测试歌曲").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("测试歌曲的更多操作").performClick()
            compose.runOnIdle { assertTrue(menu); assertFalse(played) }
            compose.onNodeWithText("测试歌曲").performClick()
            compose.runOnIdle { assertTrue(played) }
            compose.onNodeWithText("专辑", substring = false).performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("测试专辑").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("测试歌曲").assertDoesNotExist()
            compose.onNodeWithText("测试专辑").performClick()
            compose.runOnIdle { assertTrue(album) }
            compose.onNodeWithText("艺术家", substring = false).performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("测试艺人").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("歌曲", substring = false).performClick()
            compose.onNodeWithText("测试歌曲").assertIsDisplayed()
            assertEquals(1, songRequests.get())
            compose.onNodeWithContentDescription("清空搜索").performClick()
            compose.onNodeWithText("测试歌曲").assertDoesNotExist()
        }
    }
}
