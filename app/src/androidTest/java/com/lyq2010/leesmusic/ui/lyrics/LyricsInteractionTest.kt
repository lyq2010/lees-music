package com.lyq2010.leesmusic.ui.lyrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lyq2010.leesmusic.data.api.LyricLine
import com.lyq2010.leesmusic.data.api.StructuredLyrics
import com.lyq2010.leesmusic.ui.theme.LeesTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LyricsInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val lyrics = StructuredLyrics(true, line = (0..30).map { LyricLine("测试歌词第 $it 行", it * 1000L) })

    @Test fun manualBrowseDoesNotSeekOrGetPulledBackAndCanResumeFollowing() {
        val position = mutableLongStateOf(3000)
        var sought = -1L
        compose.setContent { LeesTheme { Box(Modifier.fillMaxSize()) { LyricsLines(lyrics, position.longValue) { sought = it } } } }
        compose.waitForIdle()
        compose.onNodeWithTag("lyric-3").assertIsSelected()
        compose.onNodeWithTag("lyrics-lines").performTouchInput { swipeUp() }
        compose.onNodeWithTag("lyrics-follow").assertIsDisplayed()
        val before = compose.onNodeWithTag("lyrics-lines").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        compose.runOnIdle { position.longValue = 10000 }
        compose.waitForIdle()
        val after = compose.onNodeWithTag("lyrics-lines").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertEquals(before, after, 1f)
        assertEquals(-1L, sought)
        compose.onNodeWithTag("lyrics-follow").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("lyric-10").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("lyrics-follow").assertDoesNotExist()
    }

    @Test fun tappingTimedLineUsesOffsetAndPlainLyricsCannotSeek() {
        var sought = -1L
        compose.setContent { LeesTheme {
            LyricsLines(StructuredLyrics(true, 100, listOf(LyricLine("可跳转", 1000))), 1000) { sought = it }
        } }
        compose.onNodeWithTag("lyric-0").performClick()
        compose.runOnIdle { assertEquals(900L, sought) }
    }

    @Test fun plainLyricsAreReadableWithoutJumpActions() {
        compose.setContent { LeesTheme {
            LyricsLines(StructuredLyrics(line = listOf(LyricLine("没有时间戳的歌词"))), 0) { fail("plain lyrics must not seek") }
        } }
        compose.onNodeWithTag("lyric-0").assertIsDisplayed().assertHasNoClickAction()
        compose.onNodeWithText("纯文本歌词").assertIsDisplayed()
    }

    @Test fun failureOffersRetryAndEmptyStateDoesNotPretendToLoad() {
        var retried = false
        compose.setContent { LeesTheme { LyricsMessage(LyricsUiState.Failed) { retried = true } } }
        compose.onNodeWithText("重试").performClick()
        compose.runOnIdle { assertTrue(retried) }
    }
}
