package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.data.settings.*
import com.lyq2010.leesmusic.ui.settings.SettingsScreen
import com.lyq2010.leesmusic.ui.shell.ShellTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SettingsInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun changesPersistAcrossStoreInstancesAndLinksWork() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = PlaybackPreferences(context)
        val oldMix = preferences.mixAudio
        val oldQuality = preferences.quality
        var server = false
        var downloads = false
        try {
            compose.setContent { ShellTheme { SettingsScreen(preferences, { server = true }, { downloads = true }, {}) } }
            compose.onNodeWithText("与其他应用同时播放").performClick()
            compose.runOnIdle { assertEquals(!oldMix, PlaybackPreferences(context).mixAudio) }
            compose.onNodeWithText("在线播放音质").performClick()
            compose.onNodeWithText(StreamQuality.Balanced.label).performClick()
            compose.runOnIdle { assertEquals(StreamQuality.Balanced, PlaybackPreferences(context).quality) }
            compose.onNodeWithText("播放速度").assertDoesNotExist()
            compose.onNodeWithText("跳过静音").assertDoesNotExist()
            compose.onNodeWithText("服务器", substring = false).performScrollTo().performClick()
            compose.runOnIdle { assertTrue(server) }
            compose.onNodeWithText("下载", substring = false).performScrollTo().performClick()
            compose.runOnIdle { assertTrue(downloads) }
            compose.onNodeWithText("关于", substring = false).performScrollTo().performClick()
            compose.onNodeWithText("知道了").assertIsDisplayed().performClick()
        } finally {
            preferences.mixAudio = oldMix
            preferences.quality = oldQuality
        }
    }
}
