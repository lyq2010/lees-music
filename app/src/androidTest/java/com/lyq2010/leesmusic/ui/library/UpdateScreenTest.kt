package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lyq2010.leesmusic.BuildConfig
import com.lyq2010.leesmusic.ui.settings.UpdateScreen
import com.lyq2010.leesmusic.ui.settings.UpdateHistory
import com.lyq2010.leesmusic.ui.shell.ShellTheme
import org.junit.Rule
import org.junit.Test

class UpdateScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun initialPageShowsOnlyCurrentVersionNotes() {
        compose.setContent { ShellTheme { UpdateScreen {} } }
        compose.onNodeWithText("应用更新").assertIsDisplayed()
        compose.onNodeWithText("点击检查更新，获取最新版本").assertIsDisplayed()
        compose.onNodeWithText("检查更新", substring = false).assertIsDisplayed()
        compose.onNodeWithText("本次更新").performScrollTo().assertIsDisplayed()
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("${BuildConfig.VERSION_NAME} · 公开测试版").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("${BuildConfig.VERSION_NAME} · 公开测试版").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("0.1.0 · 公开测试版").assertDoesNotExist()
    }
    @Test fun availableReleaseReplacesInstalledNotes() {
        compose.setContent { ShellTheme { UpdateHistory("## 9.9.9 · 公开测试版\n\n- ✨ 改善播放体验。") } }
        compose.onNodeWithText("9.9.9 · 公开测试版").assertIsDisplayed()
        compose.onNodeWithText("${BuildConfig.VERSION_NAME} · 公开测试版").assertDoesNotExist()
        compose.onNodeWithText("• ✨ 改善播放体验。").assertIsDisplayed()
    }

}
