package com.lyq2010.leesmusic.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaybackDismissTest {
    @get:Rule val compose = createComposeRule()

    @Test fun downwardDragDismissesOnce() {
        var dismissed = 0
        compose.setContent { PlaybackDismiss({ dismissed++ }) { Box(Modifier.fillMaxSize()) } }
        compose.onNodeWithTag("playback-dismiss").performTouchInput { swipeDown() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, dismissed) }
    }

    @Test fun shortDragAndHorizontalSwipeDoNotDismiss() {
        var dismissed = 0
        compose.setContent { PlaybackDismiss({ dismissed++ }) { Box(Modifier.fillMaxSize()) } }
        compose.onNodeWithTag("playback-dismiss").performTouchInput {
            swipe(center, center + Offset(0f, 20f), 600)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("playback-dismiss").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, dismissed) }
    }
}
