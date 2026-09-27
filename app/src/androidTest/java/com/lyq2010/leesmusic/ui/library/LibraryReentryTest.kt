package com.lyq2010.leesmusic.ui.library

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.data.library.LibraryDownloads
import com.lyq2010.leesmusic.ui.shell.ShellTheme
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class LibraryReentryTest {
    @get:Rule val compose = createComposeRule()
    @Test fun homeRequestReturnsFromLibrarySubpage() {
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = MockResponse().setBody(
                    """{"subsonic-response":{"status":"ok","version":"1.16.1","starred2":{},"playlists":{"playlist":[]},"searchResult3":{}}}""")
            }
            val source = SubsonicServer(server.url("/").toString(), "lee", "secret")
            val home = mutableIntStateOf(0)
            compose.setContent {
                val context = LocalContext.current
                val cache = remember { LibraryBrowseCache() }
                val downloads = remember { LibraryDownloads(context, "home-test") }
                ShellTheme { LibraryFeature(source, "Test", emptyList(), remember { OkHttpClient() }, 0,
                    downloads, cache, {}, {}, { _, _, _ -> }, {}, {}, homeRequest = home.intValue) }
            }
            compose.onNodeWithText("歌曲").performClick()
            compose.onNodeWithText("歌曲").assertExists()
            compose.runOnIdle { home.intValue++ }
            compose.waitForIdle()
            compose.onNodeWithText("音乐库").assertExists()
        }
    }

    @Test fun deletingPlaylistRequiresConfirmation() {
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = MockResponse().setBody(
                    """{"subsonic-response":{"status":"ok","version":"1.16.1","starred2":{},"playlists":{"playlist":[{"id":"p","name":"测试歌单","owner":"lee"}]}}}""")
            }
            val source = SubsonicServer(server.url("/").toString(), "lee", "secret")
            compose.setContent {
                val context = LocalContext.current
                val cache = remember { LibraryBrowseCache() }
                val downloads = remember { LibraryDownloads(context, "delete-test") }
                ShellTheme { LibraryFeature(source, "Test", emptyList(), remember { OkHttpClient() }, 0,
                    downloads, cache, {}, {}, { _, _, _ -> }, {}, {}) }
            }
            compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("删除歌单测试歌单").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("删除歌单测试歌单").performClick()
            compose.onNodeWithText("确定删除“测试歌单”？此操作会删除服务器上的歌单，不会删除歌曲文件。").assertExists()
            assertEquals(2, server.requestCount)
            compose.onNodeWithText("取消").performClick()
            assertEquals(2, server.requestCount)
            compose.onNodeWithContentDescription("分享歌单测试歌单").performClick()
            compose.onNodeWithText("将为“测试歌单”生成无需登录的公开链接；有效期由服务器设置。").assertExists()
            compose.onNodeWithText("取消").performClick()
            assertEquals(2, server.requestCount)
        }
    }
    @Test fun returningHomeReusesConfirmedEmptyPlaylistsWithoutLoadingFlash() {
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = MockResponse().setBody(
                    """{"subsonic-response":{"status":"ok","version":"1.16.1","starred2":{},"playlists":{"playlist":[]}}}""")
            }
            val source = SubsonicServer(server.url("/").toString(), "lee", "secret")
            val visible = mutableStateOf(true)
            compose.setContent {
                val context = LocalContext.current
                val cache = remember { LibraryBrowseCache() }
                val http = remember { OkHttpClient() }
                val downloads = remember { LibraryDownloads(context, "overview-reentry") }
                ShellTheme {
                    if (visible.value) LibraryFeature(source, "Test",
                        emptyList(), http, 0, downloads, cache, {}, {}, { _, _, _ -> }, {}, {})
                }
            }
            compose.waitUntil(5000) { compose.onAllNodesWithText("还没有歌单").fetchSemanticsNodes().isNotEmpty() }
            val requests = server.requestCount
            repeat(3) {
                compose.runOnIdle { visible.value = false }
                compose.waitForIdle()
                compose.runOnIdle { visible.value = true }
                compose.waitForIdle()
                compose.onNodeWithText("还没有歌单").assertExists()
                compose.onNodeWithText("正在读取歌单").assertDoesNotExist()
            }
            assertEquals(2, requests)
            assertEquals(requests, server.requestCount)
        }
    }

    @Test fun reopeningSongsKeepsDataFilterAndDoesNotRequestAgain() {
        MockWebServer().use { server ->
            val firstPages = AtomicInteger()
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val first = request.requestUrl?.queryParameter("songOffset") == "0"
                    if (first) firstPages.incrementAndGet()
                    val songs = if (first) """[{"id":"a","title":"测试歌曲"},{"id":"b","title":"另一首"}]""" else "[]"
                    return MockResponse().setBody("""{"subsonic-response":{"status":"ok","version":"1.16.1","searchResult3":{"song":$songs}}}""")
                }
            }
            val visible = mutableStateOf(true)
            val source = SubsonicServer(server.url("/").toString(), "lee", "secret")
            compose.setContent {
                val context = LocalContext.current
                val client = remember { SubsonicClient() }
                val http = remember { OkHttpClient() }
                val cache = remember { LibraryBrowseCache() }
                val downloads = remember { LibraryDownloads(context, "reentry-test") }
                ShellTheme {
                    if (visible.value) LibraryBrowser(LibraryDestination("songs", "歌曲"), source, client, http,
                        0, downloads, cache, {}, {}, {}, { _, _, _ -> }, {})
                }
            }
            compose.waitUntil(5000) { compose.onAllNodesWithText("测试歌曲").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("排序").performClick()
            compose.onNodeWithText("标题 A–Z").assertExists()
            compose.onNodeWithText("标题 A–Z").performClick()
            compose.onNode(hasSetTextAction()).performTextInput("测试")
            compose.runOnIdle { visible.value = false }
            compose.waitForIdle()
            compose.runOnIdle { visible.value = true }
            compose.waitUntil(5000) { compose.onAllNodesWithText("测试歌曲").fetchSemanticsNodes().isNotEmpty() }
            assertEquals("再次进入不应重新拉取", 1, firstPages.get())
            compose.onNode(hasSetTextAction()).assertTextContains("测试")
        }
    }
}
