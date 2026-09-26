package com.lyq2010.leesmusic.update

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.security.MessageDigest

class UpdateClientTest {
    private val bytes = "signed package placeholder for transport test".toByteArray()
    private val manifest = UpdateManifest("0.1.1", 2, "com.lyq2010.leesmusic", "lees-music-0.1.1.apk",
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }, bytes.size.toLong(), 26)
    private fun client(response: (Request) -> Pair<Int, ByteArray>) = UpdateClient(listOf("https://primary.test", "https://backup.test"),
        OkHttpClient.Builder().addInterceptor { chain ->
            val (code, body) = response(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("test")
                .body(body.toResponseBody()).build()
        }.build())
    private fun metadata(value: UpdateManifest = manifest) = 200 to Json.encodeToString(value).toByteArray()

    @Test fun unavailablePrimaryFallsBackToBackup() {
        val update = client { if (it.url.host == "primary.test") 503 to byteArrayOf() else metadata() }.check(1, 26)!!
        assertEquals("backup.test", update.mirrors.single().host)
    }
    @Test fun stalePrimaryDoesNotHideNewBackupRelease() {
        val update = client { metadata(if (it.url.host == "primary.test") manifest.copy(versionCode = 1) else manifest) }.check(1, 26)!!
        assertEquals(2L, update.manifest.versionCode)
        assertEquals("backup.test", update.mirrors.single().host)
    }
    @Test fun currentOrUnsupportedVersionIsNotOffered() {
        assertNull(client { metadata() }.check(2, 26))
        assertNull(client { metadata(manifest.copy(minSdk = 28)) }.check(1, 26))
    }
    @Test fun malformedManifestCannotBecomeAnUpdate() {
        listOf(manifest.copy(apk = "../payload.apk"), manifest.copy(packageName = "another.app"),
            manifest.copy(sha256 = "bad"), manifest.copy(size = Long.MAX_VALUE)).forEach { invalid ->
            assertTrue(runCatching { client { metadata(invalid) }.check(1, 26) }.isFailure)
        }
    }
    @Test fun corruptPrimaryPackageFallsBackAndChecksExactBytes() {
        val transport = client { request ->
            if (request.url.encodedPath.endsWith("json")) metadata()
            else 200 to if (request.url.host == "primary.test") ByteArray(bytes.size) else bytes
        }
        val update = transport.check(1, 26)!!
        val file = Files.createTempFile("music-update-test", ".apk").toFile()
        try { transport.download(update, file) { _, _ -> }; assertArrayEquals(bytes, file.readBytes()) }
        finally { file.delete() }
    }
    @Test fun oversizedOrTruncatedDownloadFailsClosed() {
        for (body in listOf(bytes + byteArrayOf(1), bytes.copyOf(2))) {
            val transport = client { if (it.url.encodedPath.endsWith("json")) metadata() else 200 to body }
            val file = Files.createTempFile("music-update-test", ".apk").toFile()
            try { assertTrue(runCatching { transport.download(transport.check(1, 26)!!, file) { _, _ -> } }.isFailure) }
            finally { file.delete() }
        }
    }
}
