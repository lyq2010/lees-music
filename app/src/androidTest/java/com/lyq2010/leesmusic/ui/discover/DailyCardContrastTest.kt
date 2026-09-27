package com.lyq2010.leesmusic.ui.discover

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.data.library.resourceCacheIdentity
import com.lyq2010.leesmusic.ui.catalog.CoverImages
import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

class DailyCardContrastTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lightThemeBlackCover() = checkCard(false, android.graphics.Color.BLACK)
    @Test fun lightThemeWhiteCover() = checkCard(false, android.graphics.Color.WHITE)
    @Test fun darkThemeBlackCover() = checkCard(true, android.graphics.Color.BLACK)
    @Test fun darkThemeWhiteCover() = checkCard(true, android.graphics.Color.WHITE)

    private fun checkCard(dark: Boolean, coverColor: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bytes = ByteArrayOutputStream().apply output@{
            Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
                eraseColor(coverColor)
                compress(Bitmap.CompressFormat.PNG, 100, this@output)
            }
        }.toByteArray()
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(bytes.toResponseBody()).build()
        }.build()
        val url = "https://daily-card-test.invalid/cover/$coverColor"
        val cover = runBlocking { CoverImages.load(context, resourceCacheIdentity(url), url, http) }
        assertNotNull(cover)
        assertEquals(coverColor, cover!!.getPixel(0, 0))
        var opened = 0
        var played = 0
        var refreshed = 0
        compose.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                DiscoverScreen(
                    listOf(LibrarySong("test", "Daylight", "Artist", "cover", url)), false,
                    { opened++ }, { played++ }, { refreshed++ },
                    emptyList(), emptyList(), emptyList(), emptyList(), http, "", {},
                )
            }
        }
        val nodes = listOf(
            compose.onNodeWithText("每日推荐"), compose.onNodeWithText("Daylight"),
            compose.onNodeWithText("50 首歌曲    查看全部  >"),
            compose.onNodeWithContentDescription("刷新每日推荐"),
            compose.onNodeWithContentDescription("顺序播放每日推荐"),
        )
        nodes.forEach { node ->
            val pixels = node.assertIsDisplayed().captureToImage().toPixelMap()
            val whitePixels = (0 until pixels.width).sumOf { x ->
                (0 until pixels.height).count { y ->
                    val color = pixels[x, y]
                    color.red > .95f && color.green > .95f && color.blue > .95f
                }
            }
            assertTrue("Each label and icon must retain visible white foreground", whitePixels > 10)
        }
        val screenshot = File(context.getExternalFilesDir(null), "daily-card-$dark-$coverColor.png")
        screenshot.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        nodes[2].performClick()
        nodes[3].performClick()
        nodes[4].performClick()
        compose.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, refreshed)
            assertEquals(1, played)
        }
    }
}
