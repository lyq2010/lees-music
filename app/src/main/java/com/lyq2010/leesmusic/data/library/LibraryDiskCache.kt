package com.lyq2010.leesmusic.data.library

import android.util.AtomicFile
import com.lyq2010.leesmusic.data.api.SubsonicClient
import com.lyq2010.leesmusic.data.api.SubsonicServer
import com.lyq2010.leesmusic.ui.library.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.security.MessageDigest

/** Account-scoped metadata; signed artwork URLs are regenerated on read. */
class LibraryDiskCache(private val root: File, private val server: SubsonicServer,
    private val maxBytes: Long = 64L * 1024 * 1024) {
    private val writeLock = Any()
    private val json = Json { ignoreUnknownKeys = true }
    private val client = SubsonicClient()
    private fun file(key: String) = AtomicFile(File(root, MessageDigest.getInstance("SHA-256")
        .digest(key.toByteArray()).joinToString("") { "%02x".format(it) } + ".json"))
    fun updatedAt(target: LibraryDestination): Long = file("${target.kind}:${target.id}").baseFile.lastModified()
    private fun read(key: String) = runCatching { file(key).openRead().bufferedReader().use { it.readText() } }.getOrNull()
    private fun write(key: String, text: String) = synchronized(writeLock) {
        root.mkdirs()
        val target = file(key)
        val bytes = text.toByteArray()
        val used = root.listFiles().orEmpty().filter { it.isFile }.sumOf { it.length() }
        if (used - target.baseFile.length() + bytes.size > maxBytes) return@synchronized
        val stream = target.startWrite()
        try { stream.write(bytes); target.finishWrite(stream) }
        catch (error: Exception) { target.failWrite(stream); throw error }
    }
    suspend fun page(target: LibraryDestination): LibraryPage? = withContext(Dispatchers.IO) {
        runCatching { read("${target.kind}:${target.id}")?.let { json.decodeFromString<LibraryPage>(it) }?.let { page ->
            page.copy(songs = page.songs.map { it.copy(coverUrl = it.coverArtId?.let { id -> client.coverArtUrl(server, id) }) },
                albums = page.albums.map { it.copy(coverUrl = it.coverArtId?.let { id -> client.coverArtUrl(server, id) }) })
        } }.getOrNull()
    }
    suspend fun save(target: LibraryDestination, page: LibraryPage) = withContext(Dispatchers.IO) {
        runCatching { write("${target.kind}:${target.id}", json.encodeToString(page.copy(
            songs = page.songs.map { it.copy(coverUrl = null, localUri = null) },
            albums = page.albums.map { it.copy(coverUrl = null) }))) }; Unit
    }
    suspend fun overview(): LibraryOverview? = withContext(Dispatchers.IO) {
        runCatching { read("overview")?.let { json.decodeFromString<LibraryOverview>(it) }?.let { data ->
            data.copy(favorites = data.favorites.map { it.copy(coverUrl = it.coverArtId?.let { id -> client.coverArtUrl(server, id) }) })
        } }.getOrNull()
    }
    suspend fun saveOverview(data: LibraryOverview) = withContext(Dispatchers.IO) {
        runCatching { write("overview", json.encodeToString(data.copy(favorites = data.favorites.map { it.copy(coverUrl = null, localUri = null) }))) }; Unit
    }
    suspend fun album(id: String): com.lyq2010.leesmusic.data.api.Album? = withContext(Dispatchers.IO) {
        runCatching { read("album:$id")?.let { json.decodeFromString<com.lyq2010.leesmusic.data.api.Album>(it) } }.getOrNull()
    }
    suspend fun saveAlbum(album: com.lyq2010.leesmusic.data.api.Album) = withContext(Dispatchers.IO) {
        runCatching { write("album:${album.id}", json.encodeToString(album)) }; Unit
    }
}
