package com.lyq2010.leesmusic.ui.login

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lyq2010.leesmusic.data.settings.ServerKind
import com.lyq2010.leesmusic.data.settings.ServerSettings
import com.lyq2010.leesmusic.ui.shell.ShellTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pastedHttpsUrlIsSavedWithItsSchemePortAndPath() {
        var saved: ServerSettings? = null
        compose.setContent { ShellTheme {
            LoginScreen(ServerKind.Navidrome, null, "", false, {}, { saved = it })
        } }
        compose.onNodeWithText("主机地址").performTextInput("https://music.example:8443/navidrome")
        compose.onNodeWithText("用户名").performTextInput("lee")
        compose.onNodeWithText("密码").performTextInput("secret")
        compose.onNodeWithContentDescription("保存").performClick()
        compose.runOnIdle { assertEquals("https://music.example:8443/navidrome", saved?.url) }
    }
}
