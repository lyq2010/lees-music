package com.lyq2010.leesmusic.playback

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.session.MediaController
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.data.api.SubsonicAuth
import com.lyq2010.leesmusic.data.api.SubsonicServer
import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppPlayerRecoveryTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun await(check: () -> Boolean) = compose.waitUntil(10000) {
        var result = false
        compose.runOnUiThread { result = check() }
        result
    }

    @Test fun passwordChangeResignsExistingQueueWithoutDroppingItsSelection() {
        lateinit var app: AppPlayer
        compose.runOnUiThread { app = AppPlayer(context) }
        try {
            await { app.playerOrNull() != null }
            val songs = (0..2).map { LibrarySong("song-$it", "Song $it", "Artist", "cover-$it", null) }
            compose.runOnUiThread {
                app.playerOrNull()!!.clearMediaItems()
                app.play(SubsonicServer("http://127.0.0.1:18765", "lee", "old-pass"), songs, 1)
            }
            await { app.playerOrNull()?.mediaItemCount == 3 }
            compose.runOnUiThread {
                app.refreshCredentials(SubsonicServer("http://127.0.0.1:18765", "lee", "new-pass"))
            }
            await { app.playerOrNull()?.mediaItemCount == 3 && app.playerOrNull()?.currentMediaItemIndex == 1 }
            compose.runOnUiThread {
                val player = app.playerOrNull()!!
                assertFalse(player.playWhenReady)
                (0 until player.mediaItemCount).forEach { index ->
                    val item = player.getMediaItemAt(index)
                    val stream = Uri.parse(item.localConfiguration!!.uri.toString())
                    assertEquals(SubsonicAuth.token("new-pass", stream.getQueryParameter("s")!!), stream.getQueryParameter("t"))
                    val cover = Uri.parse(item.mediaMetadata.artworkUri.toString())
                    assertEquals(SubsonicAuth.token("new-pass", cover.getQueryParameter("s")!!), cover.getQueryParameter("t"))
                }
            }
        } finally {
            compose.runOnUiThread { app.playerOrNull()?.clearMediaItems(); app.release() }
        }
    }

    @Test fun releasedControllerIsReplacedWhileAppPlayerStaysAlive() {
        lateinit var app: AppPlayer
        compose.runOnUiThread { app = AppPlayer(context) }
        try {
            await { app.playerOrNull() != null }
            lateinit var first: MediaController
            compose.runOnUiThread {
                first = app.playerOrNull() as MediaController
                first.release()
            }
            await { app.playerOrNull()?.let { it !== first && (it as MediaController).isConnected } == true }
        } finally {
            compose.runOnUiThread { app.release() }
        }
    }
}
