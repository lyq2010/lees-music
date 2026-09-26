package com.lyq2010.leesmusic.update

import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class UpdateCacheTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun installedAndOlderPackagesAreRemovedWithoutTouchingOtherFiles() {
        val other = temporary.newFile("cover.jpg").apply { writeText("cover") }
        for (version in listOf(1L, 2L)) {
            val file = temporary.newFile("update-$version.apk")
            UpdateCache.removeInstalled(file, 2) { version }
            assertFalse(file.exists())
        }
        assertTrue(other.exists())
    }

    @Test fun newerPackageSurvivesRestartUntilInstalled() {
        val file = temporary.newFile("update.apk").apply { writeText("verified") }
        UpdateCache.removeInstalled(file, 2) { 3 }
        assertEquals("verified", file.readText())
        UpdateCache.removeInstalled(file, 3) { 3 }
        assertFalse(file.exists())
    }

    @Test fun invalidPackageLeftByInterruptedProcessIsRemoved() {
        val file = temporary.newFile("update.apk")
        UpdateCache.removeInstalled(file, 2) { null }
        assertFalse(file.exists())
    }

    @Test fun failedCancelledAndRejectedDownloadsRemovePartialFile() {
        listOf(IOException("network"), CancellationException("cancelled"), IllegalStateException("signature")).forEachIndexed { i, error ->
            val file = temporary.newFile("update-$i.apk")
            val result = runCatching { UpdateCache.download(file) { file.writeText("partial"); throw error } }
            assertSame(error, result.exceptionOrNull())
            assertFalse(file.exists())
        }
    }

    @Test fun successfulVerifiedDownloadIsKeptForInstallation() {
        val file = temporary.newFile("update.apk")
        UpdateCache.download(file) { file.writeText("verified") }
        assertEquals("verified", file.readText())
    }
}
