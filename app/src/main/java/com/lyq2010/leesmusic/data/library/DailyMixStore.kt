package com.lyq2010.leesmusic.data.library

import android.content.Context
import com.lyq2010.leesmusic.data.api.Song
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

class DailyMixStore(context: Context) {
    private val file = File(context.filesDir, "daily-mix.json")
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<Song> {
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(Song.serializer()), file.readText())
        }.getOrDefault(emptyList())
    }

    fun save(songs: List<Song>) {
        file.writeText(json.encodeToString(ListSerializer(Song.serializer()), songs))
    }
}
