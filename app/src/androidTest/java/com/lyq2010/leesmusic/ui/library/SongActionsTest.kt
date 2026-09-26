package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.data.library.LibraryDownloads
import com.lyq2010.leesmusic.ui.catalog.*
import com.lyq2010.leesmusic.ui.theme.LeesTheme
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class SongActionsTest {
    @get:Rule val compose = createComposeRule()
    private val song = LibrarySong("song", "测试歌", "艺人", null, null)

    @Test fun favoriteFailureDoesNotDismissOrReportSuccessAndCanRetry() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"subsonic-response":{"status":"ok","version":"1.16.1","starred2":{}}}"""))
            server.enqueue(MockResponse().setResponseCode(500))
            server.enqueue(MockResponse().setBody("""{"subsonic-response":{"status":"ok","version":"1.16.1"}}"""))
            var dismissed = false
            var changed = 0
            var message = ""
            compose.setContent { LeesTheme {
                SongActions(song, SubsonicServer(server.url("/").toString(), "lee", "secret"), SubsonicClient(),
                    LibraryDownloads(LocalContext.current, "interaction-test"),
                    { dismissed = true }, {}, { _, _ -> true }, { changed++ }, { message = it })
            } }
            compose.waitUntil(5000) { server.requestCount >= 1 && compose.onAllNodesWithText("收藏歌曲").fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(5000) { runCatching { compose.onNodeWithText("收藏歌曲").assertIsEnabled() }.isSuccess }
            compose.onNodeWithText("收藏歌曲").performScrollTo().performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("操作失败，未确认成功，请重试").fetchSemanticsNodes().isNotEmpty() }
            compose.runOnIdle { assertFalse(dismissed); assertEquals(0, changed); assertEquals("", message) }
            compose.onNodeWithText("收藏歌曲").performScrollTo().performClick()
            compose.waitUntil(5000) { dismissed }
            compose.runOnIdle { assertEquals(1, changed); assertEquals("已收藏", message) }
            assertEquals("/rest/getStarred2.view", server.takeRequest(2, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath)
            repeat(2) { assertEquals("/rest/star.view", server.takeRequest(2, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath) }
        }
    }
}
