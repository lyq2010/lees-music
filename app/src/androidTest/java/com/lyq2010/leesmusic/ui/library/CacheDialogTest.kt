package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lyq2010.leesmusic.ui.catalog.CoverImages
import com.lyq2010.leesmusic.ui.settings.CacheDialog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CacheDialogTest {
    @get:Rule val compose = createComposeRule()
    @Test fun cleaningRequiresConfirmationAndFailureCanBeRetried() {
        var clears = 0
        compose.setContent {
            CacheDialog({}, { CoverImages.Usage(2048, 1024) }, {
                clears++
                if (clears == 1) error("denied")
                CoverImages.Usage(0, 0)
            })
        }
        compose.waitForIdle()
        compose.onNodeWithText("清理缓存", substring = false).performClick()
        compose.onNodeWithText("取消", substring = false).performClick()
        compose.runOnIdle { assertEquals(0, clears) }
        compose.onNodeWithText("清理缓存", substring = false).performClick()
        compose.onNodeWithText("确认清理", substring = false).performClick()
        compose.onNodeWithText("部分缓存未能清理，请重试").assertIsDisplayed()
        compose.onNodeWithText("确认清理", substring = false).performClick()
        compose.onNodeWithText("封面缓存已清理").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, clears) }
    }
}
