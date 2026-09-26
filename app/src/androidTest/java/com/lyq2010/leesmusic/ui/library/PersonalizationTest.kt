package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.data.settings.PlaybackPreferences
import com.lyq2010.leesmusic.ui.settings.PersonalizationScreen
import com.lyq2010.leesmusic.ui.shell.ShellTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PersonalizationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun appearanceAndAccentPersistAndSelectedStateUpdates() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = PlaybackPreferences(context)
        val oldMode = prefs.appearance; val oldAccent = prefs.accent
        try {
            compose.setContent { ShellTheme { PersonalizationScreen(prefs) {} } }
            compose.onNodeWithText("浅色", substring = false).performClick()
            compose.onNodeWithText("玫瑰", substring = false).performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals("light", PlaybackPreferences(context).appearance)
                assertEquals("rose", PlaybackPreferences(context).accent)
            }
            compose.onNodeWithText("跟随系统").performScrollTo().performClick()
            compose.runOnIdle { assertEquals("system", PlaybackPreferences(context).appearance) }
        } finally { prefs.appearance = oldMode; prefs.accent = oldAccent }
    }
}
