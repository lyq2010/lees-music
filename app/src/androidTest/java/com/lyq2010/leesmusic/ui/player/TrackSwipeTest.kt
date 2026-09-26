package com.lyq2010.leesmusic.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TrackSwipeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun swipesChangeOneTrackAndTapsStillOpenPlayer() {
        var previous = 0; var next = 0; var open = 0
        compose.setContent {
            Box(Modifier.fillMaxWidth().height(64.dp).testTag("bar")
                .trackSwipe(true, true, { previous++ }, { next++ }).clickable { open++ }) { Text("歌曲") }
        }
        compose.onNodeWithTag("bar").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, next); assertEquals(0, previous); assertEquals(0, open) }
        compose.onNodeWithTag("bar").performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.onNodeWithTag("bar").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, previous); assertEquals(1, open) }
    }

    @Test fun shortDragAndUnavailableDirectionDoNotSkip() {
        var count = 0
        compose.setContent { Box(Modifier.fillMaxWidth().height(64.dp).testTag("bar")
            .trackSwipe(false, true, { count++ }, { count++ })) }
        compose.onNodeWithTag("bar").performTouchInput { swipe(center, center - Offset(20f, 0f), 600) }
        compose.waitForIdle()
        compose.onNodeWithTag("bar").performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, count) }
    }
}
