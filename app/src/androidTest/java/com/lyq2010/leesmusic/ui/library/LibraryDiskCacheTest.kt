package com.lyq2010.leesmusic.ui.library

import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.data.api.SubsonicServer
import com.lyq2010.leesmusic.data.library.LibraryDiskCache
import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class LibraryDiskCacheTest {
    @Test fun metadataSurvivesNewStoreWithoutSavingSignedUrls() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "test-library-${UUID.randomUUID()}")
        try {
            val server = SubsonicServer("https://example.invalid", "test", "test-password")
            val page = LibraryPage(songs = listOf(LibrarySong("one", "歌曲", "艺术家", "cover", "https://example.invalid/?t=old-secret")))
            val target = LibraryDestination("songs", "歌曲")
            LibraryDiskCache(root, server).save(target, page)
            assertTrue(LibraryDiskCache(root, server).updatedAt(target) > 0L)
            val restored = LibraryDiskCache(root, server).page(target)!!
            assertEquals("one", restored.songs.single().id)
            assertNotEquals(page.songs.single().coverUrl, restored.songs.single().coverUrl)
            val saved = root.listFiles()!!.filter { it.extension == "json" }.joinToString { it.readText() }
            assertFalse(saved.contains("old-secret"))
            assertFalse(saved.contains("test-password"))
            val other = LibraryDiskCache(File(root, "other-account"), server)
            assertNull(other.page(target))
        } finally { root.deleteRecursively() }
    }

    @Test fun capacityLimitPreservesOlderOfflineData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "test-library-policy-${UUID.randomUUID()}")
        val server = SubsonicServer("https://example.invalid", "test", "secret")
        val target = LibraryDestination("songs", "歌曲")
        try {
            val page = LibraryPage(songs = listOf(LibrarySong("one", "一", "歌手", null, null)))
            LibraryDiskCache(root, server, maxBytes = 1).save(target, page)
            assertNull(LibraryDiskCache(root, server).page(target))
            LibraryDiskCache(root, server).save(target, page)
            val saved = root.listFiles()!!.single { it.extension == "json" }
            assertNotNull(LibraryDiskCache(root, server).page(target))
            assertTrue(saved.setLastModified(System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000))
            assertNotNull(LibraryDiskCache(root, server).page(target))
            assertTrue(saved.exists())
        } finally { root.deleteRecursively() }
    }
}
