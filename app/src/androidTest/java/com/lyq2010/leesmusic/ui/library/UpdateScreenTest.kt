package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lyq2010.leesmusic.BuildConfig
import com.lyq2010.leesmusic.ui.settings.UpdateScreen
import com.lyq2010.leesmusic.ui.settings.UpdateHistory
import com.lyq2010.leesmusic.ui.shell.ShellTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class UpdateScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun backArrowAndTitleHaveTheSameVerticalCenter() {
        compose.setContent { ShellTheme {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { UpdateScreen {} }
        } }
        assertHeadingAligned()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, "update-heading.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun backArrowAndTitleStayAlignedWithLargeText() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) { ShellTheme { UpdateScreen {} } }
        }
        assertHeadingAligned()
    }

    private fun assertHeadingAligned() {
        val arrow = compose.onNodeWithContentDescription("返回", substring = true).fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("应用更新").fetchSemanticsNode().boundsInRoot
        assertEquals(title.center.y, arrow.center.y, 1f)
    }

    @Test fun initialPageShowsOnlyCurrentVersionNotes() {
        compose.setContent { ShellTheme { UpdateScreen {} } }
        compose.onNodeWithText("应用更新").assertIsDisplayed()
        compose.onNodeWithText("点击检查更新，获取最新版本").assertIsDisplayed()
        compose.onNodeWithText("检查更新", substring = false).assertIsDisplayed()
        compose.onNodeWithText("本次更新").performScrollTo().assertIsDisplayed()
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("${BuildConfig.VERSION_NAME} · 正式版").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("${BuildConfig.VERSION_NAME} · 正式版").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("0.1.0 · 公开测试版").assertDoesNotExist()
    }
    @Test fun availableReleaseReplacesInstalledNotes() {
        compose.setContent { ShellTheme { UpdateHistory("## 9.9.9 · 公开测试版\n\n- ✨ 改善播放体验。") } }
        compose.onNodeWithText("9.9.9 · 公开测试版").assertIsDisplayed()
        compose.onNodeWithText("${BuildConfig.VERSION_NAME} · 公开测试版").assertDoesNotExist()
        compose.onNodeWithText("• ✨ 改善播放体验。").assertIsDisplayed()
    }

}
