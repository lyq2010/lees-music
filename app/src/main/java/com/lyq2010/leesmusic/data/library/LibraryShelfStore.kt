package com.lyq2010.leesmusic.data.library

import android.content.Context
import com.lyq2010.leesmusic.data.api.Album
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class LibraryShelf(
    val newest: List<Album> = emptyList(),
    val recent: List<Album> = emptyList(),
    val frequent: List<Album> = emptyList(),
    val random: List<Album> = emptyList(),
) {
    fun isEmpty(): Boolean = newest.isEmpty() && recent.isEmpty() && frequent.isEmpty() && random.isEmpty()
}

class LibraryShelfStore(context: Context, namespace: String) {
    private val file = File(context.filesDir, "libraries/$namespace/library-shelf.json")
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): LibraryShelf? {
        if (!file.exists()) return null
        return runCatching { json.decodeFromString(LibraryShelf.serializer(), file.readText()) }.getOrNull()
    }

    fun save(shelf: LibraryShelf) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(LibraryShelf.serializer(), shelf))
    }
}
