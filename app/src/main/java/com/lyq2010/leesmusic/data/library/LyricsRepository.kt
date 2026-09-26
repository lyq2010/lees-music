package com.lyq2010.leesmusic.data.library

import com.lyq2010.leesmusic.data.api.StructuredLyrics
import com.lyq2010.leesmusic.data.api.SubsonicClient
import com.lyq2010.leesmusic.data.api.SubsonicServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LyricsRepository(private val client: SubsonicClient) {
    private val cache = object : LinkedHashMap<String, StructuredLyrics>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, StructuredLyrics>?): Boolean = size > 30
    }

    suspend fun load(server: SubsonicServer, id: String, artist: String, title: String): StructuredLyrics? {
        val key = "${serverCacheIdentity(server.baseUrl, server.username)}:$id"
        synchronized(cache) { cache[key] }?.let { return it }
        val result = withContext(Dispatchers.IO) { client.lyrics(server, id, artist, title) }
        if (result != null) synchronized(cache) { cache[key] = result }
        return result
    }
}
