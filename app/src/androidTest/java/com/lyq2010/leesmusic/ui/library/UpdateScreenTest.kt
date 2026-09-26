package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lyq2010.leesmusic.ui.settings.UpdateScreen
import com.lyq2010.leesmusic.ui.shell.ShellTheme
import org.junit.Rule
import org.junit.Test

class UpdateScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun initialPageShowsVersionActionAndOfflineHistory() {
        compose.setContent { ShellTheme { UpdateScreen {} } }
        compose.onNodeWithText("应用更新").assertIsDisplayed()
        compose.onNodeWithText("点击检查更新，获取最新版本").assertIsDisplayed()
        compose.onNodeWithText("检查更新", substring = false).assertIsDisplayed()
        compose.onNodeWithText("更新记录").performScrollTo().assertIsDisplayed()
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("0.1.0 · 公开测试版").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("0.1.0 · 公开测试版").performScrollTo().assertIsDisplayed()
    }
}
