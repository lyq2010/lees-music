package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.ui.catalog.*
import com.lyq2010.leesmusic.ui.theme.LeesTheme
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LibraryInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val song = LibrarySong("a", "测试歌曲", "测试艺人", null, null)

    @Test fun moreButtonDoesNotTriggerSongPlayback() {
        var plays = 0
        var menus = 0
        compose.setContent { LeesTheme { SongRow(song, OkHttpClient(), { plays++ }, { menus++ }) } }
        compose.onNodeWithContentDescription("测试歌曲的更多操作").performClick()
        compose.runOnIdle { assertEquals(0, plays); assertEquals(1, menus) }
        compose.onNodeWithText("测试歌曲").performClick()
        compose.runOnIdle { assertEquals(1, plays) }
    }

    @Test fun libraryTilesAndPlaylistTabsNavigateToRealTargets() {
        var target: LibraryDestination? = null
        var created = false
        val overview = LibraryOverview(listOf(song), listOf(
            MusicPlaylist("mine", "通勤", "lee", songCount = 1), MusicPlaylist("shared", "分享", "other", true)))
        compose.setContent { LeesTheme {
            LibraryScreen("Navidrome", "lee", emptyList(), overview, false, null, OkHttpClient(),
                {}, { target = it }, {}, {}, { created = true }, {})
        } }
        compose.onNodeWithText("歌曲", substring = false).performClick()
        compose.runOnIdle { assertEquals("songs", target?.kind) }
        compose.onNodeWithText("通勤").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("mine", target?.id) }
        compose.onNodeWithText("共享歌单").performScrollTo().performClick()
        compose.onNodeWithText("分享").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("shared", target?.id) }
        compose.onNodeWithText("通勤").assertDoesNotExist()
        compose.onNodeWithContentDescription("新建歌单").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(created) }
    }

    @Test fun emptyLibraryShowsGuidanceAndFailedOverviewOffersRetry() {
        var retries = 0
        compose.setContent { LeesTheme {
            LibraryScreen("Navidrome", "lee", emptyList(), null, false, "音乐库读取失败", OkHttpClient(),
                { retries++ }, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("音乐库读取失败").assertIsDisplayed()
        compose.onNodeWithText("重试").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        compose.onNodeWithText("还没有歌单").assertDoesNotExist()
    }
}
